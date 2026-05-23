// src/main/java/growzapp/backend/service/EmailSenderService.java
package growzapp.backend.module.email;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.investissement.model.Investissement;

@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private final EmailService emailService;

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendContratEmail(Investissement inv, byte[] pdf) {
        emailService.envoyerContratParEmail(inv, pdf);
    }

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendPasswordResetMail(String email, String token) {
        emailService.sendPasswordResetMail(email, token);
    }

}