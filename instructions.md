AI Engineer Take-Home Assignment: RAG
Pipeline PoC

1. Objective
   The goal of this assignment is to build a Proof of Concept (PoC) for a RAG
   (Retrieval-Augmented Generation) pipeline and to present your architecture and strategic
   thinking in a formal presentation.
   This exercise is designed to assess your end-to-end engineering skills, from implementing a
   specified data science pipeline to system design, scalability planning, and technical
   communication. We want to see how you build, and more importantly, how you think.
2. The Scenario
   A team at our company has a hypothesis: an internal API for asking natural language questions
   about our technical documentation would significantly improve productivity. The Data Science
   (DS) team has already researched and provided a specific processing pipeline to use.
   Your task is to build a quick, containerized PoC to validate the technical feasibility of this
   pipeline. Following the implementation, you will prepare a short slide deck explaining your
   solution's architecture and how you would evolve it into a production-ready system.
3. Core Technical Tasks (The PoC Implementation)
   Your implementation should be a straightforward, working prototype that correctly implements
   the pipeline provided by the Data Science (DS) team.
   Note on the DS Pipeline: For this PoC, assume the DS team has provided the following
   components.
   ● Chunking Strategy: Paragraph-Based Chunking
   ● Embedding Model: Gemini Embedding
   ● Large Language Model (LLM): Gemini 2.5 Flash
   Your tasks are:
4. Data Ingestion Script:
   ○ Create a simple script (ingest.py) to process and load the provided sample
   documents into a vector database.
   ○ The script must use the specified Chunking Strategy and Embedding Model.
   ○ Key Hyperparameters for this process should be configurable (configuration
   file).
5. Core API Service:○ Develop a minimal Service with a REST API using a Web Framework (preferably
   Spring) to expose the functionality.
6. RAG Flow:
   ○ Implement the core RAG logic within the Service endpoint: receive a question,
   retrieve relevant context from the vector database, and generate an answer using
   the LLM.
7. Containerization:
   ○ Provide a Dockerfile for your service and a docker-compose.yml to run the entire
   PoC (your API and the database) with a single docker-compose up command.
8. Essential Logging:
   ○ Add basic, structured logging for critical events only (e.g., service start, query
   failure, LLM API latency).
9. Deliverables
   You are expected to submit two key items:
10. A .zip archive containing:
    ○ All your source code (api.py, ingest.py, config files, etc.).
    ○ The Dockerfile and docker-compose.yml.
    ○ A requirements.txt file.
    ○ A simple README.md file with only the instructions on how to run the PoC.
    All design discussion should be in the presentation.
11. A PDF Slide Presentation (approx. 5-6 slides) covering the topics detailed in the next
    section. This is a critical part of the evaluation.
12. Presentation Guidelines
    Please structure your PDF presentation to cover the following topics, ideally with one topic per
    slide.
    Slide 1: Title Slide
    ● Your Name, Project Title, Date.
    Slide 2: Architecture Overview
    ● Include a flow chart or diagram illustrating the complete architecture of your PoC.
    ● Clearly show the data flow for both ingestion and the live query process.
    ● Briefly annotate the key components and technologies used.
    Slide 3: Assumptions & Trade-offs
    ● Discuss the key assumptions you made for this PoC (e.g., about data format, network
    access, LLM rate limits).● What shortcuts or trade-offs did you make to deliver the PoC quickly? (e.g., minimal
    error handling, no authentication, telemetry, automated tests).
    Slide 4: From PoC to production
    ● Identify the primary performance bottlenecks that should be addressed to bring the
    project to production.
    ● Describe a high-level technical roadmap to evolve this PoC to handle high throughput
    (e.g., 1,000 requests per minute).
    Slide 5: Monitoring & Metrics
    ● List the key metrics you would monitor if this service were in production, covering:
    ○ Service Health
    ○ API Performance
    ○ Query Quality & Effectiveness
    ○ Cost
13. Evaluation Criteria
    You will be evaluated on:
    ● PoC Functionality: Does the submitted code work as described and correctly
    implement the specified pipeline?
    ● Best practices: Did you make tradeoffs and focus on what you consider important in
    production grade software. Examples aspects to consider are:
    ○ Test coverage: unit and/or integration
    ○ Readme and/or API documentation (Swagger)
    ○ Observability and monitoring through logs and metrics
    ○ Rate limiting, error handling, fault tolerance
    ○ Overall clean code and best practices
    ● Code & Containerization: Is the code clean, are parameters configurable, and is the
    Docker setup effective for replication?
    ● Presentation & Communication: How clearly do you communicate your design and
    future plans?
    ● Architectural & Systems Thinking: Does your presentation demonstrate a strong
    grasp of building scalable and maintainable systems, including the foresight to integrate
    future components like the reranker?
    Good luck! We look forward to seeing your work.
