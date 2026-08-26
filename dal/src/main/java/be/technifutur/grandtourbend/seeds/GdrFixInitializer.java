package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fixes "God Rare[GDR]" rarity data quality issues that predate this fix and
 * are already baked into some already-seeded databases: some GDR variants
 * were missing entirely, some were mislabeled with a "_PR" card-number
 * suffix instead of "_GDR", and two had duplicate entries (one correctly
 * "_GDR"-suffixed, one incorrectly "_PR"-suffixed, both the same physical
 * card). Unlike {@link CardsInitializer}, this is NOT gated by an
 * empty-table check — it runs on every startup and is idempotent, so it
 * self-heals the data regardless of when a given database was first seeded.
 *
 * <p>Runs after {@link CardsInitializer} (via {@code @Order}) because on a
 * freshly empty database this fixer depends on the base {@code Card} rows
 * that {@link CardsInitializer} creates.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class GdrFixInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GdrFixInitializer.class);
    private static final String GDR_RARITY = "God Rare[GDR]";
    private static final String IMAGE_CONTENT_TYPE = "image/webp";

    private final CardRepository cardRepository;
    private final CardVariantRepository cardVariantRepository;

    private record GdrEntry(String parentCardNumber, Integer syntheticSourceIdIfMissing) {}

    // syntheticSourceIdIfMissing is non-null only for variants that don't exist
    // under ANY name yet (neither "_GDR" nor a mislabeled "_PR"); it must not
    // collide with any real scraped sourceId (all real ones are well under
    // 900000 as of this writing).
    private static final List<GdrEntry> GDR_ENTRIES = List.of(
            new GdrEntry("BT16-147", null),
            new GdrEntry("BT18-147", 900001),
            new GdrEntry("BT18-148", null),
            new GdrEntry("BT21-147", 900002),
            new GdrEntry("BT21-148", null),
            new GdrEntry("BT22-140", null),
            new GdrEntry("BT23-140", null),
            new GdrEntry("BT24-138", null),
            new GdrEntry("BT25-147", null),
            new GdrEntry("BT25-148", null),
            new GdrEntry("BT26-138", null),
            new GdrEntry("BT27-138", null),
            new GdrEntry("BT28-148", null),
            new GdrEntry("BT29-149", 900003),
            new GdrEntry("BT30-149", 900004),
            new GdrEntry("BT30-150", null),
            new GdrEntry("BT30-151", 900005),
            new GdrEntry("BT31-151", null),
            new GdrEntry("BT7-131", 900006)
    );

    @Override
    public void run(String... args) {
        for (GdrEntry entry : GDR_ENTRIES) {
            fixOne(entry);
        }
    }

    private void fixOne(GdrEntry entry) {
        String parentNumber = entry.parentCardNumber();
        String correctNumber = parentNumber + "_GDR";

        Card parent = cardRepository.findByCardNumber(parentNumber)
                .orElseThrow(() -> new IllegalStateException("GDR fix: base card not found: " + parentNumber));

        Optional<CardVariant> correct = cardVariantRepository.findByCardNumber(correctNumber);
        if (correct.isPresent()) {
            CardVariant variant = correct.get();
            variant.setRarity(GDR_RARITY);
            variant.setImgLink(correctNumber);
            populateImageIfMissing(variant, correctNumber);
            cardVariantRepository.save(variant);
            removeStrayMislabeledDuplicate(parentNumber, correctNumber);
            return;
        }

        String mislabeledNumber = parentNumber + "_PR";
        Optional<CardVariant> mislabeled = cardVariantRepository.findByCardNumber(mislabeledNumber)
                .filter(v -> GDR_RARITY.equals(v.getRarity()));
        if (mislabeled.isPresent()) {
            CardVariant variant = mislabeled.get();
            variant.setCardNumber(correctNumber);
            variant.setRarity(GDR_RARITY);
            variant.setImgLink(correctNumber);
            populateImageIfMissing(variant, correctNumber);
            cardVariantRepository.save(variant);
            return;
        }

        if (entry.syntheticSourceIdIfMissing() == null) {
            throw new IllegalStateException(
                    "GDR fix: no existing variant found for " + correctNumber
                            + " and no synthetic sourceId was configured for it");
        }

        CardVariant variant = new CardVariant();
        variant.setCard(parent);
        variant.setSourceId(entry.syntheticSourceIdIfMissing());
        variant.setCardNumber(correctNumber);
        variant.setSeries(parent.getSeries());
        variant.setRarity(GDR_RARITY);
        variant.setImgLink(correctNumber);
        variant.setFinishes(new ArrayList<>());
        variant.setBanned(false);
        variant.setLimited(false);
        variant.setHasErrata(false);
        populateImageIfMissing(variant, correctNumber);
        cardVariantRepository.save(variant);
    }

    // Populates imageData/imageContentType directly on this variant from the
    // already-downloaded, already-correct local static file, consolidating
    // the earlier stopgap fix into the new DB-storage scheme. Only runs once
    // per variant (idempotent) so it never re-reads the file, and never
    // re-downloads these 19 from the unreliable CDN via the generic backfill.
    private void populateImageIfMissing(CardVariant variant, String correctNumber) {
        if (variant.getImageData() != null) {
            return;
        }
        byte[] bytes = readLocalImageBytes(correctNumber);
        if (bytes != null) {
            variant.setImageData(bytes);
            variant.setImageContentType(IMAGE_CONTENT_TYPE);
        }
    }

    // Package-private seam so tests can stub/verify this without mocking
    // ClassPathResource directly.
    byte[] readLocalImageBytes(String correctNumber) {
        String path = "static/images/cards/" + correctNumber + ".webp";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            log.warn("GDR fix: could not read local image file {} for {}: {}", path, correctNumber, e.getMessage());
            return null;
        }
    }

    // Some card numbers (e.g. BT23-140) had a duplicate: a correct "_GDR"
    // variant AND a stray "_PR"-suffixed one for the exact same rarity. Once
    // the "_GDR" side is confirmed present/correct (the branch that calls this),
    // delete the stray duplicate if it exists.
    private void removeStrayMislabeledDuplicate(String parentNumber, String correctNumber) {
        String mislabeledNumber = parentNumber + "_PR";
        cardVariantRepository.findByCardNumber(mislabeledNumber)
                .filter(v -> GDR_RARITY.equals(v.getRarity()) && !v.getCardNumber().equals(correctNumber))
                .ifPresent(cardVariantRepository::delete);
    }
}
