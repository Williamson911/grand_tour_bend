package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the backfill LOGIC only. These never let the class make a
 * real network call: {@link CardImageBackfillInitializer#fetchImageBytes} is
 * stubbed on a spy, and {@link CardImageBackfillInitializer#backfillAll()} is
 * invoked directly (synchronously, on the test thread) rather than via
 * {@code run()}, which would spin up a background thread.
 */
@ExtendWith(MockitoExtension.class)
class CardImageBackfillInitializerTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardVariantRepository cardVariantRepository;

    private CardImageBackfillInitializer spyInitializer;

    private Card cardWithImgLink(String imgLink) {
        Card card = new Card();
        card.setImgLink(imgLink);
        return card;
    }

    private CardVariant variantWithImgLink(String imgLink) {
        CardVariant variant = new CardVariant();
        variant.setImgLink(imgLink);
        return variant;
    }

    @BeforeEach
    void setUp() {
        spyInitializer = spy(new CardImageBackfillInitializer(cardRepository, cardVariantRepository));
    }

    @Test
    void backfillAll_downloadsMissingCardImage_andSaves() {
        Card card = cardWithImgLink("BT18-030");
        when(cardRepository.findAll()).thenReturn(List.of(card));
        when(cardVariantRepository.findAll()).thenReturn(List.of());
        byte[] bytes = {1, 2, 3};
        doReturn(Optional.of(bytes)).when(spyInitializer).fetchImageBytes(any(HttpClient.class), eq("BT18-030"));

        spyInitializer.backfillAll();

        assertThat(card.getImageData()).isEqualTo(bytes);
        assertThat(card.getImageContentType()).isEqualTo("image/webp");
        verify(cardRepository).save(card);
    }

    @Test
    void backfillAll_downloadsMissingVariantImage_andSaves() {
        CardVariant variant = variantWithImgLink("BT18-030_GDR");
        when(cardRepository.findAll()).thenReturn(List.of());
        when(cardVariantRepository.findAll()).thenReturn(List.of(variant));
        byte[] bytes = {4, 5, 6};
        doReturn(Optional.of(bytes)).when(spyInitializer).fetchImageBytes(any(HttpClient.class), eq("BT18-030_GDR"));

        spyInitializer.backfillAll();

        assertThat(variant.getImageData()).isEqualTo(bytes);
        assertThat(variant.getImageContentType()).isEqualTo("image/webp");
        verify(cardVariantRepository).save(variant);
    }

    @Test
    void backfillAll_skipsCardsThatAlreadyHaveImageData() {
        Card alreadyPopulated = cardWithImgLink("BT18-030");
        alreadyPopulated.setImageData(new byte[]{9});
        when(cardRepository.findAll()).thenReturn(List.of(alreadyPopulated));
        when(cardVariantRepository.findAll()).thenReturn(List.of());

        spyInitializer.backfillAll();

        verify(spyInitializer, never()).fetchImageBytes(any(HttpClient.class), anyString());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void backfillAll_skipsRowsWithNullImgLink() {
        Card noImgLink = cardWithImgLink(null);
        when(cardRepository.findAll()).thenReturn(List.of(noImgLink));
        when(cardVariantRepository.findAll()).thenReturn(List.of());

        spyInitializer.backfillAll();

        verify(spyInitializer, never()).fetchImageBytes(any(HttpClient.class), anyString());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void backfillAll_onFailedDownload_doesNotSaveButContinuesToNextItem() {
        Card failing = cardWithImgLink("BT99-999");
        Card succeeding = cardWithImgLink("BT18-030");
        when(cardRepository.findAll()).thenReturn(List.of(failing, succeeding));
        when(cardVariantRepository.findAll()).thenReturn(List.of());
        doReturn(Optional.empty()).when(spyInitializer).fetchImageBytes(any(HttpClient.class), eq("BT99-999"));
        byte[] bytes = {7, 8};
        doReturn(Optional.of(bytes)).when(spyInitializer).fetchImageBytes(any(HttpClient.class), eq("BT18-030"));

        spyInitializer.backfillAll();

        assertThat(failing.getImageData()).isNull();
        verify(cardRepository, never()).save(failing);
        assertThat(succeeding.getImageData()).isEqualTo(bytes);
        verify(cardRepository).save(succeeding);
    }

    @Test
    void run_startsBackgroundThreadWithoutBlockingCallingThread() throws InterruptedException {
        when(cardRepository.findAll()).thenReturn(List.of());
        when(cardVariantRepository.findAll()).thenReturn(List.of());

        long start = System.nanoTime();
        spyInitializer.run();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        // run() must return almost immediately; the actual work happens on a
        // separate daemon thread.
        assertThat(elapsedMillis).isLessThan(1000);

        // Poll (rather than a fixed sleep) for the background thread to reach
        // the mocked repository call, to avoid timing flakiness.
        verify(cardRepository, org.mockito.Mockito.timeout(5000).atLeastOnce()).findAll();
    }
}
