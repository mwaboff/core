#!/bin/bash

# Start PostgreSQL database using Docker Compose
echo "Starting PostgreSQL database..."
docker compose up -d

echo "Waiting for database to be ready..."
sleep 5

echo "Database started successfully!"
echo "Connection info:"
echo "Host: localhost"
echo "Port: 5432"
echo "Database: ${POSTGRES_DB:-heartandfear}"
echo "User: ${POSTGRES_USER:-postgres}"
