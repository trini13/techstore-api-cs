# Guía paso a paso — Despliegue en AWS Academy (Evaluación 3)

> Requisito previo: tener el **Learner Lab** iniciado (botón "Start Lab") y
> haber copiado las credenciales temporales desde "AWS Details" -> "AWS CLI".

---

## Fase 2 — Amazon SQS, AWS Lambda e IAM (LabRole)

### 2.1 Crear la cola SQS
1. Consola AWS -> buscar **SQS** -> **Create queue**.
2. Tipo: **Standard**.
3. Nombre exacto: `techstore-audit-queue`.
4. Dejar el resto por defecto -> **Create queue**.
5. Copiar la **URL de la cola** (la necesitarás como variable de entorno `AWS_SQS_AUDIT_QUEUE_URL` en la Task Definition de ECS).

### 2.2 Crear la función Lambda
1. Consola AWS -> **Lambda** -> **Create function**.
2. "Author from scratch".
3. Nombre: `techstore-audit-logger`.
4. Runtime: **Python 3.12**.
5. Permisos de ejecución: **Use an existing role** -> seleccionar **`LabRole`** (nunca "Create a new role", porque el lab bloquea la creación de roles IAM).
6. Crear la función.
7. En el editor de código, reemplazar el contenido de `lambda_function.py` por el archivo `lambda-audit-logger/lambda_function.py` de este repo -> **Deploy**.

### 2.3 Configurar el trigger de SQS
1. Dentro de la función Lambda -> pestaña **Configuration** -> **Triggers** -> **Add trigger**.
2. Fuente: **SQS**.
3. Seleccionar la cola `techstore-audit-queue`.
4. Batch size: 1 (para ver los logs mensaje por mensaje) -> **Add**.

### 2.4 Verificar CloudWatch Logs
- Cada vez que la Lambda se ejecute, se creará un log group `/aws/lambda/techstore-audit-logger` visible en **CloudWatch -> Log groups**.

---

## Fase 3 — Amazon ECR (registro de imágenes)

### 3.1 Crear el repositorio
1. Consola AWS -> **ECR** -> **Create repository**.
2. Visibilidad: **Private**.
3. Nombre: `techstore-api`.
4. **Create repository**.

### 3.2 Autenticar Docker local con ECR (AWS CLI)
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account_id>.dkr.ecr.us-east-1.amazonaws.com
```

### 3.3 Build y push manual (primera vez, antes de tener el pipeline activo)
```bash
docker build -t techstore-api .
docker tag techstore-api:latest <account_id>.dkr.ecr.us-east-1.amazonaws.com/techstore-api:latest
docker push <account_id>.dkr.ecr.us-east-1.amazonaws.com/techstore-api:latest
```

---

## Fase 4 — Amazon RDS (PostgreSQL)

1. Consola AWS -> **RDS** -> **Create database**.
2. Método: **Standard create**.
3. Motor: **PostgreSQL**.
4. Plantilla: **Free tier** (o "Dev/Test", según disponibilidad en el lab).
5. Identificador: `techstore-db`.
6. Usuario maestro: `admin` / contraseña a elección (guárdala, será `DB_PASSWORD`).
7. Instancia: `db.t3.micro`.
8. Conectividad: **misma VPC** que usarás para ECS. En "Public access" se recomienda **No** (solo el ECS Fargate dentro de la VPC debe poder llegar a la base de datos).
9. Security Group de la base de datos: permitir tráfico entrante en el puerto **5432** únicamente desde el Security Group que usarán las tareas ECS (lo crearás en la Fase 5).
10. Crear la base de datos y esperar a que el estado sea "Available".
11. Copiar el **endpoint** (DNS) -> tu `DB_URL` será:
    `jdbc:postgresql://<endpoint-rds>:5432/techstore`

---

## Fase 5 — Amazon ECS Fargate + ALB + Security Groups

### 5.1 Crear el clúster
1. Consola AWS -> **ECS** -> **Clusters** -> **Create cluster**.
2. Infraestructura: **AWS Fargate (serverless)**.
3. Nombre: `techstore-cluster`.

### 5.2 Crear la Task Definition
1. **Task Definitions** -> **Create new Task Definition**.
2. Launch type: **Fargate**.
3. Task Role y Task Execution Role: seleccionar **`LabRole`** en ambos.
4. CPU: **0.25 vCPU** — Memoria: **0.5 GB**.
5. Agregar contenedor:
   - Nombre: `techstore-api` (debe coincidir con `CONTAINER_NAME` del workflow de GitHub Actions).
   - Imagen: `<account_id>.dkr.ecr.us-east-1.amazonaws.com/techstore-api:latest`.
   - Puerto: **8080**.
   - Variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AWS_REGION`, `AWS_SQS_AUDIT_QUEUE_URL` (ver tabla del README).

### 5.3 Security Groups
1. Crear SG `sg-alb-techstore`: permite entrada HTTP (80) desde `0.0.0.0/0` (o desde el rango del API Gateway si se restringe más).
2. Crear SG `sg-ecs-techstore`: permite entrada en el puerto 8080 **únicamente desde `sg-alb-techstore`** (no desde `0.0.0.0/0`). Esto cumple la Actividad 3.2.
3. En el SG de RDS (Fase 4), permitir entrada en 5432 únicamente desde `sg-ecs-techstore`.

### 5.4 Crear el Application Load Balancer y el Servicio
1. Al crear el **Service** dentro del cluster (Fargate), en la sección "Load balancing" elegir **Application Load Balancer**.
2. Crear uno nuevo, público, listener HTTP 80, target group apuntando al puerto 8080 del contenedor.
3. Asociar el SG `sg-ecs-techstore` a las tareas del servicio y `sg-alb-techstore` al ALB.
4. Desired tasks: 1 (el auto scaling se configura después).
5. Copiar el **DNS del ALB** una vez que el servicio esté "Running" (lo necesitarás para el API Gateway).

### 5.5 Configurar Auto Scaling (Actividad 1.4)
1. En el Service -> pestaña **Auto Scaling** -> **Update**.
2. Min tasks: 1 — Max tasks: 3 o 4.
3. Policy type: **Target tracking**.
4. Métrica: `ECSServiceAverageCPUUtilization`, valor objetivo: 60-70%.

---

## Fase 6 — Amazon API Gateway

1. Consola AWS -> **API Gateway** -> **Create API** -> **HTTP API** (más simple y económica que REST API para este caso).
2. Integration: **Private/HTTP** apuntando al DNS del ALB (`http://<dns-alb>/{proxy}`).
3. Rutas: `ANY /{proxy+}` -> integración con el ALB, para no tener que definir manualmente cada endpoint (`/auth/login`, `/api/productos`, etc.).
4. Deploy stage: `prod` (o `$default`).
5. Copiar la **Invoke URL** del API Gateway: ese es el endpoint público final que usarás en Postman.

---

## Fase 7 — GitHub Actions

1. En tu repo de GitHub -> **Settings -> Secrets and variables -> Actions** -> **New repository secret**, crear:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_SESSION_TOKEN`
   (copiados desde "AWS Details -> AWS CLI" del Learner Lab; **deben actualizarse cada vez que reinicies el lab**, porque expiran en pocas horas).
2. Revisar y ajustar en `.github/workflows/deploy.yml` los valores de `ECR_REPOSITORY`, `ECS_CLUSTER`, `ECS_SERVICE`, `ECS_TASK_DEFINITION_FAMILY` y `CONTAINER_NAME` si usaste otros nombres.
3. Hacer `git push` a `main` y verificar en la pestaña **Actions** de GitHub que el pipeline corra: test -> build imagen -> push a ECR -> deploy a ECS.

---

## Fase 8 — Pruebas E2E con Postman

1. `POST {invoke-url-api-gateway}/auth/login` con body `{"username":"admin@techstore.cl","password":"Admin1234"}` -> copiar el `token`.
2. `POST {invoke-url}/api/productos` con header `Authorization: Bearer <token>` y un body de producto -> debe responder `201 Created`.
3. Revisar en **SQS** (consola -> métricas de la cola) que el mensaje fue enviado/consumido.
4. Revisar en **CloudWatch Logs -> /aws/lambda/techstore-audit-logger** que aparezca el log de auditoría con los datos del producto creado.
5. Repetir con `PUT` y `DELETE` para demostrar los 3 tipos de evento (`CREAR`, `MODIFICAR`, `ELIMINAR`).
6. Probar que golpear directamente el DNS del ALB en el puerto 8080 **no** deba ser posible desde fuera (o esté bloqueado), demostrando que solo el API Gateway/ALB puede llegar al contenedor.
