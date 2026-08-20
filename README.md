# E-Ticket

A Support Desk Ticket REST API. Agents create and manage tickets; viewers get read-only access to their organization's tickets.

## Features

- JWT login with role-based access: AGENT, VIEWER, ADMIN
- Tickets are scoped to your organization (you only see your own)
- Status lifecycle: OPEN → IN_PROGRESS → RESOLVED → CLOSED
- Agent invitation flow for viewers (emailed invite links)
- Swagger UI documentation
- Scheduled job that flags overdue tickets

## Requirements

- Java 21 (JDK)
- Maven is bundled via the Maven wrapper — no need to install it

## Setup & Run

1. Copy `.env.example` to `.env` and fill in the values (see table below).
2. Start the app:

   ```
   .\mvnw.cmd spring-boot:run
   ```

3. Open http://localhost:8080

## Useful pages

- Swagger UI (API docs, try the endpoints): http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

## Run tests

```
.\mvnw.cmd test
```

## Environment variables

| Variable | What it is for |
| --- | --- |
| `ETICKET_JWT_SECRET` | Secret key used to sign login tokens. Use a long random string. |
| `ETICKET_MAIL_USERNAME` | SMTP username (Gmail) used to send invitation emails. |
| `ETICKET_MAIL_PASSWORD` | SMTP app password for that account. |
| `ETICKET_APP_BASE_URL` | Public base URL of the app, used in invitation email links (e.g. `http://localhost:8080`). |
| `ETICKET_MAIL_FROM_NAME` | Name shown as the sender of invitation emails. |

## API overview

All endpoints live under `/api/v1`. Authentication and ticket endpoints are public; everything else needs a JWT.

- **Register an agent** — `POST /api/v1/auth/register`. Creates your organization and its first agent, returns a JWT.
- **Log in** — `POST /api/v1/auth/login`. Returns a JWT.
- **Register a viewer** — `POST /api/v1/auth/register/viewer`. Creates a read-only account (no tickets until invited).
- **Accept an invitation** — `POST /api/v1/auth/invitations/accept`. Redeems an emailed invite link and joins an organization.
- **Tickets** — `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets`, `PUT /api/v1/tickets/{id}`, `PATCH /api/v1/tickets/{id}/status`, `DELETE /api/v1/tickets/{id}`. Anyone logged in can read; creating, updating, and deleting require AGENT or ADMIN.

The easiest way to try the API is the Swagger UI page — you can log in there and call the endpoints from your browser.