#!/usr/bin/env python3
"""
Data Ingestion Script for RAG Pipeline using LlamaIndex and Docling
Processes documents, chunks them, generates embeddings, and stores in ChromaDB.
"""

import os
import sys
import time
import yaml
import logging
import json
from pathlib import Path
from typing import List, Dict, Any
from datetime import datetime

import chromadb
from llama_index.core import VectorStoreIndex, StorageContext, Settings
from llama_index.vector_stores.chroma import ChromaVectorStore
from llama_index.embeddings.gemini import GeminiEmbedding
from llama_index.llms.gemini import Gemini
from llama_index.core.node_parser import SentenceSplitter
from llama_index.readers.docling import DoclingReader


class StructuredLogger:
    """JSON structured logger for observability"""

    def __init__(self, name: str, level: str = "INFO"):
        self.logger = logging.getLogger(name)
        self.logger.setLevel(getattr(logging, level))

        handler = logging.StreamHandler()
        handler.setFormatter(logging.Formatter('%(message)s'))
        self.logger.addHandler(handler)

    def _log(self, level: str, message: str, **kwargs):
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "level": level,
            "message": message,
            **kwargs
        }
        self.logger.log(getattr(logging, level), json.dumps(log_entry))

    def info(self, message: str, **kwargs):
        self._log("INFO", message, **kwargs)

    def error(self, message: str, **kwargs):
        self._log("ERROR", message, **kwargs)

    def warning(self, message: str, **kwargs):
        self._log("WARNING", message, **kwargs)


class IngestionPipeline:
    """Main ingestion pipeline using LlamaIndex and Docling"""

    def __init__(self, config_path: str):
        # Load configuration
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        # Initialize logger
        log_config = self.config.get('logging', {})
        self.logger = StructuredLogger('ingestion', log_config.get('level', 'INFO'))

        self.logger.info("Ingestion pipeline initialized", config=config_path)

        # Get API key
        api_key = os.getenv(self.config['embedding']['api_key_env'])
        if not api_key:
            raise ValueError(f"Environment variable {self.config['embedding']['api_key_env']} not set")

        # Configure LlamaIndex Settings
        Settings.embed_model = GeminiEmbedding(
            model_name=self.config['embedding']['model'],
            api_key=api_key
        )

        Settings.llm = Gemini(
            model=self.config['llm']['model'],
            api_key=api_key,
            temperature=self.config['llm']['temperature']
        )

        # Configure node parser (chunking strategy)
        Settings.node_parser = SentenceSplitter(
            chunk_size=self.config['chunking']['chunk_size'],
            chunk_overlap=self.config['chunking']['chunk_overlap']
        )

        # Initialize Docling reader
        self.reader = DoclingReader()

        self.logger.info("LlamaIndex and Docling configured successfully")

    def load_documents(self, data_dir: Path) -> List:
        """Load documents using Docling reader"""
        try:
            start_time = time.time()

            if not data_dir.exists():
                self.logger.error(f"Data directory does not exist", path=str(data_dir))
                return []

            # Get all supported document files
            supported_formats = self.config['ingestion']['supported_formats']
            file_paths = []

            for ext in supported_formats:
                file_paths.extend(list(data_dir.glob(f"*.{ext}")))

            if not file_paths:
                self.logger.warning("No documents found", directory=str(data_dir))
                return []

            self.logger.info(f"Found documents", count=len(file_paths))

            # Load documents with Docling
            documents = []
            for file_path in file_paths:
                try:
                    # Docling reader handles various formats robustly
                    docs = self.reader.load_data(file_path=str(file_path))
                    documents.extend(docs)
                    self.logger.info(f"Loaded document",
                                   file=str(file_path),
                                   num_docs=len(docs))
                except Exception as e:
                    self.logger.error(f"Failed to load document",
                                    file=str(file_path),
                                    error=str(e))
                    continue

            latency = time.time() - start_time
            self.logger.info(f"Document loading complete",
                           total_documents=len(documents),
                           latency_seconds=round(latency, 2))

            return documents

        except Exception as e:
            self.logger.error(f"Document loading failed", error=str(e))
            raise

    def create_vector_store(self, reset: bool = False):
        """Create or get ChromaDB vector store"""
        try:
            # Initialize ChromaDB client
            if self.config['vector_db']['host'] == 'localhost':
                persist_dir = self.config['vector_db'].get('persist_directory', './chroma_db')
                os.makedirs(persist_dir, exist_ok=True)
                chroma_client = chromadb.PersistentClient(path=persist_dir)
            else:
                chroma_client = chromadb.HttpClient(
                    host=self.config['vector_db']['host'],
                    port=self.config['vector_db']['port']
                )

            collection_name = self.config['vector_db']['collection_name']

            # Reset collection if requested
            if reset:
                try:
                    chroma_client.delete_collection(name=collection_name)
                    self.logger.info("Deleted existing collection")
                except Exception:
                    pass

            # Get or create collection
            chroma_collection = chroma_client.get_or_create_collection(
                name=collection_name
            )

            # Create ChromaVectorStore
            vector_store = ChromaVectorStore(chroma_collection=chroma_collection)

            self.logger.info("Vector store ready", collection=collection_name)

            return vector_store

        except Exception as e:
            self.logger.error("Failed to create vector store", error=str(e))
            raise

    def run(self, reset: bool = False):
        """Run the complete ingestion pipeline"""
        try:
            pipeline_start = time.time()
            self.logger.info("Starting ingestion pipeline", reset_collection=reset)

            # Step 1: Load documents with Docling
            data_dir = Path(self.config['ingestion']['data_directory'])
            documents = self.load_documents(data_dir)

            if not documents:
                self.logger.error("No documents loaded")
                return

            # Step 2: Create vector store
            vector_store = self.create_vector_store(reset=reset)

            # Step 3: Create storage context
            storage_context = StorageContext.from_defaults(vector_store=vector_store)

            # Step 4: Build index (this handles chunking, embedding, and storage)
            index_start = time.time()
            self.logger.info("Building vector index...")

            index = VectorStoreIndex.from_documents(
                documents,
                storage_context=storage_context,
                show_progress=True
            )

            index_time = time.time() - index_start
            self.logger.info("Vector index built successfully",
                           latency_seconds=round(index_time, 2))

            # Get statistics
            total_time = time.time() - pipeline_start

            self.logger.info("Ingestion pipeline completed successfully",
                           total_documents=len(documents),
                           total_time_seconds=round(total_time, 2))

            return index

        except Exception as e:
            self.logger.error("Ingestion pipeline failed", error=str(e))
            raise


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(description='RAG Pipeline Data Ingestion with LlamaIndex')
    parser.add_argument('--config', default='../config.yaml', help='Path to config file')
    parser.add_argument('--reset', action='store_true', help='Reset collection before ingesting')
    args = parser.parse_args()

    try:
        pipeline = IngestionPipeline(args.config)
        pipeline.run(reset=args.reset)

    except Exception as e:
        print(json.dumps({
            "timestamp": datetime.utcnow().isoformat(),
            "level": "ERROR",
            "message": "Pipeline execution failed",
            "error": str(e)
        }))
        sys.exit(1)


if __name__ == '__main__':
    main()
