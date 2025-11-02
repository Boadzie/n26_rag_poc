# RAG Pipeline PoC

> A production-ready Retrieval-Augmented Generation (RAG) system built with **LlamaIndex** and **Docling** for robust document processing, using Google Gemini for embeddings and text generation, with ChromaDB as the vector store.

## Architecture

This system uses a polyglot architecture:

- **Python** (`ingest.py`): Batch data ingestion using **LlamaIndex** framework with **Docling** reader for robust document parsing
- **Kotlin/Spring Boot** (`api`): REST API service for real-time queries
- **ChromaDB 0.5.20**: Vector database for similarity search (API v2)
- **Google Gemini**: Embedding model (text-embedding-004) and LLM (gemini-2.0-flash-exp)

### Key Technologies

- **LlamaIndex 0.14.7**: Production-ready RAG framework with automatic optimization
- **Docling 0.4.1**: Advanced document reader with layout-aware extraction for PDFs, DOCX, Markdown, and more
- **ChromaDB 0.5.20**: Latest vector database with improved performance
- **Spring Boot 3.2.1**: Enterprise-grade REST API framework

## Prerequisites

- Docker runtime:
  - **Docker Desktop**, or
  - **Colima** + Docker CLI (recommended for macOS): `brew install docker colima && colima start --cpu 4 --memory 8`
- Google Gemini API key ([Get one here](https://makersuite.google.com/app/apikey))
- At least 8GB RAM available for Docker (recommended)

## Quick Start

### 1. Clone and Configure

```bash
# Copy environment template
cp .env.example .env

# Edit .env and add your Gemini API key
nano .env  # or use your preferred editor
```

### 2. Add Your Documents

Place your documentation files in the `data/documents/` directory:

```bash
# Create documents directory
mkdir -p data/documents

# Supported formats: .txt, .md, .pdf
cp your-docs/*.md ./data/documents/

# Or use the included sample
# Sample document is already at: data/documents/sample_doc.md
```

### 3. Start the Services

```bash
# Start ChromaDB and API service
docker-compose up -d

# Wait for services to be healthy (check with)
docker-compose ps
```

### 4. Run Data Ingestion

```bash
# Run the ingestion script
./run_ingestion.sh
```

This will:

- Load documents from `data/documents/` directory using **Docling** reader (robust PDF, DOCX, Markdown parsing)
- Parse and chunk using **LlamaIndex** SentenceSplitter (paragraph-based strategy, 1000 chars, 200 overlap)
- Generate embeddings using Gemini text-embedding-004 (768 dimensions)
- Store in ChromaDB collection with automatic batching and error handling

**Expected output:**

```json
{
  "timestamp": "2025-11-01T06:43:31.454780",
  "level": "INFO",
  "message": "Ingestion pipeline completed successfully",
  "total_documents": 1,
  "total_time_seconds": 3.26
}
```

### 5. Query the API

```bash
# Health check
curl http://localhost:8080/api/v1/health

# Example query
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the N26 backend architecture?"}'
```

## API Documentation

### POST /api/v1/query

Query the RAG system with a natural language question.

**Request:**

```json
{
  "question": "What technologies does N26 use for the backend?"
}
```

**Response:**

```json
{
  "question": "What technologies does N26 use for the backend?",
  "answer": "N26's backend is composed of multiple microservices written in Kotlin and Java...",
  "sources": [
    {
      "id": "sample_doc_chunk_0",
      "content": "The backend is composed of multiple microservices...",
      "source": "/data/sample_doc.md",
      "score": 0.89,
      "metadata": {
        "source_file": "/data/sample_doc.md",
        "chunk_index": 0,
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
    "timestamp": "2025-01-15T10:30:00Z"
  }
}
```

### GET /api/v1/health

Health check endpoint.

**Response:**

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

### GET /actuator/prometheus

Prometheus metrics endpoint for monitoring.

## Configuration

All configuration is in `config.yaml`:

```yaml
# Vector Database
vector_db:
  type: "chroma"
  host: "chromadb"
  port: 8000
  collection_name: "n26_docs"

# Embedding Model (used by LlamaIndex)
embedding:
  provider: "gemini"
  model: "models/text-embedding-004"
  batch_size: 100
  dimensions: 768

# LLM for Generation (used by LlamaIndex)
llm:
  provider: "gemini"
  model: "gemini-2.0-flash-exp"
  temperature: 0.7
  max_tokens: 1024

# Chunking Strategy (LlamaIndex SentenceSplitter)
chunking:
  strategy: "paragraph"
  chunk_size: 1000
  chunk_overlap: 200

# Retrieval Settings
retrieval:
  top_k: 5
  similarity_threshold: 0.0 # Lowered for L2 distance compatibility
  search_type: "similarity"
```

## Project Structure

```
.
├── api/                          # Kotlin/Spring Boot API
│   ├── src/main/kotlin/com/n26/rag/
│   │   ├── RagApiApplication.kt
│   │   ├── config/              # Configuration classes
│   │   ├── controller/          # REST controllers
│   │   ├── model/               # Data models
│   │   └── service/             # Business logic
│   │       ├── ChromaDbClient.kt    # ChromaDB v2 client
│   │       ├── GeminiService.kt     # Gemini embeddings
│   │       └── RagService.kt        # RAG orchestration
│   ├── build.gradle.kts
│   └── Dockerfile
├── ingestion/                    # Python ingestion (LlamaIndex)
│   ├── ingest.py                # LlamaIndex + Docling ingestion
│   ├── requirements.txt         # Python dependencies
│   └── Dockerfile
├── data/
│   └── documents/               # Document storage
│       └── sample_doc.md
├── config.yaml                   # Global configuration
├── docker-compose.yml
├── run_ingestion.sh
└── README.md
```

## Monitoring and Observability

### Structured Logging

All services emit JSON-structured logs:

```json
{
  "timestamp": "2025-01-15T10:30:00.123Z",
  "level": "INFO",
  "service": "rag-api",
  "message": "Query processed successfully",
  "retrievalTime": 245,
  "llmTime": 1823,
  "totalTime": 2068
}
```

### Metrics

Prometheus metrics available at `/actuator/prometheus`:

- `http_server_requests_seconds`: API latency
- `jvm_memory_used_bytes`: Memory usage
- Custom metrics for RAG operations

### Health Checks

- API: `http://localhost:8080/actuator/health`
- ChromaDB: `http://localhost:8000/api/v1/heartbeat`

## Troubleshooting

### Services won't start

```bash
# Check logs
docker-compose logs api
docker-compose logs chromadb

# Restart services
docker-compose down
docker-compose up -d
```

### Ingestion fails

```bash
# Verify API key is set
echo $GEMINI_API_KEY

# Check ChromaDB is running (API v2)
curl http://localhost:8000/api/v2/pre-flight-checks

# Verify documents directory exists and has files
ls -la data/documents/

# Check ingestion logs
docker-compose logs chromadb

# Common issues:
# - Missing GEMINI_API_KEY in .env
# - Empty data/documents/ directory
# - ChromaDB not healthy before ingestion
```

### API returns 503 or "No relevant documents found"

```bash
# Check health endpoint
curl http://localhost:8080/api/v1/health

# Verify ChromaDB connection (API v2)
docker exec rag-api curl http://chromadb:8000/api/v2/pre-flight-checks

# Check if collection exists and has documents
curl http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections

# Common issues:
# - Ingestion not run yet (empty collection)
# - ChromaDB collection not created
# - API key not configured in API service
```

## Development

### Running Tests

```bash
# API tests
cd api
./gradlew test

# Build without tests
cd api
./gradlew build -x test
```

### Local Development

```bash
# Run API locally (requires ChromaDB running)
cd api
export GEMINI_API_KEY="your-key-here"
./gradlew bootRun

# Run ingestion locally
cd ingestion
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
export GEMINI_API_KEY="your-key-here"
python ingest.py

# Or rebuild and run ingestion in Docker
./run_ingestion.sh
```

### Key Dependencies

**Python (Ingestion)**:

- llama-index-core==0.14.7
- llama-index-readers-docling==0.4.1
- llama-index-vector-stores-chroma==0.5.3
- chromadb==0.5.20

**Kotlin (API)**:

- Spring Boot 3.2.1
- OkHttp 4.12.0
- Gson for JSON parsing

## Stopping the Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clears data)
docker-compose down -v
```

## License

Proprietary - N26 Internal Use Only
