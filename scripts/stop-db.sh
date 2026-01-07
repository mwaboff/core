#!/bin/bash

# Stop PostgreSQL database using Docker Compose
echo "Stopping PostgreSQL database..."
docker compose down

echo "Database stopped successfully!"
