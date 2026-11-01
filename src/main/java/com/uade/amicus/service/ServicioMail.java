package com.uade.amicus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de mails.
 *
 * Spring solo crea el JavaMailSender si en application.properties esta
 * configurado spring.mail.host. Sin SMTP (el caso normal en desarrollo) el
 * contenido del mail se escribe en la consola, para poder copiar el link y
 * probar el flujo completo sin una cuenta de correo.
 */
@Service
public class ServicioMail {

    private static final Logger log = LoggerFactory.getLogger(ServicioMail.class);

    private final ObjectProvider<JavaMailSender> proveedorDeMail;
    private final String remitente;

    public ServicioMail(ObjectProvider<JavaMailSender> proveedorDeMail,
                        @Value("${amicus.mail.remitente:no-responder@amicus.com}") String remitente) {
        this.proveedorDeMail = proveedorDeMail;
        this.remitente = remitente;
    }

    public void enviarRecuperacion(String destinatario, String link) {
        String texto = """
                Hola,

                Recibimos un pedido para cambiar la contrasena de tu cuenta de Amicus.
                Para elegir una nueva, entra a este link (vence en 30 minutos):

                %s

                Si no fuiste vos, ignora este mail: tu contrasena sigue siendo la misma.
                """.formatted(link);

        JavaMailSender mailSender = proveedorDeMail.getIfAvailable();
        if (mailSender == null) {
            log.info("SMTP sin configurar. Link para recuperar la contrasena de {}: {}", destinatario, link);
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Amicus - Cambiar tu contrasena");
        mensaje.setText(texto);
        try {
            mailSender.send(mensaje);
        } catch (MailException ex) {
            // No se corta el pedido: la respuesta tiene que ser la misma exista o no
            // la cuenta. Queda el link en la consola para no perderlo.
            log.warn("No se pudo mandar el mail de recuperacion a {}. Link: {}", destinatario, link, ex);
        }
    }
}
