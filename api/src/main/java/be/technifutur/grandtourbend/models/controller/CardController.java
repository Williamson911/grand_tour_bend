package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.CardService;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardFacetsResponse;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cards")
@CrossOrigin("*")
@Tag(name = "Cards", description = "Catalogue des cartes Dragon Ball Super Card Game")
public class CardController {

    private static final MediaType IMAGE_WEBP = MediaType.parseMediaType("image/webp");

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "Lister les cartes", description = "Liste paginée, filtrable par type (ex: type=LEADER) et par recherche de nom (search=...).")
    @ApiResponse(responseCode = "200", description = "Page de cartes")
    public ResponseEntity<Page<CardResponse>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(cardService.getAll(type, search, PageRequest.of(page, size)));
    }

    @GetMapping("/printings")
    @Operation(summary = "Lister les impressions de cartes", description = "Une ligne par impression réelle (carte de base + chaque variante), filtrable par type, recherche de nom, couleur, série et rareté.")
    @ApiResponse(responseCode = "200", description = "Page d'impressions")
    public ResponseEntity<Page<CardPrintingResponse>> getPrintings(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String series,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(cardService.getPrintings(type, search, color, series, rarity, PageRequest.of(page, size)));
    }

    @GetMapping("/facets")
    @Operation(summary = "Valeurs de filtres disponibles", description = "Liste les couleurs, séries et raretés réellement présentes dans le catalogue, pour peupler des filtres en dropdown.")
    @ApiResponse(responseCode = "200", description = "Couleurs, séries et raretés distinctes")
    public ResponseEntity<CardFacetsResponse> getFacets() {
        return ResponseEntity.ok(cardService.getFacets());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une carte", description = "Inclut ses variantes (alt-arts).")
    @ApiResponse(responseCode = "200", description = "Détail de la carte")
    @ApiResponse(responseCode = "404", description = "Carte introuvable")
    public ResponseEntity<CardDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getById(id));
    }

    @GetMapping("/images/{imgLink}")
    @Operation(summary = "Image d'une carte", description = "Sert l'image stockée en base pour une carte ou une impression, identifiée par son imgLink.")
    @ApiResponse(responseCode = "200", description = "Image trouvée")
    @ApiResponse(responseCode = "404", description = "Image introuvable")
    public ResponseEntity<byte[]> getImage(@PathVariable String imgLink) {
        return cardService.getImage(imgLink)
                .map(image -> ResponseEntity.ok()
                        .contentType(resolveMediaType(image.contentType()))
                        .body(image.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return IMAGE_WEBP;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return IMAGE_WEBP;
        }
    }
}
