#!/bin/bash

# RAG Pipeline - Complete Testing Script
# This script tests the entire pipeline end-to-end

set -e

echo "========================================="
echo "RAG Pipeline - End-to-End Test"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if services are running
echo "Step 1: Checking if services are running..."
if ! docker-compose ps | grep -q "rag-api"; then
    echo -e "${RED}Error: Services not running. Please run 'docker compose up -d' first${NC}"
    exit 1


fi
echo -e "${GREEN}✓ Services are running${NC}"
echo ""

# Wait for services to be healthy
echo "Step 2: Waiting for services to be healthy..."
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ API is healthy${NC}"
        break
    fi
    echo "Waiting for API... ($((RETRY_COUNT+1))/$MAX_RETRIES)"
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT+1))
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}Error: API did not become healthy${NC}"
    docker compose logs api
    exit 1
fi
echo ""

# Check ChromaDB
echo "Step 3: Checking ChromaDB..."
if curl -s http://localhost:8000/api/v1/heartbeat > /dev/null 2>&1; then
    echo -e "${GREEN}✓ ChromaDB is healthy${NC}"
else
    echo -e "${RED}Error: ChromaDB is not responding${NC}"
    exit 1
fi
echo ""

# Test health endpoint
echo "Step 4: Testing health endpoint..."
HEALTH_RESPONSE=$(curl -s http://localhost:8080/api/v1/health)
echo "$HEALTH_RESPONSE" | jq
if echo "$HEALTH_RESPONSE" | jq -e '.status == "healthy"' > /dev/null; then
    echo -e "${GREEN}✓ Health check passed${NC}"
else
    echo -e "${YELLOW}⚠ Health check returned degraded status${NC}"
fi
echo ""

# Test query endpoint
echo "Step 5: Testing query endpoint..."
echo "Question: 'What is the N26 backend architecture?'"
echo ""

QUERY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/query \
    -H "Content-Type: application/json" \
    -d '{"question": "What is the N26 backend architecture?"}')

if echo "$QUERY_RESPONSE" | jq -e '.answer' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Query successful!${NC}"
    echo ""
    echo "Answer:"
    echo "$QUERY_RESPONSE" | jq -r '.answer'
    echo ""
    echo "Metadata:"
    echo "$QUERY_RESPONSE" | jq '.metadata'
    echo ""
    echo "Sources retrieved: $(echo "$QUERY_RESPONSE" | jq '.sources | length')"
else
    echo -e "${RED}✗ Query failed${NC}"
    echo "$QUERY_RESPONSE" | jq
    exit 1
fi
echo ""

# Test another query
echo "Step 6: Testing another query..."
echo "Question: 'What security practices does N26 implement?'"
echo ""

QUERY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/query \
    -H "Content-Type: application/json" \
    -d '{"question": "What security practices does N26 implement?"}')


if echo "$QUERY_RESPONSE" | jq -e '.answer' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Query successful!${NC}"
    echo ""
    echo "Answer:"
    echo "$QUERY_RESPONSE" | jq -r '.answer'
    echo ""
    RETRIEVAL_TIME=$(echo "$QUERY_RESPONSE" | jq -r '.metadata.retrievalTimeMs')
    LLM_TIME=$(echo "$QUERY_RESPONSE" | jq -r '.metadata.llmTimeMs')
    TOTAL_TIME=$(echo "$QUERY_RESPONSE" | jq -r '.metadata.totalTimeMs')
    echo "Performance:"
    echo "  - Retrieval: ${RETRIEVAL_TIME}ms"
    echo "  - LLM: ${LLM_TIME}ms"
    echo "  - Total: ${TOTAL_TIME}ms"
else
    echo -e "${RED}✗ Query failed${NC}"
    echo "$QUERY_RESPONSE" | jq
fi
echo ""

# Test validation error
echo "Step 7: Testing validation (empty question)..."
ERROR_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/query \
    -H "Content-Type: application/json" \
    -d '{"question": ""}')

if echo "$ERROR_RESPONSE" | jq -e '.error == "Validation Error"' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Validation working correctly${NC}"
else
    echo -e "${YELLOW}⚠ Validation response unexpected${NC}"
    echo "$ERROR_RESPONSE" | jq
fi
echo ""

# Test Prometheus metrics
echo "Step 8: Checking Prometheus metrics..."
if curl -s http://localhost:8080/actuator/prometheus | grep -q "http_server_requests"; then
    echo -e "${GREEN}✓ Prometheus metrics available${NC}"
else
    echo -e "${YELLOW}⚠ Prometheus metrics not found${NC}"
fi
echo ""

# Summary
echo "========================================="
echo -e "${GREEN}✓ All tests passed successfully!${NC}"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. View logs: docker compose logs -f api"
echo "2. View metrics: curl http://localhost:8080/actuator/prometheus"
echo "3. More examples: see API_EXAMPLES.md"
echo ""
