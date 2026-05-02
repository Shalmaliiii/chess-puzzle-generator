# Chess Puzzle Platform

A full-stack microservices-based chess puzzle platform that generates, serves, and tracks **"Mate in N"** chess puzzles. Users register, solve puzzles of varying difficulty, earn Elo ratings, and compete on a leaderboard — all powered by the Stockfish chess engine.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Why This Architecture?](#why-this-architecture)
4. [Component Deep Dive](#component-deep-dive)
   - [Frontend (React SPA)](#1-frontend--chess-puzzle-frontend)
   - [API Gateway](#2-api-gateway--chess-puzzle-gateway)
   - [User Service](#3-user-service--chess-puzzle-user-service)
   - [Puzzle Service](#4-puzzle-service--chess-puzzle-puzzle-service)
   - [Engine Service](#5-engine-service--chess-puzzle-generator-this-repo)
5. [Database Design & Data Storage](#database-design--data-storage)
6. [Kafka Event Architecture](#kafka-event-architecture)
7. [How Puzzle Generation Works](#how-puzzle-generation-works)
8. [Authentication & Authorization Flow](#authentication--authorization-flow)
9. [Puzzle Solving Flow — End to End](#puzzle-solving-flow--end-to-end)
10. [Elo Rating System](#elo-rating-system)
11. [How to Run the Platform](#how-to-run-the-platform)
12. [Inspecting Data — MongoDB, Kafka, Logs](#inspecting-data--mongodb-kafka-logs)
13. [API Reference](#api-reference)
14. [Repository Map](#repository-map)
15. [Future Enhancements](#future-enhancements)

---

## Project Overview

| Aspect | Detail |
|--------|--------|
| **Domain** | Chess tactics training (Mate-in-N puzzles) |
| **Architecture** | Microservices with event-driven communication |
| **Backend** | Java 21/25, Spring Boot 3.4/3.5, Gradle |
| **Frontend** | React 19, TypeScript, Vite, Tailwind CSS |
| **Database** | MongoDB 7 (separate database per service) |
| **Messaging** | Apache Kafka (Confluent 7.6) |
| **Chess Engine** | Stockfish (native OS process via UCI protocol) |
| **Auth** | JWT (access + refresh tokens), BCrypt password hashing |
| **Rating** | Elo rating system with difficulty-based expected scores |
| **Containerization** | Docker + Docker Compose (8 containers) |
| **CI/CD** | GitHub Actions per repository |

### Core User Flows

1. **Register → Login → Dashboard** — Create account, authenticate via JWT, view stats
2. **Solve Puzzles** — Interactive chessboard with drag-and-drop, real-time move validation
3. **Retry & Show Solution** — On failure, retry the same puzzle or watch the animated solution
4. **Rating Progression** — Elo rating adjusts based on puzzle difficulty and outcome
5. **Leaderboard** — Global ranking of players by Elo rating
6. **Admin Puzzle Generation** — Trigger bulk puzzle generation via Stockfish analysis

---

## System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                           FRONTEND                                   │
│                 React 19 + TypeScript + Vite                         │
│          react-chessboard · chess.js · Zustand · Recharts            │
│                        Port 3000                                     │
└───────────────────────────┬──────────────────────────────────────────┘
                            │ HTTP REST (all requests go through /api)
                            ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY                                   │
│              Spring Cloud Gateway (Reactive/Netty)                   │
│       JWT Validation · Route Matching · CORS · Rate Limiting         │
│                        Port 8080                                     │
└──────────┬──────────────────┬──────────────────┬─────────────────────┘
           │                  │                  │
    /api/auth/**       /api/puzzles/**     /api/engine/**
    /api/users/**              │                  │
           │                  │                  │
           ▼                  ▼                  ▼
┌──────────────────┐ ┌────────────────┐ ┌──────────────────────┐
│   USER SERVICE   │ │ PUZZLE SERVICE │ │   ENGINE SERVICE     │
│   Port 8081      │ │  Port 8082     │ │   Port 8083          │
│                  │ │                │ │                      │
│ • Registration   │ │ • Serve puzzle │ │ • Stockfish process  │
│ • Login (JWT)    │ │ • Validate     │ │ • Position analysis  │
│ • Profile/Stats  │ │   moves        │ │ • Mate detection     │
│ • Leaderboard    │ │ • Solution API │ │ • Puzzle generation  │
│ • Rating calc    │ │ • Seed data    │ │ • UCI protocol       │
│                  │ │ • Kafka events │ │ • Kafka events       │
└────────┬─────────┘ └───────┬────────┘ └──────────┬───────────┘
         │                   │                     │
         ▼                   ▼                     ▼
┌──────────────────┐ ┌────────────────┐ ┌──────────────────────┐
│   MongoDB        │ │   MongoDB      │ │   Stockfish Engine   │
│   chess_users    │ │   chess_puzzles│ │   (native binary)    │
└──────────────────┘ └────────────────┘ └──────────────────────┘
         │                   │                     │
         └───────────────────┼─────────────────────┘
                             ▼
              ┌─────────────────────────────┐
              │        APACHE KAFKA         │
              │                             │
              │  puzzle.generate   ──────▶  Engine consumes, generates puzzles
              │  puzzle.generated  ──────▶  Puzzle Service consumes, saves to DB
              │  puzzle.solved     ──────▶  User Service consumes, updates stats
              │  user.rating.update         (reserved for future use)
              │                             │
              │  Zookeeper (coordination)   │
              └─────────────────────────────┘
```

---

## Why This Architecture?

### Why Microservices Over a Monolith?

| Reason | Explanation |
|--------|-------------|
| **Independent scaling** | The Engine Service is CPU-bound (Stockfish analysis at depth 25 takes seconds per position). It needs horizontal scaling independently of the stateless User/Puzzle services. In a monolith, scaling the engine would mean scaling everything. |
| **Domain isolation** | Each service owns its domain: users (auth + profiles), puzzles (CRUD + validation), engine (chess analysis). Teams can work on each independently. A bug in puzzle validation won't crash the auth system. |
| **Technology flexibility** | Engine Service uses Java 25 (latest Stockfish bindings), while other services use Java 21 (LTS). The frontend is a completely separate React app. In a monolith, you're locked to one runtime version. |
| **Fault isolation** | If Stockfish crashes, the Engine Service restarts independently. Users can still log in, solve pre-generated puzzles, and view the leaderboard. In a monolith, an engine crash takes everything down. |
| **Database-per-service** | Each service has its own MongoDB database (`chess_users`, `chess_puzzles`). No shared tables, no cross-service joins, no accidental coupling. Each schema evolves independently. |

### Why MongoDB?

| Reason | Explanation |
|--------|-------------|
| **Schema flexibility** | Chess puzzles have varying structures — different solution lengths, tags, metadata. MongoDB's document model handles this naturally without migration scripts for every schema change. |
| **Embedded documents** | User stats (`totalSolved`, `accuracy`, `byDifficulty`, `recentPuzzles`) are nested objects — perfect for MongoDB's embedded document pattern. In SQL, this would require 3–4 joined tables. |
| **FEN storage** | FEN strings (e.g., `"r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"`) and solution arrays (`["h5f7"]`) are naturally JSON-like. MongoDB stores them as-is. |
| **Read-heavy workload** | The platform reads puzzles far more than it writes them. MongoDB's document model and indexing (by difficulty, status, solvedBy) optimize for this access pattern. |
| **No complex transactions** | Chess puzzle operations are single-document updates (validate move → update puzzle stats). No need for multi-table ACID transactions that would favor SQL. |

### Why Kafka?

| Reason | Explanation |
|--------|-------------|
| **Async puzzle generation** | Generating a puzzle requires Stockfish to analyze at depth 25 — this takes 2–10 seconds per position. Kafka decouples the request from execution: the Puzzle Service publishes a `puzzle.generate` event and responds immediately. The Engine Service processes it asynchronously. |
| **Backpressure handling** | If 100 users request puzzle generation simultaneously, Kafka queues the requests. The Engine Service processes them at its own pace without being overwhelmed. Without Kafka, the Engine would need to reject requests or timeout. |
| **Event-driven stats** | When a puzzle is solved, the `puzzle.solved` event flows to the User Service asynchronously. The user gets an immediate "Solved!" response while rating updates happen in the background. |
| **Retry semantics** | If the User Service is temporarily down when a `puzzle.solved` event fires, Kafka retains the message. When the service recovers, it processes the event — no data loss. |
| **Service decoupling** | Services communicate via events, not direct HTTP calls. The Puzzle Service doesn't need to know the User Service's URL or API contract — it just publishes events to a topic. |

### Why Spring Cloud Gateway?

| Reason | Explanation |
|--------|-------------|
| **Single entry point** | Frontend only needs to know one URL (`http://localhost:8080/api`). The gateway routes to the correct service based on path predicates. |
| **Centralized auth** | JWT validation happens once at the gateway. Individual services receive pre-validated `X-User-Id` and `X-User-Role` headers — they don't need their own JWT libraries. |
| **Header sanitization** | The gateway strips incoming `X-User-Id` / `X-User-Role` headers to prevent spoofing. Only headers injected by the gateway after JWT validation are trusted. |
| **Reactive stack** | Built on Netty + Project Reactor — non-blocking I/O for high throughput with minimal threads. |

---

## Component Deep Dive

### 1. Frontend — `chess-puzzle-frontend`

**Tech Stack:** React 19, TypeScript, Vite, Tailwind CSS, Zustand, chess.js, react-chessboard, Recharts, Axios

**Repository:** [github.com/Shalmaliiii/chess-puzzle-frontend](https://github.com/Shalmaliiii/chess-puzzle-frontend)

#### Source Structure

```
src/
├── App.tsx                    # Root router: defines all routes + auth orchestration
├── main.tsx                   # React DOM mounting point
├── index.css                  # Global CSS + Tailwind + CSS custom properties
├── types/index.ts             # TypeScript interfaces (User, Puzzle, PuzzleDifficulty, etc.)
│
├── components/
│   ├── Navbar.tsx             # Top nav bar: dynamic links based on auth state + rating display
│   ├── Layout.tsx             # Page wrapper: Navbar + main content + footer
│   ├── ProtectedRoute.tsx     # Auth gate: redirects to /login if not authenticated
│   ├── DifficultyBadge.tsx    # Color-coded difficulty label (BEGINNER=green, MASTER=red)
│   ├── PuzzleTimer.tsx        # Live stopwatch: starts on puzzle load, stops on solve/fail
│   ├── MoveHistory.tsx        # Displays list of moves with correct/incorrect indicators
│   └── LoadingSpinner.tsx     # Animated SVG spinner used during API calls
│
├── pages/
│   ├── LoginPage.tsx          # Email + password form → calls authService.login()
│   ├── RegisterPage.tsx       # Registration form with password validation (8+ chars, match)
│   ├── DashboardPage.tsx      # User stats: puzzles solved, accuracy, rating, streak, charts
│   ├── PuzzlePage.tsx         # Core puzzle UI: chessboard + validation + retry + show solution
│   ├── LeaderboardPage.tsx    # Ranked list of users by Elo rating with podium
│   └── AdminPage.tsx          # Admin panel: trigger puzzle generation, view stats
│
├── stores/
│   ├── authStore.ts           # Zustand store: JWT tokens, user info, login/logout/refresh
│   └── puzzleStore.ts         # Zustand store: current puzzle, validation, retry, solution
│
└── services/
    ├── api.ts                 # Axios instance: base URL, JWT interceptor, 401 refresh logic
    ├── authService.ts         # login(), register(), refreshToken(), getProfile()
    ├── puzzleService.ts       # getNextPuzzle(), validateMove(), solvePuzzle(), getSolution()
    ├── userService.ts         # getProfile(), getDashboard(), getLeaderboard()
    └── engineService.ts       # analyzePosition(), getEngineHealth()
```

#### Key Design Decisions

- **Zustand over Redux**: Minimal boilerplate, no action creators or reducers. State is just a plain object with setter functions. Two stores (auth + puzzle) keep concerns separated.
- **chess.js for local validation**: Before sending a move to the backend, chess.js validates it locally (legal move check). This gives instant feedback and avoids unnecessary API calls for illegal moves.
- **react-chessboard for drag-and-drop**: Renders an SVG chessboard with DnD Kit integration. The `onPieceDrop` callback receives source/target squares and triggers the validation pipeline.
- **Axios interceptor for JWT refresh**: When a 401 is received, the interceptor automatically calls `/api/auth/refresh` with the refresh token, updates the stored JWT, and retries the original request.

#### PuzzlePage.tsx — The Core Component

This is the most complex page. Here's what happens:

1. **Puzzle Selection**: User picks difficulty → `fetchPuzzle()` → API returns FEN string, mate-in-N, side to move
2. **Board Rendering**: FEN is loaded into chess.js and react-chessboard. Board orientation matches `sideToMove`.
3. **Move Validation**: User drags a piece → `onDrop()` → chess.js validates legality → API `POST /api/puzzles/{id}/validate` with UCI move → response says correct/incorrect
4. **Opponent Response**: If correct and not complete, API returns `opponentMove` → chess.js applies it → board updates → user makes next move
5. **Puzzle Complete**: When the last correct move is made, `puzzleComplete: true` → `solvePuzzle()` → rating update via Kafka
6. **Puzzle Failed**: Wrong move → board reverts to pre-move FEN → "Retry" and "Show Solution" buttons appear
7. **Retry**: Resets the board to the original FEN, clears move history, user can try again
8. **Show Solution**: Fetches solution line from API → animates each move sequentially with 1.2s delay → progress bar shows completion

### 2. API Gateway — `chess-puzzle-gateway`

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Cloud Gateway (Reactive), JJWT, Netty

**Repository:** [github.com/Shalmaliiii/chess-puzzle-gateway](https://github.com/Shalmaliiii/chess-puzzle-gateway)

#### Source Structure

```
src/main/java/com/.../gateway/
├── GatewayApplication.java           # Spring Boot entry point
├── filter/
│   ├── JwtAuthenticationFilter.java  # Global filter: validates JWT, injects X-User-Id/X-User-Role
│   └── HeaderSanitizationFilter.java # Strips incoming X-User-Id/X-User-Role to prevent spoofing
├── config/
│   └── GatewayErrorHandler.java      # Standardized JSON error responses for reactive streams
└── util/
    └── JwtUtil.java                  # JWT parsing: extracts claims (sub, role) from token
```

#### Route Configuration (`application-docker.yml`)

```yaml
spring.cloud.gateway.routes:
  - id: auth-service
    uri: http://user-service:8081
    predicates: Path=/api/auth/**       # No JWT required (public)

  - id: user-service
    uri: http://user-service:8081
    predicates: Path=/api/users/**      # JWT required

  - id: puzzle-service
    uri: http://puzzle-service:8082
    predicates: Path=/api/puzzles/**    # JWT required

  - id: engine-service
    uri: http://engine-service:8083
    predicates: Path=/api/engine/**     # JWT required
```

#### How JWT Authentication Works in the Gateway

```
Client Request → Gateway
  │
  ├── Is path in OPEN_ENDPOINTS? (/api/auth/**, /actuator/**)
  │   └── Yes → Pass through without validation
  │
  ├── Has Authorization: Bearer <token> header?
  │   └── No → Return 401 Unauthorized
  │
  ├── JwtUtil.parseToken(token)
  │   └── Invalid/expired → Return 401 Unauthorized
  │
  ├── Strip existing X-User-Id, X-User-Role headers (anti-spoofing)
  │
  ├── Inject X-User-Id: <userId>, X-User-Role: <role> from JWT claims
  │
  └── Forward to downstream service
```

### 3. User Service — `chess-puzzle-user-service`

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Data MongoDB, Spring Security, BCrypt, JJWT, Kafka

**Repository:** [github.com/Shalmaliiii/chess-puzzle-user-service](https://github.com/Shalmaliiii/chess-puzzle-user-service)

#### Source Structure

```
src/main/java/com/.../user_service/
├── UserServiceApplication.java
├── config/
│   ├── SecurityConfig.java            # Disables CSRF, permits all (auth handled by gateway)
│   └── JwtAuthenticationFilter.java   # Parses JWT for direct service access (dev mode)
├── controller/
│   ├── AuthController.java            # POST /api/auth/register, /login, /refresh
│   └── UserController.java            # GET /api/users/me, /dashboard, /leaderboard
├── dto/
│   ├── request/  → LoginRequest, RegisterRequest, RefreshTokenRequest
│   └── response/ → LoginResponse, RegisterResponse, UserProfileResponse, LeaderboardEntry
├── exception/
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice: maps exceptions to HTTP responses
│   ├── DuplicateEmailException.java   # 409 Conflict
│   ├── InvalidCredentialsException.java # 401 Unauthorized
│   └── UserNotFoundException.java     # 404 Not Found
├── kafka/
│   ├── PuzzleSolvedConsumer.java      # Listens to 'puzzle.solved' → updates user stats + rating
│   └── PuzzleSolvedEvent.java         # DTO for deserialized Kafka message
├── model/
│   ├── User.java                      # MongoDB document: email, passwordHash, rating, stats
│   ├── UserRole.java                  # Enum: USER, ADMIN
│   ├── UserStats.java                 # Embedded: totalSolved, accuracy, streak, byDifficulty
│   ├── DifficultyStats.java           # Embedded: solved/attempted per difficulty level
│   └── RecentPuzzle.java              # Embedded: last 10 solved puzzles with time/difficulty
├── repository/
│   └── UserRepository.java            # Spring Data MongoDB: findByEmail, top players query
└── service/
    ├── UserService.java               # Core logic: register, login, profile, leaderboard
    ├── JwtService.java                # Generate/validate JWT tokens (access: 1hr, refresh: 7d)
    └── RatingService.java             # Elo rating calculation
```

#### How Registration Works

```
POST /api/auth/register { email, username, password }
  │
  ├── Check if email already exists → 409 if duplicate
  ├── Hash password with BCrypt (cost factor 10)
  ├── Create User document with defaults:
  │   • rating: 1200 (starting Elo)
  │   • role: USER
  │   • stats: { totalSolved: 0, accuracy: 0, currentStreak: 0 }
  ├── Save to MongoDB (chess_users.users collection)
  ├── Generate access JWT (1 hour) + refresh JWT (7 days)
  └── Return { id, email, username, token }
```

#### How Kafka Updates User Stats

When a puzzle is solved, the Puzzle Service publishes a `puzzle.solved` event. The User Service's `PuzzleSolvedConsumer` processes it:

```
Kafka: puzzle.solved → PuzzleSolvedConsumer
  │
  ├── Deserialize PuzzleSolvedEvent { userId, puzzleId, difficulty, timeMs, correct }
  ├── Load User from MongoDB
  ├── Calculate new Elo rating via RatingService
  ├── Update UserStats:
  │   • Increment totalSolved / totalAttempted
  │   • Recalculate accuracy (totalSolved / totalAttempted * 100)
  │   • Update currentStreak / bestStreak
  │   • Update averageSolveTimeMs
  │   • Update byDifficulty[difficulty].solved / .attempted
  ├── Add to recentPuzzles (max 10, FIFO)
  └── Save updated User to MongoDB
```

### 4. Puzzle Service — `chess-puzzle-puzzle-service`

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Data MongoDB, Kafka, Lombok

**Repository:** [github.com/Shalmaliiii/chess-puzzle-puzzle-service](https://github.com/Shalmaliiii/chess-puzzle-puzzle-service)

#### Source Structure

```
src/main/java/com/.../puzzle_service/
├── PuzzleServiceApplication.java
├── config/
│   ├── DataSeeder.java                # Seeds 15 verified puzzles on first startup
│   └── KafkaConfig.java              # Topic creation: puzzle.generate, puzzle.generated, puzzle.solved
├── controller/
│   └── PuzzleController.java         # REST endpoints: next, validate, solve, solution, stats
├── dto/
│   ├── MoveValidationRequest.java    # { move: "e2e4", moveNumber: 1 }
│   ├── MoveValidationResponse.java   # { correct, opponentMove, puzzleComplete, movesRemaining }
│   ├── PuzzleResponse.java           # { id, fen, sideToMove, mateIn, difficulty }
│   ├── PuzzleSolveRequest.java       # { timeMs: 3500 }
│   ├── PuzzleSolveResponse.java      # { message: "Puzzle solved successfully" }
│   ├── PuzzleStatsResponse.java      # { total, byDifficulty, byStatus }
│   ├── PuzzleGenerateRequest.java    # { difficulty: "INTERMEDIATE", count: 5 }
│   ├── PuzzleGenerateEvent.java      # Kafka event: trigger generation
│   ├── PuzzleGeneratedEvent.java     # Kafka event: new puzzle created
│   └── PuzzleSolvedEvent.java        # Kafka event: puzzle completed
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── NoPuzzlesAvailableException.java  # 404 when no unsolved puzzles remain
│   └── PuzzleNotFoundException.java      # 404 for invalid puzzle ID
├── kafka/
│   ├── PuzzleGenerateProducer.java   # Publishes to 'puzzle.generate'
│   ├── PuzzleGeneratedConsumer.java  # Listens to 'puzzle.generated' → saves new puzzles to DB
│   └── PuzzleSolvedProducer.java     # Publishes to 'puzzle.solved'
├── model/
│   ├── Puzzle.java                   # MongoDB document: fen, solutionLine, mateIn, difficulty, solvedBy
│   ├── PuzzleDifficulty.java         # Enum: BEGINNER, INTERMEDIATE, ADVANCED, MASTER
│   └── PuzzleStatus.java             # Enum: ACTIVE, ARCHIVED
├── repository/
│   └── PuzzleRepository.java         # Custom queries: findByDifficultyAndStatusAndSolvedByNotContaining
└── service/
    └── PuzzleService.java            # Core: getNextPuzzle, validateMove, solvePuzzle, getSolution
```

#### How Move Validation Works

This is the heart of the puzzle-solving logic:

```
POST /api/puzzles/{id}/validate { move: "h5f7", moveNumber: 1 }

1. Load puzzle from MongoDB (includes solutionLine: ["h5f7"])
2. Calculate expected move index:
   • Player moves are at even indices: moveNumber=1 → index 0, moveNumber=2 → index 2
   • Opponent moves are at odd indices: index 1, 3, 5...
3. Compare user's move with solutionLine[playerMoveIndex]
4. If WRONG:
   → Return { correct: false, puzzleComplete: false }
   → Frontend shows "Incorrect move. Puzzle failed."
5. If CORRECT:
   → Check if there's an opponent response at index+1
   → Check if there are more player moves after opponent
   → Return { correct: true, opponentMove: "g8h8", puzzleComplete: false, movesRemaining: 1 }
   → Frontend plays opponent move on board, waits for next user move
6. When puzzleComplete: true → Frontend calls POST /api/puzzles/{id}/solve
```

**Example — Mate in 2 (Scholar's Mate):**

```
FEN: "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
solutionLine: ["h5f7"]   (Qxf7#, checkmate in 1 move)

Move 1: User plays h5f7 (Qxf7#)
  → index 0: "h5f7" matches ✓
  → No opponent move (index 1 doesn't exist)
  → puzzleComplete: true
  → Frontend: "Puzzle solved!"
```

**Example — Mate in 2 (with opponent response):**

```
solutionLine: ["d5f6", "g7f6", "c4f7"]  (Nf6+ gxf6 Bxf7#)

Move 1: User plays d5f6 (Nf6+)
  → index 0: "d5f6" matches ✓
  → opponent move at index 1: "g7f6" → return opponentMove
  → puzzleComplete: false (more moves at index 2)

Move 2: User plays c4f7 (Bxf7#)
  → index 2: "c4f7" matches ✓
  → No more moves
  → puzzleComplete: true
```

#### Seed Data — 15 Verified Puzzles

The `DataSeeder` runs on first startup and loads 15 chess puzzles, all verified using chess.js to ensure legal moves and checkmate:

| Difficulty | Count | Mate In | Example Theme |
|-----------|-------|---------|---------------|
| BEGINNER | 5 | 1 | Back-rank mate, Scholar's mate, Queen mate |
| INTERMEDIATE | 5 | 2 | Knight fork, Queen sacrifice, Rook-Queen battery |
| ADVANCED | 5 | 3 | Queen sacrifice + follow-up, Bishop sacrifice, King hunt |

Every puzzle was validated by:
1. Loading FEN into chess.js
2. Playing each move in the solution line
3. Verifying `game.isCheckmate() === true` after the last move

### 5. Engine Service — `chess-puzzle-generator` (this repo)

**Tech Stack:** Java 25, Spring Boot 3.5, Stockfish (native process), UCI protocol, Kafka

**Repository:** [github.com/Shalmaliiii/chess-puzzle-generator](https://github.com/Shalmaliiii/chess-puzzle-generator)

#### Source Structure

```
src/main/java/com/.../puzzle_service/
├── PuzzleServiceApplication.java
├── controller/
│   └── EngineController.java          # REST: /api/engine/health, /analyze, /generate
├── dto/
│   ├── AnalysisRequest.java           # { fen, depth }
│   ├── AnalysisResponse.java          # { bestMove, evaluation, principalVariation }
│   ├── AnalysisResult.java            # Internal: parsed Stockfish output
│   ├── EngineHealthResponse.java      # { status, engineVersion, uptime }
│   ├── GeneratedPuzzle.java           # { fen, solutionLine, mateIn, difficulty, sideToMove }
│   ├── ValidateMoveRequest.java       # { fen, move }
│   ├── ValidateMoveResponse.java      # { valid, evaluation, bestMove }
│   ├── PuzzleGenerateEvent.java       # Kafka: inbound generation request
│   └── PuzzleGeneratedEvent.java      # Kafka: outbound generated puzzle
├── exception/
│   └── GlobalExceptionHandler.java
├── generator/
│   ├── PuzzleGeneratorService.java    # Orchestrates: iterate FEN pool → analyze → detect mate → classify
│   ├── FenGenerator.java             # Pool of ~40 known mate positions (M1, M2, M3, M4+)
│   ├── MateDetector.java             # Parses Stockfish "score mate N" → boolean isForcedMate
│   └── DifficultyClassifier.java     # M1=BEGINNER, M2=INTERMEDIATE, M3=ADVANCED, M4+=MASTER
├── kafka/
│   ├── PuzzleGenerateConsumer.java    # Listens to 'puzzle.generate' → runs PuzzleGeneratorService
│   └── PuzzleGeneratedProducer.java   # Publishes generated puzzles to 'puzzle.generated'
└── service/
    └── StockfishService.java          # Core: manages Stockfish OS process lifecycle + UCI I/O
```

#### Stockfish Integration Deep Dive

The `StockfishService` is the most critical component — it manages a native OS process:

```
@PostConstruct: startEngine()
  │
  ├── ProcessBuilder(stockfishPath).start()     # Spawn Stockfish binary
  ├── Wire stdin/stdout via BufferedReader/Writer
  ├── Send "uci" → wait for "uciok"             # UCI handshake
  ├── Send "isready" → wait for "readyok"        # Engine ready
  └── Log engine version (e.g., "Stockfish 17")

analyzePosition(fen, depth):
  │
  ├── Send "ucinewgame"                          # Reset engine state
  ├── Send "isready" → wait for "readyok"
  ├── Send "position fen <fen>"                   # Set board position
  ├── Send "go depth <depth>"                     # Start analysis
  │
  ├── Read output lines:
  │   ├── "info depth 25 score mate 2 pv h5f7 e8d8 c4f7"
  │   │   → Parse score: "M2" (mate in 2)
  │   │   → Parse PV: ["h5f7", "e8d8", "c4f7"]
  │   └── "bestmove h5f7 ponder e8d8"
  │       → Extract bestMove: "h5f7"
  │
  └── Return AnalysisResult { bestMove, evaluation: "M2", principalVariation, depth: 25 }

@PreDestroy: stopEngine()
  └── engineProcess.destroyForcibly()             # Kill Stockfish process
```

**UCI Protocol** (Universal Chess Interface) is the standard for communicating with chess engines:
- Commands are sent via stdin: `position fen ...`, `go depth ...`
- Responses are read from stdout: `info ...`, `bestmove ...`
- The engine runs as a long-lived process (not spawned per request)

---

## Database Design & Data Storage

### MongoDB: `chess_users` Database

**Collection: `users`**

```json
{
  "_id": ObjectId("..."),
  "email": "player@example.com",
  "username": "ChessKing",
  "passwordHash": "$2a$10$...",       // BCrypt hash
  "role": "USER",                     // USER or ADMIN
  "rating": 1209,                     // Elo rating
  "stats": {
    "totalSolved": 3,
    "totalAttempted": 3,
    "accuracy": 100.0,
    "currentStreak": 3,
    "bestStreak": 3,
    "averageSolveTimeMs": 70798,
    "byDifficulty": {
      "BEGINNER": { "solved": 3, "attempted": 3 }
    }
  },
  "recentPuzzles": [
    {
      "puzzleId": "69f5b5facb7c616f3206737e",
      "solvedAt": ISODate("2026-05-02T08:40:37.672Z"),
      "timeMs": 3328,
      "correct": true,
      "difficulty": "BEGINNER"
    }
  ],
  "version": 3                        // Optimistic locking
}
```

### MongoDB: `chess_puzzles` Database

**Collection: `puzzles`**

```json
{
  "_id": ObjectId("..."),
  "fen": "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
  "solutionLine": ["h5f7"],            // Moves in UCI format (from+to+promotion)
  "sideToMove": "WHITE",
  "mateIn": 1,
  "difficulty": "BEGINNER",
  "status": "ACTIVE",                  // ACTIVE or ARCHIVED
  "themes": ["mate-in-1", "scholars-mate"],
  "solvedBy": ["userId1", "userId2"],  // Users who solved this puzzle
  "solvedByCount": 2,
  "attemptedCount": 5,
  "averageSolveTimeMs": 4500,
  "createdAt": ISODate("...")
}
```

### Data Access Patterns

| Query | Used By | Index |
|-------|---------|-------|
| Find puzzles by difficulty + status, excluding user's solved IDs | Puzzle Service (getNextPuzzle) | `{ difficulty, status, solvedBy }` |
| Find user by email | User Service (login) | `{ email }` unique |
| Top N users by rating | User Service (leaderboard) | `{ rating: -1 }` |
| Find puzzle by ID | Puzzle Service (validate, solve) | `{ _id }` default |

---

## Kafka Event Architecture

### Topics and Message Flow

```
┌─────────────────┐   puzzle.generate   ┌─────────────────┐
│  Puzzle Service  │ ─────────────────▶ │  Engine Service  │
│  (Producer)      │                    │  (Consumer)      │
└─────────────────┘                    └────────┬────────┘
                                                │
                                       puzzle.generated
                                                │
┌─────────────────┐                    ┌────────▼────────┐
│  Puzzle Service  │ ◀───────────────── │  Engine Service  │
│  (Consumer)      │                    │  (Producer)      │
└────────┬────────┘                    └─────────────────┘
         │
    puzzle.solved
         │
┌────────▼────────┐
│  User Service   │
│  (Consumer)     │
└─────────────────┘
```

### Event Schemas

**`puzzle.generate`** — Request puzzle generation
```json
{ "difficulty": "INTERMEDIATE", "count": 5, "requestedBy": "userId" }
```

**`puzzle.generated`** — New puzzle created by engine
```json
{ "fen": "...", "solutionLine": ["e2e4", "d7d5", "e4d5"], "mateIn": 2, "difficulty": "INTERMEDIATE", "sideToMove": "WHITE" }
```

**`puzzle.solved`** — User completed a puzzle
```json
{ "userId": "...", "puzzleId": "...", "difficulty": "BEGINNER", "timeMs": 3500, "correct": true, "mateIn": 1 }
```

**`user.rating.update`** — Reserved for future cross-service rating sync

### Why Not Direct HTTP Calls?

If the Puzzle Service called the Engine Service via HTTP to generate puzzles:
- The HTTP request would timeout (generation takes 2-10s per puzzle × 5 puzzles = 10-50s)
- No retry on failure — the request is just lost
- No backpressure — 100 concurrent requests would overwhelm Stockfish

With Kafka:
- Puzzle Service publishes event and responds instantly to the user
- Engine Service processes requests from the queue at its own pace
- Failed messages are automatically retried
- Multiple Engine Service instances can consume from the same partition

---

## How Puzzle Generation Works

This is the most technically interesting part of the system. Here's the complete pipeline:

### Step 1: Trigger (Manual or Automatic)

```
User clicks "Generate Puzzles" on Admin page
  → Frontend: POST /api/puzzles/generate { difficulty: "INTERMEDIATE", count: 5 }
  → Puzzle Service: publishes to Kafka topic 'puzzle.generate'
  → Returns 202 Accepted immediately

OR

User requests next puzzle but none available (all solved)
  → Puzzle Service: automatically publishes to 'puzzle.generate'
  → Throws NoPuzzlesAvailableException (404) for now
```

### Step 2: Engine Service Receives Generation Request

```
PuzzleGenerateConsumer listens to 'puzzle.generate'
  → Receives: { difficulty: "INTERMEDIATE", count: 5 }
  → Calls PuzzleGeneratorService.generatePuzzles("INTERMEDIATE", 5)
```

### Step 3: FEN Pool → Stockfish Analysis

```
PuzzleGeneratorService:
  1. Load all FEN positions from FenGenerator.getAllMatePositions()
     (pool of ~40 known positions that might contain forced mates)

  2. For each FEN position:
     a. Send to StockfishService.analyzePosition(fen, depth=25)
        - Stockfish analyzes at depth 25 (very thorough)
        - Returns bestMove, evaluation, principalVariation

     b. MateDetector.isForcedMate(result)
        - Check if evaluation starts with "M" (e.g., "M2" = mate in 2)
        - If not a forced mate → skip this position

     c. MateDetector.getMateInN(result)
        - Extract the N from "MN" → e.g., 2

     d. DifficultyClassifier.matchesDifficulty(mateIn, "INTERMEDIATE")
        - M1 = BEGINNER
        - M2 = INTERMEDIATE
        - M3 = ADVANCED
        - M4+ = MASTER
        - If doesn't match requested difficulty → skip

     e. Build GeneratedPuzzle:
        - fen: the position
        - solutionLine: principalVariation (e.g., ["h5f7", "g8h8", "f7f8"])
        - mateIn: 2
        - difficulty: "INTERMEDIATE"
        - sideToMove: extracted from FEN ("w" → "WHITE")

  3. Stop when count puzzles are generated
```

### Step 4: Publish Generated Puzzles

```
For each generated puzzle:
  PuzzleGeneratedProducer.publish(puzzle) → Kafka topic 'puzzle.generated'
```

### Step 5: Puzzle Service Saves to MongoDB

```
PuzzleGeneratedConsumer listens to 'puzzle.generated'
  → Deserializes GeneratedPuzzle
  → Creates Puzzle MongoDB document
  → Sets status: ACTIVE
  → Saves to chess_puzzles.puzzles collection
```

### What Stockfish Actually Does

Stockfish is the world's strongest open-source chess engine. When we send it a position:

```
Command: position fen r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4
Command: go depth 25

Stockfish output:
info depth 1 score mate 1 nodes 35 pv h5f7
info depth 2 score mate 1 nodes 87 pv h5f7
...
info depth 25 score mate 1 nodes 12847 pv h5f7
bestmove h5f7 ponder e8d8
```

Key outputs:
- `score mate 1` — forced checkmate in 1 move
- `pv h5f7` — principal variation (best move sequence): Queen from h5 captures on f7
- `bestmove h5f7` — the single best move in this position

For deeper puzzles (mate in 3):
```
info depth 25 score mate 3 pv e7f6 h8g8 e1e8 ... 
```
The principal variation contains ALL moves (player + opponent) needed for checkmate.

---

## Authentication & Authorization Flow

### JWT Token Structure

```
Header:  { "alg": "HS384" }
Payload: {
  "sub": "69f5b690fa8fbf23f00f964b",    // User ID (MongoDB _id)
  "username": "ChessKing",
  "role": "USER",
  "token_type": "access",               // "access" or "refresh"
  "iat": 1777712427,                     // Issued at
  "exp": 1777716027                      // Expires at (access: +1hr, refresh: +7d)
}
Signature: HMAC-SHA384 with shared secret
```

### Full Auth Flow

```
1. User registers:
   POST /api/auth/register → User Service creates user + returns access + refresh tokens

2. Frontend stores tokens in Zustand store (memory) + localStorage (persistence)

3. Every API request:
   Axios interceptor adds: Authorization: Bearer <access_token>

4. Gateway receives request:
   JwtAuthenticationFilter validates token → injects X-User-Id header → forwards

5. When access token expires (401):
   Axios interceptor catches 401 → calls POST /api/auth/refresh with refresh token
   → Gets new access token → retries original request

6. When refresh token expires:
   User is logged out → redirected to /login
```

### Role-Based Access

| Endpoint | Required Role |
|----------|--------------|
| `POST /api/auth/register`, `/login`, `/refresh` | None (public) |
| `GET /api/puzzles/next`, `/validate`, `/solve` | USER or ADMIN |
| `POST /api/puzzles/generate` | ADMIN only |
| `GET /api/users/leaderboard` | USER or ADMIN |
| `GET /api/engine/health`, `/analyze` | ADMIN only |

---

## Puzzle Solving Flow — End to End

Here is the complete sequence from user clicking "Start Puzzle" to rating update:

```
Step 1: Frontend → GET /api/puzzles/next?difficulty=BEGINNER
  → Gateway validates JWT, adds X-User-Id
  → Puzzle Service finds unsolved puzzle for this user (random selection)
  → Returns: { id, fen, sideToMove: "WHITE", mateIn: 1, difficulty: "BEGINNER" }

Step 2: Frontend loads FEN into chess.js + react-chessboard
  → Board renders with correct orientation (white at bottom)
  → Timer starts

Step 3: User drags piece (e.g., Queen from h5 to f7)
  → chess.js validates move locally (legal check)
  → Frontend sends: POST /api/puzzles/{id}/validate { move: "h5f7", moveNumber: 1 }

Step 4: Puzzle Service compares move against solutionLine[0]
  → "h5f7" === "h5f7" ✓ correct!
  → No opponent response (mate in 1)
  → Returns: { correct: true, puzzleComplete: true, movesRemaining: 0 }

Step 5: Frontend shows "Puzzle solved!" with green banner
  → Sends: POST /api/puzzles/{id}/solve { timeMs: 3500 }

Step 6: Puzzle Service:
  → Adds userId to puzzle's solvedBy array
  → Increments solvedByCount
  → Publishes 'puzzle.solved' event to Kafka

Step 7: User Service (async via Kafka):
  → Receives puzzle.solved event
  → Calculates new Elo: 1200 → 1209 (solved BEGINNER puzzle)
  → Updates stats: totalSolved++, accuracy recalculated, streak updated
  → Saves to MongoDB

Step 8: Frontend shows rating change: "+9"
  → User clicks "Next Puzzle" → repeat from Step 1
```

---

## Elo Rating System

The platform uses a standard Elo rating system adapted for puzzles:

### Formula

```
Expected Score = 1 / (1 + 10^((DifficultyRating - PlayerRating) / 400))
New Rating = OldRating + K × (ActualScore - ExpectedScore)
```

### Parameters

| Difficulty | Assigned Rating |
|-----------|----------------|
| BEGINNER | 800 |
| INTERMEDIATE | 1200 |
| ADVANCED | 1600 |
| MASTER | 2000 |

| Player Rating | K-Factor |
|--------------|----------|
| < 1600 | 32 (high volatility — rating adjusts quickly) |
| 1600–2000 | 24 |
| > 2000 | 16 (low volatility — rating adjusts slowly) |

### Example Calculations

**New player (1200) solves BEGINNER (800) puzzle:**
```
Expected = 1 / (1 + 10^((800-1200)/400)) = 1 / (1 + 10^-1) = 0.909
New = 1200 + 32 × (1 - 0.909) = 1200 + 2.9 ≈ 1203
Gain: +3 (small gain — puzzle was easy for this player)
```

**New player (1200) solves ADVANCED (1600) puzzle:**
```
Expected = 1 / (1 + 10^((1600-1200)/400)) = 1 / (1 + 10^1) = 0.091
New = 1200 + 32 × (1 - 0.091) = 1200 + 29.1 ≈ 1229
Gain: +29 (large gain — puzzle was hard for this player)
```

**Player (1200) fails BEGINNER (800) puzzle:**
```
Expected = 0.909
New = 1200 + 32 × (0 - 0.909) = 1200 - 29.1 ≈ 1171
Loss: -29 (large loss — should have solved this)
```

---

## How to Run the Platform

### Prerequisites

- **Docker** v20+ and **Docker Compose** v2+
- ~8 GB free RAM, ~10 GB disk space
- Git

### Quick Start (Docker Compose)

```bash
# 1. Create workspace
mkdir chess-puzzle-platform && cd chess-puzzle-platform

# 2. Clone all repositories
git clone https://github.com/Shalmaliiii/chess-puzzle-frontend.git
git clone https://github.com/Shalmaliiii/chess-puzzle-gateway.git
git clone https://github.com/Shalmaliiii/chess-puzzle-user-service.git
git clone https://github.com/Shalmaliiii/chess-puzzle-puzzle-service.git
git clone https://github.com/Shalmaliiii/chess-puzzle-generator.git

# 3. Checkout feature branches (if PRs not merged yet)
cd chess-puzzle-frontend && git checkout devin/1777620209-full-frontend && cd ..
cd chess-puzzle-gateway && git checkout devin/1777622974-api-gateway && cd ..
cd chess-puzzle-user-service && git checkout devin/1777622952-feat-user-service && cd ..
cd chess-puzzle-puzzle-service && git checkout devin/1777622947-puzzle-service && cd ..
cd chess-puzzle-generator && git checkout devin/1777622969-engine-service && cd ..

# 4. Create docker-compose.yml (see below) in the chess-puzzle-platform/ directory

# 5. Start everything
docker compose up --build -d

# 6. Wait for all services to be healthy (~2 minutes)
docker compose ps

# 7. Open the app
open http://localhost:3000
```

### docker-compose.yml

```yaml
version: "3.9"

services:
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh --quiet
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    healthcheck:
      test: ["CMD-SHELL", "bash -c 'echo srvr > /dev/tcp/localhost/2181'"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on:
      zookeeper:
        condition: service_healthy
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    healthcheck:
      test: kafka-topics --bootstrap-server localhost:9092 --list
      interval: 15s
      timeout: 10s
      retries: 10

  user-service:
    build: ./chess-puzzle-user-service
    ports:
      - "8081:8081"
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/chess_users
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      JWT_SECRET: chess-puzzle-platform-secret-key-min-256-bits-long-enough
    restart: on-failure

  puzzle-service:
    build: ./chess-puzzle-puzzle-service
    ports:
      - "8082:8082"
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/chess_puzzles
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    restart: on-failure

  engine-service:
    build: ./chess-puzzle-generator
    ports:
      - "8083:8083"
    depends_on:
      kafka:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      STOCKFISH_PATH: /usr/games/stockfish
      STOCKFISH_ENABLED: "true"
    restart: on-failure

  api-gateway:
    build: ./chess-puzzle-gateway
    ports:
      - "8080:8080"
    depends_on:
      - user-service
      - puzzle-service
      - engine-service
    environment:
      SPRING_PROFILES_ACTIVE: docker
      JWT_SECRET: chess-puzzle-platform-secret-key-min-256-bits-long-enough
    restart: on-failure

  frontend:
    build: ./chess-puzzle-frontend
    ports:
      - "3000:80"
    depends_on:
      - api-gateway

volumes:
  mongo-data:
```

### Port Summary

| Service | Port | URL |
|---------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| API Gateway | 8080 | http://localhost:8080/api |
| User Service | 8081 | http://localhost:8081 (direct, dev only) |
| Puzzle Service | 8082 | http://localhost:8082 (direct, dev only) |
| Engine Service | 8083 | http://localhost:8083 (direct, dev only) |
| MongoDB | 27017 | mongodb://localhost:27017 |
| Kafka | 9092 | localhost:9092 |
| Zookeeper | 2181 | (internal only) |

---

## Inspecting Data — MongoDB, Kafka, Logs

### MongoDB — View and Query Data

```bash
# Connect to MongoDB shell
docker exec -it chess-puzzle-platform-mongodb-1 mongosh

# List all databases
show dbs
# Output: chess_users, chess_puzzles, admin, config, local

# View users
use chess_users
db.users.find().pretty()

# View a specific user's stats
db.users.find({ username: "ChessKing" }, { stats: 1, rating: 1 }).pretty()

# Count users
db.users.countDocuments()

# View puzzles
use chess_puzzles
db.puzzles.find().pretty()

# View puzzles by difficulty
db.puzzles.find({ difficulty: "BEGINNER" }).pretty()

# View a puzzle's solution line
db.puzzles.find({ mateIn: 2 }, { fen: 1, solutionLine: 1, mateIn: 1 }).pretty()

# Count puzzles by difficulty
db.puzzles.aggregate([
  { $group: { _id: "$difficulty", count: { $sum: 1 } } }
])

# See who solved which puzzles
db.puzzles.find({}, { solvedBy: 1, solvedByCount: 1 }).pretty()
```

### MongoDB Compass (GUI)

Connect with: `mongodb://localhost:27017` — no auth required for local dev.

### Kafka — View Topics and Messages

```bash
# List all Kafka topics
docker exec chess-puzzle-platform-kafka-1 \
  kafka-topics --bootstrap-server localhost:9092 --list

# Output:
#   puzzle.generate
#   puzzle.generated
#   puzzle.solved
#   user.rating.update

# Read messages from a topic (from beginning)
docker exec chess-puzzle-platform-kafka-1 \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic puzzle.solved --from-beginning

# Read messages as they arrive (live tail)
docker exec chess-puzzle-platform-kafka-1 \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic puzzle.solved

# View topic details (partitions, offsets)
docker exec chess-puzzle-platform-kafka-1 \
  kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic puzzle.solved

# Check consumer group lag
docker exec chess-puzzle-platform-kafka-1 \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --all-groups
```

### Service Logs

```bash
# View logs for a specific service
docker logs chess-puzzle-platform-puzzle-service-1 -f

# View only error logs
docker logs chess-puzzle-platform-engine-service-1 2>&1 | grep ERROR

# View all service logs together
docker compose logs -f

# View last 50 lines of user service
docker logs chess-puzzle-platform-user-service-1 --tail 50
```

### API Testing with curl

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","username":"TestUser","password":"Test1234!"}'

# Login and extract token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234!"}' | jq -r '.token')

# Get next puzzle
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/puzzles/next

# Get next puzzle by difficulty
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/puzzles/next?difficulty=BEGINNER"

# Validate a move
curl -X POST http://localhost:8080/api/puzzles/{id}/validate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"move":"h5f7","moveNumber":1}'

# Get puzzle solution
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/puzzles/{id}/solution

# View dashboard
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me/dashboard

# View leaderboard
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/leaderboard

# Check engine health
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/engine/health

# View puzzle stats
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/puzzles/stats
```

---

## API Reference

### Auth Endpoints (Public — no JWT required)

| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/auth/register` | `{ email, username, password }` | `{ id, email, username, token }` |
| POST | `/api/auth/login` | `{ email, password }` | `{ id, email, username, token, refreshToken, role, rating }` |
| POST | `/api/auth/refresh` | `{ refreshToken }` | `{ token, refreshToken }` |

### User Endpoints (JWT required)

| Method | Path | Response |
|--------|------|----------|
| GET | `/api/users/me` | User profile with stats |
| GET | `/api/users/me/dashboard` | Dashboard data (stats, charts, recent puzzles) |
| GET | `/api/users/leaderboard` | Array of `{ rank, username, rating, totalSolved }` |

### Puzzle Endpoints (JWT required)

| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/puzzles/next` | Query: `?difficulty=BEGINNER` | `{ id, fen, sideToMove, mateIn, difficulty }` |
| GET | `/api/puzzles/{id}` | — | Puzzle details |
| POST | `/api/puzzles/{id}/validate` | `{ move, moveNumber }` | `{ correct, opponentMove, puzzleComplete, movesRemaining }` |
| POST | `/api/puzzles/{id}/solve` | `{ timeMs }` | `{ message }` |
| GET | `/api/puzzles/{id}/solution` | — | `{ solutionLine, fen, mateIn }` |
| GET | `/api/puzzles/stats` | — | `{ total, byDifficulty, byStatus }` |
| POST | `/api/puzzles/generate` | `{ difficulty, count }` | 202 Accepted |

### Engine Endpoints (JWT required, ADMIN role)

| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/engine/health` | — | `{ status, engineVersion, uptime }` |
| POST | `/api/engine/analyze` | `{ fen, depth }` | `{ bestMove, evaluation, principalVariation }` |
| POST | `/api/engine/generate` | `{ difficulty, count }` | Array of generated puzzles |

---

## Repository Map

| Repository | Description | Tech |
|-----------|-------------|------|
| [`chess-puzzle-frontend`](https://github.com/Shalmaliiii/chess-puzzle-frontend) | React SPA — interactive chess UI, dashboard, leaderboard | React 19, TypeScript, Vite, Zustand |
| [`chess-puzzle-gateway`](https://github.com/Shalmaliiii/chess-puzzle-gateway) | API Gateway — JWT validation, routing, CORS | Spring Cloud Gateway, Java 21 |
| [`chess-puzzle-user-service`](https://github.com/Shalmaliiii/chess-puzzle-user-service) | User Service — auth, profiles, rating, leaderboard | Spring Boot, MongoDB, Kafka, BCrypt |
| [`chess-puzzle-puzzle-service`](https://github.com/Shalmaliiii/chess-puzzle-puzzle-service) | Puzzle Service — CRUD, move validation, seed data | Spring Boot, MongoDB, Kafka |
| [`chess-puzzle-generator`](https://github.com/Shalmaliiii/chess-puzzle-generator) | Engine Service — Stockfish integration, puzzle generation | Spring Boot, Stockfish, Kafka, Java 25 |

---

## Future Enhancements

### Short-term

| Enhancement | Description |
|------------|-------------|
| **WebSocket for real-time** | Replace polling with WebSocket for live move validation — reduces latency from ~200ms to ~50ms |
| **Puzzle hints** | Highlight the correct piece to move (without revealing the target square) as a partial hint |
| **Timed challenges** | Blitz mode: solve as many puzzles as possible in 5 minutes |
| **Puzzle rating** | Individual puzzles get their own Elo rating based on solve rates — better difficulty calibration |
| **Social features** | Share solved puzzles, challenge friends, puzzle of the day |

### Medium-term

| Enhancement | Description |
|------------|-------------|
| **Dynamic puzzle generation** | Generate puzzles from real grandmaster games (PGN parsing) instead of static FEN pools |
| **Multiple solution paths** | Some positions have multiple forcing lines — accept any valid mate sequence |
| **Tactical themes** | Auto-tag puzzles by theme (pin, fork, skewer, discovered attack) using Stockfish analysis |
| **Spaced repetition** | Re-present failed puzzles at increasing intervals for better learning |
| **Mobile app** | React Native or Flutter app with offline puzzle cache |

### Long-term

| Enhancement | Description |
|------------|-------------|
| **Kubernetes deployment** | Migrate from Docker Compose to K8s with Helm charts for production scaling |
| **Redis caching** | Cache frequently accessed puzzles and leaderboard data |
| **Grafana + Prometheus** | Full observability stack: service metrics, Kafka lag, MongoDB query performance |
| **A/B testing** | Test different puzzle difficulty curves and UI layouts |
| **AI-powered difficulty** | Train an ML model on user solve data to predict puzzle difficulty more accurately than mate-in-N alone |
| **Multi-language support** | i18n for the frontend (English, Hindi, Spanish, etc.) |

---

## Contributing

1. Fork the relevant repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make changes and ensure tests pass: `./gradlew test` (backend) or `npm run lint && npm run build` (frontend)
4. Submit a Pull Request

## License

This project is for educational and portfolio purposes.
