# Casino Platform Prototype

카지노 플랫폼 프로토타입 프로젝트입니다. 회원가입부터 가상 머니 지갑, 게임 로비, **Seamless Wallet 통합**, 그리고 실제 플레이 가능한 데모 슬롯 게임까지 갖춘 풀스택 웹 애플리케이션입니다.

> ⚠️ **주의**: 이 프로젝트는 학습 및 프로토타입 목적으로 개발되었습니다. 실제 돈을 취급하지 않으며, 가상 머니(DEMO)만 사용합니다. 실제 외부 게임 제공업체와 연동하지 않습니다.

## 📋 목차

- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [아키텍처](#아키텍처)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [API 문서](#api-문서)
- [Seamless Wallet 통합](#seamless-wallet-통합)
- [보안](#보안)
- [트러블슈팅](#트러블슈팅)

## 🛠 기술 스택

### Frontend
- **Framework**: Next.js 16 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: React Context API
- **HTTP Client**: Fetch API

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Security**: Spring Security (Dual SecurityFilterChain)
  - Session-based authentication for user API
  - HMAC-SHA256 authentication for Provider Wallet API
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **Build Tool**: Gradle 8.5
- **Concurrency**: Pessimistic Locking + Optimistic Retry

### Infrastructure
- **Database**: PostgreSQL (Docker)
- **Container**: Docker Compose

## ✨ 주요 기능

### Phase 1: 개발 환경 구축 ✅
- PostgreSQL 데이터베이스 설정
- Spring Boot 백엔드 구축
- Next.js 프론트엔드 구축
- CORS 설정 및 API 연결

### Phase 2: 회원 시스템 ✅
- 회원가입 (이메일, 사용자명, 비밀번호)
- 로그인 (세션 기반 인증)
- 로그아웃
- 사용자 정보 조회
- 역할 관리 (USER, ADMIN)

### Phase 3: 가상 머니 Wallet ✅
- 회원가입 시 DEMO 지갑 자동 생성
- 잔액 조회
- 거래 내역 조회 (페이징)
- 가상 머니 지급 (개발용)
- **동시성 제어**: Pessimistic Locking (SELECT FOR UPDATE)
- **멱등성 보장**: referenceId 기반 중복 방지
- **데이터 정합성**: BigDecimal, Transaction

### Phase 4: 게임 로비 ✅
- 게임 목록 조회
- 카테고리별 필터링 (Slots, Cards, Roulette, Dice)
- 게임 상세 정보
- 최소/최대 베팅 금액 표시
- 게임 실행 (Launch API)

### Phase 5: Seamless Wallet 통합 ✅
**Provider Wallet API**
- **HMAC-SHA256 인증**: 외부 provider 인증 (nonce replay 방지)
- **Balance API**: 플레이어 잔액 조회
- **Bet API**: 베팅 처리 (멱등성 보장)
- **Win API**: 승리 지급 (무승부 0원 지원)
- **Rollback API**: 거래 취소 (out-of-order rollback 지원)
- **Provider 관리**: 다중 provider 지원 구조
- **Player Mapping**: 플랫폼 user ↔ provider player 매핑

**Demo Slot Game**
- 5x3 릴, 25 페이라인 슬롯 게임
- 서버 기반 게임 결과 생성 (클라이언트 신뢰 안 함)
- Provider Wallet API 통합 (Bet → Spin → Win)
- 게임 세션 관리 (SHA-256 토큰 해싱)
- 라운드 상태 관리 (BET_PENDING → WIN_SUCCESS)
- 멱등성: Idempotency-Key 헤더 기반

### Phase 6: 테스트 ⚠️
- 프로토타입 특성상 생략
- 핵심 기능은 수동 테스트로 검증

### Phase 7: 마이그레이션 및 정리 ✅
- **V8 마이그레이션**: 기존 테이블 확장
  - `ledger.provider_transaction_id` 추가 (audit trail)
  - `games.provider_id`, `games.provider_game_id` 추가
- Entity 업데이트 (Game, Ledger)
- 양방향 참조 완성

### Phase 8: 문서화 ✅
- README 업데이트 (이 문서)
- API 명세 작성
- Seamless Wallet 가이드 작성

### UX 개선 ✅
- 통합 네비게이션 레이아웃
- Toast 알림 시스템
- Skeleton 로딩 UI
- 전역 인증 상태 관리 (AuthContext)
- Protected Route 컴포넌트

## 🏗 아키텍처

### Backend Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot                          │
│                                                         │
│  ┌─────────────┐  ┌──────────────────────────────┐   │
│  │   User API  │  │  Provider Wallet API (HMAC)  │   │
│  │  (Session)  │  │  /api/provider-wallet/**     │   │
│  └──────┬──────┘  └───────────┬──────────────────┘   │
│         │                     │                        │
│  ┌──────▼─────────────────────▼──────────────┐       │
│  │     Service Layer                          │       │
│  │  ┌──────────┐  ┌─────────────────────┐   │       │
│  │  │ Game     │  │ ProviderWallet      │   │       │
│  │  │ Launch   │  │ Service             │   │       │
│  │  └────┬─────┘  └──────┬──────────────┘   │       │
│  │       │                │                   │       │
│  │  ┌────▼────────────────▼──────────┐      │       │
│  │  │  WalletCoreService             │      │       │
│  │  │  (Pessimistic Locking +        │      │       │
│  │  │   Optimistic Retry)            │      │       │
│  │  └────────────┬────────────────────┘      │       │
│  └───────────────┼────────────────────────────┘       │
│                  │                                     │
│  ┌───────────────▼─────────────────────────────┐     │
│  │         PostgreSQL Database                  │     │
│  │  users, wallets, ledger, games,             │     │
│  │  providers, provider_transactions,          │     │
│  │  game_sessions, demo_rounds, etc.           │     │
│  └──────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### Seamless Wallet Flow

```
Frontend                Backend (Session Auth)              Backend (Provider Wallet API)
   │                            │                                      │
   │  POST /api/games/1/launch │                                      │
   ├──────────────────────────►│                                      │
   │  { currency: "DEMO" }     │  Create GameSession                  │
   │                            │  Generate token (SHA-256 hash)       │
   │◄───────────────────────────┤                                      │
   │  { sessionToken, url }    │                                      │
   │                            │                                      │
   │  POST /api/demo-provider/sessions/{token}/spin                   │
   │  Header: Idempotency-Key  │                                      │
   ├──────────────────────────►│                                      │
   │                            │                                      │
   │                            │  DemoRoundService.playRound()        │
   │                            │     │                                │
   │                            │     ├─► processBet()                 │
   │                            │     │      │                         │
   │                            │     │      └────────────────────────►│
   │                            │     │      POST /provider-wallet/bet │
   │                            │     │      HMAC signature            │
   │                            │     │                                │
   │                            │     │      WalletCoreService.debit() │
   │                            │     │◄───────────────────────────────┤
   │                            │     │                                │
   │                            │     ├─► SlotGameEngine.spin()        │
   │                            │     │                                │
   │                            │     ├─► processWin()                 │
   │                            │     │      │                         │
   │                            │     │      └────────────────────────►│
   │                            │     │     POST /provider-wallet/win  │
   │                            │     │                                │
   │                            │     │     WalletCoreService.credit() │
   │                            │     │◄───────────────────────────────┤
   │                            │     │                                │
   │◄───────────────────────────┤◄────┘                                │
   │  { reels, winAmount,      │                                      │
   │    balance, winningLines } │                                      │
```

## 📁 프로젝트 구조

```
casino-platform/
├── backend/                           # Spring Boot 백엔드
│   ├── src/main/java/com/casino/platform/
│   │   ├── auth/                     # 인증 (로그인, 회원가입)
│   │   ├── user/                     # 사용자 관리
│   │   ├── wallet/                   # 지갑 Core Service
│   │   ├── ledger/                   # 거래 내역
│   │   ├── game/                     # 게임 관리, 실행
│   │   ├── provider/                 # ⭐ Provider Wallet API
│   │   │   ├── entity/               # Provider, ProviderTransaction, GameSession, etc.
│   │   │   ├── security/             # HMAC 인증, Nonce 검증
│   │   │   ├── wallet/               # Provider Wallet Service, DTO
│   │   │   ├── adapter/              # Provider Adapter 패턴
│   │   │   ├── rollback/             # Rollback Intent Service
│   │   │   └── repository/           # JPA Repositories
│   │   ├── demo/                     # ⭐ Demo Slot Game
│   │   │   ├── entity/               # DemoRound
│   │   │   ├── service/              # DemoRoundService
│   │   │   ├── controller/           # DemoGameController
│   │   │   └── dto/                  # SpinRequest, SpinResponse
│   │   ├── config/                   # Spring Security 설정 (Dual FilterChain)
│   │   └── common/                   # 공통 컴포넌트
│   ├── src/main/resources/
│   │   ├── db/migration/             # Flyway 마이그레이션 (V1~V8)
│   │   └── application.yml           # 애플리케이션 설정
│   └── build.gradle                  # Gradle 빌드
├── frontend/                          # Next.js 프론트엔드
│   ├── app/                          # App Router 페이지
│   │   ├── (auth)/                   # 인증 관련 페이지
│   │   ├── dashboard/                # 대시보드
│   │   ├── wallet/                   # 지갑
│   │   └── games/                    # 게임 로비
│   ├── components/                   # 재사용 컴포넌트
│   ├── contexts/                     # React Context
│   └── package.json                  # npm 패키지
├── infra/                            # 인프라 설정
│   └── docker-compose.yml            # PostgreSQL 컨테이너
├── CLAUDE.md                         # 프로젝트 개발 지침
├── SEAMLESS_WALLET_IMPLEMENTATION_PLAN.md  # Seamless Wallet 구현 계획
└── README.md                         # 이 문서
```

## 🚀 시작하기

### 필수 요구사항

- **Java**: 21 이상
- **Node.js**: 24.x 이상
- **Docker**: Docker Desktop 설치
- **Git**: 버전 관리

### 1. 저장소 클론

```bash
git clone <repository-url>
cd casino-platform
```

### 2. PostgreSQL 시작

```bash
cd infra
docker-compose up -d
```

데이터베이스 연결 확인:
```bash
docker exec casino-platform-db pg_isready -U casino_admin -d casino_platform
```

### 3. 백엔드 실행

```bash
cd backend
./gradlew bootRun
```

백엔드가 `http://localhost:8080` 에서 실행됩니다.

**Flyway 마이그레이션**이 자동으로 실행되어 다음 테이블이 생성됩니다:
- V1: `users` (사용자 계정)
- V2: `wallets`, `ledger` (지갑, 거래 내역)
- V3: `games` (게임 정보, 샘플 데이터 포함)
- V4: `admin_audit_log` (관리자 감사 로그)
- V5: `providers`, `player_provider_mapping` (Provider 관리)
- V6: `provider_transactions`, `nonces` (Provider Wallet API)
- V7: `game_sessions`, `demo_rounds`, `rollback_intents` (게임 세션, 데모 게임)
- V8: 기존 테이블 확장 (audit trail 강화)

### 4. 프론트엔드 실행

새 터미널에서:

```bash
cd frontend
npm install
npm run dev
```

프론트엔드가 `http://localhost:3000` 에서 실행됩니다.

### 5. 테스트 시나리오

#### 5.1. 회원가입 및 로그인
1. http://localhost:3000 접속
2. **Sign Up** 클릭하여 회원가입
3. 생성한 계정으로 **Login**

#### 5.2. 가상 머니 지급
1. Dashboard 접속
2. **Wallet** 페이지로 이동
3. "Add Funds (Dev)" 버튼 클릭
4. 금액 입력 (예: 1000.00)
5. 잔액 증가 확인

#### 5.3. 게임 실행 (Demo Slot)
1. **Games** 페이지로 이동
2. "Demo Slot" 게임 선택
3. "Play Now" 클릭
4. 게임 세션 토큰 생성 확인
5. Spin 버튼 클릭 (betPerLine 설정)
6. 게임 결과 확인:
   - 5x3 릴 결과
   - 승리 라인 (있는 경우)
   - 승리 금액
   - 업데이트된 잔액
7. **Wallet** → **Transactions**에서 거래 내역 확인
   - BET: -2.50 (0.10 × 25 paylines)
   - WIN: +5.00 (예시)

## 📚 API 문서

### 인증 (Authentication)

#### 회원가입
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "user123",
  "email": "user@example.com",
  "password": "password123"
}
```

#### 로그인
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user123",
  "password": "password123"
}
```

#### 로그아웃
```http
POST /api/auth/logout
```

#### 현재 사용자 정보
```http
GET /api/auth/me
```

### Wallet (User API)

#### 지갑 조회
```http
GET /api/wallet
```

**Response:**
```json
{
  "balance": 1000.00,
  "currencyType": "DEMO"
}
```

#### 거래 내역 조회
```http
GET /api/wallet/transactions?page=0&size=20
```

#### 가상 머니 지급 (개발용)
```http
POST /api/wallet/dev/deposit
Content-Type: application/json

{
  "amount": 1000.00,
  "description": "Initial balance"
}
```

### Games (User API)

#### 게임 목록 조회
```http
GET /api/games
GET /api/games?category=SLOTS
```

#### 게임 상세 조회
```http
GET /api/games/{id}
```

#### 게임 실행
```http
POST /api/games/{id}/launch
Content-Type: application/json

{
  "currency": "DEMO"
}
```

**Response:**
```json
{
  "launchUrl": "/demo-game?game=1",
  "sessionToken": "plain-token-for-client",
  "sessionId": 123
}
```

### Demo Game API

#### 스핀 (Demo Game)
```http
POST /api/demo-provider/sessions/{sessionToken}/spin
Content-Type: application/json
Idempotency-Key: unique-key-per-request

{
  "betPerLine": 0.10
}
```

**Response:**
```json
{
  "reels": [
    ["7️⃣", "💎", "🍒"],
    ["🔔", "🍋", "🍇"],
    ["⭐", "💎", "7️⃣"],
    ["🍇", "🍒", "🔔"],
    ["💰", "⭐", "🍋"]
  ],
  "winAmount": 5.00,
  "balance": 1002.50,
  "winningLines": [
    {
      "lineNumber": 0,
      "symbol": "💎",
      "count": 3,
      "win": 2.00
    },
    {
      "lineNumber": 5,
      "symbol": "🍒",
      "count": 4,
      "win": 3.00
    }
  ],
  "roundId": "round-uuid",
  "status": "WIN_SUCCESS"
}
```

**Notes:**
- `Idempotency-Key` 헤더 필수 (중복 요청 방지)
- 동일 key로 재요청 시 저장된 결과 반환 (재추첨 안 함)
- 총 베팅 금액 = betPerLine × 25 paylines

## 🔗 Seamless Wallet 통합

Seamless Wallet은 외부 게임 제공업체(Provider)가 플랫폼의 지갑 API를 호출하여 베팅, 승리, 롤백을 처리하는 통합 방식입니다.

### Provider Wallet API (HMAC 인증)

모든 Provider Wallet API 요청은 **HMAC-SHA256 서명**이 필요합니다.

#### 1. Balance (잔액 조회)
```http
GET /api/provider-wallet/v1/{providerCode}/balance?playerId={playerId}&currency=DEMO
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456789
X-Nonce: unique-nonce-uuid
X-Signature: hmac-sha256-signature
X-Player-Id: DEMO-123
```

**Response:**
```json
{
  "success": true,
  "balance": 1000.00,
  "currency": "DEMO"
}
```

#### 2. Bet (베팅 처리)
```http
POST /api/provider-wallet/v1/{providerCode}/bet
Content-Type: application/json
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456789
X-Nonce: unique-nonce-uuid
X-Signature: hmac-sha256-signature
X-Player-Id: DEMO-123

{
  "transactionId": "bet-tx-uuid",
  "playerId": "DEMO-123",
  "currency": "DEMO",
  "gameId": "1",
  "roundId": "round-uuid",
  "amount": 2.50
}
```

**Response:**
```json
{
  "success": true,
  "balance": 997.50,
  "currency": "DEMO"
}
```

#### 3. Win (승리 지급)
```http
POST /api/provider-wallet/v1/{providerCode}/win
Content-Type: application/json
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456789
X-Nonce: unique-nonce-uuid
X-Signature: hmac-sha256-signature
X-Player-Id: DEMO-123

{
  "transactionId": "win-tx-uuid",
  "playerId": "DEMO-123",
  "currency": "DEMO",
  "gameId": "1",
  "roundId": "round-uuid",
  "amount": 5.00,
  "originalTransactionId": "bet-tx-uuid"
}
```

**Response:**
```json
{
  "success": true,
  "balance": 1002.50,
  "currency": "DEMO"
}
```

#### 4. Rollback (거래 취소)
```http
POST /api/provider-wallet/v1/{providerCode}/rollback
Content-Type: application/json
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456789
X-Nonce: unique-nonce-uuid
X-Signature: hmac-sha256-signature
X-Player-Id: DEMO-123

{
  "transactionId": "rollback-tx-uuid",
  "playerId": "DEMO-123",
  "currency": "DEMO",
  "originalTransactionId": "bet-tx-uuid"
}
```

**Response:**
```json
{
  "success": true,
  "balance": 1000.00,
  "currency": "DEMO"
}
```

### HMAC Signature 생성

```java
String signature = HMAC-SHA256(
    method + url + timestamp + nonce + requestBody,
    secretKey
);
```

**예시:**
```
Method: POST
URL: /api/provider-wallet/v1/DEMO/bet
Timestamp: 1697123456789
Nonce: 550e8400-e29b-41d4-a716-446655440000
Body: {"transactionId":"bet-001",...}
SecretKey: demo-secret-key-2024

Signature = HMAC-SHA256(
    "POST/api/provider-wallet/v1/DEMO/bet1697123456789550e8400-e29b-41d4-a716-446655440000{\"transactionId\":\"bet-001\",...}",
    "demo-secret-key-2024"
)
```

### 보안 규칙
1. **Nonce Replay 방지**: 각 nonce는 5분간 유효, 중복 사용 불가
2. **Timestamp 검증**: 요청 시간은 ±5분 이내
3. **Signature 검증**: Constant-time comparison (timing attack 방지)
4. **멱등성**: transactionId로 중복 요청 자동 처리

### 에러 코드

| Code | HTTP | Description |
|------|------|-------------|
| `PLAYER_NOT_FOUND` | 404 | 플레이어를 찾을 수 없음 |
| `INSUFFICIENT_FUNDS` | 403 | 잔액 부족 |
| `INVALID_AMOUNT` | 400 | 잘못된 금액 (음수, null) |
| `TRANSACTION_CONFLICT` | 409 | 거래 충돌 (중복 referenceId) |
| `UNSUPPORTED_CURRENCY` | 400 | 지원하지 않는 통화 |
| `AUTHENTICATION_FAILED` | 401 | 인증 실패 (잘못된 signature) |
| `INVALID_SIGNATURE` | 401 | 잘못된 HMAC 서명 |
| `NONCE_ALREADY_USED` | 400 | Nonce 재사용 감지 |
| `TIMESTAMP_EXPIRED` | 400 | Timestamp 만료 |
| `ORIGINAL_TRANSACTION_NOT_FOUND` | 404 | 원본 거래를 찾을 수 없음 (rollback) |
| `RETRYABLE_ERROR` | 500 | 재시도 가능한 오류 |

**에러 응답 예시:**
```json
{
  "success": false,
  "errorCode": "INSUFFICIENT_FUNDS",
  "errorMessage": "Insufficient balance for bet"
}
```

## 🔐 보안

### 인증 방식 (Dual SecurityFilterChain)

#### 1. User API (Session-based)
- **Path**: `/api/auth/**`, `/api/wallet/**`, `/api/games/**`, `/api/demo-provider/**`
- **방식**: HttpSession + JSESSIONID 쿠키
- **필터**: Spring Security의 기본 UsernamePasswordAuthenticationFilter
- **공개 경로**: `/api/health`, `/api/auth/register`, `/api/auth/login`

#### 2. Provider Wallet API (HMAC-SHA256)
- **Path**: `/api/provider-wallet/**`
- **방식**: HMAC-SHA256 서명 검증
- **헤더**: `X-Provider-Key-Id`, `X-Timestamp`, `X-Nonce`, `X-Signature`, `X-Player-Id`
- **필터**: `ProviderAuthenticationFilter` (Custom)
- **Stateless**: 세션 사용 안 함

### 비밀번호
- **해시 알고리즘**: BCryptPasswordEncoder
- **최소 길이**: 8자
- **저장**: 해시된 값만 DB에 저장

### 게임 세션 보안
- **토큰 생성**: UUID (plain token)
- **저장**: SHA-256 해시만 DB에 저장
- **전송**: Plain token을 클라이언트에 전송 (1회성)
- **검증**: 클라이언트가 보낸 plain token을 해싱하여 DB와 비교
- **만료**: 24시간 후 자동 만료

### 데이터 정합성
- **BigDecimal**: 모든 금액 계산 (부동소수점 오차 방지)
- **@Transactional**: 잔액 변경과 거래 기록을 원자적으로 처리
- **Pessimistic Lock**: `SELECT FOR UPDATE` (동시성 제어)
- **Optimistic Retry**: `@Retryable` + `@Version` (충돌 시 재시도)
- **멱등성**: `referenceId` / `transactionId` UNIQUE constraint

### CORS
- **허용 Origin**: `http://localhost:3000`
- **Credentials**: `true` (쿠키 포함)

## 🐛 트러블슈팅

### Docker가 시작되지 않음
- Docker Desktop이 실행 중인지 확인
- WSL 2가 설치되어 있는지 확인 (Windows)

### 백엔드가 시작되지 않음
- PostgreSQL이 실행 중인지 확인: `docker ps`
- 포트 8080이 사용 중인지 확인: `netstat -an | findstr 8080`
- Flyway 마이그레이션 오류 확인: 콘솔 로그 확인

### 프론트엔드가 백엔드에 연결되지 않음
- CORS 설정 확인
- 백엔드가 http://localhost:8080에서 실행 중인지 확인
- 브라우저 개발자 도구의 Network 탭에서 요청 확인

### Wallet이 없다는 에러
- 회원가입 후 자동으로 생성됨 (V2 마이그레이션 이후)
- 기존 계정은 SQL로 직접 생성:
```sql
INSERT INTO wallets (user_id, balance, currency_type, version, created_at, updated_at)
VALUES ({user_id}, 0.00, 'DEMO', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

### Provider Wallet API 401 Unauthorized
- HMAC signature 생성 확인:
  - method, url, timestamp, nonce, body 순서 확인
  - secretKey가 올바른지 확인
- Timestamp가 ±5분 이내인지 확인
- Nonce가 재사용되지 않았는지 확인

### Demo Game Spin 실패
- 세션 토큰이 유효한지 확인 (24시간 이내)
- Idempotency-Key 헤더 포함 확인
- 잔액이 충분한지 확인 (betPerLine × 25)

## 📝 다음 개선 사항

### 기능 확장
- [ ] 관리자 페이지 구현
  - 사용자 관리
  - 거래 내역 조회
  - Provider 관리
  - 게임 활성화/비활성화
- [ ] 다양한 게임 추가
  - Blackjack
  - Roulette
  - Dice
- [ ] 실시간 통계 대시보드

### 성능 최적화
- [ ] Redis 캐싱 (잔액, 게임 목록)
- [ ] Database Connection Pool 튜닝
- [ ] API Rate Limiting

### 운영 개선
- [ ] 로깅 강화 (ELK Stack)
- [ ] 모니터링 (Prometheus + Grafana)
- [ ] CI/CD 파이프라인
- [ ] Docker 기반 배포

### 보안 강화
- [ ] Rate Limiting (Provider API)
- [ ] IP Whitelist (Provider API)
- [ ] 2FA (Two-Factor Authentication)
- [ ] 비밀번호 정책 강화

## 📄 관련 문서

- [CLAUDE.md](./CLAUDE.md) - 프로젝트 개발 지침
- [SEAMLESS_WALLET_IMPLEMENTATION_PLAN.md](./SEAMLESS_WALLET_IMPLEMENTATION_PLAN.md) - Seamless Wallet 구현 계획
- [API_SPECIFICATION.md](./API_SPECIFICATION.md) - 상세 API 명세
- [SEAMLESS_WALLET_GUIDE.md](./SEAMLESS_WALLET_GUIDE.md) - Seamless Wallet 통합 가이드

## 📄 라이선스

이 프로젝트는 학습 목적의 프로토타입입니다.

## 👥 기여

이 프로젝트는 개인 학습 프로젝트입니다.

---

**마지막 업데이트**: 2026-10-12  
**버전**: Phase 8 완료 - Seamless Wallet 통합 완성
