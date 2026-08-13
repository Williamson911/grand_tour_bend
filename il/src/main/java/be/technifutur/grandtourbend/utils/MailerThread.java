package be.technifutur.grandtourbend.utils;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

public class MailerThread implements Runnable {

    private final MailerUtils mailerUtils;
    private String subject;
    private List<String> to;
    private String templateName;
    private Context context;

    public MailerThread(MailerUtils mailerUtils,
                        String subject,
                        String templateName,
                        Context context,
                        String... to) {
        this.mailerUtils = mailerUtils;
        this.subject = subject;
        this.templateName = templateName;
        this.context = context;
        this.to = List.of(to);
    }


    @Override
    public void run() {
        mailerUtils.sendMail(
                this.subject,
                this.templateName,
                this.context,
                this.to.toArray(new String[0])
        );
    }
}
