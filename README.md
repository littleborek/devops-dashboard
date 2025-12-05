DevOps Dashboard 🚀

A comprehensive, full-stack monitoring and management solution built with Spring Boot, designed to provide frictionless, unified observability across traditional servers, Docker containers, Kubernetes clusters, and IoT devices.

This project goes beyond simple status checks by integrating deep system metrics and offering a unique "Agentless" monitoring strategy for frictionless enterprise adoption.

Key Features

Hybrid Monitoring: Unified visibility for traditional servers, Docker, Kubernetes, and IoT (WLED/ESP32).

Agentless Strategy: Monitors remote Docker installations via direct TCP connection (No need to install Java on target machines).

Auto-Discovery: Simplifies multi-cloud management through a Project/Token-based architecture.

Deep Metrics: Tracks CPU usage, RAM usage, and application-specific metrics (e.g., WiFi signal strength for WLED).

Real-time Status: Provides live status updates and historical data visualization via Chart.js.

Management Console: Supports CRUD operations for servers and manages related deployments.

Architecture & Technology Stack

The application is built around a modern Spring Boot ecosystem, leveraging microservices principles for data collection and a clean separation between monitoring strategies.

Layer

Technology / Component

Purpose

Backend (Core)

Spring Boot 3, Java 21, Maven

REST API, Scheduler, Transaction Management

Data Persistence

PostgreSQL, Spring Data JPA

Reliable and scalable data storage

Orchestration

docker-java (Zerodep), Kubernetes Java Client

Communication with Docker Daemon and K8s API

Metrics

OSHI (System Metrics), Custom HTTP/JSON

Collecting CPU/RAM/IoT data from native OS

Frontend

Thymeleaf, Tailwind CSS, Chart.js, SweetAlert2

Responsive UI, data visualization, and interaction

Agentless/Cloud

Custom TCP/IP polling logic

Monitoring external services without requiring a local Java agent
