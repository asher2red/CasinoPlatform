# Seamless Wallet 구현 계획

## 작업 원칙

1. **점진적 전환**: 기존 기능을 깨뜨리지 않고 단계적으로 추가
2. **작은 커밋**: 각 Step마다 빌드 및 기본 테스트 확인
3. **기존 보존**: V1~V4 마이그레이션 수정 금지, 사용자 데이터 유지
4. **테스트 우선**: 핵심 로직은 반드시 테스트 작성

## Phase 1: DB 및 인프라 (1일)

### Step 1: V5 마이그레이션 - Providers 및 Mappings
**파일**:
- `V5__create_providers_and_mappings.sql`

**내용**:
- providers 테이블
- player_provider_mappings 테이블
- game_sessions 테이블
- DEMO provider 초기 데이터 INSERT

**검증**:
```bash
./gradlew bootRun
# 로그에서 V5 마이그레이션 성공 확인
docker exec casino-platform-db psql -U casino_admin -d casino_platform -c "\d providers"
```

### Step 2: V6 마이그레이션 - Provider Transactions
**파일**:
- `V6__create_provider_transactions.sql`

**내용**:
- provider_transactions 테이블
- nonce_store 테이블
- rollback_intents 테이블

**검증**: 마이그레이션 성공 및 제약 조건 확인

### Step 3: V7 마이그레이션 - Demo Rounds
**파일**:
- `V7__create_demo_rounds.sql`

**내용**:
- demo_rounds 테이블

**검증**: 전체 마이그레이션 통과

---

## Phase 2: Provider 인증 및 Core (2일)

### Step 4: Provider Entity 및 설정
**파일**:
- `backend/src/main/java/com/casino/platform/provider/entity/Provider.java`
- `backend/src/main/java/com/casino/platform/provider/entity/PlayerProviderMapping.java`
- `backend/src/main/java/com/casino/platform/provider/entity/GameSession.java`
- `backend/src/main/java/com/casino/platform/provider/repository/ProviderRepository.java`
- `backend/src/main/java/com/casino/platform/provider/repository/PlayerProviderMappingRepository.java`
- `backend/src/main/java/com/casino/platform/provider/repository/GameSessionRepository.java`
- `backend/src/main/java/com/casino/platform/provider/service/ProviderConfigService.java`

**검증**: 빌드 성공, Provider 조회 가능

### Step 5: HMAC 인증 필터
**파일**:
- `backend/src/main/java/com/casino/platform/provider/security/HmacAuthenticationFilter.java`
- `backend/src/main/java/com/casino/platform/provider/security/ProviderAuthenticationToken.java`
- `backend/src/main/java/com/casino/platform/provider/security/HmacSignatureValidator.java`
- `backend/src/main/java/com/casino/platform/provider/service/NonceService.java`
- `backend/src/main/java/com/casino/platform/config/ProviderSecurityConfig.java` (새 SecurityFilterChain)

**검증**:
- 서명 없는 요청 → 401
- 잘못된 서명 → 401
- 올바른 서명 → 통과

### Step 6: WalletCoreService 리팩토링
**파일**:
- `backend/src/main/java/com/casino/platform/wallet/service/WalletCoreService.java` (신규)
- 기존 `WalletService.java` 수정 (사용자 API용 래퍼로 변경)

**변경**:
```java
// 기존
@Transactional
public Ledger deposit(Long walletId, ...) { ... }

// 신규
@Transactional
public WalletTransactionResult executeTransaction(WalletTransactionRequest req) {
    // 1. 중복 체크 (providerId + transactionId)
    // 2. SELECT FOR UPDATE
    // 3. 잔액 변경
    // 4. Ledger 저장
    // 5. Provider Transaction 저장
    return result;
}
```

**검증**: 기존 dev/deposit 동작 유지

---

## Phase 3: Provider Wallet API (3일)

### Step 7: Balance 및 Bet 엔드포인트
**파일**:
- `backend/src/main/java/com/casino/platform/provider/wallet/dto/BalanceRequest.java`
- `backend/src/main/java/com/casino/platform/provider/wallet/dto/BetRequest.java`
- `backend/src/main/java/com/casino/platform/provider/wallet/dto/ProviderWalletResponse.java`
- `backend/src/main/java/com/casino/platform/provider/wallet/ProviderWalletController.java`
- `backend/src/main/java/com/casino/platform/provider/wallet/ProviderWalletService.java`

**구현**:
```java
@RestController
@RequestMapping("/api/provider-wallet/v1/{providerCode}")
public class ProviderWalletController {
    
    @PostMapping("/balance")
    public ProviderWalletResponse getBalance(...) { ... }
    
    @PostMapping("/bet")
    public ProviderWalletResponse placeBet(...) { ... }
}
```

**검증**: Postman으로 HMAC 서명 생성 후 호출

### Step 8: Win 엔드포인트
**파일**: 동일 Controller에 추가

**구현**:
```java
@PostMapping("/win")
public ProviderWalletResponse creditWin(
    @PathVariable String providerCode,
    @RequestBody WinRequest request
) {
    // 1. 원베팅 조회 및 검증
    // 2. 잔액 지급
    // 3. Provider Transaction 저장
}
```

**검증**: Bet → Win 순서 호출

### Step 9: Rollback 엔드포인트
**파일**:
- `backend/src/main/java/com/casino/platform/provider/rollback/RollbackService.java`
- `backend/src/main/java/com/casino/platform/provider/rollback/entity/RollbackIntent.java`

**구현**:
- 원거래 존재: 역거래 생성
- 원거래 없음: rollback_intents에 기록
- 중복 취소: 기존 결과 반환

**검증**:
- Bet → Rollback → 잔액 복구
- Rollback → Bet → Bet 거절

### Step 10: 오류 처리 및 중복 검증
**파일**:
- `backend/src/main/java/com/casino/platform/provider/exception/ProviderExceptionHandler.java`
- `backend/src/main/java/com/casino/platform/provider/exception/ProviderErrorCode.java`

**검증**:
- 동일 transactionId 재전송 → 기존 결과
- 동일 transactionId + 다른 금액 → TRANSACTION_CONFLICT

---

## Phase 4: 게임 실행 (2일)

### Step 11: 게임 Launch API
**파일**:
- `backend/src/main/java/com/casino/platform/game/service/GameLaunchService.java`
- `backend/src/main/java/com/casino/platform/game/controller/GameLaunchController.java`
- `backend/src/main/java/com/casino/platform/game/dto/LaunchGameRequest.java`
- `backend/src/main/java/com/casino/platform/game/dto/LaunchGameResponse.java`

**구현**:
```java
POST /api/games/{gameId}/launch
Response: { launchUrl, sessionToken }
```

**검증**: 토큰 발급 및 game_sessions에 저장

### Step 12: Provider Adapter 인터페이스
**파일**:
- `backend/src/main/java/com/casino/platform/provider/adapter/ProviderAdapter.java` (인터페이스)
- `backend/src/main/java/com/casino/platform/provider/adapter/ProviderAdapterFactory.java`

**인터페이스**:
```java
public interface ProviderAdapter {
    LaunchResult launch(User user, Game game, GameSession session);
    String getProviderCode();
}
```

### Step 13: Demo Provider Adapter
**파일**:
- `backend/src/main/java/com/casino/platform/provider/adapter/DemoProviderAdapter.java`

**구현**:
```java
public LaunchResult launch(...) {
    String launchUrl = "/demo-game?session=" + session.getToken();
    return new LaunchResult(launchUrl);
}
```

---

## Phase 5: Demo Game 전환 (2일)

### Step 14: Demo Round Service
**파일**:
- `backend/src/main/java/com/casino/platform/demo/entity/DemoRound.java`
- `backend/src/main/java/com/casino/platform/demo/repository/DemoRoundRepository.java`
- `backend/src/main/java/com/casino/platform/demo/service/DemoRoundService.java`

**구현**:
- 라운드 생성 (BET_PENDING)
- 베팅 처리 (Provider Wallet API 호출)
- 결과 저장
- 정산 처리

### Step 15: Demo 스핀 API
**파일**:
- `backend/src/main/java/com/casino/platform/demo/controller/DemoGameController.java`
- 기존 `SlotGameEngine.java`를 demo 패키지로 이동

**구현**:
```java
POST /api/demo-provider/sessions/{sessionToken}/spin
Header: Idempotency-Key
Body: { betPerLine }
Response: { reels, winAmount, balance }
```

**프론트엔드 수정**:
- 게임 launch → 토큰 받기
- 스핀 API 경로 변경
- Idempotency-Key 헤더 추가

**검증**:
- 스핀 → 잔액 변경
- 재시도 → 재추첨 없음

---

## Phase 6: 테스트 (2일)

### Step 16: 통합 테스트
**파일**:
- `backend/src/test/java/com/casino/platform/provider/ProviderWalletIntegrationTest.java`
- `backend/src/test/java/com/casino/platform/demo/DemoGameIntegrationTest.java`

**테스트 케이스**:
- 전체 게임 플로우
- 중복 처리
- 동시성
- 취소

### Step 17: 보안 테스트
**파일**:
- `backend/src/test/java/com/casino/platform/provider/security/HmacAuthTest.java`

**테스트 케이스**:
- 서명 검증
- 타임스탬프 만료
- Nonce 재사용
- 권한 체크

---

## Phase 7: 마이그레이션 및 정리 (1일)

### Step 18: V8 마이그레이션 - 기존 테이블 확장
**파일**:
- `V8__extend_existing_tables.sql`

**내용**:
- ledger에 provider_transaction_id 추가
- games에 provider_id, provider_game_id 추가

**데이터 마이그레이션**:
- 기존 games의 provider를 DEMO로 설정

### Step 19: 기존 API 정리
**작업**:
- 기존 `/api/games/{id}/play` 엔드포인트 deprecated 표시 또는 제거
- 프론트엔드 완전 전환 확인
- 사용하지 않는 코드 정리

---

## Phase 8: 문서화 (1일)

### Step 20: 문서 갱신
**파일**:
- `README.md`: 전체 업데이트
- `CLAUDE.md`: 외부 공급사 연동 제외 지침 수정
- `API_SPECIFICATION.md` (신규): 상세 API 명세
- `SEAMLESS_WALLET_GUIDE.md` (신규): 운영 가이드

**내용**:
- 아키텍처 다이어그램
- API 엔드포인트 목록
- 서명 생성 예시
- 환경 변수 설정
- 로컬 실행 방법
- 실제 공급사 연동 시 체크리스트

---

## 예상 일정

| Phase | 기간 | 누적 |
|-------|------|------|
| Phase 1 | 1일 | 1일 |
| Phase 2 | 2일 | 3일 |
| Phase 3 | 3일 | 6일 |
| Phase 4 | 2일 | 8일 |
| Phase 5 | 2일 | 10일 |
| Phase 6 | 2일 | 12일 |
| Phase 7 | 1일 | 13일 |
| Phase 8 | 1일 | 14일 |

**총 예상 기간**: 약 2~3주 (테스트 및 수정 포함)

## 리스크 및 대응

### 리스크 1: 동시성 이슈
- **대응**: Phase 6에서 집중 테스트, 필요시 격리 수준 조정

### 리스크 2: 프론트엔드 전환 복잡도
- **대응**: Phase 5에서 기존 UI 재사용, 최소 변경

### 리스크 3: 테스트 커버리지 부족
- **대응**: 핵심 흐름 위주 통합 테스트 우선 작성

### 리스크 4: 기존 데이터 마이그레이션
- **대응**: V8에서 안전하게 처리, 백업 필수

## 체크포인트

각 Phase 완료 시 다음을 확인:
1. ✅ 빌드 성공
2. ✅ 기존 기능 정상 동작
3. ✅ 신규 기능 동작 확인
4. ✅ 주요 테스트 통과
5. ✅ 문서 업데이트

---

**현재 상태**: 설계 완료, Phase 1 준비 완료
**다음 단계**: Step 1 (V5 마이그레이션) 시작
