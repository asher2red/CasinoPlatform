# Seamless Wallet 연동 구조 설계

## 1. 개요

### 1.1 목적
기존 자체 슬롯 중심 구조를 외부 게임 공급사 연동을 수용할 수 있는 Seamless Wallet 구조로 전환합니다.

### 1.2 범위
- **구현**: DEMO 가상 머니 기반의 Provider Wallet API와 임시 슬롯 연동
- **제외**: 실제 공급사 연결, 실제 금전 결제
- **목표**: 로컬에서 실행·검증 가능한 연동 구조 완성

### 1.3 제약사항
- Spring Boot 모놀리스 구조 유지
- 외부 서버나 메시지 큐 추가 없음
- 기존 V1~V4 마이그레이션 수정 금지
- 기존 사용자 API와 관리자 기능 보존

## 2. 아키텍처

### 2.1 책임 분리

```
┌─────────────────────────────────────────────────────────────┐
│                    Casino Platform                          │
├─────────────────────────────────────────────────────────────┤
│ User API (Session-based)                                    │
│  - 회원가입/로그인                                             │
│  - 게임 실행 (launch)                                         │
│  - 지갑 조회                                                  │
│  - 거래 내역 조회                                             │
│  - dev/deposit (DEMO 머니 충전)                              │
├─────────────────────────────────────────────────────────────┤
│ Wallet Core Service                                         │
│  - 잔액 변경 통합 처리                                         │
│  - 거래 원장 기록                                             │
│  - 중복 방지 (providerCode + transactionId)                  │
│  - 동시성 제어 (DB 트랜잭션 + 행 잠금)                         │
│  - 취소·정정 처리                                             │
├─────────────────────────────────────────────────────────────┤
│ Provider Wallet API (HMAC-authenticated)                    │
│  - POST /balance   (잔액 조회)                               │
│  - POST /bet       (베팅)                                     │
│  - POST /win       (당첨금 지급)                              │
│  - POST /rollback  (취소)                                     │
├─────────────────────────────────────────────────────────────┤
│ Provider Adapter Interface                                  │
│  - 공급사별 요청/응답 변환                                     │
│  - 게임 실행 URL 생성                                         │
│  - 공급사 식별자 ↔ 내부 게임 매핑                             │
├─────────────────────────────────────────────────────────────┤
│ Demo Provider Implementation                                │
│  - 슬롯 게임 결과 계산                                         │
│  - Wallet API 호출 (JVM 내부 호출)                           │
│  - 라운드 상태 관리                                           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 컴포넌트

#### Platform Layer
- **AuthService**: 사용자 인증/세션
- **GameLaunchService**: 게임 실행 세션 생성
- **PlayerMappingService**: 사용자 ↔ Provider PlayerId 매핑

#### Wallet Layer
- **WalletCoreService**: 잔액 변경 통합 처리
- **LedgerService**: 거래 원장 기록
- **IdempotencyService**: 중복 요청 방지
- **RollbackService**: 취소 처리

#### Provider Layer
- **ProviderAuthFilter**: 공급사 요청 인증 (HMAC)
- **ProviderWalletController**: Wallet API 엔드포인트
- **ProviderAdapter**: 공급사별 변환 인터페이스
- **DemoProviderAdapter**: DEMO 구현

#### Demo Game Layer
- **DemoGameController**: 슬롯 스핀 사용자 API
- **SlotEngine**: 릴 결과 계산
- **DemoRoundService**: 라운드 상태 관리

## 3. API 명세

### 3.1 사용자 API (기존 유지)

#### GET /api/wallet
- 인증: 사용자 세션 쿠키
- 응답: `{ balance, currency }`

#### GET /api/wallet/transactions?page=0&size=20
- 인증: 사용자 세션 쿠키
- 응답: Page<LedgerDTO>

#### POST /api/wallet/dev/deposit
- 인증: 사용자 세션 쿠키
- 헤더: `Idempotency-Key` (필수)
- 요청: `{ amount }`
- 응답: `{ balance, transactionId }`
- 환경: dev/test만 활성화

#### POST /api/games/{gameId}/launch
- 인증: 사용자 세션 쿠키
- 응답: `{ launchUrl, sessionToken }`

### 3.2 Provider Wallet API

Base Path: `/api/provider-wallet/v1/{providerCode}`

공통 헤더:
- `X-Provider-Signature`: HMAC-SHA256 서명
- `X-Provider-Key-Id`: 공급사 Key ID
- `X-Provider-Timestamp`: Unix timestamp (ms)
- `X-Provider-Nonce`: 고유 nonce

#### POST /balance
```json
{
  "playerId": "user123",
  "currency": "DEMO",
  "sessionToken": "abc..."
}
```
응답:
```json
{
  "code": "SUCCESS",
  "balance": "1234.56",
  "currency": "DEMO"
}
```

#### POST /bet
```json
{
  "transactionId": "bet-uuid",
  "playerId": "user123",
  "currency": "DEMO",
  "gameId": "slot001",
  "roundId": "round-uuid",
  "amount": "10.00",
  "sessionToken": "abc..."
}
```
응답:
```json
{
  "code": "SUCCESS",
  "transactionId": "bet-uuid",
  "balance": "1224.56",
  "currency": "DEMO"
}
```

#### POST /win
```json
{
  "transactionId": "win-uuid",
  "playerId": "user123",
  "currency": "DEMO",
  "gameId": "slot001",
  "roundId": "round-uuid",
  "amount": "25.50",
  "originalTransactionId": "bet-uuid"
}
```
응답: 동일 구조

#### POST /rollback
```json
{
  "transactionId": "rollback-uuid",
  "playerId": "user123",
  "currency": "DEMO",
  "originalTransactionId": "bet-uuid"
}
```
응답: 동일 구조

### 3.3 Demo Game API

#### POST /api/demo-provider/sessions/{sessionToken}/spin
- 인증: 사용자 세션 쿠키 + sessionToken 소유권 검증
- 헤더: `Idempotency-Key` (필수)
- 요청: `{ betPerLine }`
- 응답: `{ reels, winAmount, balance, winningLines }`

## 4. 오류 코드

```java
enum ProviderErrorCode {
    SUCCESS,
    INVALID_REQUEST,           // 잘못된 요청 형식
    INVALID_SIGNATURE,         // 서명 검증 실패
    INVALID_TIMESTAMP,         // 타임스탬프 만료
    NONCE_REUSED,             // nonce 재사용
    PLAYER_NOT_FOUND,         // 플레이어 없음
    INVALID_SESSION,          // 세션 무효
    UNSUPPORTED_CURRENCY,     // 지원하지 않는 통화
    INSUFFICIENT_FUNDS,       // 잔액 부족
    TRANSACTION_CONFLICT,     // 거래 ID 충돌 (다른 내용)
    ORIGINAL_TRANSACTION_NOT_FOUND,  // 원거래 없음
    PLAYER_SUSPENDED,         // 사용자 정지
    GAME_INACTIVE,            // 게임 비활성
    INVALID_AMOUNT,           // 잘못된 금액
    RETRYABLE_ERROR          // 일시적 오류
}
```

HTTP 상태 매핑:
- `SUCCESS`: 200 OK
- `INVALID_*`, `UNSUPPORTED_*`: 400 Bad Request
- `PLAYER_NOT_FOUND`, `ORIGINAL_TRANSACTION_NOT_FOUND`: 404 Not Found
- `INSUFFICIENT_FUNDS`, `PLAYER_SUSPENDED`: 403 Forbidden
- `TRANSACTION_CONFLICT`: 409 Conflict
- `RETRYABLE_ERROR`: 503 Service Unavailable

## 5. 데이터베이스 설계

### 5.1 신규 테이블

#### providers (V5)
```sql
CREATE TABLE providers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    key_id VARCHAR(100) NOT NULL,
    secret_key VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### player_provider_mappings (V5)
```sql
CREATE TABLE player_provider_mappings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    provider_player_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider_id, provider_player_id),
    UNIQUE(user_id, provider_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (provider_id) REFERENCES providers(id)
);
```

#### game_sessions (V5)
```sql
CREATE TABLE game_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    session_token_hash VARCHAR(64) NOT NULL UNIQUE,
    currency VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (provider_id) REFERENCES providers(id),
    FOREIGN KEY (game_id) REFERENCES games(id),
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'CLOSED'))
);
```

#### provider_transactions (V6)
```sql
CREATE TABLE provider_transactions (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    transaction_id VARCHAR(200) NOT NULL,
    user_id BIGINT NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2),
    currency VARCHAR(20) NOT NULL,
    game_id BIGINT,
    round_id VARCHAR(200),
    original_transaction_id VARCHAR(200),
    ledger_id BIGINT,
    balance_after NUMERIC(19, 2),
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    request_body TEXT,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider_id, transaction_id),
    FOREIGN KEY (provider_id) REFERENCES providers(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (ledger_id) REFERENCES ledger(id),
    CHECK (operation_type IN ('BALANCE', 'BET', 'WIN', 'ROLLBACK')),
    CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING'))
);
```

#### nonce_store (V6)
```sql
CREATE TABLE nonce_store (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    nonce VARCHAR(100) NOT NULL,
    timestamp BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    UNIQUE(provider_id, nonce),
    FOREIGN KEY (provider_id) REFERENCES providers(id)
);
CREATE INDEX idx_nonce_store_expires_at ON nonce_store(expires_at);
```

#### rollback_intents (V6)
```sql
CREATE TABLE rollback_intents (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    rollback_transaction_id VARCHAR(200) NOT NULL,
    original_transaction_id VARCHAR(200) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider_id, rollback_transaction_id),
    FOREIGN KEY (provider_id) REFERENCES providers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### demo_rounds (V7)
```sql
CREATE TABLE demo_rounds (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    game_session_id BIGINT NOT NULL,
    round_id VARCHAR(200) NOT NULL UNIQUE,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    bet_amount NUMERIC(19, 2) NOT NULL,
    bet_transaction_id VARCHAR(200),
    win_amount NUMERIC(19, 2),
    win_transaction_id VARCHAR(200),
    reel_result JSONB,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id),
    CHECK (status IN ('BET_PENDING', 'BET_SUCCESS', 'WIN_PENDING', 'WIN_SUCCESS', 'FAILED'))
);
```

### 5.2 기존 테이블 확장 (V8)

#### ledger 확장
```sql
ALTER TABLE ledger ADD COLUMN provider_transaction_id BIGINT;
ALTER TABLE ledger ADD FOREIGN KEY (provider_transaction_id) 
    REFERENCES provider_transactions(id);
CREATE INDEX idx_ledger_provider_transaction_id ON ledger(provider_transaction_id);
```

#### games 확장
```sql
ALTER TABLE games ADD COLUMN provider_id BIGINT;
ALTER TABLE games ADD COLUMN provider_game_id VARCHAR(100);
ALTER TABLE games ADD FOREIGN KEY (provider_id) REFERENCES providers(id);
CREATE INDEX idx_games_provider_id ON games(provider_id);
```

## 6. 인증 및 보안

### 6.1 Provider 인증 - HMAC-SHA256

서명 생성 방식 (예시 계약):
```
String signatureBase = 
    method + "\n" +
    path + "\n" +
    timestamp + "\n" +
    nonce + "\n" +
    requestBodyJson;

String signature = HMAC_SHA256(secretKey, signatureBase);
String encodedSignature = Base64.encode(signature);
```

검증:
1. 타임스탬프 검증 (±5분 허용)
2. Nonce 중복 검증
3. 서명 재계산 및 상수 시간 비교
4. providerCode와 keyId 일치 확인

### 6.2 사용자 인증

- 기존 세션 기반 인증 유지
- CSRF 보호: 사용자 API는 CSRF 토큰 검증
- Provider Wallet API는 CSRF 제외 (별도 SecurityFilterChain)

### 6.3 게임 세션 토큰

- 길이 32+ 랜덤 바이트 (Base64 인코딩)
- DB에 SHA-256 해시 저장
- 만료 시간: 기본 24시간
- 사용자·공급사·게임·통화 연결

## 7. 거래 흐름

### 7.1 게임 실행

```
User -> Platform: POST /api/games/{gameId}/launch
Platform: 
  1. 사용자 인증 확인
  2. 사용자 상태 확인 (ACTIVE)
  3. 게임 활성 상태 확인
  4. Provider playerId 조회/생성
  5. 게임 세션 생성 (토큰 생성)
  6. Provider Adapter 호출
Platform -> User: { launchUrl, sessionToken }

User -> Browser: launchUrl 열기
Browser -> Demo Provider UI
```

### 7.2 스핀 (Demo)

```
Browser -> Platform: POST /demo-provider/sessions/{token}/spin
                     Header: Idempotency-Key
                     Body: { betPerLine }
Platform:
  1. 세션 토큰 검증
  2. 세션 소유권 확인
  3. Idempotency 체크 (demo_rounds)
  4. 총 베팅액 계산 (betPerLine * 25)
  5. 라운드 생성 (BET_PENDING)
  
  6. BET 호출:
     - Provider Wallet API (내부)
     - WalletCore -> 잔액 차감
     - provider_transactions 기록
     - demo_rounds 상태 -> BET_SUCCESS
  
  7. 슬롯 결과 계산:
     - SlotEngine.spin()
     - 결과를 demo_rounds.reel_result에 저장
  
  8. WIN 호출 (winAmount > 0):
     - Provider Wallet API (내부)
     - WalletCore -> 잔액 지급
     - provider_transactions 기록
     - demo_rounds 상태 -> WIN_SUCCESS

Platform -> Browser: { reels, winAmount, balance }
```

재시도 시:
- 같은 Idempotency-Key로 요청
- 기존 demo_round 조회
- 상태에 따라:
  - BET_PENDING: 베팅 재시도
  - BET_SUCCESS: 저장된 결과 반환 (재추첨 안 함)
  - WIN_PENDING: 당첨금 지급 재시도

### 7.3 취소 (Rollback)

```
Provider -> Platform: POST /provider-wallet/v1/{code}/rollback
                      { transactionId, originalTransactionId }

Platform:
  1. 인증 검증
  2. provider_transactions에서 originalTx 조회
  3. 중복 확인 (rollback_intents)
  
  Case A: 원거래 존재
    - 원거래 금액 조회
    - 역방향 ledger 생성
    - provider_transactions 기록 (ROLLBACK)
  
  Case B: 원거래 미존재
    - rollback_intents에 기록
    - 나중에 원거래 도착 시 거절
  
  Case C: 이미 취소됨
    - 기존 결과 반환 (중복)

Platform -> Provider: { code, balance }
```

## 8. 중복 처리

### 8.1 Provider 거래

식별자: `(provider_id, transaction_id)` UNIQUE

동일 ID 재요청:
1. provider_transactions 조회
2. 요청 내용 비교 (playerId, amount, currency, operation 등)
3. 일치: 기존 결과 반환 (당시 balance_after)
4. 불일치: TRANSACTION_CONFLICT 오류

### 8.2 Demo 스핀

식별자: `idempotency_key` UNIQUE

동일 키 재요청:
1. demo_rounds 조회
2. 상태에 따라 처리:
   - BET_PENDING/BET_SUCCESS: 재시도 또는 기존 결과
   - WIN_SUCCESS: 기존 결과 그대로 반환

### 8.3 Dev Deposit

식별자: `Idempotency-Key` 헤더 → reference_id

기존 로직 유지 (ledger.reference_id 중복 체크)

## 9. 동시성 제어

### 9.1 Wallet 잔액 변경

```java
@Transactional
public void updateBalance(...) {
    // 1. SELECT ... FOR UPDATE (행 잠금)
    Wallet wallet = walletRepository.findByIdWithLock(walletId);
    
    // 2. 잔액 검증
    if (wallet.getBalance().compareTo(amount) < 0) {
        throw new InsufficientFundsException();
    }
    
    // 3. 잔액 변경
    wallet.updateBalance(amount);
    
    // 4. Ledger 저장
    ledgerRepository.save(ledger);
    
    // 5. Wallet 저장 (optimistic lock 체크)
    walletRepository.save(wallet);
}
```

- DB 트랜잭션 + 행 잠금으로 순서 보장
- @Version은 추가 안전장치
- 트랜잭션은 짧게 유지

### 9.2 UNIQUE 제약 충돌

```java
try {
    // 신규 거래 저장 시도
    providerTransactionRepository.save(tx);
} catch (DataIntegrityViolationException e) {
    // UNIQUE 충돌 - 트랜잭션 롤백됨
    // 별도 읽기 트랜잭션으로 기존 결과 조회
    return findExistingResult(providerId, transactionId);
}
```

## 10. 구현 단계

### Phase 1: DB 및 인프라 (Step 1~3)
- **Step 1**: V5 마이그레이션 (providers, mappings, sessions)
- **Step 2**: V6 마이그레이션 (provider_transactions, nonce, rollback_intents)
- **Step 3**: V7 마이그레이션 (demo_rounds)

### Phase 2: Provider 인증 및 Core (Step 4~6)
- **Step 4**: Provider entity 및 설정 관리
- **Step 5**: HMAC 인증 필터 및 SecurityFilterChain 분리
- **Step 6**: WalletCoreService 리팩토링

### Phase 3: Provider Wallet API (Step 7~10)
- **Step 7**: ProviderWalletController (/balance, /bet)
- **Step 8**: WIN 엔드포인트
- **Step 9**: ROLLBACK 엔드포인트 및 선후관계 처리
- **Step 10**: 중복 처리 및 에러 핸들링

### Phase 4: 게임 실행 (Step 11~13)
- **Step 11**: 게임 세션 생성 및 launch API
- **Step 12**: Provider Adapter 인터페이스
- **Step 13**: Demo Provider Adapter 구현

### Phase 5: Demo Game 전환 (Step 14~15)
- **Step 14**: Demo Round Service 및 상태 관리
- **Step 15**: Demo 스핀 API 전환

### Phase 6: 테스트 (Step 16~17)
- **Step 16**: 통합 테스트 작성
- **Step 17**: 보안 및 동시성 테스트

### Phase 7: 마이그레이션 (Step 18~19)
- **Step 18**: V8 마이그레이션 (ledger, games 확장)
- **Step 19**: 기존 데이터 호환 및 전환 완료

### Phase 8: 문서화 (Step 20)
- **Step 20**: README, CLAUDE.md 갱신

## 11. 테스트 체크리스트

### 11.1 인증 및 권한
- [ ] 비로그인 사용자의 지갑 API 접근 차단
- [ ] 다른 사용자의 게임 세션 토큰 사용 차단
- [ ] Provider 인증 실패 (서명 없음, 잘못된 서명, 만료 timestamp)
- [ ] Nonce 재사용 거절

### 11.2 중복 처리
- [ ] 동일 transactionId 순차 재전송 → 잔액 1회 변경
- [ ] 동일 transactionId 동시 재전송 → 잔액 1회 변경
- [ ] 동일 transactionId + 다른 금액 → TRANSACTION_CONFLICT
- [ ] 동일 Idempotency-Key 스핀 → 재추첨 없음

### 11.3 잔액 및 동시성
- [ ] 잔액 부족 시 베팅 거절
- [ ] 동시 베팅에서 음수 잔액 방지
- [ ] 잔액 변경과 ledger 저장 원자성
- [ ] 10개 동시 베팅 → 모두 정확히 처리

### 11.4 취소 처리
- [ ] BET 취소 → 잔액 복구
- [ ] WIN 취소 → 잔액 차감
- [ ] 동일 원거래 2회 취소 → 1회만 적용
- [ ] 원거래보다 먼저 도착한 취소 → rollback_intents 기록
- [ ] 취소 후 원거래 도착 → 거절

### 11.5 게임 흐름
- [ ] 게임 launch → 세션 토큰 발급
- [ ] 만료된 세션 토큰 사용 → 거절
- [ ] 정지된 사용자 신규 베팅 → 거절
- [ ] 정지된 사용자 기존 라운드 정산 → 허용

### 11.6 데이터 정합성
- [ ] 잘못된 금액 소수점 → 거절
- [ ] 음수 베팅/당첨금 → 거절
- [ ] 0원 당첨 → 거래 기록 생성
- [ ] 스핀 중간 실패 후 재시도 → 정산 완료

### 11.7 환경 분리
- [ ] production에서 /api/wallet/dev/deposit 접근 불가

## 12. 남은 제약 및 확장 지점

### 12.1 현재 제약
- 공급사: DEMO만 구현
- 통화: DEMO만 지원
- 게임: 슬롯 1종
- 인증: 예시 HMAC 방식 (실제 공급사 명세 아님)

### 12.2 실제 공급사 연동 시 교체
1. **Provider Adapter 구현**
   - 공급사별 launchUrl 생성 로직
   - 공급사별 게임 ID 매핑
   - 공급사별 오류 코드 변환

2. **인증 방식**
   - 공급사별 인증 구현 (OAuth, API Key 등)
   - 공급사별 SecurityFilterChain 추가

3. **거래 모델**
   - 복합 정산 (bonus, jackpot 등)
   - 공급사별 필드 확장

4. **콜백 URL**
   - 실제 HTTPS 배포
   - Load Balancer 뒤의 클러스터 고려

### 12.3 확장 지점
- provider_transactions.request_body/response_body에 원본 저장
- 공급사별 설정 테이블 확장
- 대사 배치 작업
- 비동기 정산 큐

## 13. 마일스톤

| Phase | Steps | 완료 조건 |
|-------|-------|----------|
| Phase 1 | 1-3 | DB 마이그레이션 성공, 빌드 통과 |
| Phase 2 | 4-6 | Provider 인증 필터 동작, WalletCore 분리 |
| Phase 3 | 7-10 | Provider Wallet API 전체 구현, Postman 테스트 |
| Phase 4 | 11-13 | 게임 launch → sessionToken 발급 확인 |
| Phase 5 | 14-15 | Demo 스핀 → 잔액 변경 확인 |
| Phase 6 | 16-17 | 주요 테스트 통과 |
| Phase 7 | 18-19 | 기존 데이터 유지, 프론트엔드 전환 |
| Phase 8 | 20 | 문서 완성 |

---

**다음 단계**: Phase 1 Step 1 (V5 마이그레이션) 시작
