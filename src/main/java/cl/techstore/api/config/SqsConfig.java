package cl.techstore.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Configuracion del cliente de Amazon SQS.
 *
 * Usa DefaultCredentialsProvider para que funcione tanto:
 *  - En local: leyendo las credenciales temporales exportadas como variables
 *    de entorno (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)
 *    desde el boton "AWS Details" del Learner Lab.
 *  - En AWS ECS Fargate: leyendo automaticamente las credenciales del rol
 *    "LabRole" asociado a la Task Definition (Task Role), sin necesidad de
 *    hardcodear ninguna clave.
 */
@Configuration
public class SqsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
