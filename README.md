# ManHunt 🏹

🌐 **Language / 언어**: **English** | [한국어 (Korean)](README_ko.md)

> A robust, modern **Minecraft Paper plugin** for the Manhunt minigame written in **Kotlin**.  
> The Runner(s) must defeat the Ender Dragon, while the Hunters track and eliminate all Runners.

---

## ✨ Key Features

- 🧭 **Craftable Real-Time Tracker Compass**
  - Compasses are **not** automatically granted; hunters must craft a vanilla compass.
  - **Right-clicking** a normal compass transforms it into a unique `Runner Tracking Compass` (limited to 1 per hunter).
  - Automatically updates the compass target (`compassTarget`) to the nearest alive runner in the same world every second and upon right-click.
- 💓 **Thrilling Heartbeat Proximity Sensor**
  - When a hunter enters within range (default: 50 blocks) of a runner, the runner receives Warden heartbeat sound effects, note block bass cues, and an action bar warning.
- ❄️ **Robust Death & Freeze System**
  - **Hunter Death**: Re-spawns at the death location in a frozen state (strictly blocks XYZ movement caused by water currents, knockbacks, or pushing, while allowing camera rotation) until the respawn countdown expires.
  - **Runner Death**: Immediately switches to `SPECTATOR` mode if other runners are still alive. Automatically restores all players to Survival mode once the game finishes.
- 🛡️ **Concurrency & Edge Case Resilience**
  - Uses a **single global tick task** rather than disjointed player-bound loops (`while` + `wait`).
  - Tracks player state by **`UUID`** instead of transient `Player` objects, safely handling **server disconnects, crashes, and rejoins** without losing freeze countdowns or corrupting game states.
- ⚙️ **In-Game Settings GUI (`/manhunt option`)**
  - An intuitive 3-row chest GUI to configure the heartbeat detection radius, hunter start delay, hunter respawn delay, and runner elimination mode in real-time.
- 🌐 **Dual-Language Command Tree (English & Korean)**
  - Fully supports both `/manhunt` and `/맨헌트` with identical logic and intelligent tab completion.

---

## 📜 Commands & Permissions

### Permissions
* **Node**: `manhunt.admin`
* **Default**: `OP`

### Command List
Both `/manhunt` and `/맨헌트` share the exact same underlying command handler. English and Korean subcommands are fully interchangeable.

| English Command | Korean Command | Description |
|---|---|---|
| `/manhunt start` | `/맨헌트 시작` | Starts the game (all non-runners automatically become hunters). |
| `/manhunt stop` | `/맨헌트 종료` | Force stops the active game and resets player states. |
| `/manhunt runner <player>` | `/맨헌트 러너 <플레이어>` | Registers or unregisters a player as a runner. |
| `/manhunt option` | `/맨헌트 설정` | Opens the in-game configuration GUI. |

> 💡 **Smart Tab Completion**: Typing `/manhunt ` automatically suggests English subcommands, while `/맨헌트 ` suggests Korean subcommands.

---

## 🎮 Game Rules & Win Conditions

1. **Setup**:
   - An admin designates runner(s) using `/manhunt runner <player>`.
   - Start the game using `/manhunt start`. All other connected players automatically become **hunters**.
2. **Grace Period**:
   - Hunters are frozen in place during the configured start delay (default: 60s) before being released.
3. **Win Conditions**:
   - **Runners Win**: Slay the Ender Dragon in the End dimension.
   - **Hunters Win**:
     - `All Runners Dead Mode (Default)`: All runners are eliminated. (Fallen runners spectate until the match concludes)
     - `One Runner Dead Mode`: The hunters win immediately upon the death of any single runner.

---

## 📦 Building & Installation

### Requirements
- **Java**: JDK 25 or higher
- **Server Platform**: Paper 26.2 or higher compatible builds

### Build
```bash
# Build the ShadowJar using Gradle Wrapper
./gradlew shadowJar
```

### Installation
Upon a successful build, check the `build/libs/` directory.
- ⚠️ You **must** deploy the jar file ending with **`-all.jar`** (e.g., `ManHunt-1.0-SNAPSHOT-all.jar`) into your server's `plugins/` directory. This is the Fat JAR bundled with the required Kotlin runtime libraries.
- Start or restart your Paper server to load the plugin.

---

## 📄 License & Author
- **Author**: SDXMQ
