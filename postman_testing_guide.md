# Postman Manual Testing Guide (No Frontend)

This guide walks you through testing the Discord OAuth flow using Postman and your browser.

## 1. Setup Environment in Postman

Create a new Environment in Postman and add these variables:

| Variable | Value (Example) |
| :--- | :--- |
| `BASE_URL` | `http://localhost:8080/api` |
| `USER_ID` | `12345` (any test ID) |
| `CALLBACK_URL` | `http://localhost:8080/api/auth/callback` |

## 2. The Authentication Flow

Since the flow requires Discord's authorization page (which uses browser cookies and UI), you must start in a browser.

### Step A: Initiate Login
1. Open your browser and go to:
   `http://localhost:8080/api/auth/login?id=12345`
2. You will be redirected to Discord.
3. Log in (if needed) and click **Authorize**.

### Step B: Capture the Callback
1. After clicking Authorize, Discord will redirect you back to:
   `http://localhost:8080/api/auth/callback?code=...&state=...`
2. Since your backend is running, you should see a JSON response: `{"authenticated":true}`.
3. **Crucial:** In your browser, open Developer Tools (F12) -> Application -> Cookies -> `http://localhost:8080`.
4. Copy the value of the `sessionId` cookie.

## 3. Testing Authenticated Endpoints in Postman

Now that you have a `sessionId`, you can test protected endpoints in Postman.

### Step C: Configure Session in Postman
1. Open Postman.
2. Click on **Cookies** (top right, under the Send button).
3. Search for `localhost`.
4. Add a new cookie or edit the existing one:
   - Domain: `localhost`
   - Path: `/`
   - Name: `sessionId`
   - Value: [(Paste the value you copied from the browser)](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/Main.java#18-75)
5. Click **Save**.

### Step D: Call Protected APIs

Ensure the `sessionId` cookie is present for the requests below. If not, they will return `401 Unauthorized`.

#### User & Profile Endpoints

**1. Get User Profile**
- **Method:** `GET`
- **URL:** `{{BASE_URL}}/profile?id={{USER_ID}}`
- **Expected Result:** `200 OK`
```json
{
  "userId": "12345",
  "username": "YourDiscordName",
  "roleName": "DEVELOPER",
  "devScore": 10,
  "qaScore": 5
}
```

**2. Get Leaderboard**
- **Method:** `GET`
- **URL:** `{{BASE_URL}}/user/members?type=dev` (type can be `dev` or `qa`)
- **Expected Result:** `200 OK`
```json
[
  {"userId":"12345","score":10},
  {"userId":"67890","score":8}
]
```

**3. Update User Role**
- **Method:** `PATCH`
- **URL:** `{{BASE_URL}}/user/{{USER_ID}}`
- **Body (raw JSON):**
```json
{
  "role": "QA"
}
```
- **Expected Result:** `200 OK` with `{"message":"User role updated successfully..."}`

#### Ticket Endpoints

Assume `{{TICKET_ID}}` is the Discord Thread ID for the ticket.

**0. Load Tickets via API**
- **Method:** `POST`
- **URL:** `{{BASE_URL}}/tickets/load`
- **Body (raw JSON):**
```json
{
  "folder": "test",
  "channelId": 123456789012345678
}
```
*(Replace `123456789012345678` with a valid channel ID accessible to your bot)*
- **Expected Result:** `200 OK` with `{"message":"Loaded X new tickets."}`

**0.5. Rebuild Database from Discord Threads**
- **Method:** `POST`
- **URL:** `{{BASE_URL}}/tickets/rebuild`
- **Body (raw JSON):**
```json
{
  "channelId": 123456789012345678
}
```
*(Scans the given channel's threads and rebuilds ticket records in the DB from their names/statuses)*
- **Expected Result:** `200 OK` with `{"message":"Database rebuilt from X threads."}`

**1. List All Active Tickets**
- **Method:** `GET`
- **URL:** `{{BASE_URL}}/tickets/list`
- **Expected Result:** `200 OK` with JSON array of ticket details.

**2. Get Specific Ticket Details**
- **Method:** `GET`
- **URL:** `{{BASE_URL}}/tickets/{{TICKET_ID}}` (supports both UUID ticket IDs and numeric Discord thread IDs)
- **Expected Result:** `200 OK` with JSON ticket details (status, title, etc).

**3. Claim a Ticket**
- **Method:** `PATCH`
- **URL:** `{{BASE_URL}}/tickets/{{TICKET_ID}}/claim`
- **Body (raw JSON):**
```json
{
  "userId": 12345
}
```
- **Expected Result:** `200 OK` with `{"message":"Ticket claimed successfully..."}`

**4. Resolve a Ticket & Submit PR**
- **Method:** `PATCH`
- **URL:** `{{BASE_URL}}/tickets/{{TICKET_ID}}/resolve`
- **Body (raw JSON):**
```json
{
  "prUrl": "https://github.com/my-repo/pull/1"
}
```
- **Expected Result:** `200 OK` with `{"message":"Ticket submitted for review..."}`

**5. Adjust Ticket Statuses**
You can forcibly transition a ticket state using these PATCH endpoints (no body required):
- `PATCH {{BASE_URL}}/tickets/{{TICKET_ID}}/close`
- `PATCH {{BASE_URL}}/tickets/{{TICKET_ID}}/review`
- `PATCH {{BASE_URL}}/tickets/{{TICKET_ID}}/demote`
- **Expected Result:** `200 OK` with success message.

## 4. Troubleshooting

- **401 Unauthorized:** Your `sessionId` cookie is missing or invalid. Repeat Steps A and B to get a new cookie.
- **400 Bad Request:** Missing required parameters in URL or Body (e.g. `userId` vs [id](file:///mnt/data/Workspaces/Projects/capstone-discord-bot/src/main/java/org/example/BotConfig.java#44-47)). Double-check the exact payload format above.
- **404 Not Found:** Invalid API endpoint route or ID that does not exist in DB/Discord.
