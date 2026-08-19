# Imports the CSVs exported from Supabase (SQL Editor / Table Editor "Export to CSV")
# into the local Postgres DB.
# - user.csv + user_email.csv (joined by id) map onto the User JPA entity.
# - events_rows.csv maps onto the Event JPA entity. event.id is now a
#   Hibernate-generated uuid (the old Supabase rows used a human-readable slug
#   id, e.g. "w1-pec-occitania-rungis-2026-06"), so each row gets a fresh
#   random uuid; the old slug -> new uuid mapping is kept in a permanent
#   event_id_map table (not dropped like the other staging tables) so
#   expenses/registrations (and later results) can resolve their event_id
#   references against it.
# - expenses_rows.csv / registrations_rows.csv map onto their JPA entities via
#   event_id_map. One expenses row has a bogus 161-digit test amount (clearly
#   fuzzing input, notes="test") that overflows numeric(38,2); it's filtered
#   out rather than left to fail the whole import.
#
# NOTE: this script does not import results_rows.csv yet: results.leader_played
# was replaced by leader_card_id, a FK into the new cards catalog with no
# automatic way to resolve the old free-text leader names (needs a human to
# match them, not a blind replay of the old SQL).
#
# Requires the Spring Boot app to have run at least once already (so Hibernate created
# the schema and AuthInitializer seeded the USER/ADMIN roles, and the Initializer /
# CardsInitializer seeded event_type / cards).
#
# Usage:
#   $env:DB_URL = "postgresql://postgres:<local-password>@localhost:5432/grand_tour_bend"  (optional, defaults below)
#   ./import-to-local.ps1

param(
    [string]$LocalUrl = $(if ($env:DB_URL) { $env:DB_URL } else { "postgresql://postgres:postgres@localhost:5432/grand_tour_bend" })
)

$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$dataDir = Join-Path $PSScriptRoot "data"

function CsvPath($name) { (Join-Path $dataDir $name) -replace '\\','/' }

$usersCsv = CsvPath "user.csv"
$userEmailsCsv = CsvPath "user_email.csv"
$eventsCsv = CsvPath "events_rows.csv"
$expensesCsv = CsvPath "expenses_rows.csv"
$registrationsCsv = CsvPath "registrations_rows.csv"

foreach ($f in @($usersCsv, $userEmailsCsv, $eventsCsv, $expensesCsv, $registrationsCsv)) {
    if (-not (Test-Path $f)) {
        Write-Error "Missing $f"
        exit 1
    }
}

$sql = @"
BEGIN;

-- users -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staging_users (
    id uuid,
    username text,
    password text,
    bandai_tcg_id text,
    is_admin boolean,
    created_at timestamptz,
    updated_at timestamptz
);
TRUNCATE staging_users;
-- the SQL Editor's CSV export writes NULLs as the literal text "null" (unlike Table Editor exports)
\copy staging_users (id, username, password, bandai_tcg_id, is_admin, created_at, updated_at) FROM '$usersCsv' WITH (FORMAT csv, HEADER true, NULL 'null')

CREATE TABLE IF NOT EXISTS staging_user_emails (
    id uuid,
    email text
);
TRUNCATE staging_user_emails;
\copy staging_user_emails (id, email) FROM '$userEmailsCsv' WITH (FORMAT csv, HEADER true, NULL 'null')

INSERT INTO user_ (id, username, password, bandai_tcg_id, email, confirmed, created_at, updated_at)
SELECT s.id, s.username, s.password, s.bandai_tcg_id, e.email, true, s.created_at, s.updated_at
FROM staging_users s
JOIN staging_user_emails e ON e.id = s.id
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_role (user_id, role_id)
SELECT s.id, r.id
FROM staging_users s
JOIN role_ r ON r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_id)
SELECT s.id, r.id
FROM staging_users s
JOIN role_ r ON r.name = 'ADMIN'
WHERE s.is_admin = true
ON CONFLICT DO NOTHING;

DROP TABLE staging_users;
DROP TABLE staging_user_emails;

-- events ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staging_event (
    old_id text,
    name text,
    type text,
    date date,
    city text,
    country text,
    venue text,
    lat float8,
    lng float8,
    register_link text,
    created_at timestamptz,
    updated_at timestamptz
);
TRUNCATE staging_event;
\copy staging_event (old_id, name, type, date, city, country, venue, lat, lng, register_link, created_at, updated_at) FROM '$eventsCsv' WITH (FORMAT csv, HEADER true, NULL 'null')

-- permanent (not dropped) mapping from the old Supabase slug id to the new
-- Hibernate-generated uuid, so a later expenses/registrations/results import
-- can resolve their event_id references against it.
CREATE TABLE IF NOT EXISTS event_id_map (
    old_id text PRIMARY KEY,
    new_id uuid NOT NULL
);
INSERT INTO event_id_map (old_id, new_id)
SELECT s.old_id, gen_random_uuid()
FROM staging_event s
ON CONFLICT (old_id) DO NOTHING;

-- Supabase stores type as "Regional"/"Final"; event_type.name is seeded as "Regional"/"Finals".
INSERT INTO event (id, name, event_type_id, date, city, country, venue, lat, lng, register_link, created_at, updated_at)
SELECT m.new_id, s.name, t.id, s.date, s.city, s.country, s.venue, s.lat, s.lng, s.register_link, s.created_at, s.updated_at
FROM staging_event s
JOIN event_id_map m ON m.old_id = s.old_id
JOIN event_type t ON t.name = s.type
ON CONFLICT (name) DO NOTHING;

DROP TABLE staging_event;

-- expenses ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staging_expenses (
    id uuid,
    user_id uuid,
    event_id text,
    category text,
    amount text,
    currency text,
    notes text,
    created_at timestamptz
);
TRUNCATE staging_expenses;
\copy staging_expenses (id, user_id, event_id, category, amount, currency, notes, created_at) FROM '$expensesCsv' WITH (FORMAT csv, HEADER true, NULL 'null')

-- one row has a bogus 161-digit test amount that overflows numeric(38,2); skip it rather than fail the whole import.
INSERT INTO expenses (id, user_id, event_id, category, amount, currency, notes, created_at, updated_at)
SELECT s.id, s.user_id, m.new_id, s.category, s.amount::numeric, s.currency, s.notes, s.created_at, s.created_at
FROM staging_expenses s
JOIN event_id_map m ON m.old_id = s.event_id
WHERE s.amount::numeric < 1e36
ON CONFLICT (id) DO NOTHING;

DROP TABLE staging_expenses;

-- registrations ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staging_registrations (
    id uuid,
    user_id uuid,
    event_id text,
    created_at timestamptz
);
TRUNCATE staging_registrations;
\copy staging_registrations (id, user_id, event_id, created_at) FROM '$registrationsCsv' WITH (FORMAT csv, HEADER true, NULL 'null')

INSERT INTO registrations (id, user_id, event_id, created_at, updated_at)
SELECT s.id, s.user_id, m.new_id, s.created_at, s.created_at
FROM staging_registrations s
JOIN event_id_map m ON m.old_id = s.event_id
ON CONFLICT (id) DO NOTHING;

DROP TABLE staging_registrations;

COMMIT;
"@

$tempSql = Join-Path $dataDir "_import.generated.sql"
Set-Content -Path $tempSql -Value $sql -Encoding utf8

& $psql $LocalUrl -f $tempSql

Remove-Item $tempSql
