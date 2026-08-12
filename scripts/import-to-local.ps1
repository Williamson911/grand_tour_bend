# Imports the CSVs exported from Supabase (SQL Editor / Table Editor "Export to CSV")
# into the local Postgres DB.
# - user.csv / events_rows.csv map onto the JPA entities (User, Event).
# - expenses_rows.csv / registrations_rows.csv / results_rows.csv have no JPA entity yet,
#   so they are created and filled with raw SQL, mirroring the Supabase structure exactly.
# Requires the Spring Boot app to have run at least once already (so Hibernate created
# the schema and AuthInitializer seeded the USER/ADMIN roles).
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
$eventsCsv = CsvPath "events_rows.csv"
$expensesCsv = CsvPath "expenses_rows.csv"
$registrationsCsv = CsvPath "registrations_rows.csv"
$resultsCsv = CsvPath "results_rows.csv"

foreach ($f in @($usersCsv, $eventsCsv, $expensesCsv, $registrationsCsv, $resultsCsv)) {
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

INSERT INTO user_ (id, username, password, bandai_tcg_id, created_at, updated_at)
SELECT id, username, password, bandai_tcg_id, created_at, updated_at
FROM staging_users
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

-- events ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staging_event (
    id text,
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
\copy staging_event (id, name, type, date, city, country, venue, lat, lng, register_link, created_at, updated_at) FROM '$eventsCsv' WITH (FORMAT csv, HEADER true)

-- Supabase stores type as "Regional"/"Final", the Java enum expects REGIONAL/FINAL.
INSERT INTO event (id, name, type, date, city, country, venue, lat, lng, register_link, created_at, updated_at)
SELECT id, name, UPPER(type), date, city, country, venue, lat, lng, register_link, created_at, updated_at
FROM staging_event
ON CONFLICT (id) DO NOTHING;

DROP TABLE staging_event;

-- expenses / registrations / results: no JPA entity yet, plain tables mirroring Supabase --
CREATE TABLE IF NOT EXISTS expenses (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES user_(id),
    event_id text REFERENCES event(id),
    category text,
    amount numeric,
    currency text,
    notes text,
    created_at timestamptz
);
CREATE TABLE IF NOT EXISTS staging_expenses (LIKE expenses);
TRUNCATE staging_expenses;
\copy staging_expenses (id, user_id, event_id, category, amount, currency, notes, created_at) FROM '$expensesCsv' WITH (FORMAT csv, HEADER true)
INSERT INTO expenses SELECT * FROM staging_expenses ON CONFLICT (id) DO NOTHING;
DROP TABLE staging_expenses;

CREATE TABLE IF NOT EXISTS registrations (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES user_(id),
    event_id text REFERENCES event(id),
    created_at timestamptz
);
CREATE TABLE IF NOT EXISTS staging_registrations (LIKE registrations);
TRUNCATE staging_registrations;
\copy staging_registrations (id, user_id, event_id, created_at) FROM '$registrationsCsv' WITH (FORMAT csv, HEADER true)
INSERT INTO registrations SELECT * FROM staging_registrations ON CONFLICT (id) DO NOTHING;
DROP TABLE staging_registrations;

CREATE TABLE IF NOT EXISTS results (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES user_(id),
    event_id text REFERENCES event(id),
    deck_name text,
    leader_played text,
    placement integer,
    total_players integer,
    prizes numeric,
    notes text,
    matches jsonb,
    created_at timestamptz,
    updated_at timestamptz
);
CREATE TABLE IF NOT EXISTS staging_results (LIKE results);
TRUNCATE staging_results;
\copy staging_results (id, user_id, event_id, deck_name, leader_played, placement, total_players, prizes, notes, matches, created_at, updated_at) FROM '$resultsCsv' WITH (FORMAT csv, HEADER true)
INSERT INTO results SELECT * FROM staging_results ON CONFLICT (id) DO NOTHING;
DROP TABLE staging_results;

COMMIT;
"@

$tempSql = Join-Path $dataDir "_import.generated.sql"
Set-Content -Path $tempSql -Value $sql -Encoding utf8

& $psql $LocalUrl -f $tempSql

Remove-Item $tempSql
