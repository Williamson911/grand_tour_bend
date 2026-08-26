package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GdrFixInitializerTest {

    private static final String GDR_RARITY = "God Rare[GDR]";

    // All 19 real parent card numbers the fixer will process on every run().
    private static final String[] ALL_PARENT_NUMBERS = {
            "BT16-147", "BT18-147", "BT18-148", "BT21-147", "BT21-148",
            "BT22-140", "BT23-140", "BT24-138", "BT25-147", "BT25-148",
            "BT26-138", "BT27-138", "BT28-148", "BT29-149", "BT30-149",
            "BT30-150", "BT30-151", "BT31-151", "BT7-131"
    };

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardVariantRepository cardVariantRepository;

    private GdrFixInitializer initializer;

    private Card makeCard(String cardNumber) {
        Card card = new Card();
        card.setCardNumber(cardNumber);
        card.setSeries("BT18");
        return card;
    }

    private CardVariant makeVariant(String cardNumber, String rarity) {
        CardVariant variant = new CardVariant();
        variant.setCardNumber(cardNumber);
        variant.setRarity(rarity);
        return variant;
    }

    @BeforeEach
    void setUp() {
        initializer = new GdrFixInitializer(cardRepository, cardVariantRepository);

        // Default: every real parent card number resolves to a valid Card.
        for (String number : ALL_PARENT_NUMBERS) {
            lenient().when(cardRepository.findByCardNumber(eq(number)))
                    .thenReturn(Optional.of(makeCard(number)));
        }

        // Default: nothing is found for any card-variant lookup...
        lenient().when(cardVariantRepository.findByCardNumber(anyString()))
                .thenReturn(Optional.empty());
        // ...except that every "_GDR" variant is already present with stale data.
        // This puts every one of the 19 entries on the "already correct" happy
        // path by default, so run() never throws for an unconfigured entry;
        // individual tests override specific numbers to exercise other branches.
        for (String number : ALL_PARENT_NUMBERS) {
            lenient().when(cardVariantRepository.findByCardNumber(number + "_GDR"))
                    .thenReturn(Optional.of(makeVariant(number + "_GDR", "Stale Rarity")));
        }

        lenient().when(cardVariantRepository.save(any(CardVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CardVariant savedVariantFor(String cardNumber) {
        ArgumentCaptor<CardVariant> captor = ArgumentCaptor.forClass(CardVariant.class);
        verify(cardVariantRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(v -> cardNumber.equals(v.getCardNumber()))
                .reduce((first, second) -> second) // last save wins
                .orElseThrow(() -> new AssertionError("no save() call for " + cardNumber));
    }

    @Test
    void alreadyCorrect_updatesRarityAndImgLinkAndDoesNotDelete() {
        // BT18-148 uses the default "_GDR" variant already present (setUp).
        initializer.run();

        CardVariant saved = savedVariantFor("BT18-148_GDR");
        assertThat(saved.getRarity()).isEqualTo(GDR_RARITY);
        assertThat(saved.getImgLink()).isEqualTo("BT18-148_GDR");
        verify(cardVariantRepository, never()).delete(any(CardVariant.class));
    }

    @Test
    void duplicateCleanup_updatesCorrectVariantAndDeletesStrayMislabeledOne() {
        CardVariant stray = makeVariant("BT23-140_PR", GDR_RARITY);
        when(cardVariantRepository.findByCardNumber("BT23-140_PR")).thenReturn(Optional.of(stray));

        initializer.run();

        CardVariant saved = savedVariantFor("BT23-140_GDR");
        assertThat(saved.getRarity()).isEqualTo(GDR_RARITY);
        assertThat(saved.getImgLink()).isEqualTo("BT23-140_GDR");
        verify(cardVariantRepository).delete(stray);
    }

    @Test
    void mislabeledRename_updatesExistingPrVariantInPlace() {
        // Override BT16-147 so no "_GDR" variant exists, but a mislabeled
        // "_PR" one with GDR rarity does.
        CardVariant mislabeled = makeVariant("BT16-147_PR", GDR_RARITY);
        when(cardVariantRepository.findByCardNumber("BT16-147_GDR")).thenReturn(Optional.empty());
        when(cardVariantRepository.findByCardNumber("BT16-147_PR")).thenReturn(Optional.of(mislabeled));

        initializer.run();

        verify(cardVariantRepository).save(mislabeled);
        assertThat(mislabeled.getCardNumber()).isEqualTo("BT16-147_GDR");
        assertThat(mislabeled.getRarity()).isEqualTo(GDR_RARITY);
        assertThat(mislabeled.getImgLink()).isEqualTo("BT16-147_GDR");
        verify(cardVariantRepository, never()).delete(any(CardVariant.class));
    }

    @Test
    void genuinelyMissing_createsNewVariantWithSyntheticSourceId() {
        // Override BT18-147 (has a configured synthetic sourceId 900001) so
        // neither "_GDR" nor a GDR-rarity "_PR" variant exists.
        Card parent = makeCard("BT18-147");
        parent.setSeries("BT18");
        when(cardRepository.findByCardNumber("BT18-147")).thenReturn(Optional.of(parent));
        when(cardVariantRepository.findByCardNumber("BT18-147_GDR")).thenReturn(Optional.empty());

        initializer.run();

        CardVariant created = savedVariantFor("BT18-147_GDR");
        assertThat(created.getSourceId()).isEqualTo(900001);
        assertThat(created.getSeries()).isEqualTo("BT18");
        assertThat(created.getRarity()).isEqualTo(GDR_RARITY);
        assertThat(created.getImgLink()).isEqualTo("BT18-147_GDR");
        assertThat(created.getCard()).isEqualTo(parent);
        assertThat(created.getFinishes()).isNotNull();
        verify(cardVariantRepository, never()).delete(any(CardVariant.class));
    }

    @Test
    void genuinelyMissing_withNoConfiguredSyntheticId_throwsIllegalStateException() {
        // BT18-148 has no synthetic sourceId configured; if neither "_GDR" nor
        // a mislabeled "_PR" variant exists, the fixer cannot safely proceed.
        when(cardVariantRepository.findByCardNumber("BT18-148_GDR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initializer.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BT18-148_GDR");
    }

    @Test
    void parentCardNotFound_throwsIllegalStateException() {
        when(cardRepository.findByCardNumber("BT7-131")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initializer.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BT7-131");
    }

    @Test
    void populatesImageDataFromLocalFileWhenMissing() {
        // BT18-148 uses the default "already correct" variant (imageData null).
        GdrFixInitializer spyInitializer = spy(new GdrFixInitializer(cardRepository, cardVariantRepository));
        byte[] fakeImageBytes = {1, 2, 3, 4};
        doReturn(fakeImageBytes).when(spyInitializer).readLocalImageBytes(anyString());

        spyInitializer.run();

        verify(spyInitializer).readLocalImageBytes(eq("BT18-148_GDR"));
        CardVariant saved = savedVariantFor("BT18-148_GDR");
        assertThat(saved.getImageData()).isEqualTo(fakeImageBytes);
        assertThat(saved.getImageContentType()).isEqualTo("image/webp");
    }

    @Test
    void doesNotReReadLocalFileWhenImageDataAlreadyPresent() {
        byte[] existingBytes = {9, 9, 9};
        CardVariant alreadyPopulated = makeVariant("BT18-148_GDR", "Stale Rarity");
        alreadyPopulated.setImageData(existingBytes);
        alreadyPopulated.setImageContentType("image/webp");
        when(cardVariantRepository.findByCardNumber("BT18-148_GDR")).thenReturn(Optional.of(alreadyPopulated));

        GdrFixInitializer spyInitializer = spy(new GdrFixInitializer(cardRepository, cardVariantRepository));
        doReturn(new byte[]{0}).when(spyInitializer).readLocalImageBytes(anyString());

        spyInitializer.run();

        verify(spyInitializer, never()).readLocalImageBytes(eq("BT18-148_GDR"));
        assertThat(alreadyPopulated.getImageData()).isEqualTo(existingBytes);
    }
}
