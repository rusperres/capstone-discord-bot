# 🤖 Capstone Discord Bot

A powerful Discord bot designed for project management, ticket tracking, and role-based permissions. It helps streamline the workflow between Project Managers, Developers, and QA testers.

## 🚀 Getting Started

### Prerequisites

To build and run this project, you need the following installed:

*   **Java JDK 15** or higher
*   **Maven** (for dependency management and building)
*   **SQLite** (for the local database)

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd capstone-discord-bot
    ```

2.  **Configure environment variables:**
    Create a `.env` file in the root directory and add your bot credentials:
    ```env
    DISCORD_TOKEN=your_bot_token_here
    GUILD_ID=your_target_guild_id
    ```

3.  **Build the project:**
    ```bash
    mvn clean install
    ```

4.  **Run the bot:**
    ```bash
    mvn exec:java -Dexec.mainClass="org.example.Main"
    ```

---

## 📋 Roles & Permissions

This bot uses a strict role system where each user can hold only **one** role at a time.

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

## 🛠️ Built With
*   [JDA (Java Discord API)](https://github.com/discord-jda/JDA) - Discord integration
*   [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) - Database management
*   [dotenv-java](https://github.com/cdimascio/dotenv-java) - Environment configuration
*   [Commonmark](https://github.com/commonmark/commonmark-java) - Markdown parsing