package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.CardVariant;
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
import java.util.List;

/**
 * Populates {@code imageData} for card variants that {@link
 * be.technifutur.grandtourbend.seeds.CardImageBackfillInitializer} could never
 * fetch because the DeckPlanet CDN doesn't have them (confirmed via repeated
 * 403 responses), by reading a locally-hosted copy instead. Same idea as
 * {@link GdrFixInitializer}'s image population, generalized for one-off
 * exceptions outside the GDR rarity. Idempotent: only runs when {@code
 * imageData} is still null, so it never re-reads once populated.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class CardImageStopgapInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CardImageStopgapInitializer.class);
    private static final String IMAGE_CONTENT_TYPE = "image/webp";

    private final CardVariantRepository cardVariantRepository;

    // imgLink values whose image DeckPlanet's CDN doesn't serve (confirmed
    // via manual HTTP checks), backed instead by a local file of the same
    // name under static/images/cards/. Sourced from TopDeckDiffusion.com.
    private static final List<String> MISSING_FROM_CDN = List.of(
            "BT30-017_SLR"
    );

    @Override
    public void run(String... args) {
        for (String imgLink : MISSING_FROM_CDN) {
            fixOne(imgLink);
        }
    }

    private void fixOne(String imgLink) {
        CardVariant variant = cardVariantRepository.findByImgLink(imgLink)
                .orElseThrow(() -> new IllegalStateException("Image stopgap: variant not found for imgLink: " + imgLink));

        if (variant.getImageData() != null) {
            return;
        }
        byte[] bytes = readLocalImageBytes(imgLink);
        if (bytes != null) {
            variant.setImageData(bytes);
            variant.setImageContentType(IMAGE_CONTENT_TYPE);
            cardVariantRepository.save(variant);
        }
    }

    // Package-private seam so tests can stub/verify this without mocking
    // ClassPathResource directly.
    byte[] readLocalImageBytes(String imgLink) {
        String path = "static/images/cards/" + imgLink + ".webp";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            log.warn("Image stopgap: could not read local image file {} for {}: {}", path, imgLink, e.getMessage());
            return null;
        }
    }
}
