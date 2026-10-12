# Backend - Casino Platform

Spring Boot 기반 백엔드 API 서버입니다.

## 기술 스택

- Java 21
- Spring Boot 3.2.0
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Gradle 8.5

## 프로젝트 구조

```
backend/
├── src/main/java/com/casino/platform/
│   ├── CasinoPlatformApplication.java    # 메인 애플리케이션
│   ├── auth/                             # 인증 및 회원가입
│   │   ├── controller/                   # API 엔드포인트
│   │   ├── service/                      # 비즈니스 로직
│   │   └── dto/                          # 데이터 전송 객체
│   ├── user/                             # 사용자 관리
│   │   ├── entity/                       # JPA 엔티티
│   │   ├── repository/                   # 데이터 접근
│   │   └── dto/                          # DTO
│   ├── wallet/                           # 지갑 시스템
│   │   ├── entity/                       # Wallet 엔티티
│   │   ├── repository/                   # Repository
│   │   ├── service/                      # 트랜잭션 처리
│   │   ├── controller/                   # API
│   │   └── dto/                          # DTO
│   ├── ledger/                           # 거래 내역
│   │   ├── entity/                       # Ledger 엔티티
│   │   ├── repository/                   # Repository
│   │   └── dto/                          # DTO
│   ├── game/                             # 게임 관리
│   │   ├── entity/                       # Game 엔티티
│   │   ├── repository/                   # Repository
│   │   ├── service/                      # Service
│   │   ├── controller/                   # API
│   │   └── dto/                          # DTO
│   ├── config/                           # 설정
│   │   └── SecurityConfig.java           # Spring Security 설정
│   └── common/                           # 공통 컴포넌트
│       └── controller/                   # Health check 등
└── src/main/resources/
    ├── application.yml                   # 애플리케이션 설정
    └── db/migration/                     # Flyway 마이그레이션
        ├── V1__create_users_table.sql
        ├── V2__create_wallet_and_ledger_tables.sql
        └── V3__create_games_table.sql
```

## 빌드 및 실행

### 빌드

```bash
./gradlew build
```

### 테스트 제외 빌드

```bash
./gradlew build -x test
```

### 실행

```bash
./gradlew bootRun
```

서버가 http://localhost:8080 에서 실행됩니다.

## 데이터베이스 마이그레이션

Flyway를 사용하여 데이터베이스 스키마를 관리합니다.

### 마이그레이션 파일 명명 규칙

```
V{version}__{description}.sql
예: V1__create_users_table.sql
```

### 새 마이그레이션 추가

1. `src/main/resources/db/migration/` 에 새 파일 생성
2. 애플리케이션 재시작 시 자동 적용

## API 엔드포인트

### 인증 (Authentication)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/register` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/logout` | 로그아웃 | O |
| GET | `/api/auth/me` | 현재 사용자 정보 | O |

### Wallet

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/wallet` | 내 지갑 조회 | O |
| GET | `/api/wallet/transactions` | 거래 내역 (페이징) | O |
| POST | `/api/wallet/dev/deposit` | 가상 머니 지급 (개발용) | O |

### Games

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/games` | 게임 목록 | O |
| GET | `/api/games?category={category}` | 카테고리별 필터 | O |
| GET | `/api/games/{id}` | 게임 상세 | O |

### Health Check

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/health` | 서버 상태 확인 | X |

## 보안

### 세션 관리
- Spring Security의 HttpSession 사용
- 세션 ID는 JSESSIONID 쿠키로 전송
- 최대 동시 세션: 1개

### 비밀번호
- BCrypt 해시 알고리즘 사용
- 최소 8자 이상
- 데이터베이스에 해시만 저장

### CORS
- 허용된 Origin: `http://localhost:3000`
- Credentials: true
- 모든 HTTP 메서드 허용

## 데이터 정합성

### 금액 처리
- **BigDecimal** 사용 (부동소수점 오차 방지)
- PostgreSQL **NUMERIC(19, 2)** 타입

### 동시성 제어
- **Optimistic Locking**: `@Version` 사용
- **@Retryable**: 충돌 시 최대 3회 재시도
- **@Transactional**: 원자적 처리

### 중복 방지
- 거래마다 고유한 `referenceId` 생성
- 중복 `referenceId` 요청 거부

## 설정

### application.yml

```yaml
spring:
  application:
    name: casino-platform
  
  datasource:
    url: jdbc:postgresql://localhost:5432/casino_platform
    username: casino_admin
    password: casino_dev_password
  
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway가 스키마 관리
    show-sql: true
  
  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  port: 8080

logging:
  level:
    com.casino.platform: DEBUG
```

## 개발 가이드

### 새 엔티티 추가

```java
@Entity
@Table(name = "table_name")
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // fields, getters, setters
}
```

### 새 API 추가

1. **Controller**: HTTP 요청 처리
2. **Service**: 비즈니스 로직
3. **Repository**: 데이터 접근
4. **DTO**: 요청/응답 객체

### 트랜잭션 처리

```java
@Transactional
public void businessLogic() {
    // 모든 작업이 성공하거나 모두 롤백됨
}
```

## 트러블슈팅

### Port 8080 already in use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID {pid} /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Flyway 마이그레이션 실패
```bash
# 마이그레이션 상태 확인
docker exec -it casino-platform-db psql -U casino_admin -d casino_platform
SELECT * FROM flyway_schema_history;
```

### 세션 문제
- 브라우저 쿠키 확인
- CORS 설정 확인
- `credentials: "include"` 설정 확인
