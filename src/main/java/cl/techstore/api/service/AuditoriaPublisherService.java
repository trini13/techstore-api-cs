package cl.techstore.api.service;

import cl.techstore.api.dto.AuditoriaEventoDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;

/**
 * Actividad 2.2: Integracion del Productor (Spring Boot).
 *
 * Publica de forma asincrona en la cola Amazon SQS "techstore-audit-queue"
 * un evento JSON cada vez que se crea (POST), modifica (PUT) o elimina de
 * forma logica (DELETE) un producto del catalogo.
 */
@Service
public class AuditoriaPublisherService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaPublisherService.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.sqs.audit-queue-url:}")
    private String queueUrl;

    public AuditoriaPublisherService(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Async
    public void publicarEvento(String accion, Long productoId, String nombre, String usuario) {
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("[Auditoria SQS] AWS_SQS_AUDIT_QUEUE_URL no configurada. Se omite el envio del evento.");
            return;
        }

        AuditoriaEventoDTO evento = new AuditoriaEventoDTO(
                accion,
                productoId,
                nombre,
                usuario != null ? usuario : "desconocido",
                Instant.now().toString()
        );

        try {
            String mensajeJson = objectMapper.writeValueAsString(evento);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(mensajeJson)
                    .build();

            sqsClient.sendMessage(request);
            log.info("[Auditoria SQS] Evento publicado correctamente: {}", mensajeJson);
        } catch (JsonProcessingException e) {
            log.error("[Auditoria SQS] Error serializando el evento de auditoria", e);
        } catch (Exception e) {
            // No se relanza la excepcion: un fallo en SQS no debe romper la
            // operacion principal de escritura sobre el catalogo.
            log.error("[Auditoria SQS] Error publicando el evento en la cola", e);
        }
    }
}
