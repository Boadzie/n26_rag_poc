# Quick Start Guide

## Prerequisites Check

Before starting, ensure Docker Desktop is running:

```bash
docker --version
docker compose version
```

If Docker is not installed, download it from: https://www.docker.com/products/docker-desktop

## Step-by-Step Startup

### 1. Navigate to Project Directory

```bash
cd /Users/boadzie/Documents/Dev_center/Projects/n26_rag
```

### 2. Verify Environment Variables

```bash
# Check that .env file exists and has your API key
cat .env
```

Should show:
```
GEMINI_API_KEY=your_actual_key_here
```

### 3. Start Services

```bash
# Start ChromaDB and API service
docker compose up -d

# Check status (wait until both are "healthy")
docker compose ps
```

Expected output:
```
NAME              STATUS          PORTS
rag-api          Up (healthy)    0.0.0.0:8080->8080/tcp
rag-chromadb     Up (healthy)    0.0.0.0:8000->8000/tcp
```

### 4. View Logs (Optional)

```bash
# Follow API logs
docker compose logs -f api

# Or view ChromaDB logs
docker compose logs -f chromadb

# Press Ctrl+C to stop following
```

### 5. Run Data Ingestion

```bash
# This will process documents and load them into ChromaDB
./run_ingestion.sh
```

Expected output:
```json
{"timestamp": "...", "level": "INFO", "message": "Ingestion pipeline initialized", ...}
{"timestamp": "...", "level": "INFO", "message": "Loaded all documents", "total_count": 1}
{"timestamp": "...", "level": "INFO", "message": "Chunking complete", "total_chunks": 5}
{"timestamp": "...", "level": "INFO", "message": "Generated embeddings batch", ...}
{"timestamp": "...", "level": "INFO", "message": "Documents added to vector store", ...}
{"timestamp": "...", "level": "INFO", "message": "Ingestion pipeline completed successfully", ...}
```

### 6. Test the API

#### Test 1: Health Check

```bash
curl http://localhost:8080/api/v1/health | jq
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-15T...",
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

#### Test 2: Simple Query

```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is N26?"}' | jq
```

#### Test 3: Backend Architecture Query

```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What technologies are used in the N26 backend?"}' | jq
```

#### Test 4: Security Query

```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What security practices does N26 implement?"}' | jq
```

### 7. Run Complete Test Suite

```bash
./test_pipeline.sh
```

This will run all tests automatically and show you the results.

## Understanding the Response

```json
{
  "question": "What technologies are used in the N26 backend?",
  "answer": "The backend is composed of multiple microservices written in Kotlin and Java...",
  "sources": [
    {
      "id": "sample_doc_chunk_1",
      "content": "Relevant document chunk...",
      "source": "/data/sample_doc.md",
      "score": 0.92,
      "metadata": {...}
    }
  ],
  "metadata": {
    "retrievalTimeMs": 245,      // Time to search vectors
    "llmTimeMs": 1823,            // Time to generate answer
    "totalTimeMs": 2068,          // Total request time
    "documentsRetrieved": 5,      // Number of relevant chunks
    "model": "gemini-2.0-flash-exp",
    "timestamp": "2025-01-15T10:30:00Z"
  }
}
```

## Common Issues

### Issue 1: Docker not running

```bash
# Error: Cannot connect to the Docker daemon
# Solution: Start Docker Desktop application
```

### Issue 2: Port already in use

```bash
# Error: port is already allocated
# Solution: Stop conflicting services
docker compose down
lsof -ti:8080 | xargs kill -9  # Kill process on port 8080
lsof -ti:8000 | xargs kill -9  # Kill process on port 8000
docker compose up -d
```

### Issue 3: API returns 503

```bash
# Check if ingestion was run
docker compose logs chromadb | grep "n26_docs"

# Re-run ingestion
./run_ingestion.sh
```

### Issue 4: Ingestion fails with API error

```bash
# Check GEMINI_API_KEY is set correctly
echo $GEMINI_API_KEY

# Verify it's in .env file
cat .env

# Check Gemini API quota: https://console.cloud.google.com/apis/
```

## Monitoring

### View Real-time Logs

```bash
# API logs
docker compose logs -f api

# All logs
docker compose logs -f
```

### Check Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Spring Boot actuator
curl http://localhost:8080/actuator | jq
```

### Check ChromaDB Collections

```bash
# List collections
curl http://localhost:8000/api/v1/collections | jq

# Get collection details
curl http://localhost:8000/api/v1/collections/n26_docs | jq
```

## Stopping Services

```bash
# Stop services (keeps data)
docker compose stop

# Stop and remove containers (keeps data)
docker compose down

# Stop and remove everything including data
docker compose down -v
```

## Adding More Documents

```bash
# 1. Add your documents to the data/ directory
cp your-docs/*.md ./data/

# 2. Re-run ingestion with --reset flag to clear old data
./run_ingestion.sh

# 3. Test with new queries
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Your question about new documents"}' | jq
```

## Performance Tips

1. **Adjust chunk size**: Edit `config.yaml` → `chunking.chunk_size`
2. **Change retrieval count**: Edit `config.yaml` → `retrieval.top_k`
3. **Tune LLM parameters**: Edit `config.yaml` → `llm.temperature`
4. **Monitor latency**: Check `metadata.totalTimeMs` in responses

## Next Steps

1. ✅ Add your own documents to `data/` directory
2. ✅ Adjust configuration in `config.yaml`
3. ✅ Read `API_EXAMPLES.md` for more query examples
4. ✅ Check `README.md` for architecture details
5. ✅ View presentation guidelines in `instructions.md`

## Need Help?

- Check logs: `docker compose logs api`
- View health: `curl http://localhost:8080/api/v1/health`
- Test ChromaDB: `curl http://localhost:8000/api/v1/heartbeat`
- Restart: `docker compose restart api`

Happy querying! 🚀
