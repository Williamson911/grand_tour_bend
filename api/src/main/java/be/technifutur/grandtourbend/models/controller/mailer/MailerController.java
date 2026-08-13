package be.technifutur.grandtourbend.models.controller.mailer;

import be.technifutur.grandtourbend.utils.MailerThread;
import be.technifutur.grandtourbend.utils.MailerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.Context;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MailerController {

    private final MailerUtils mailerUtils;

    @GetMapping("/send")
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