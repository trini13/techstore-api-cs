# TechStore API

Microservicio RESTful para gestión de productos de TechStore Chile.
Desarrollado con Java 17, Spring Boot, PostgreSQL y Docker.

## Requisitos
- Java 17
- Docker Desktop
- Maven

## Cómo ejecutar localmente

### 1. Clonar el repositorio
```bash
git clone https://github.com/trini13/techstore-api-cs.git
cd techstore-api-cs
```

### 2. Levantar la base de datos
```bash
docker run --name techstore_db -e POSTGRES_DB=techstore -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin123 -p 5432:5432 -d postgres:15
```

### 3. Compilar y ejecutar
```bash
.\mvnw clean package -DskipTests
java -jar target/api-0.0.1-SNAPSHOT.jar
```

### 4. O levantar todo con Docker Compose
```bash
.\mvnw clean package -DskipTests
docker compose up --build
```

## Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | /auth/login | Obtener token JWT |
| GET | /api/productos | Listar productos |
| POST | /api/productos | Crear producto |
| PUT | /api/productos/{id} | Modificar producto |
| DELETE | /api/productos/{id} | Eliminar producto (borrado lógico) |

## Credenciales de prueba
- Usuario: admin@techstore.cl
- Contraseña: Admin1234

---

## Evaluación 3 — Despliegue Cloud en AWS Academy

### Arquitectura desplegada

```
Postman/Cliente --HTTPS/JWT--> API Gateway --> ALB --> ECS Fargate (techstore-api)
                                                              |
                                                              |--> RDS PostgreSQL (persistencia)
                                                              |
                                                              '--> SQS techstore-audit-queue --> Lambda techstore-audit-logger --> CloudWatch Logs
```

1. El cliente (Postman) envía peticiones HTTP con `Authorization: Bearer <JWT>` al **API Gateway**.
2. API Gateway reenvía la petición al **Application Load Balancer (ALB)**.
3. El ALB enruta hacia las tareas de **ECS Fargate** que ejecutan el contenedor `techstore-api`.
4. El microservicio persiste en **Amazon RDS PostgreSQL**.
5. En cada `POST`, `PUT` o `DELETE` sobre `/api/productos`, el microservicio publica de forma **asíncrona** (`@Async`) un evento JSON de auditoría en la cola **Amazon SQS `techstore-audit-queue`**.
6. La cola dispara automáticamente la función **AWS Lambda `techstore-audit-logger`**, que imprime el log de auditoría en **Amazon CloudWatch Logs**.
7. Los Security Groups de ECS solo permiten tráfico entrante desde el ALB (no hay acceso público directo al contenedor).

### Variables de entorno (Task Definition de ECS)

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | JDBC URL de la instancia RDS | `jdbc:postgresql://<rds-endpoint>:5432/techstore` |
| `DB_USERNAME` | Usuario de RDS | `admin` |
| `DB_PASSWORD` | Password de RDS | *(secreto)* |
| `AWS_REGION` | Región del Learner Lab | `us-east-1` |
| `AWS_SQS_AUDIT_QUEUE_URL` | URL completa de la cola SQS | `https://sqs.us-east-1.amazonaws.com/<account_id>/techstore-audit-queue` |

Estas credenciales de AWS (para el SDK de SQS) **no se configuran a mano**: el SDK usa `DefaultCredentialsProvider`, que en ECS Fargate obtiene automáticamente las credenciales del rol **`LabRole`** asociado a la Task Definition (Task Role).

### Escalabilidad y monitoreo (Actividad 1.4)

- **ECS Service Auto Scaling**: se configura un *Target Tracking Scaling Policy* sobre la métrica `ECSServiceAverageCPUUtilization` del servicio Fargate, con un objetivo de ~60-70% de uso de CPU, mínimo 1 tarea y máximo 3-4 tareas. Cuando la carga sube, ECS lanza nuevas tareas (réplicas) del mismo contenedor detrás del ALB; cuando baja, las retira.
- **Reinicio de tareas**: a diferencia de un contenedor Docker local (donde si el proceso muere hay que reiniciarlo manualmente o con `restart: always`), ECS Fargate reemplaza automáticamente cualquier tarea que falle su *health check* del ALB o que termine inesperadamente, manteniendo siempre el número deseado de tareas (`desired count`).
- **Límites de CPU/Memoria**: se definieron 0.25 vCPU / 0.5 GB RAM por tarea (tal como exige la pauta), equivalentes a los límites que en local se configurarían con `docker run --cpus` y `--memory`, pero aquí administrados de forma nativa y elástica por Fargate sin necesidad de aprovisionar servidores.
- **Monitoreo**: las métricas de CPU/memoria de ECS y los logs de la aplicación y de la Lambda quedan centralizados en **Amazon CloudWatch**.

### Guía paso a paso de despliegue en AWS Academy

Ver la guía detallada de consola AWS en `docs/DEPLOY_AWS.md` (Amazon SQS, AWS Lambda, Amazon ECR, Amazon ECS Fargate + ALB, Amazon API Gateway, Security Groups y GitHub Actions).
