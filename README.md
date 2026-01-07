# Core Backend Project

A Java 25 Spring Boot application with PostgreSQL database integration using Flyway for migrations.

## Database Setup

### Prerequisites
- Docker and Docker Compose installed
- Java 25
- Maven

### Environment Configuration
The application uses environment variables for database configuration. Create a `.env` file in the project root:

```bash
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
```

### Database Operations

#### Start Database
```bash
docker compose up -d
```

#### Stop Database
```bash
docker compose down
```

#### Connect to Database
```bash
./scripts/connect-db.sh
```

Or manually:
```bash
docker compose exec postgres psql -U postgres -d heartandfear
```

#### Drop Database (Development)
```bash
./scripts/drop-db.sh
```

### Application Configuration
The application automatically reads database connection details from environment variables:
- Database URL: `jdbc:postgresql://localhost:5432/${POSTGRES_DB:heartandfear}`
- Username: `${POSTGRES_USER:postgres}`
- Password: `${POSTGRES_PASSWORD:password}`

### Flyway Migrations
Place SQL migration files in `src/main/resources/db/migration/`. Flyway will automatically apply them on application startup.

### Running the Application
1. Start the database: `./scripts/start-db.sh`
2. Run the application: `./mvnw spring-boot:run`

The application will be available at `http://localhost:8080`.

### Maven Commands

#### Start the Application
```bash
./mvnw spring-boot:run
```

#### Development Commands
```bash
# Clean and start
./mvnw clean spring-boot:run

# Build and run JAR
./mvnw clean package
java -jar target/core-0.0.1-SNAPSHOT.jar

# Skip tests during development
./mvnw spring-boot:run -Dmaven.test.skip=true

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Scripts
Use the scripts in the `scripts/` directory for common database operations:
- `start-db.sh` - Start the database
- `stop-db.sh` - Stop the database
- `connect-db.sh` - Connect to the database
- `drop-db.sh` - Drop the database (development only)
- `create-migration.sh` - Generate a blank Flyway migration file

### Creating Migrations
```bash
# Create a new migration file
./scripts/create-migration.sh create_users_table

# This creates: src/main/resources/db/migration/V20260107093512345__create_users_table.sql
```
