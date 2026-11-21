# OliveSmart — Development Conventions

# Project scope

#### First end-to-end workflow

1. **Authentication**  
   User registers and logs in.
2. **Workspace**  
   User creates a workspace.
3. **Parcel management**  
   User creates a parcel and draws its boundaries on the map.
4. **Analysis**  
   User selects an analysis methodology and launches an analysis.
5. **ML inference**  
   Analysis Service communicates with the Python service.
6. **Results**  
   User sees predictions and their geographical locations.

---

# Ports

| Component | Port |
|---|---:|
| Frontend | 3000 |
| Gateway | 8080 |
| Eureka | 8761 |
| User Service | 8081 |
| Workspace Service | 8082 |
| Zone Service | 8083 |
| Analysis Service | 8084 |
| DL Service | 8000 |

---

# A few rules we all follow to keep the code consistent across the project...

## 1. Unified API Response

All Spring Boot services must use the shared `api-contract` dependency when returning JSON API responses.

**Successful response:**

```json
{
  "success": true,
  "message": "Workspace retrieved successfully",
  "data": {
    "id": 1,
    "name": "My Workspace"
  },
  "timestamp": "2026-11-21T10:30:00Z",
  "error": null
}
```

**Failed response:**

```json
{
  "success": false,
  "message": "Workspace not found",
  "data": null,
  "timestamp": "2026-11-21T10:30:00Z",
  "error": {
    "code": "WORKSPACE_NOT_FOUND",
    "details": null
  }
}
```

### Rules

- Always use `ApiResponse<T>` for standard JSON API responses.
- `data` contains the actual response DTO.
- `data` must be `null` on failure.
- `error` must be `null` on success.
- Always provide a meaningful message.
- Use appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 409, 500, etc.).
- Use stable error codes rather than relying on error messages.

Exceptions: 204 No Content, file downloads and streaming responses don't need this envelope.

---

## 2. Using the Shared Contract

The Java contract is located in:

`backend/api-contract`

Import it into your service:

```java
import ma.olivesmart.contract.ApiResponse;
import ma.olivesmart.contract.ApiError;
```

Use it instead of creating your own response wrapper.

The frontend and Python service should follow the same response specification using their own TypeScript and Python definitions. They do NOT import the Java module.

### TypeScript Contract

The TypeScript contract is located in:

`frontend/olivesmart-client/types/api/`

It contains:

**`api-error.ts`**

```typescript
export interface ApiError {
  code: string;
  details: unknown | null;
}
```

**`api-response.ts`**

```typescript
import type { ApiError } from "./api-error";

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  timestamp: string;
  error: ApiError | null;
}
```

### Rules

- Always use `ApiResponse<T>` when typing standard JSON API responses.
- `T` represents the actual response data type.
- Define feature-specific interfaces for response data instead of putting every entity inside the shared API contract.
- Use `ApiResponse<Workspace>`, `ApiResponse<User>`, `ApiResponse<Parcel[]>`, etc.
- Use `ApiResponse<null>` when an endpoint has no response data but still returns the standard envelope.
- `timestamp` is an ISO 8601 date-time string.
- Do NOT create duplicate API response or error interfaces in individual features.
- Keep the TypeScript contract synchronized with the Java contract.

Example:

```typescript
import type { ApiResponse } from "@/types/api/api-response";

interface Workspace {
  id: number;
  name: string;
}

const response: ApiResponse<Workspace> = {
  success: true,
  message: "Workspace retrieved successfully",
  data: {
    id: 1,
    name: "My Workspace"
  },
  timestamp: new Date().toISOString(),
  error: null
};
```

---

## 3. Layer Responsibilities

Keep responsibilities separated.

- **Presentation:** Controllers, HTTP requests, DTOs and API responses.
- **Application:** Use cases and business operation orchestration.
- **Domain:** Business rules, entities and business exceptions.
- **Infrastructure:** Database access, external APIs and technical implementations.

Important:

- Do NOT return `ApiResponse` from every internal method.
- Application methods return domain objects or application-specific results.
- Repositories return persistence results.
- Controllers convert application results into API responses.
- Business logic must not depend on HTTP.

---

## 4. Exception Handling

Use custom exceptions when appropriate.

Examples:

- `WorkspaceNotFoundException`
- `AnalysisNotFoundException`
- `InvalidParcelException`

Keep business exceptions in the relevant feature's domain/application packages.

Rules:

- Do NOT scatter try/catch blocks throughout controllers.
- Do NOT expose stack traces or internal technical details to API consumers.
- Convert exceptions into the unified error response.
- Always return an appropriate HTTP status code.

---

## 5. Microservice Communication

When services communicate with each other:

- Use DTOs, not another service's database entities.
- Respect the agreed API contracts.
- Use Eureka service discovery for Spring-to-Spring communication.
- Do not hardcode service IP addresses or ports.
- Keep each service responsible for its own database and business logic.

The API Gateway forwards responses. It must not wrap an already wrapped response in another `ApiResponse`.

---

## 6. General Rules

1. Keep naming and response structures consistent.
2. Don't duplicate shared classes.
3. Don't put business logic in controllers.
4. Don't add unnecessary abstractions or modules.
5. When changing a shared contract, communicate the change to the team.

**The goal:** consistent APIs, clear responsibilities, and services that can evolve independently.