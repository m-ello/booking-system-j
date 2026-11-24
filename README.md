# terminal 1, backend (or run from IntelliJ)
docker compose up -d db && ./mvnw spring-boot:run

# terminal 2, frontend
cd frontend && npm run dev
