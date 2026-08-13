package be.technifutur.grandtourbend.models.controller.mailer;

import be.technifutur.grandtourbend.utils.MailerThread;
import be.technifutur.grandtourbend.utils.MailerUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.Context;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mailer", description = "Envoi d'emails (endpoint de test)")
public class MailerController {

    private final MailerUtils mailerUtils;

    @GetMapping("/send")
    @Operation(summary = "Envoyer un email de test", description = "Endpoint de démo, envoie un email fixe à une adresse codée en dur.")
    @ApiResponse(responseCode = "200", description = "Envoi déclenché (asynchrone, pas de garantie de délivrance)")
    public ResponseEntity<Void> sendMail() {

        Context context = new Context();
        context.setVariable("username","Seb");

        MailerThread mailerThread = mailerUtils.createThread(
                "Cool encore plus cool",
                "welcome",
                context,
                "byasebastien@hotmail.com"
        );

        Thread thread = new Thread(mailerThread);
        thread.start();

        return ResponseEntity.ok().build();
    }
}