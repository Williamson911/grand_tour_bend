package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardImageStopgapInitializerTest {

    @Mock
    private CardVariantRepository cardVariantRepository;

    private CardVariant makeVariant(String imgLink) {
        CardVariant variant = new CardVariant();
        variant.setImgLink(imgLink);
        return variant;
    }

    @BeforeEach
    void setUp() {
        lenient().when(cardVariantRepository.save(org.mockito.ArgumentMatchers.any(CardVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void populatesImageDataFromLocalFileWhenMissing() {
        CardVariant variant = makeVariant("BT30-017_SLR");
        when(cardVariantRepository.findByImgLink("BT30-017_SLR")).thenReturn(Optional.of(variant));

        CardImageStopgapInitializer spyInitializer = spy(new CardImageStopgapInitializer(cardVariantRepository));
        byte[] fakeImageBytes = {1, 2, 3, 4};
        doReturn(fakeImageBytes).when(spyInitializer).readLocalImageBytes(eq("BT30-017_SLR"));

        spyInitializer.run();

        assertThat(variant.getImageData()).isEqualTo(fakeImageBytes);
        assertThat(variant.getImageContentType()).isEqualTo("image/webp");
        verify(cardVariantRepository).save(variant);
    }

    @Test
    void doesNotReReadLocalFileWhenImageDataAlreadyPresent() {
        byte[] existingBytes = {9, 9, 9};
        CardVariant variant = makeVariant("BT30-017_SLR");
        variant.setImageData(existingBytes);
        variant.setImageContentType("image/webp");
        when(cardVariantRepository.findByImgLink("BT30-017_SLR")).thenReturn(Optional.of(variant));

        CardImageStopgapInitializer spyInitializer = spy(new CardImageStopgapInitializer(cardVariantRepository));

        spyInitializer.run();

        verify(spyInitializer, never()).readLocalImageBytes(eq("BT30-017_SLR"));
        verify(cardVariantRepository, never()).save(variant);
        assertThat(variant.getImageData()).isEqualTo(existingBytes);
    }

    @Test
    void variantNotFound_throwsIllegalStateException() {
        when(cardVariantRepository.findByImgLink("BT30-017_SLR")).thenReturn(Optional.empty());

        CardImageStopgapInitializer initializer = new CardImageStopgapInitializer(cardVariantRepository);

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BT30-017_SLR");
    }
}
