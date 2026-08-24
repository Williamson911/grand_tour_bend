package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.CardService;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cards")
@CrossOrigin("*")
@Tag(name = "Cards", description = "Catalogue des cartes Dragon Ball Super Card Game")
public class CardController {

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
    @Operation(summary = "Lister les impressions de cartes", description = "Une ligne par impression réelle (carte de base + chaque variante), filtrable par type, recherche de nom, couleur et série.")
    @ApiResponse(responseCode = "200", description = "Page d'impressions")
    public ResponseEntity<Page<CardPrintingResponse>> getPrintings(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String series,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(cardService.getPrintings(type, search, color, series, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une carte", description = "Inclut ses variantes (alt-arts).")
    @ApiResponse(responseCode = "200", description = "Détail de la carte")
    @ApiResponse(responseCode = "404", description = "Carte introuvable")
    public ResponseEntity<CardDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getById(id));
    }
}
