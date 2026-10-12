# Seamless Wallet Integration Guide

Casino Platform Provider Wallet API 통합 가이드

**Version**: 1.0.0  
**Last Updated**: 2026-10-12

## 목차

- [개요](#개요)
- [통합 절차](#통합-절차)
- [인증 구현](#인증-구현)
- [API 통합](#api-통합)
- [게임 런처 구현](#게임-런처-구현)
- [에러 처리](#에러-처리)
- [테스트](#테스트)
- [운영 체크리스트](#운영-체크리스트)
- [FAQ](#faq)

## 개요

### Seamless Wallet이란?

Seamless Wallet은 플레이어가 플랫폼과 게임 간에 자유롭게 이동하면서 단일 지갑으로 베팅하고 승리 금액을 받을 수 있는 통합 방식입니다.

**전통적인 Transfer Wallet vs Seamless Wallet:**

| 항목 | Transfer Wallet | Seamless Wallet |
|------|----------------|-----------------|
| 지갑 위치 | 플랫폼 + 게임 별도 | 플랫폼 단일 지갑 |
| 자금 이동 | 게임 시작 시 이체 필요 | 실시간 API 호출 |
| 플레이어 경험 | 불편함 (이체 대기) | 원활함 |
| 구현 복잡도 | 낮음 | 높음 |
| 정합성 | 이체 과정에서 오류 가능 | API 레벨 보장 |

### 통합 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    Casino Platform                          │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │         Provider Wallet API                        │   │
│  │  (HMAC-SHA256 Authentication)                      │   │
│  │                                                    │   │
│  │  GET  /balance      ─► 잔액 조회                  │   │
│  │  POST /bet          ─► 베팅 처리 (차감)           │   │
│  │  POST /win          ─► 승리 지급 (증액)           │   │
│  │  POST /rollback     ─► 거래 취소                  │   │
│  └────────────────────────────────────────────────────┘   │
│                       ▲                                     │
└───────────────────────┼─────────────────────────────────────┘
                        │ HTTPS
                        │ JSON
                        │ HMAC-SHA256
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              Game Provider Backend                          │
│                                                             │
│  ┌─────────────────┐      ┌──────────────────────┐        │
│  │  Game Server    │◄────►│  Wallet API Client   │        │
│  │                 │      │  (HMAC Signature)    │        │
│  │  - Spin         │      └──────────────────────┘        │
│  │  - Result       │                                       │
│  │  - RNG          │                                       │
│  └─────────────────┘                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                        ▲
                        │ WebSocket / HTTP
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              Game Client (Browser)                          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Game UI (Unity WebGL / HTML5 Canvas / React)       │  │
│  │  - Displays reels                                    │  │
│  │  - Shows balance                                     │  │
│  │  - Handles user input                                │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 주요 개념

#### 1. Provider
플랫폼에 게임을 제공하는 외부 업체입니다.

**Provider 등록 정보:**
- `code`: Provider 고유 코드 (예: "DEMO", "PG-SOFT")
- `name`: Provider 이름
- `keyId`: API 인증용 Key ID
- `secretKey`: HMAC 서명용 비밀 키

#### 2. Player Mapping
플랫폼의 사용자 ID와 Provider의 플레이어 ID를 매핑합니다.

**매핑 테이블:**
```
user_id (Platform) ↔ provider_player_id (Provider)
```

**예시:**
```
Platform User ID: 123
Provider Player ID: DEMO-123-a1b2c3
```

#### 3. Game Session
게임 실행 시 생성되는 세션입니다.

**세션 정보:**
- `sessionToken`: 클라이언트에 전달되는 plain UUID
- `sessionTokenHash`: 서버에 저장되는 SHA-256 해시
- `expiresAt`: 만료 시간 (24시간)
- `status`: ACTIVE, EXPIRED, CLOSED

#### 4. Transaction Idempotency
동일한 요청의 재전송을 안전하게 처리합니다.

**멱등성 키:**
- Provider Wallet API: `(provider_id, transaction_id)` UNIQUE
- Demo Game API: `idempotency_key` UNIQUE

**동작:**
- 첫 번째 요청: 처리 후 결과 저장
- 두 번째 요청 (동일 키): 저장된 결과 반환 (재처리 안 함)

## 통합 절차

### Step 1: Provider 등록

플랫폼 관리자에게 다음 정보를 제공합니다:

1. **Provider 정보**
   - Provider Code (고유 식별자, 예: "YOUR-PROVIDER")
   - Provider Name (표시 이름)
   - Website URL
   - Support Email

2. **API 인증 정보**
   - Key ID (예: "your-provider-key-001")
   - Secret Key (HMAC 서명용, 최소 32자)

3. **게임 목록**
   - Game ID (Provider 내부 ID)
   - Game Name
   - Category (SLOTS, CARDS, ROULETTE, DICE)
   - Min/Max Bet

**예시 SQL (플랫폼 관리자 실행):**

```sql
-- Provider 등록
INSERT INTO providers (code, name, key_id, secret_key, is_active, created_at, updated_at)
VALUES (
  'YOUR-PROVIDER',
  'Your Provider Name',
  'your-provider-key-001',
  'your-secret-key-min-32-chars-long',
  true,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

-- 게임 등록
INSERT INTO games (name, category, description, thumbnail_url, status, min_bet, max_bet, provider_id, provider_game_id, created_at, updated_at)
VALUES (
  'Your Awesome Slot',
  'SLOTS',
  'Description of your game',
  '/images/your-game.png',
  'ACTIVE',
  0.10,
  100.00,
  (SELECT id FROM providers WHERE code = 'YOUR-PROVIDER'),
  'your-game-001',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);
```

### Step 2: 개발 환경 설정

#### 2.1. API Base URL

**Sandbox:**
```
https://sandbox.casino-platform.example.com/api/provider-wallet/v1
```

**Production:**
```
https://api.casino-platform.example.com/api/provider-wallet/v1
```

#### 2.2. Test Credentials

Sandbox 환경의 테스트 계정:

```json
{
  "providerCode": "TEST-PROVIDER",
  "keyId": "test-key-001",
  "secretKey": "test-secret-key-for-sandbox-only",
  "testPlayerId": "TEST-123"
}
```

### Step 3: HMAC 인증 구현

Provider Wallet API의 모든 요청은 HMAC-SHA256 서명이 필요합니다.

#### 3.1. Required Headers

| Header | Description | Example |
|--------|-------------|---------|
| `X-Provider-Key-Id` | Provider Key ID | "your-provider-key-001" |
| `X-Timestamp` | Unix timestamp (밀리초) | "1697123456789" |
| `X-Nonce` | 고유 UUID (재사용 불가) | "550e8400-e29b-41d4-a716-446655440000" |
| `X-Signature` | HMAC-SHA256 서명 (hex) | "a1b2c3..." |
| `X-Player-Id` | Provider Player ID | "YOUR-PROVIDER-123" |

#### 3.2. Signature Generation Algorithm

```
message = method + url + timestamp + nonce + body
signature = HMAC-SHA256(message, secretKey)
```

**상세 설명:**

1. **method**: HTTP 메서드 (대문자, 예: "POST", "GET")
2. **url**: 경로 + 쿼리 문자열 (예: "/api/provider-wallet/v1/YOUR-PROVIDER/bet")
3. **timestamp**: X-Timestamp 헤더 값 (문자열)
4. **nonce**: X-Nonce 헤더 값 (UUID 문자열)
5. **body**: Request Body JSON 문자열 (GET 요청은 빈 문자열 "")
6. **secretKey**: Provider Secret Key

**주의사항:**
- 순서가 정확해야 함: method → url → timestamp → nonce → body
- 구분자 없음 (단순 연결)
- body는 실제 전송되는 JSON 문자열 그대로 사용
- 최종 signature는 hex 문자열 (lowercase)

### Step 4: HTTP 클라이언트 구현 예제

#### Node.js (JavaScript)

```javascript
const crypto = require('crypto');
const axios = require('axios');

class ProviderWalletClient {
  constructor(baseUrl, providerCode, keyId, secretKey) {
    this.baseUrl = baseUrl;
    this.providerCode = providerCode;
    this.keyId = keyId;
    this.secretKey = secretKey;
  }

  generateSignature(method, path, timestamp, nonce, body) {
    const message = method + path + timestamp + nonce + body;
    return crypto
      .createHmac('sha256', this.secretKey)
      .update(message)
      .digest('hex');
  }

  async request(method, path, data = null, playerId) {
    const timestamp = Date.now().toString();
    const nonce = crypto.randomUUID();
    const body = data ? JSON.stringify(data) : '';
    
    const signature = this.generateSignature(
      method,
      path,
      timestamp,
      nonce,
      body
    );

    const headers = {
      'Content-Type': 'application/json',
      'X-Provider-Key-Id': this.keyId,
      'X-Timestamp': timestamp,
      'X-Nonce': nonce,
      'X-Signature': signature,
      'X-Player-Id': playerId
    };

    const config = {
      method,
      url: this.baseUrl + path,
      headers,
      ...(data && { data })
    };

    try {
      const response = await axios(config);
      return response.data;
    } catch (error) {
      if (error.response) {
        throw new Error(
          `API Error: ${error.response.data.errorCode} - ${error.response.data.errorMessage}`
        );
      }
      throw error;
    }
  }

  // Balance API
  async getBalance(playerId, currency = 'DEMO') {
    const path = `/api/provider-wallet/v1/${this.providerCode}/balance?playerId=${playerId}&currency=${currency}`;
    return await this.request('GET', path, null, playerId);
  }

  // Bet API
  async placeBet(transactionId, playerId, gameId, roundId, amount, currency = 'DEMO') {
    const path = `/api/provider-wallet/v1/${this.providerCode}/bet`;
    const data = {
      transactionId,
      playerId,
      currency,
      gameId,
      roundId,
      amount
    };
    return await this.request('POST', path, data, playerId);
  }

  // Win API
  async processWin(transactionId, playerId, gameId, roundId, amount, originalTransactionId, currency = 'DEMO') {
    const path = `/api/provider-wallet/v1/${this.providerCode}/win`;
    const data = {
      transactionId,
      playerId,
      currency,
      gameId,
      roundId,
      amount,
      originalTransactionId
    };
    return await this.request('POST', path, data, playerId);
  }

  // Rollback API
  async rollback(transactionId, playerId, originalTransactionId, currency = 'DEMO') {
    const path = `/api/provider-wallet/v1/${this.providerCode}/rollback`;
    const data = {
      transactionId,
      playerId,
      currency,
      originalTransactionId
    };
    return await this.request('POST', path, data, playerId);
  }
}

// Usage
const client = new ProviderWalletClient(
  'https://sandbox.casino-platform.example.com',
  'YOUR-PROVIDER',
  'your-provider-key-001',
  'your-secret-key-min-32-chars-long'
);

(async () => {
  try {
    // Get balance
    const balance = await client.getBalance('YOUR-PROVIDER-123');
    console.log('Balance:', balance);

    // Place bet
    const betResult = await client.placeBet(
      'bet-' + crypto.randomUUID(),
      'YOUR-PROVIDER-123',
      'your-game-001',
      'round-' + crypto.randomUUID(),
      2.50
    );
    console.log('Bet Result:', betResult);

    // Process win
    const winResult = await client.processWin(
      'win-' + crypto.randomUUID(),
      'YOUR-PROVIDER-123',
      'your-game-001',
      betResult.roundId,
      5.00,
      betResult.transactionId
    );
    console.log('Win Result:', winResult);
  } catch (error) {
    console.error('Error:', error.message);
  }
})();
```

#### Java (Spring Boot)

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ProviderWalletClient {
    private final String baseUrl;
    private final String providerCode;
    private final String keyId;
    private final String secretKey;
    private final RestTemplate restTemplate;

    public ProviderWalletClient(String baseUrl, String providerCode, String keyId, String secretKey) {
        this.baseUrl = baseUrl;
        this.providerCode = providerCode;
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.restTemplate = new RestTemplate();
    }

    private String generateSignature(String method, String path, String timestamp, String nonce, String body) {
        try {
            String message = method + path + timestamp + nonce + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    public WalletResponse placeBet(String transactionId, String playerId, 
                                   String gameId, String roundId, BigDecimal amount) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        String path = String.format("/api/provider-wallet/v1/%s/bet", providerCode);
        
        BetRequest request = new BetRequest(
            transactionId, playerId, "DEMO", gameId, roundId, amount);
        String body = new ObjectMapper().writeValueAsString(request);
        
        String signature = generateSignature("POST", path, timestamp, nonce, body);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Provider-Key-Id", keyId);
        headers.set("X-Timestamp", timestamp);
        headers.set("X-Nonce", nonce);
        headers.set("X-Signature", signature);
        headers.set("X-Player-Id", playerId);
        
        HttpEntity<BetRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<WalletResponse> response = restTemplate.exchange(
            baseUrl + path, HttpMethod.POST, entity, WalletResponse.class);
        
        return response.getBody();
    }
}
```

## API 통합

### Typical Game Flow

```
1. Player clicks "Play" button
   ↓
2. Platform calls Game Launch API
   → Returns sessionToken
   ↓
3. Game Client connects to Game Server with sessionToken
   ↓
4. Player clicks "Spin" button
   ↓
5. Game Server → Provider Wallet API: POST /bet
   ← Response: { success: true, balance: 997.50 }
   ↓
6. Game Server generates result (RNG)
   ↓
7. Game Server → Provider Wallet API: POST /win
   ← Response: { success: true, balance: 1002.50 }
   ↓
8. Game Client displays result + updated balance
```

### API Call Sequence

#### 1. Balance Check (Optional)

게임 시작 시 잔액을 조회할 수 있습니다 (선택적).

```javascript
const balance = await client.getBalance('YOUR-PROVIDER-123');
// Response: { success: true, balance: 1000.00, currency: "DEMO" }
```

#### 2. Place Bet (Required)

플레이어가 베팅하면 반드시 호출해야 합니다.

```javascript
const betResult = await client.placeBet(
  'bet-' + generateUUID(),      // Unique transaction ID
  'YOUR-PROVIDER-123',           // Player ID
  'your-game-001',               // Game ID
  'round-' + generateUUID(),     // Unique round ID
  2.50                           // Bet amount
);

// Success Response:
// { success: true, balance: 997.50, currency: "DEMO" }

// Error Response (Insufficient Funds):
// { success: false, errorCode: "INSUFFICIENT_FUNDS", errorMessage: "..." }
```

**중요:**
- `transactionId`는 재전송 시 동일해야 함 (멱등성)
- `roundId`는 게임 라운드당 고유해야 함
- `amount`는 반드시 양수

#### 3. Process Win (Required)

게임 결과에 따라 승리 금액을 지급합니다.

```javascript
const winResult = await client.processWin(
  'win-' + generateUUID(),       // Unique transaction ID
  'YOUR-PROVIDER-123',           // Player ID
  'your-game-001',               // Game ID
  roundId,                       // Same round ID from bet
  5.00,                          // Win amount (0 for no win)
  betTransactionId               // Original bet transaction ID
);

// Response: { success: true, balance: 1002.50, currency: "DEMO" }
```

**무승부 처리:**
- `amount: 0`으로 호출 (무승부 = 승리 금액 0원)
- 플랫폼은 0원도 정상 처리

#### 4. Rollback (Error Recovery)

게임 오류 발생 시 거래를 취소합니다.

```javascript
const rollbackResult = await client.rollback(
  'rollback-' + generateUUID(),  // Unique rollback transaction ID
  'YOUR-PROVIDER-123',           // Player ID
  betTransactionId               // Original transaction ID to rollback
);

// Response: { success: true, balance: 1000.00, currency: "DEMO" }
```

**사용 사례:**
- 게임 서버 오류로 결과 생성 실패
- 네트워크 오류로 win API 호출 실패
- 게임 로직 오류 발견

**주의:**
- 성공한 bet을 rollback하면 잔액 복구
- 성공한 win을 rollback하면 지급 금액 회수
- Rollback 자체도 멱등성 보장

### Idempotency (멱등성) 구현

Provider는 **반드시** 동일한 `transactionId`로 재전송할 수 있어야 합니다.

**예시 시나리오:**

```javascript
// 1. First attempt: Network timeout
try {
  const result = await client.placeBet(txId, playerId, gameId, roundId, amount);
} catch (error) {
  // Network timeout - did the bet succeed on platform?
}

// 2. Retry with SAME transactionId
const result = await client.placeBet(
  txId,       // ✅ Same transaction ID
  playerId,
  gameId,
  roundId,
  amount
);

// Platform returns:
// - If first attempt succeeded: Same result (no duplicate debit)
// - If first attempt failed: Process normally
```

**구현 가이드:**

1. **Transaction ID 생성 및 저장**
   ```javascript
   const txId = 'bet-' + generateUUID();
   await db.savePendingTransaction(txId, { playerId, roundId, amount });
   ```

2. **API 호출 전 재전송 체크**
   ```javascript
   const existing = await db.getTransactionResult(txId);
   if (existing) {
     return existing; // Already processed
   }
   ```

3. **API 호출 및 결과 저장**
   ```javascript
   const result = await client.placeBet(...);
   await db.saveTransactionResult(txId, result);
   ```

4. **Timeout 시 재시도**
   ```javascript
   let retries = 3;
   while (retries > 0) {
     try {
       return await callApiWithSameTransactionId();
     } catch (error) {
       if (error.code === 'TIMEOUT') {
         retries--;
         await sleep(1000);
       } else {
         throw error;
       }
     }
   }
   ```

## 게임 런처 구현

### Game Launch Flow

```
Platform Frontend
  ↓
  POST /api/games/{id}/launch
  ↓
Platform Backend
  - Create GameSession
  - Generate sessionToken (plain UUID)
  - Store SHA-256 hash
  ↓
Response: { launchUrl, sessionToken, sessionId }
  ↓
Platform Frontend
  - Open game in iframe/new tab
  - Pass sessionToken in URL
  ↓
Game Client
  - Connect to Game Server with sessionToken
  ↓
Game Server
  - Validate sessionToken via Demo Game API (optional)
  - Start game
```

### Session Token Handling

**Platform Side (Already Implemented):**
```java
// Generate
String plainToken = UUID.randomUUID().toString();
String hash = SHA-256(plainToken);

// Store
gameSession.setSessionTokenHash(hash);

// Return to client
return new LaunchGameResponse(launchUrl, plainToken, sessionId);
```

**Game Provider Side:**

**Option 1: Direct Game Server**

게임 서버가 직접 세션 토큰을 검증:

```javascript
app.get('/game-client', async (req, res) => {
  const { sessionToken } = req.query;
  
  // Store token for subsequent API calls
  await redis.set(`session:${sessionToken}`, JSON.stringify({
    playerId: 'YOUR-PROVIDER-123',
    gameId: 'your-game-001',
    expiresAt: Date.now() + 24 * 60 * 60 * 1000
  }), 'EX', 86400);
  
  res.render('game-client', { sessionToken });
});
```

**Option 2: Token Validation API (Demo Game API)**

Demo Game API를 사용하여 세션 검증:

```javascript
// POST /api/demo-provider/sessions/{sessionToken}/spin
// This validates session internally
```

### Game Client Implementation

**HTML5 Canvas Example:**

```html
<!DOCTYPE html>
<html>
<head>
  <title>Your Awesome Slot</title>
</head>
<body>
  <div id="game-container">
    <div id="balance">Balance: $<span id="balance-amount">0.00</span></div>
    <canvas id="reels" width="800" height="600"></canvas>
    <button id="spin-btn" onclick="spin()">SPIN ($2.50)</button>
  </div>

  <script>
    const urlParams = new URLSearchParams(window.location.search);
    const sessionToken = urlParams.get('sessionToken');
    
    let currentBalance = 0;
    
    async function spin() {
      const idempotencyKey = 'spin-' + generateUUID();
      
      try {
        const response = await fetch(
          `/api/demo-provider/sessions/${sessionToken}/spin`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Idempotency-Key': idempotencyKey
            },
            credentials: 'include', // Include session cookie
            body: JSON.stringify({ betPerLine: 0.10 })
          }
        );
        
        const result = await response.json();
        
        if (response.ok) {
          updateBalance(result.balance);
          displayReels(result.reels);
          if (result.winAmount > 0) {
            showWinAnimation(result.winAmount, result.winningLines);
          }
        } else {
          alert('Error: ' + result.message);
        }
      } catch (error) {
        console.error('Spin error:', error);
        alert('Network error. Please try again.');
      }
    }
    
    function updateBalance(balance) {
      currentBalance = balance;
      document.getElementById('balance-amount').textContent = balance.toFixed(2);
    }
    
    function generateUUID() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    }
  </script>
</body>
</html>
```

## 에러 처리

### Error Handling Strategy

```javascript
async function callProviderWalletAPI(apiCall) {
  try {
    const result = await apiCall();
    return { success: true, data: result };
  } catch (error) {
    if (error.response) {
      const { errorCode, errorMessage } = error.response.data;
      
      switch (errorCode) {
        case 'INSUFFICIENT_FUNDS':
          // Show "Not enough balance" to player
          return { success: false, reason: 'insufficient_funds' };
        
        case 'PLAYER_NOT_FOUND':
          // Should not happen - log error
          console.error('Player not found:', errorMessage);
          return { success: false, reason: 'player_error' };
        
        case 'TRANSACTION_CONFLICT':
          // Duplicate transaction - safe to ignore
          console.warn('Transaction conflict (idempotent):', errorMessage);
          return { success: false, reason: 'duplicate' };
        
        case 'AUTHENTICATION_FAILED':
        case 'INVALID_SIGNATURE':
          // Critical: Check HMAC implementation
          console.error('Authentication failed:', errorMessage);
          return { success: false, reason: 'auth_error' };
        
        case 'TIMESTAMP_EXPIRED':
        case 'RETRYABLE_ERROR':
          // Retry after delay
          console.warn('Retryable error:', errorMessage);
          await sleep(1000);
          return await callProviderWalletAPI(apiCall); // Retry
        
        default:
          console.error('Unknown error:', errorCode, errorMessage);
          return { success: false, reason: 'unknown' };
      }
    } else {
      // Network error
      console.error('Network error:', error.message);
      return { success: false, reason: 'network_error' };
    }
  }
}
```

### Retry Logic

```javascript
async function callWithRetry(apiCall, maxRetries = 3) {
  let lastError;
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await apiCall();
    } catch (error) {
      lastError = error;
      
      // Retry only on network errors or retryable errors
      if (error.response) {
        const { errorCode } = error.response.data;
        if (!['TIMESTAMP_EXPIRED', 'RETRYABLE_ERROR'].includes(errorCode)) {
          throw error; // Don't retry
        }
      }
      
      if (attempt < maxRetries) {
        const delay = Math.pow(2, attempt) * 1000; // Exponential backoff
        console.log(`Retry attempt ${attempt} after ${delay}ms`);
        await sleep(delay);
      }
    }
  }
  
  throw lastError;
}
```

## 테스트

### Unit Test Example (Jest)

```javascript
const ProviderWalletClient = require('./provider-wallet-client');

describe('ProviderWalletClient', () => {
  let client;
  
  beforeEach(() => {
    client = new ProviderWalletClient(
      'https://sandbox.casino-platform.example.com',
      'TEST-PROVIDER',
      'test-key-001',
      'test-secret-key-for-sandbox-only'
    );
  });
  
  test('should generate correct HMAC signature', () => {
    const signature = client.generateSignature(
      'POST',
      '/api/provider-wallet/v1/TEST-PROVIDER/bet',
      '1697123456789',
      '550e8400-e29b-41d4-a716-446655440000',
      '{"transactionId":"bet-001","amount":2.50}'
    );
    
    // Expected signature (calculated separately)
    expect(signature).toBe('expected-hex-signature');
  });
  
  test('should place bet successfully', async () => {
    const result = await client.placeBet(
      'bet-test-001',
      'TEST-123',
      'test-game-001',
      'round-test-001',
      2.50
    );
    
    expect(result.success).toBe(true);
    expect(result.balance).toBeLessThan(1000.00);
  });
  
  test('should handle insufficient funds', async () => {
    await expect(
      client.placeBet(
        'bet-test-002',
        'TEST-123',
        'test-game-001',
        'round-test-002',
        10000.00 // More than balance
      )
    ).rejects.toThrow('INSUFFICIENT_FUNDS');
  });
  
  test('should handle idempotent bet requests', async () => {
    const txId = 'bet-test-003';
    
    // First request
    const result1 = await client.placeBet(
      txId, 'TEST-123', 'test-game-001', 'round-test-003', 2.50
    );
    
    // Second request with same transaction ID
    const result2 = await client.placeBet(
      txId, 'TEST-123', 'test-game-001', 'round-test-003', 2.50
    );
    
    expect(result1.balance).toBe(result2.balance); // Same result
  });
});
```

### Integration Test Checklist

- [ ] **Balance API**
  - [ ] Get balance successfully
  - [ ] Handle player not found
  - [ ] Handle authentication failure

- [ ] **Bet API**
  - [ ] Place bet with sufficient balance
  - [ ] Handle insufficient funds
  - [ ] Idempotent request (same transaction ID)
  - [ ] Invalid amount (negative, zero)

- [ ] **Win API**
  - [ ] Process win successfully
  - [ ] Process win with amount = 0 (no win)
  - [ ] Idempotent request

- [ ] **Rollback API**
  - [ ] Rollback bet successfully
  - [ ] Rollback win successfully
  - [ ] Out-of-order rollback (rollback before bet arrives)

- [ ] **Security**
  - [ ] Invalid signature rejected
  - [ ] Expired timestamp rejected
  - [ ] Reused nonce rejected

## 운영 체크리스트

### Pre-Launch

- [ ] **API Credentials**
  - [ ] Production Key ID 발급
  - [ ] Production Secret Key 발급 (안전하게 저장)
  - [ ] Sandbox 테스트 완료

- [ ] **Game Registration**
  - [ ] 모든 게임 정보 제공 (name, category, min/max bet)
  - [ ] 게임 썸네일 이미지 제공
  - [ ] 게임 설명 및 규칙 제공

- [ ] **Monitoring**
  - [ ] API 호출 로깅 구현
  - [ ] 에러 알림 설정
  - [ ] Performance monitoring (response time)

- [ ] **Error Handling**
  - [ ] 모든 에러 코드 처리 구현
  - [ ] Retry 로직 구현
  - [ ] Fallback UI 구현

- [ ] **Security**
  - [ ] HMAC signature 검증 로직 테스트
  - [ ] Nonce 중복 방지 확인
  - [ ] HTTPS 통신 확인

### Post-Launch

- [ ] **Monitoring**
  - [ ] 일일 API 호출 통계 확인
  - [ ] 에러율 모니터링 (target: < 1%)
  - [ ] Response time 모니터링 (target: < 500ms)

- [ ] **Reconciliation (정산)**
  - [ ] 일일 거래 내역 확인
  - [ ] 플랫폼과 Provider 간 잔액 일치 확인
  - [ ] Rollback 거래 검토

- [ ] **Support**
  - [ ] 플레이어 문의 대응
  - [ ] 게임 오류 리포트 처리
  - [ ] 플랫폼과의 커뮤니케이션

## FAQ

### Q1: Timestamp 검증 실패가 자주 발생합니다.

**A:** 서버 시간 동기화를 확인하세요.

```bash
# NTP 동기화 확인
ntpdate -q pool.ntp.org

# 현재 시간 확인
date +%s%3N  # Unix timestamp in milliseconds
```

플랫폼 시간과 ±5분 이내여야 합니다.

### Q2: Nonce 재사용 에러가 발생합니다.

**A:** 각 요청마다 새로운 UUID를 생성하세요.

```javascript
// ❌ Wrong: Reusing same nonce
const nonce = 'fixed-nonce-value';

// ✅ Correct: Generate new UUID for each request
const nonce = crypto.randomUUID();
```

### Q3: Bet API 호출 후 네트워크 오류가 발생하면 어떻게 하나요?

**A:** 동일한 `transactionId`로 재시도하세요.

```javascript
const txId = 'bet-' + generateUUID(); // Generate once

try {
  return await client.placeBet(txId, ...);
} catch (error) {
  if (error.code === 'NETWORK_ERROR') {
    // Retry with SAME transaction ID
    return await client.placeBet(txId, ...);
  }
}
```

### Q4: Win API 호출 시 원본 거래를 찾을 수 없다는 에러가 발생합니다.

**A:** `originalTransactionId`가 Bet API의 `transactionId`와 일치하는지 확인하세요.

```javascript
// Bet
const betTxId = 'bet-' + generateUUID();
const betResult = await client.placeBet(betTxId, ...);

// Win
const winTxId = 'win-' + generateUUID();
const winResult = await client.processWin(
  winTxId,
  playerId,
  gameId,
  roundId,
  amount,
  betTxId  // ✅ Use bet transaction ID
);
```

### Q5: Rollback API는 언제 호출해야 하나요?

**A:** 다음 상황에서 호출합니다:

1. 게임 서버 오류로 결과 생성 실패
2. Win API 호출 실패 (네트워크 오류)
3. 게임 로직 오류 발견 (버그, 부정 행위 등)

```javascript
try {
  const betResult = await client.placeBet(...);
  const gameResult = await generateGameResult();
  const winResult = await client.processWin(...);
} catch (error) {
  // Rollback bet
  await client.rollback('rollback-' + generateUUID(), playerId, betTxId);
  throw error;
}
```

### Q6: Out-of-order Rollback이 무엇인가요?

**A:** Rollback 요청이 원본 거래보다 먼저 플랫폼에 도착하는 경우입니다.

**예시:**
```
1. Provider: Send BET request
2. Network delay...
3. Provider: Timeout, decide to rollback
4. Provider: Send ROLLBACK request
5. Platform: Receives ROLLBACK (but BET not arrived yet)
6. Platform: Save RollbackIntent, return SUCCESS
7. Platform: Receives BET (late arrival)
8. Platform: Check RollbackIntent, auto-rollback BET
```

플랫폼이 자동으로 처리하므로 Provider는 신경 쓸 필요 없습니다.

### Q7: 플레이어 ID 형식은 무엇인가요?

**A:** `{providerCode}-{platformUserId}` 형식입니다.

**예시:**
- `YOUR-PROVIDER-123`
- `YOUR-PROVIDER-456`

플랫폼이 자동으로 매핑을 생성하고 관리합니다.

### Q8: 게임 세션이 만료되면 어떻게 되나요?

**A:** 세션은 24시간 후 자동 만료됩니다.

만료된 세션으로 요청 시:
```json
{
  "success": false,
  "errorCode": "SESSION_EXPIRED",
  "errorMessage": "Game session expired"
}
```

플레이어에게 "세션 만료. 게임을 다시 실행하세요." 메시지를 표시하세요.

---

## 지원

### 문의처

- **기술 지원**: tech-support@casino-platform.example.com
- **API 문의**: api-support@casino-platform.example.com
- **운영 문의**: ops@casino-platform.example.com

### 참고 문서

- [API Specification](./API_SPECIFICATION.md)
- [README](./README.md)
- [Implementation Plan](./SEAMLESS_WALLET_IMPLEMENTATION_PLAN.md)

---

**End of Guide**  
**Version**: 1.0.0  
**Last Updated**: 2026-10-12
