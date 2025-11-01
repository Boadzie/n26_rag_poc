FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy ingestion script
COPY ingest.py .

# Create data directory
RUN mkdir -p /data/documents /data/chroma

# Set environment variables
ENV PYTHONUNBUFFERED=1

ENTRYPOINT ["python", "ingest.py"]
