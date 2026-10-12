# Seamless Wallet 변경 영향도 분석

## 1. 현재 구조 분석

### 1.1 패키지 구조
```
com.casino.platform
├── admin          (관리자 기능)
├── auth           (인증)
├── common         (공통)
├── config         (설정)
├── game           (게임)
│   ├── controller (GameController, GameSessionController)
│   ├── service    (GameService, GameSessionService, SlotGameEngine)
│   └── entity     (Game, GameRound)
├── ledger         (거래 원장)
│   ├── entity     (Ledger)
│   └── repository (LedgerRepository)
├── user           (사용자)
├── wallet         (지갑)
│   ├── service    (WalletService)
│   └── entity     (Wallet)
```

### 1.2 현재 게임 플로우

```
사용자 -> GameSessionController.playGame()
           -> GameSessionService.playSlotGame()
              -> WalletService.placeBet()
              -> SlotGameEngine.spin()
              -> WalletService.creditWin()
              -> GameRound 저장
```

## 2. 주요 변경 사항

### 2.1 신규 패키지
```
com.casino.platform
├── provider (NEW)
│   ├── entity
│   │   ├── Provider
│   │   ├── PlayerProviderMapping
│   │   ├── GameSession
│   │   ├── ProviderTransaction
│   │   └── NonceStore
│   ├── repository
│   ├── service
│   │   ├── ProviderConfigService
│   │   ├── NonceService
│   │   └── PlayerMappingService
│   ├── security
│   │   ├── HmacAuthenticationFilter
│   │   ├── HmacSignatureValidator
│   │   └── ProviderAuthenticationToken
│   ├── wallet
│   │   ├── ProviderWalletController
│   │   ├── ProviderWalletService
│   │   └── dto/
│   ├── rollback
│   │   ├── RollbackService
│   │   └── RollbackIntent (entity)
│   ├── adapter
│   │   ├── ProviderAdapter (interface)
│   │   ├── ProviderAdapterFactory
│   │   └── DemoProviderAdapter
│   └── exception
│       ├── ProviderExceptionHandler
│       └── ProviderErrorCode
│
├── demo (NEW)
│   ├── entity
│   │   └── DemoRound
│   ├── repository
│   │   └── DemoRoundRepository
│   ├── service
│   │   ├── DemoRoundService
│   │   └── SlotEngine (moved from game)
│   └── controller
│       └── DemoGameController
```

### 2.2 수정 패키지
```
├── config
│   ├── SecurityConfig (수정)
│   └── ProviderSecurityConfig (NEW)
│
├── wallet
│   ├── service
│   │   ├── WalletService (수정 - 사용자 API용 래퍼)
│   │   └── WalletCoreService (NEW - 통합 처리)
│   └── dto
│       ├── WalletTransactionRequest (NEW)
│       └── WalletTransactionResult (NEW)
│
├── game
│   ├── controller
│   │   ├── GameController (기존 유지)
│   │   ├── GameSessionController (deprecated 또는 제거)
│   │   └── GameLaunchController (NEW)
│   ├── service
│   │   ├── GameService (기존 유지)
│   │   ├── GameSessionService (deprecated 또는 제거)
│   │   ├── GameLaunchService (NEW)
│   │   └── SlotGameEngine (→ demo 패키지로 이동)
│   └── dto
│       ├── LaunchGameRequest (NEW)
│       └── LaunchGameResponse (NEW)
```

## 3. 파일별 변경 내역

### 3.1 완전 신규 파일 (약 40개)

#### DB 마이그레이션
- [ ] `V5__create_providers_and_mappings.sql`
- [ ] `V6__create_provider_transactions.sql`
- [ ] `V7__create_demo_rounds.sql`
- [ ] `V8__extend_existing_tables.sql`

#### Provider 패키지 (약 25개)
- [ ] Provider 관련 Entity (5개)
- [ ] Provider 관련 Repository (5개)
- [ ] Provider Service (5개)
- [ ] Provider Security (3개)
- [ ] Provider Wallet API (5개)
- [ ] Provider Adapter (3개)
- [ ] Exception 처리 (2개)

#### Demo 패키지 (약 5개)
- [ ] DemoRound Entity
- [ ] DemoRoundRepository
- [ ] DemoRoundService
- [ ] DemoGameController
- [ ] SlotEngine (이동)

#### Wallet Core (약 3개)
- [ ] WalletCoreService
- [ ] WalletTransactionRequest DTO
- [ ] WalletTransactionResult DTO

#### Game Launch (약 3개)
- [ ] GameLaunchService
- [ ] GameLaunchController
- [ ] Launch 관련 DTO

#### Config (약 1개)
- [ ] ProviderSecurityConfig

### 3.2 수정 파일 (약 10개)

#### 기존 파일 수정
- [ ] `SecurityConfig.java` - CSRF 설정 분리
- [ ] `WalletService.java` - 사용자 API 래퍼로 변경
- [ ] `GameSessionController.java` - deprecated 표시
- [ ] `GameSessionService.java` - deprecated 표시
- [ ] `Ledger.java` - provider_transaction_id 필드 추가
- [ ] `LedgerRepository.java` - 조회 메서드 추가
- [ ] `Game.java` - provider_id, provider_game_id 필드 추가
- [ ] `GameRepository.java` - provider 관련 조회 추가
- [ ] `WalletController.java` - dev/deposit CSRF 토큰 처리
- [ ] `application.yml` - provider 설정 추가

### 3.3 프론트엔드 수정 (약 3개)
- [ ] `frontend/app/games/[id]/play/page.tsx` - launch API 호출 후 스핀
- [ ] `frontend/lib/api.ts` - Idempotency-Key 헤더 추가
- [ ] `frontend/contexts/AuthContext.tsx` - CSRF 토큰 처리

### 3.4 제거 가능 파일 (Phase 7 이후)
- [ ] `SlotGameEngine.java` (demo로 이동 후 원본 제거)
- [ ] `GameSessionController.java` (선택적)
- [ ] `GameSessionService.java` (선택적)

## 4. 의존성 변경

### 4.1 신규 의존성 (없음)
- 기존 의존성으로 구현 가능
- Spring Security, Spring Data JPA, PostgreSQL 활용

### 4.2 설정 변경
```yaml
# application.yml 추가
provider:
  demo:
    key-id: "demo-key-001"
    secret-key: "${DEMO_PROVIDER_SECRET:demo-secret-change-in-production}"
  signature:
    timestamp-tolerance-seconds: 300
  nonce:
    retention-minutes: 30
```

## 5. DB 변경 영향

### 5.1 신규 테이블 (7개)
1. providers
2. player_provider_mappings
3. game_sessions
4. provider_transactions
5. nonce_store
6. rollback_intents
7. demo_rounds

### 5.2 기존 테이블 확장 (2개)
1. ledger: `provider_transaction_id BIGINT`
2. games: `provider_id BIGINT`, `provider_game_id VARCHAR(100)`

### 5.3 기존 데이터 영향
- **users**: 영향 없음
- **wallets**: 영향 없음
- **ledger**: 신규 컬럼 추가 (NULL 허용, 기존 데이터 유지)
- **games**: 신규 컬럼 추가 (NULL 허용), 데이터 마이그레이션 필요
- **game_rounds**: 영향 없음 (demo_rounds와 별도 유지)

## 6. API 변경 영향

### 6.1 사용자 API

#### 유지 (변경 없음)
- ✅ `GET /api/wallet`
- ✅ `GET /api/wallet/transactions`
- ✅ `GET /api/games`
- ✅ `GET /api/games/{id}`
- ✅ `POST /api/auth/login`
- ✅ `POST /api/auth/logout`

#### 수정 (내부 구현만 변경)
- ⚠️ `POST /api/wallet/dev/deposit` - WalletCoreService 사용

#### 신규
- ✨ `POST /api/games/{gameId}/launch` - 게임 실행

#### Deprecated/제거 예정
- ❌ `POST /api/games/{gameId}/play` - 새 demo API로 대체

### 6.2 Demo API (신규)
- ✨ `POST /api/demo-provider/sessions/{sessionToken}/spin`

### 6.3 Provider Wallet API (완전 신규)
- ✨ `POST /api/provider-wallet/v1/{providerCode}/balance`
- ✨ `POST /api/provider-wallet/v1/{providerCode}/bet`
- ✨ `POST /api/provider-wallet/v1/{providerCode}/win`
- ✨ `POST /api/provider-wallet/v1/{providerCode}/rollback`

### 6.4 관리자 API
- ✅ 모든 admin API 유지 (영향 없음)

## 7. 보안 영향

### 7.1 인증 체계 추가
- **기존**: 사용자 세션 쿠키 (유지)
- **신규**: Provider HMAC 인증 (별도 SecurityFilterChain)

### 7.2 CSRF 보호
- **사용자 API**: CSRF 토큰 유지
- **Provider API**: CSRF 제외 (HMAC 인증)

### 7.3 권한 검증
- **기존 로직 유지**: @PreAuthorize, 세션 검증
- **신규**: Provider 인증 필터

## 8. 성능 영향

### 8.1 긍정적 영향
- 중복 처리 개선 (UNIQUE 제약)
- 거래 추적 강화

### 8.2 부정적 영향 (미미)
- Provider 거래 시 추가 테이블 저장
- HMAC 서명 검증 오버헤드 (무시 가능)

### 8.3 최적화
- Nonce 정기 정리 (스케줄러)
- 만료된 세션 정리

## 9. 테스트 영향

### 9.1 기존 테스트
- 사용자 API 테스트: 대부분 유지
- 지갑 테스트: WalletCoreService로 수정 필요

### 9.2 신규 테스트 필요
- Provider 인증 테스트
- Provider Wallet API 테스트
- Demo 게임 플로우 테스트
- 중복 처리 테스트
- 동시성 테스트
- Rollback 시나리오 테스트

## 10. 배포 영향

### 10.1 배포 순서
1. DB 마이그레이션 (V5~V8)
2. 백엔드 배포
3. 프론트엔드 배포
4. 환경 변수 설정 (Provider secret key)

### 10.2 롤백 전략
- V8 이전 상태로 롤백 가능
- 프론트엔드는 기존 API 호환 유지 기간 필요

### 10.3 무중단 배포
- 신규 API는 기존 기능과 독립적
- 단계적 전환 가능

## 11. 리스크 평가

| 리스크 | 심각도 | 확률 | 대응 |
|--------|--------|------|------|
| 잔액 동시성 이슈 | 높음 | 중 | 집중 테스트, 트랜잭션 격리 |
| 중복 처리 실패 | 높음 | 저 | UNIQUE 제약 + 테스트 |
| 기존 데이터 손실 | 높음 | 저 | 마이그레이션 검증, 백업 |
| 프론트엔드 호환 | 중 | 중 | 점진적 전환, 기존 API 유지 |
| 성능 저하 | 저 | 저 | 모니터링, 인덱스 최적화 |

## 12. 체크리스트

### 시작 전
- [ ] 현재 데이터베이스 백업
- [ ] 현재 git 커밋 확인
- [ ] 개발 환경 준비 (Docker, DB 실행)

### 각 Phase 완료 시
- [ ] 빌드 성공
- [ ] 마이그레이션 성공
- [ ] 기존 기능 동작 (수동 테스트)
- [ ] 신규 기능 동작 확인
- [ ] Git 커밋

### 최종 완료 시
- [ ] 전체 통합 테스트 통과
- [ ] 문서 완성
- [ ] README 업데이트
- [ ] CLAUDE.md 업데이트

---

**총평**: 
- 신규 파일 약 50개
- 수정 파일 약 10개
- 예상 작업 기간 2~3주
- 리스크 관리 가능
- 점진적 전환 가능

**다음**: Phase 1 Step 1 시작
