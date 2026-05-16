# API Endpoints Documentation

This document explicitly maps out all available REST API endpoints exposed by the backend, including path parameters, query parameters, request bodies, and authentication requirements.

> [!IMPORTANT]
> **Authentication Status:** The `/api/auth/login` and `/api/auth/callback` endpoints handle Discord OAuth2 generation and verification, setting a `sessionId` cookie on success.
> **All other endpoints** (`/api/tickets/*`, `/api/profile`, and `/api/user/*`) **strictly enforce** this `sessionId` cookie validation. Requests without a valid cookie will return a `401 Unauthorized` response.

---

## Base URL
All endpoints are relative to the base URL: `http://localhost:8080/api`

---

## Authentication Endpoints ([AuthController](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/api/AuthController.java#17-138))

### `GET /auth/login`
Initiates the Discord OAuth2 authorization flow. Generates a temporary state and redirects the client to Discord.
- **Query Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The user's ID string starting the session (e.g., `?id=12345`).
- **Response:**
  - `302 Found`: Redirects to the Discord authorization URL.

### `GET /auth/callback`
Handles the redirect back from Discord after user authorization.
- **Query Parameters:**
  - `code` (required): The OAuth token exchange code provided by Discord.
  - `state` (required): The CSRF verification state verified against the backend.
- **Response:**
  - `200 OK`: `{"authenticated": true}` and a `Set-Cookie` header for `sessionId`.
  - `400/401/403 Error`: Authentication failure.

---

## User Endpoints ([UserController](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/api/UserController.java#25-200))

### `GET /profile`
Retrieves the profile information and leaderboard scores for a specific user.
- **Query Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord user ID (e.g., `?id=1176415109585842258`).
- **Response:**
  - `200 OK`: User JSON containing userId, username, roleName, devScore, and qaScore.

### `GET /user/members`
Fetches a leaderboard list.
- **Query Parameters:**
  - `type` (optional): The type of leaderboard score. Accepts `dev` or `qa`. Defaults to `dev`.
- **Response:**
  - `200 OK`: Array of leaderboard objects `[{"userId": "123", "score": 5}, ...]`.

### `PATCH /user/{id}`
Updates a user's role. This synchronizes the DB and assigns the role in the connected Discord server.
- **Path Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord user ID.
- **Request Body (JSON):**
  - `role` (required): The role name string to assign (e.g., `{"role": "QA"}`).

---

## Ticket Endpoints ([TicketController](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/api/TicketController.java#27-253))

### `POST /tickets/load`
Parses Markdown tickets from a specified folder, saves them to the database, and creates corresponding Discord threads in a targeted text channel.
- **Request Body (JSON):**
  - `folder` (required): The relative folder name where ticket Markdown files are stored (e.g., `"tickets"`).
  - `channelId` (required): The numeric Discord Text Channel ID where the ticket threads will be created. Example body: `{"folder": "tickets", "channelId": 1234567890123456}`.
- **Response:**
  - `200 OK`: JSON success message indicating the number of tickets loaded.
  - `400 Bad Request`: Validation failure for missing folder, or invalid channelId.

### `GET /tickets/list`
Retrieves a list of all active tickets (tickets that do not have `CLOSED` status).
- **Response:**
  - `200 OK`: JSON array of detailed ticket objects.

### `GET /tickets/{id}`
Retrieves details for a specific ticket by its Discord thread ID.
- **Path Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord thread ID.

### `PATCH /tickets/{id}/claim`
Assigns a developer to a specific ticket.
- **Path Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord thread ID.
- **Request Body (JSON):**
  - `userId` (required): The numeric ID of the user claiming the ticket (e.g., `{"userId": 123456789}`).

### `PATCH /tickets/{id}/resolve`
Marks a ticket as resolved and submits a Pull Request URL for review.
- **Path Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord thread ID.
- **Request Body (JSON):**
  - `prUrl` (required): The GitHub/GitLab PR link (e.g., `{"prUrl": "https://github.com/..."}`).

### `PATCH /tickets/{id}/close`
Closes a ticket completely.
- **Path Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord thread ID.

### `PATCH /tickets/{id}/review`
Transitions a ticket into the `IN_REVIEW` status.
- **Path Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord thread ID.

### `PATCH /tickets/{id}/demote`
Demotes a ticket back into the `OPEN` unassigned status.
- **Path Parameters:**
  - [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47) (required): The discord thread ID.
