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