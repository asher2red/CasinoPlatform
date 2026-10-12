# API Specification

Casino Platform Provider Wallet API 명세서

**Version**: 1.0.0  
**Last Updated**: 2026-10-12

## 목차

- [개요](#개요)
- [인증](#인증)
- [공통 규격](#공통-규격)
- [Provider Wallet API](#provider-wallet-api)
- [User API](#user-api)
- [Demo Game API](#demo-game-api)
- [에러 코드](#에러-코드)

## 개요

Casino Platform은 두 가지 인증 방식을 지원합니다:

1. **Session-based Authentication**: 사용자 API (회원가입, 로그인, 지갑 조회 등)
2. **HMAC-SHA256 Authentication**: Provider Wallet API (외부 게임 제공업체 연동)

### Base URLs

- **User API**: `http://localhost:8080/api`
- **Provider Wallet API**: `http://localhost:8080/api/provider-wallet/v1`
- **Demo Game API**: `http://localhost:8080/api/demo-provider`

### Content-Type

모든 요청과 응답은 `application/json` 형식을 사용합니다.

## 인증

### 1. Session-based Authentication (User API)

사용자 API는 Spring Security의 HttpSession을 사용합니다.

**로그인 플로우:**
1. `POST /api/auth/login`으로 로그인
2. 서버가 `JSESSIONID` 쿠키 발급
3. 이후 모든 요청에 쿠키 자동 포함

**인증이 필요한 경로:**
- `/api/wallet/**`
- `/api/games/**`
- `/api/demo-provider/**`
- `/api/auth/me`, `/api/auth/logout`

**공개 경로:**
- `/api/health`
- `/api/auth/register`
- `/api/auth/login`

### 2. HMAC-SHA256 Authentication (Provider Wallet API)

Provider Wallet API는 HMAC-SHA256 서명을 사용합니다.

#### 필수 헤더

| Header | Type | Description |
|--------|------|-------------|
| `X-Provider-Key-Id` | String | Provider Key ID (예: "demo-key-001") |
| `X-Timestamp` | Long | Unix timestamp (밀리초) |
| `X-Nonce` | String | 고유 UUID (재사용 불가, 5분 유효) |
| `X-Signature` | String | HMAC-SHA256 서명 (hex 문자열) |
| `X-Player-Id` | String | Provider Player ID (예: "DEMO-123") |

#### 서명 생성 알고리즘

```java
String message = method + url + timestamp + nonce + requestBody;
String signature = HMAC_SHA256(message, secretKey);
```

**예시:**

```javascript
// Input
const method = "POST";
const url = "/api/provider-wallet/v1/DEMO/bet";
const timestamp = "1697123456789";
const nonce = "550e8400-e29b-41d4-a716-446655440000";
const body = JSON.stringify({
  transactionId: "bet-tx-001",
  playerId: "DEMO-123",
  currency: "DEMO",
  gameId: "1",
  roundId: "round-uuid",
  amount: 2.50
});
const secretKey = "demo-secret-key-2024";

// Message
const message = method + url + timestamp + nonce + body;
// "POST/api/provider-wallet/v1/DEMO/bet1697123456789550e8400-e29b-41d4-a716-446655440000{...}"

// Signature
const signature = crypto
  .createHmac('sha256', secretKey)
  .update(message)
  .digest('hex');
```

**Node.js 예제:**

```javascript
const crypto = require('crypto');

function generateHmacSignature(method, url, timestamp, nonce, body, secretKey) {
  const message = method + url + timestamp + nonce + body;
  return crypto.createHmac('sha256', secretKey).update(message).digest('hex');
}

// 사용
const signature = generateHmacSignature(
  'POST',
  '/api/provider-wallet/v1/DEMO/bet',
  Date.now().toString(),
  crypto.randomUUID(),
  JSON.stringify(requestBody),
  'demo-secret-key-2024'
);
```

#### 검증 규칙

1. **Timestamp 검증**: 현재 시간 ±5분 이내
2. **Nonce 검증**: 5분 내 동일 nonce 재사용 불가
3. **Signature 검증**: Constant-time comparison으로 타이밍 공격 방지

#### 인증 실패 시

```json
{
  "success": false,
  "errorCode": "AUTHENTICATION_FAILED",
  "errorMessage": "Invalid signature"
}
```

HTTP Status: `401 Unauthorized`

## 공통 규격

### 성공 응답 (Provider Wallet API)

```json
{
  "success": true,
  "balance": 1000.00,
  "currency": "DEMO"
}
```

### 에러 응답 (Provider Wallet API)

```json
{
  "success": false,
  "errorCode": "INSUFFICIENT_FUNDS",
  "errorMessage": "Insufficient balance. Required: 10.00, Available: 5.00"
}
```

### 페이징 (User API)

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

### Timestamp 형식

- **Unix Timestamp (밀리초)**: Provider Wallet API 헤더
- **ISO 8601**: User API 응답 (예: `2026-10-12T15:30:00+09:00`)

## Provider Wallet API

### 1. Balance (잔액 조회)

플레이어의 현재 잔액을 조회합니다.

**Endpoint:**
```
GET /api/provider-wallet/v1/{providerCode}/balance
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `providerCode` | String | Yes | Provider 코드 (예: "DEMO") |

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `playerId` | String | Yes | Provider Player ID |
| `currency` | String | Yes | 통화 코드 (현재 "DEMO"만 지원) |

**Headers:**
- `X-Provider-Key-Id`
- `X-Timestamp`
- `X-Nonce`
- `X-Signature`
- `X-Player-Id`

**Request Example:**
```http
GET /api/provider-wallet/v1/DEMO/balance?playerId=DEMO-123&currency=DEMO HTTP/1.1
Host: localhost:8080
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456789
X-Nonce: 550e8400-e29b-41d4-a716-446655440000
X-Signature: a1b2c3d4e5f6...
X-Player-Id: DEMO-123
```

**Response (200 OK):**
```json
{
  "success": true,
  "balance": 1000.00,
  "currency": "DEMO"
}
```

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 404 | `PLAYER_NOT_FOUND` | 플레이어를 찾을 수 없음 |
| 400 | `UNSUPPORTED_CURRENCY` | 지원하지 않는 통화 |
| 401 | `AUTHENTICATION_FAILED` | 인증 실패 |

---

### 2. Bet (베팅 처리)

플레이어의 잔액에서 베팅 금액을 차감합니다.

**Endpoint:**
```
POST /api/provider-wallet/v1/{providerCode}/bet
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `providerCode` | String | Yes | Provider 코드 (예: "DEMO") |

**Headers:**
- `Content-Type: application/json`
- `X-Provider-Key-Id`
- `X-Timestamp`
- `X-Nonce`
- `X-Signature`
- `X-Player-Id`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `transactionId` | String | Yes | 고유 거래 ID (멱등성 키) |
| `playerId` | String | Yes | Provider Player ID |
| `currency` | String | Yes | 통화 코드 ("DEMO") |
| `gameId` | String | Yes | 게임 ID |
| `roundId` | String | Yes | 게임 라운드 ID |
| `amount` | BigDecimal | Yes | 베팅 금액 (양수) |

**Request Example:**
```http
POST /api/provider-wallet/v1/DEMO/bet HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456789
X-Nonce: 550e8400-e29b-41d4-a716-446655440000
X-Signature: a1b2c3d4e5f6...
X-Player-Id: DEMO-123

{
  "transactionId": "bet-tx-550e8400",
  "playerId": "DEMO-123",
  "currency": "DEMO",
  "gameId": "1",
  "roundId": "round-6ba7b810",
  "amount": 2.50
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "balance": 997.50,
  "currency": "DEMO"
}
```

**멱등성:**
- 동일한 `transactionId`로 재요청 시, 기존 거래 결과 반환
- 새로운 잔액 차감 없음

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 403 | `INSUFFICIENT_FUNDS` | 잔액 부족 |
| 400 | `INVALID_AMOUNT` | 잘못된 금액 (≤0, null) |
| 409 | `TRANSACTION_CONFLICT` | 거래 충돌 (내부 중복 referenceId) |
| 404 | `PLAYER_NOT_FOUND` | 플레이어를 찾을 수 없음 |

---

### 3. Win (승리 지급)

플레이어의 잔액에 승리 금액을 추가합니다.

**Endpoint:**
```
POST /api/provider-wallet/v1/{providerCode}/win
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `providerCode` | String | Yes | Provider 코드 (예: "DEMO") |

**Headers:**
- `Content-Type: application/json`
- `X-Provider-Key-Id`
- `X-Timestamp`
- `X-Nonce`
- `X-Signature`
- `X-Player-Id`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `transactionId` | String | Yes | 고유 거래 ID (멱등성 키) |
| `playerId` | String | Yes | Provider Player ID |
| `currency` | String | Yes | 통화 코드 ("DEMO") |
| `gameId` | String | Yes | 게임 ID |
| `roundId` | String | Yes | 게임 라운드 ID |
| `amount` | BigDecimal | Yes | 승리 금액 (≥0, 0은 무승부) |
| `originalTransactionId` | String | Yes | 원본 Bet transactionId |

**Request Example:**
```http
POST /api/provider-wallet/v1/DEMO/win HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456790
X-Nonce: 650e8400-e29b-41d4-a716-446655440001
X-Signature: b2c3d4e5f6a7...
X-Player-Id: DEMO-123

{
  "transactionId": "win-tx-650e8400",
  "playerId": "DEMO-123",
  "currency": "DEMO",
  "gameId": "1",
  "roundId": "round-6ba7b810",
  "amount": 5.00,
  "originalTransactionId": "bet-tx-550e8400"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "balance": 1002.50,
  "currency": "DEMO"
}
```

**무승부 (amount=0):**
```json
{
  "transactionId": "win-tx-no-win",
  ...
  "amount": 0.00
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

**멱등성:**
- 동일한 `transactionId`로 재요청 시, 기존 거래 결과 반환

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `INVALID_AMOUNT` | 잘못된 금액 (<0, null) |
| 409 | `TRANSACTION_CONFLICT` | 거래 충돌 |
| 404 | `PLAYER_NOT_FOUND` | 플레이어를 찾을 수 없음 |

---

### 4. Rollback (거래 취소)

원본 거래를 취소하고 잔액을 복구합니다.

**Endpoint:**
```
POST /api/provider-wallet/v1/{providerCode}/rollback
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `providerCode` | String | Yes | Provider 코드 (예: "DEMO") |

**Headers:**
- `Content-Type: application/json`
- `X-Provider-Key-Id`
- `X-Timestamp`
- `X-Nonce`
- `X-Signature`
- `X-Player-Id`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `transactionId` | String | Yes | 고유 Rollback 거래 ID |
| `playerId` | String | Yes | Provider Player ID |
| `currency` | String | Yes | 통화 코드 ("DEMO") |
| `originalTransactionId` | String | Yes | 취소할 원본 거래 ID (bet 또는 win) |

**Request Example:**
```http
POST /api/provider-wallet/v1/DEMO/rollback HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-Provider-Key-Id: demo-key-001
X-Timestamp: 1697123456791
X-Nonce: 750e8400-e29b-41d4-a716-446655440002
X-Signature: c3d4e5f6a7b8...
X-Player-Id: DEMO-123

{
  "transactionId": "rollback-tx-750e8400",
  "playerId": "DEMO-123",
  "currency": "DEMO",
  "originalTransactionId": "bet-tx-550e8400"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "balance": 1000.00,
  "currency": "DEMO"
}
```

**Out-of-Order Rollback:**

Rollback 요청이 원본 거래보다 먼저 도착한 경우, Rollback Intent로 저장 후 성공 응답합니다. 원본 거래가 도착하면 자동으로 취소됩니다.

```json
{
  "success": true,
  "balance": 1000.00,
  "currency": "DEMO"
}
```

**멱등성:**
- 동일한 `transactionId`로 재요청 시, 기존 rollback 결과 반환

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 404 | `ORIGINAL_TRANSACTION_NOT_FOUND` | 원본 거래가 실패했거나 존재하지 않음 |
| 403 | `INSUFFICIENT_FUNDS` | Win rollback 시 잔액 부족 |
| 404 | `PLAYER_NOT_FOUND` | 플레이어를 찾을 수 없음 |

---

## User API

### Authentication

#### 1. 회원가입

**Endpoint:**
```
POST /api/auth/register
```

**Request Body:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `username` | String | Yes | 3-50자, 영숫자+밑줄 |
| `email` | String | Yes | 유효한 이메일 형식 |
| `password` | String | Yes | 8자 이상 |

**Request Example:**
```http
POST /api/auth/register HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "username": "user123",
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "user123",
  "email": "user@example.com",
  "role": "USER",
  "createdAt": "2026-10-12T15:30:00+09:00"
}
```

**Error (400 Bad Request):**
```json
{
  "message": "Username already exists"
}
```

#### 2. 로그인

**Endpoint:**
```
POST /api/auth/login
```

**Request Body:**

| Field | Type | Required |
|-------|------|----------|
| `username` | String | Yes |
| `password` | String | Yes |

**Request Example:**
```http
POST /api/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "username": "user123",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "user123",
  "email": "user@example.com",
  "role": "USER"
}
```

**Set-Cookie:**
```
JSESSIONID=A1B2C3D4E5F6; Path=/; HttpOnly; SameSite=Lax
```

**Error (401 Unauthorized):**
```json
{
  "message": "Invalid username or password"
}
```

#### 3. 로그아웃

**Endpoint:**
```
POST /api/auth/logout
```

**Request Example:**
```http
POST /api/auth/logout HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

#### 4. 현재 사용자 정보

**Endpoint:**
```
GET /api/auth/me
```

**Request Example:**
```http
GET /api/auth/me HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "user123",
  "email": "user@example.com",
  "role": "USER"
}
```

**Error (401 Unauthorized):**
```json
{
  "message": "Authentication required"
}
```

---

### Wallet

#### 1. 지갑 조회

**Endpoint:**
```
GET /api/wallet
```

**Request Example:**
```http
GET /api/wallet HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": 1,
  "balance": 1000.00,
  "currencyType": "DEMO",
  "createdAt": "2026-10-12T15:30:00+09:00",
  "updatedAt": "2026-10-12T16:45:00+09:00"
}
```

#### 2. 거래 내역 조회

**Endpoint:**
```
GET /api/wallet/transactions
```

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | 0 | 페이지 번호 (0부터 시작) |
| `size` | Integer | 20 | 페이지 크기 (최대 100) |

**Request Example:**
```http
GET /api/wallet/transactions?page=0&size=20 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 10,
      "transactionType": "GAME_WIN",
      "amount": 5.00,
      "balanceBefore": 997.50,
      "balanceAfter": 1002.50,
      "description": "Win: win-tx-650e8400 (Round: round-6ba7b810, Bet: bet-tx-550e8400)",
      "referenceId": "DEMO-win-tx-650e8400",
      "createdAt": "2026-10-12T16:45:30+09:00"
    },
    {
      "id": 9,
      "transactionType": "GAME_BET",
      "amount": -2.50,
      "balanceBefore": 1000.00,
      "balanceAfter": 997.50,
      "description": "Bet: bet-tx-550e8400 (Round: round-6ba7b810)",
      "referenceId": "DEMO-bet-tx-550e8400",
      "createdAt": "2026-10-12T16:45:15+09:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

#### 3. 가상 머니 지급 (개발용)

**Endpoint:**
```
POST /api/wallet/dev/deposit
```

**Request Body:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `amount` | BigDecimal | Yes | 양수 |
| `description` | String | No | 최대 255자 |

**Request Example:**
```http
POST /api/wallet/dev/deposit HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Cookie: JSESSIONID=A1B2C3D4E5F6

{
  "amount": 1000.00,
  "description": "Initial balance"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": 1,
  "balance": 2000.00,
  "currencyType": "DEMO"
}
```

---

### Games

#### 1. 게임 목록 조회

**Endpoint:**
```
GET /api/games
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `category` | String | No | 게임 카테고리 (SLOTS, CARDS, ROULETTE, DICE) |

**Request Example:**
```http
GET /api/games?category=SLOTS HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Demo Slot",
    "category": "SLOTS",
    "description": "5x3 릴, 25 페이라인 슬롯 게임",
    "thumbnailUrl": "/images/demo-slot.png",
    "status": "ACTIVE",
    "minBet": 0.10,
    "maxBet": 100.00,
    "providerId": 1,
    "providerGameId": "1"
  }
]
```

#### 2. 게임 상세 조회

**Endpoint:**
```
GET /api/games/{id}
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | Yes | 게임 ID |

**Request Example:**
```http
GET /api/games/1 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Demo Slot",
  "category": "SLOTS",
  "description": "5x3 릴, 25 페이라인 슬롯 게임",
  "thumbnailUrl": "/images/demo-slot.png",
  "status": "ACTIVE",
  "minBet": 0.10,
  "maxBet": 100.00,
  "providerId": 1,
  "providerGameId": "1"
}
```

**Error (404 Not Found):**
```json
{
  "message": "Game not found"
}
```

#### 3. 게임 실행

**Endpoint:**
```
POST /api/games/{id}/launch
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | Yes | 게임 ID |

**Request Body:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `currency` | String | No | "DEMO" | 통화 코드 |

**Request Example:**
```http
POST /api/games/1/launch HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Cookie: JSESSIONID=A1B2C3D4E5F6

{
  "currency": "DEMO"
}
```

**Response (200 OK):**
```json
{
  "launchUrl": "/demo-game?game=1",
  "sessionToken": "550e8400-e29b-41d4-a716-446655440000",
  "sessionId": 1
}
```

**Notes:**
- `sessionToken`은 plain UUID (1회 전송)
- 서버는 SHA-256 해시만 저장
- 세션 유효기간: 24시간

---

## Demo Game API

### 1. 스핀 (Spin)

Demo Slot 게임의 스핀을 실행합니다.

**Endpoint:**
```
POST /api/demo-provider/sessions/{sessionToken}/spin
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `sessionToken` | String | Yes | 게임 실행 시 받은 세션 토큰 |

**Headers:**

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Idempotency-Key` | String | Yes | 고유 키 (중복 방지) |

**Request Body:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `betPerLine` | BigDecimal | Yes | ≥0.01 (라인당 베팅 금액) |

**Request Example:**
```http
POST /api/demo-provider/sessions/550e8400-e29b-41d4-a716-446655440000/spin HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Idempotency-Key: spin-850e8400-e29b-41d4-a716-446655440003
Cookie: JSESSIONID=A1B2C3D4E5F6

{
  "betPerLine": 0.10
}
```

**Response (200 OK):**
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
  "roundId": "round-6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "status": "WIN_SUCCESS"
}
```

**게임 로직:**
- 총 베팅 금액 = betPerLine × 25 (paylines)
- 예: betPerLine = 0.10 → 총 베팅 = 2.50
- 서버에서 게임 결과 생성 (클라이언트 신뢰 안 함)
- 베팅 차감 → 게임 실행 → 승리 지급 (Provider Wallet API 호출)

**멱등성:**
- 동일한 `Idempotency-Key`로 재요청 시, 기존 라운드 결과 반환
- 재추첨 없음

**Error Responses:**

| Status | Description |
|--------|-------------|
| 401 | 세션 토큰 무효 또는 만료 |
| 400 | Idempotency-Key 누락 |
| 403 | 잔액 부족 (bet 실패) |
| 500 | 게임 처리 오류 |

---

## 에러 코드

### Provider Wallet API 에러 코드

| Code | HTTP Status | Description | 재시도 가능 |
|------|-------------|-------------|-------------|
| `PLAYER_NOT_FOUND` | 404 | 플레이어를 찾을 수 없음 | No |
| `INSUFFICIENT_FUNDS` | 403 | 잔액 부족 | No |
| `INVALID_AMOUNT` | 400 | 잘못된 금액 (음수, null, 0 for bet) | No |
| `TRANSACTION_CONFLICT` | 409 | 거래 충돌 (중복 referenceId) | No |
| `UNSUPPORTED_CURRENCY` | 400 | 지원하지 않는 통화 | No |
| `AUTHENTICATION_FAILED` | 401 | 인증 실패 (잘못된 signature) | No |
| `INVALID_SIGNATURE` | 401 | 잘못된 HMAC 서명 | No |
| `NONCE_ALREADY_USED` | 400 | Nonce 재사용 감지 | No |
| `TIMESTAMP_EXPIRED` | 400 | Timestamp 만료 (±5분 초과) | Yes |
| `ORIGINAL_TRANSACTION_NOT_FOUND` | 404 | 원본 거래를 찾을 수 없음 | No |
| `RETRYABLE_ERROR` | 500 | 재시도 가능한 내부 오류 | Yes |

### User API 에러 응답

User API는 간단한 에러 메시지를 반환합니다:

```json
{
  "message": "Error description"
}
```

**일반적인 HTTP 상태 코드:**
- `400 Bad Request`: 잘못된 요청 (유효성 검증 실패)
- `401 Unauthorized`: 인증 필요 또는 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스를 찾을 수 없음
- `500 Internal Server Error`: 서버 내부 오류

---

## 변경 이력

### v1.0.0 (2026-10-12)
- 초기 버전 작성
- Provider Wallet API 명세
- User API 명세
- Demo Game API 명세
- HMAC-SHA256 인증 규격

---

**문의**: 프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.
