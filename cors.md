# Walkthrough - Remote Access Setup

I have updated the backend to support remote access from other devices on your phone hotspot. These changes include CORS support and dynamic redirect URIs.

## Changes Made

### 1. CORS Headers
All backend controllers ([TicketController](file:///mnt/data/Workspaces/Projects/OOPCapstoneTogether/backend/src/main/java/org/example/api/TicketController.java#32-553), [UserController](file:///mnt/data/Workspaces/Projects/OOPCapstoneTogether/backend/src/main/java/org/example/api/UserController.java#24-244), [AuthController](file:///mnt/data/Workspaces/Projects/OOPCapstoneTogether/backend/src/main/java/org/example/api/AuthController.java#17-194)) now include the following headers for all responses:
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type, Cookie`
- `Access-Control-Allow-Credentials: true`

This allows the browser on your second laptop to safely communicate with the backend on your primary laptop.

### 2. Dynamic Redirect URIs
The [AuthController](file:///mnt/data/Workspaces/Projects/OOPCapstoneTogether/backend/src/main/java/org/example/api/AuthController.java#17-194) now dynamically constructs the Discord redirect URI based on the `Host` header of the incoming request. This means if you access your primary laptop via its IP (e.g., `192.168.x.x`), the OAuth flow will correctly redirect back to that IP instead of `localhost`.

## How to Set It Up

### Step 1: Find your Primary Laptop's IP
On Laptop 1 (running the backend), run:
```bash
hostname -I
```
Look for an IP like `192.168.x.x` (usually the first one if you are on a hotspot).

### Step 2: Update Discord Developer Portal
You must add the IP-based redirect URI to your [Discord Application Dashboard](https://discord.com/developers/applications):
1. Go to **OAuth2** -> **General**.
2. Add a new Redirect: `http://[IP OF LAPTOP 1]:8080/api/auth/callback`
3. Click **Save Changes**.

### Step 3: Run the Backend
Start your backend on Laptop 1 as usual.

### Step 4: Connect from Laptop 2
On your second laptop, when the frontend login screen appears:
1. Enter the Backend URL as: `http://[IP OF LAPTOP 1]:8080/api`
2. Attempt to login.

## Troubleshooting
- **Firewall**: Ensure Laptop 1 allows incoming traffic on port `8080`.
- **Hotspot Isolation**: If they cannot ping each other, check if "Client Isolation" or "AP Isolation" is enabled on your phone hotspot settings.
