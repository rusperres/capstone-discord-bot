# 🤖 Capstone Backend Service (Discord Bot & REST API)

A powerful dual-purpose backend service designed for project management, ticket tracking, and role-based permissions. This service integrates a Discord bot for real-time interaction and a REST API for frontend applications, streamlining the workflow between Project Managers, Developers, and QA testers.

## 🚀 Getting Started

### Prerequisites

To build and run this project, you need the following installed:

*   **Java JDK 15** or higher
*   **Maven** (for dependency management and building)
*   **SQLite** (for the local database)

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd capstone-backend
   ```

2. **Configure environment variables:**

   Create a `.env` file in the root directory and add your credentials.  
   Refer to `.env.example` for all required variables:

   ```env
   # Discord Bot Configuration
   DISCORD_TOKEN=your_bot_token_here
   GUILD_ID=your_target_guild_id
   TICKETS_DIR=tickets/

   # OAuth2 Configuration (for REST API Auth)
   DISCORD_CLIENT_ID=your_client_id
   DISCORD_CLIENT_SECRET=your_client_secret
   DISCORD_REDIRECT_URI=http://localhost:8080/api/auth/callback
   ```

   **To get a bot token:**

   1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
   2. Create a new application
   3. Go to the **Bot** section and click **Add Bot**
   4. Copy the token and paste it into `.env`

3. **Invite the bot to your server:**

   1. In the Developer Portal, go to **OAuth2 → URL Generator**
   2. Select these scopes:
      - `bot`
      - `applications.commands`

   3. Select these permissions:
      - `Manage Channels`
      - `Manage Threads`
      - `Send Messages`
      - `Embed Links`

   4. Copy the generated URL and open it in your browser
   5. Select your server and authorize the bot
   6. Enable all **Privileged Gateway Intents** in the **Bot** section
   7. Add your redirect URL in the **OAuth2** section:

      ```txt
      http://localhost:8000/api/auth/callback
      ```

4. **Build the project:**
   ```bash
   mvn clean install
   ```

5. **Run the service:**
   ```bash
   mvn exec:java -Dexec.mainClass="org.example.Main"
   ```

   Or use the play button in IntelliJ.

   This will start both the Discord bot and the REST API server.

## 🌐 REST API

The backend exposes a REST API on port `8080` by default. This API is used by the frontend application to manage tickets, users, and authentication.

### Key Endpoints
- `GET /api/tickets` - Retrieve all tickets
- `GET /api/stats` - Retrieve system statistics
- `GET /api/profile` - Get current user profile
- `POST /api/auth/login` - Authenticate user

For full API documentation, including request/response formats and authentication details, please refer to:
👉 **[API Documentation](api_documentation.md)**

---

## 📋 Discord Roles & Permissions

This system uses a strict role system where each user can hold only **one** role at a time, synchronized between Discord and the database.

### 🔧 Project Manager (Admin)
*   **/load-tickets** - Load tickets into channels
*   **/rebuild-db** - Rebuild database from existing threads
*   **/claim** - Claim tickets (like Dev)
*   **/resolved** - Submit for review (like Dev)
*   **/reviewed** - Approve tickets (like QA)
*   **/closed** - Close tickets
*   **Permissions:** Can do EVERYTHING. Assigned the **Discord Project Manager** role.

### 👨‍💻 Developer
*   **/claim** - Claim tickets
*   **/resolved** - Submit for review
*   **/closed** - Close tickets
*   **Features:** View dev leaderboard. Assigned the **Discord Developer** role.

### 🔍 QA
*   **/reviewed** - Approve tickets
*   **/closed** - Close tickets
*   **Features:** View QA leaderboard. Assigned the **Discord QA role**.

---

## 📝 Role System Logic
> [!IMPORTANT]
> **ONE role per user only.**
> When you set a new role, your old role is automatically replaced. The Project Manager has all permissions and acts as the system administrator.

---

## 🗄️ Database
The service uses a local **SQLite** database (`database.db`) to persist ticket states, user roles, and performance metrics. This database is shared between the Discord bot and the REST API.

---

## 🛠️ Built With
*   [JDA (Java Discord API)](https://github.com/discord-jda/JDA) - Discord integration
*   [HttpServer](https://docs.oracle.com/en/java/javase/15/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) - Lightweight REST server
*   [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) - Database management
*   [dotenv-java](https://github.com/cdimascio/dotenv-java) - Environment configuration
*   [Commonmark](https://github.com/commonmark/commonmark-java) - Markdown parsing
