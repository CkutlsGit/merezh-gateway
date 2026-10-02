# Gateway Service - Merezh

Single entry point for the Merezh microservices platform. Responsible for routing, authentication, and authorization.

📖 In Russian: [перевод на русский](#)

## 📋 Overview

Gateway is a Spring Boot microservice that serves as the **single entry point** for all client requests. It validates JWT tokens, extracts `userId` and `role`, and passes them to internal services via the `X-User-Id` and `X-User-Role` headers. Additionally, Gateway performs **role-based authorization** (for example, `ADMIN` endpoints) and proxies requests to the corresponding services.

All internal services (user, auth, wallet, order, payment) are **not exposed externally** - they run only inside the Docker `dbnet` network. Gateway is the **only** container whose port is mapped to the host.

## 🚀 Technology Stack

**Backend**

- Java 21 - core language
- Spring Boot 3 - application framework
- Spring Security - authentication and authorization
- RestTemplate - HTTP request proxying

**DevOps**

- Docker - containerization
- Docker Compose - multi-container orchestration

## ✨ Features

### 🔐 Authentication & Authorization

- Access token validation (signature, `exp`, `type=access`)
- Extracting `userId` and `role` from the token
- Role-based authorization at the Gateway level (`hasRole("ADMIN")` for protected paths)
- `permitAll` for `/api/v1/auth/register`, `/login`, `/refresh`
- `AuthenticationEntryPoint` → 401 JSON
- `AccessDeniedHandler` → 403 JSON

### 🚦 Routing

- Proxying all requests to the corresponding services
- Forwarding `X-User-Id` and `X-User-Role` headers
- **Stripping** client-supplied `Authorization`, `X-User-Id`, `X-User-Role`, `Host`, `Content-Length`, `Cookie`, and hop-by-hop headers
- Service error propagation (forwards `message` from the response)
- Consistent error format when a service is unavailable (503)

### 🛡️ Security

- **`X-User-Id` spoofing is impossible** - the client header is stripped before proxying
- **Token `type` check** - refresh tokens are rejected on protected endpoints
- **Signature validation** - tokens signed with a different secret are rejected
- **Roles** - `hasRole("ADMIN")` for admin endpoints

## 🛠️ Quick Start

### Prerequisites

- Docker
- Docker Compose

### Run with Docker Compose

```bash
docker compose up --build
```

Gateway will be available at **`http://localhost:8000`**. 
Swagger path - `/swagger-ui.html`.

## 📚 API Endpoints

Gateway has **no endpoints of its own** - it proxies requests to services.

### Proxied Paths

| Path pattern                | Service         | Access              |
|-----------------------------|-----------------|---------------------|
| `/api/v1/auth/register`     | auth-service    | Public              |
| `/api/v1/auth/login`        | auth-service    | Public              |
| `/api/v1/auth/refresh`      | auth-service    | Public              |
| `/api/v1/auth/logout`       | auth-service    | Authenticated       |
| `/api/v1/users/**` (GET)    | user-service    | ADMIN               |
| `/api/v1/users/**` (DELETE) | user-service    | ADMIN               |
| `/api/v1/wallets/**`        | wallet-service  | Authenticated       |
| `/api/v1/orders/**`         | order-service   | Authenticated       |
| `/api/v1/payments/**`       | payment-service | Authenticated       |

**Internal endpoints** (`/users/create`, `/orders/update`, `/payments/place`) are **not proxied** through the Gateway - they are only reachable inside the Docker network for service-to-service communication.

## 📦 Project Structure

```
src/main/java/ru/merezh/gateway/
├── config/                    # Spring configuration (RestTemplate, SecurityConfig)
├── controller/                # GatewayController (proxying)
├── security/                  # JwtFilter, JwtValidateService
│   ├── dto/                   # JwtUserDto
│   └── service/               # JwtValidateService
└── exception/                 # Error handlers
```

## 🔒 Security

- **All requests go through the Gateway.** Internal services are not exposed externally.
- **JWT validation** - signature, `exp`, `type=access`.
- **`X-User-Id` / `X-User-Role` are stripped** from client requests.
- **Role-based authorization** - `hasRole("ADMIN")` for admin paths.
- **`AuthenticationEntryPoint`** - 401 JSON for unauthenticated requests.
- **`AccessDeniedHandler`** - 403 JSON for unauthorized requests.
- **Internal endpoints are not proxied** - `/users/create`, `/orders/update`, `/payments/place`.
- **Only the Gateway port is mapped to the host** - other services stay inside `dbnet`.

