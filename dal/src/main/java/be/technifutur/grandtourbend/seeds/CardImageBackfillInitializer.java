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
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Backfills {@code imageData}/{@code imageContentType} for every {@link Card}
 * and {@link CardVariant} that has an {@code imgLink} but no stored image yet,
 * downloading from the DeckPlanet CDN this app has always hotlinked from.
 *
 * <p>Runs as a background daemon thread so it never blocks application
 * startup — with ~9800 images to fetch at a polite pace, this can take tens
 * of minutes; the app is fully usable while it runs, and cards without a
 * stored image yet simply have no image (the frontend already handles a
 * missing image gracefully).
 *
 * <p>Idempotent/resumable: only rows with {@code imageData == null} are
 * processed, so an interrupted run picks up where it left off on the next
 * application startup. Runs at {@code @Order(3)}, after {@link CardsInitializer}
 * ({@code @Order(1)}) and {@link GdrFixInitializer} ({@code @Order(2)}), so the
 * 19 God Rare variants {@code GdrFixInitializer} already populated from local
 * files are skipped here and never re-downloaded from the unreliable CDN.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class CardImageBackfillInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CardImageBackfillInitializer.class);
    private static final String CDN_BASE = "https://multi-deckplanet.us-southeast-1.linodeobjects.com/dbs_masters/";
    private static final String IMAGE_CONTENT_TYPE = "image/webp";
    private static final Duration REQUEST_DELAY = Duration.ofMillis(120);
    private static final int PROGRESS_LOG_INTERVAL = 100;

    private final CardRepository cardRepository;
    private final CardVariantRepository cardVariantRepository;

    private int downloaded = 0;
    private int failed = 0;
    private int lastLoggedAt = 0;

    @Override
    public void run(String... args) {
        Thread backfillThread = new Thread(this::backfillAll, "card-image-backfill");
        backfillThread.setDaemon(true);
        backfillThread.start();
    }

    // Package-private (not private) so it can be invoked directly and
    // synchronously from tests, bypassing the background thread.
    void backfillAll() {
        HttpClient client = HttpClient.newHttpClient();

        List<Card> cards = cardRepository.findAll();
        for (Card card : cards) {
            if (card.getImgLink() != null && card.getImageData() == null) {
                processOne(client, card.getImgLink(), card::setImageData, card::setImageContentType);
                if (card.getImageData() != null) {
                    cardRepository.save(card);
                }
                sleepPolitely();
                logProgressIfDue();
            }
        }

        List<CardVariant> variants = cardVariantRepository.findAll();
        for (CardVariant variant : variants) {
            if (variant.getImgLink() != null && variant.getImageData() == null) {
                processOne(client, variant.getImgLink(), variant::setImageData, variant::setImageContentType);
                if (variant.getImageData() != null) {
                    cardVariantRepository.save(variant);
                }
                sleepPolitely();
                logProgressIfDue();
            }
        }

        log.info("Card image backfill finished: {} downloaded, {} failed", downloaded, failed);
    }

    private void processOne(
            HttpClient client,
            String imgLink,
            Consumer<byte[]> setData,
            Consumer<String> setContentType
    ) {
        Optional<byte[]> bytes = fetchImageBytes(client, imgLink);
        if (bytes.isPresent()) {
            setData.accept(bytes.get());
            setContentType.accept(IMAGE_CONTENT_TYPE);
            downloaded++;
        } else {
            failed++;
        }
    }

    // Package-private seam so tests can stub the network interaction rather
    // than making real HTTP calls.
    Optional<byte[]> fetchImageBytes(HttpClient client, String imgLink) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(CDN_BASE + imgLink + ".webp"))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0) {
                log.warn("Card image backfill: {} returned status {}", imgLink, response.statusCode());
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (Exception e) {
            log.warn("Card image backfill: failed to fetch {}: {}", imgLink, e.getMessage());
            return Optional.empty();
        }
    }

    private void sleepPolitely() {
        try {
            Thread.sleep(REQUEST_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logProgressIfDue() {
        int total = downloaded + failed;
        if (total - lastLoggedAt >= PROGRESS_LOG_INTERVAL) {
            lastLoggedAt = total;
            log.info("Card image backfill progress: {} downloaded, {} failed so far", downloaded, failed);
        }
    }
}
