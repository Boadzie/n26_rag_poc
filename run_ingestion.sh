#!/bin/bash

# RAG Pipeline - Data Ingestion Runner
# This script runs the ingestion process in a Docker container

set -e

echo "Starting RAG data ingestion..."

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found. Please copy .env.example to .env and set your GEMINI_API_KEY"
    exit 1
fi

# Source environment variables
export $(cat .env | xargs)

# Check if GEMINI_API_KEY is set
if [ -z "$GEMINI_API_KEY" ]; then
    echo "Error: GEMINI_API_KEY is not set in .env file"
    exit 1
fi

# Check if ChromaDB is running
if ! docker ps | grep -q rag-chromadb; then
    echo "Starting ChromaDB..."
    docker-compose up -d chromadb
    echo "Waiting for ChromaDB to be ready..."
    sleep 10
fi

# Build ingestion image
echo "Building ingestion Docker image..."
docker build -t rag-ingestion:latest ./ingestion

# Run ingestion
echo "Running ingestion process..."
docker run --rm \
    --network n26_rag_rag-network \
    -v $(pwd)/data:/data \
    -v $(pwd)/config.yaml:/app/config.yaml:ro \
    -e GEMINI_API_KEY="$GEMINI_API_KEY" \
    rag-ingestion:latest \
    --config /app/config.yaml \
    --reset

echo "Ingestion completed successfully!"
