# API Examples

Collection of example API calls for testing the RAG pipeline.

## Prerequisites

```bash
# Ensure services are running
docker-compose ps

# Set base URL
export API_URL="http://localhost:8080/api/v1"
```

## Health Check

```bash
curl -X GET "$API_URL/health" | jq
```

**Expected Response:**
```json
{
  "status": "healthy",
  "timestamp": "2025-01-15T10:30:00Z",
  "services": {
    "chromadb": {
      "status": "healthy"
    },
    "gemini": {
      "status": "healthy",
      "message": "API key configured"
    }
  }
}
```

## Query Examples

### Basic Query

```bash
curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is N26?"
  }' | jq
```

### Backend Architecture Query

```bash
curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What technologies are used in the N26 backend?"
  }' | jq
```

### Security Practices Query

```bash
curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What security practices does N26 implement?"
  }' | jq
```

### API Guidelines Query

```bash
curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the rate limiting policies?"
  }' | jq
```

### Monitoring and Observability Query

```bash
curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "How does N26 monitor their services?"
  }' | jq
```

## Error Cases

### Empty Question (Validation Error)

```bash
curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "question": ""
  }' | jq
```

**Expected Response:**
```json
{
  "error": "Validation Error",
  "message": "question: Question cannot be blank",
  "timestamp": "2025-01-15T10:30:00Z",
  "path": "/api/v1/query"
}
```

### Question Too Long

```bash
curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"$(python3 -c 'print("a" * 501)')\"
  }" | jq
```

## Performance Testing

### Measure Response Time

```bash
time curl -X POST "$API_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the deployment process?"
  }' -o /dev/null -s
```

### Load Test (using Apache Bench)

```bash
# Install ab if not available
# Ubuntu/Debian: apt-get install apache2-utils
# macOS: pre-installed

# Run 100 requests with 10 concurrent
ab -n 100 -c 10 -p query.json -T application/json "$API_URL/query"
```

Content of `query.json`:
```json
{
  "question": "What is N26?"
}
```

## Monitoring Endpoints

### Prometheus Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

### Application Info

```bash
curl http://localhost:8080/actuator/info | jq
```

### Spring Boot Health

```bash
curl http://localhost:8080/actuator/health | jq
```

## Sample Response Structure

```json
{
  "question": "What technologies are used in the N26 backend?",
  "answer": "N26's backend is built using Kotlin and Java, with microservices deployed on Kubernetes clusters. Each service is responsible for specific business domains such as accounts, transactions, cards, and customer management.",
  "sources": [
    {
      "id": "sample_doc_chunk_1",
      "content": "The backend is composed of multiple microservices written in Kotlin and Java, deployed on Kubernetes clusters...",
      "source": "/data/sample_doc.md",
      "score": 0.92,
      "metadata": {
        "source_file": "/data/sample_doc.md",
        "chunk_index": 1,
        "total_chunks": 5
      }
    }
  ],
  "metadata": {
    "retrievalTimeMs": 245,
    "llmTimeMs": 1823,
    "totalTimeMs": 2068,
    "documentsRetrieved": 5,
    "model": "gemini-2.0-flash-exp",
    "timestamp": "2025-01-15T10:30:00.000Z"
  }
}
```

## Observability

### Check API Logs

```bash
# Follow API logs
docker-compose logs -f api

# Search for specific query
docker-compose logs api | grep "Processing RAG query"
```

### Check Ingestion Logs

```bash
# View ingestion logs
docker logs rag-ingestion
```

### ChromaDB Status

```bash
# Check ChromaDB heartbeat
curl http://localhost:8000/api/v1/heartbeat

# List collections
curl http://localhost:8000/api/v1/collections | jq
```

## Tips

1. **Use jq for pretty printing**: Always pipe responses to `jq` for better readability
2. **Check metadata**: The `metadata` field contains timing information useful for performance analysis
3. **Source attribution**: Each answer includes `sources` showing which documents were used
4. **Similarity scores**: Higher scores (0.7-1.0) indicate better relevance
5. **Monitor latency**: Track `retrievalTimeMs` and `llmTimeMs` separately for optimization

## Troubleshooting

```bash
# If API is not responding
docker-compose restart api

# If getting empty answers
./run_ingestion.sh --reset

# Check ChromaDB has data
curl http://localhost:8000/api/v1/collections/n26_docs | jq
```
