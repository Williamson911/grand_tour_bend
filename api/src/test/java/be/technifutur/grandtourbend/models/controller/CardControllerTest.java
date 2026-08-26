package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private CardController cardController;

    @Test
    void getImage_whenFound_returnsBytesWithStoredContentType() {
        byte[] bytes = {1, 2, 3};
        when(cardService.getImage(eq("BT18-030")))
                .thenReturn(Optional.of(new CardService.CardImage(bytes, "image/webp")));

        ResponseEntity<byte[]> response = cardController.getImage("BT18-030");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(bytes);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("image/webp"));
    }

    @Test
    void getImage_whenContentTypeMissing_defaultsToWebp() {
        byte[] bytes = {4, 5, 6};
        when(cardService.getImage(eq("BT18-030")))
                .thenReturn(Optional.of(new CardService.CardImage(bytes, null)));

        ResponseEntity<byte[]> response = cardController.getImage("BT18-030");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("image/webp"));
    }

    @Test
    void getImage_whenNotFound_returns404() {
        when(cardService.getImage(eq("missing"))).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = cardController.getImage("missing");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }
}
