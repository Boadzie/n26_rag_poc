#!/bin/bash

# RAG Pipeline - Startup Script
# This script checks Docker and starts all services

set -e

echo "========================================="
echo "RAG Pipeline - Startup"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    echo "Please install Docker Desktop from: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo -e "${RED}Error: Docker daemon is not running${NC}"
    echo ""
    echo "Please start Docker Desktop:"
    echo "  1. Open Docker Desktop application"
    echo "  2. Wait for it to fully start"
    echo "  3. Run this script again"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ Docker is running${NC}"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}Error: .env file not found${NC}"
    echo "Please create .env file with your Gemini API key:"
    echo "  cp .env.example .env"
    echo "  # Then edit .env and add your GEMINI_API_KEY"
    exit 1
fi

# Source environment variables
export $(cat .env | grep -v '^#' | xargs)

# Check if GEMINI_API_KEY is set
if [ -z "$GEMINI_API_KEY" ]; then
    echo -e "${RED}Error: GEMINI_API_KEY is not set in .env file${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Environment variables loaded${NC}"
echo ""

# Check if docker-compose or docker compose is available
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
else
    echo -e "${RED}Error: docker-compose not found${NC}"
    exit 1
fi

echo "Starting services with $DOCKER_COMPOSE..."
echo ""

# Stop any existing containers
$DOCKER_COMPOSE down 2>/dev/null || true

# Start services
$DOCKER_COMPOSE up -d

echo ""
echo "Waiting for services to be healthy..."
echo ""

# Wait for API to be healthy
MAX_RETRIES=60
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if $DOCKER_COMPOSE ps | grep -q "rag-api.*healthy"; then
        echo -e "${GREEN}✓ API service is healthy${NC}"
        break
    fi

    if [ $((RETRY_COUNT % 5)) -eq 0 ]; then
        echo "Waiting for API to be healthy... ($((RETRY_COUNT+1))/$MAX_RETRIES)"
    fi

    sleep 2
    RETRY_COUNT=$((RETRY_COUNT+1))
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}Error: API did not become healthy in time${NC}"
    echo "Check logs with: $DOCKER_COMPOSE logs api"
    exit 1
fi

# Wait for ChromaDB
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if $DOCKER_COMPOSE ps | grep -q "rag-chromadb.*healthy"; then
        echo -e "${GREEN}✓ ChromaDB service is healthy${NC}"
        break
    fi

    if [ $((RETRY_COUNT % 5)) -eq 0 ]; then
        echo "Waiting for ChromaDB to be healthy... ($((RETRY_COUNT+1))/$MAX_RETRIES)"
    fi

    sleep 2
    RETRY_COUNT=$((RETRY_COUNT+1))
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}Error: ChromaDB did not become healthy in time${NC}"
    echo "Check logs with: $DOCKER_COMPOSE logs chromadb"
    exit 1
fi

echo ""
echo "========================================="
echo -e "${GREEN}✓ Services started successfully!${NC}"
echo "========================================="
echo ""
echo "Service Status:"
$DOCKER_COMPOSE ps
echo ""
echo "Next steps:"
echo "  1. Run ingestion: ./run_ingestion.sh"
echo "  2. Test API: curl http://localhost:8080/api/v1/health | jq"
echo "  3. Run tests: ./test_pipeline.sh"
echo ""
echo "View logs:"
echo "  - API: $DOCKER_COMPOSE logs -f api"
echo "  - ChromaDB: $DOCKER_COMPOSE logs -f chromadb"
echo ""
