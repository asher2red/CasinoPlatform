# Frontend - Casino Platform

Next.js 기반 프론트엔드 애플리케이션입니다.

## 기술 스택

- Next.js 16 (App Router)
- React 19
- TypeScript
- Tailwind CSS
- React Context API

## 프로젝트 구조

```
frontend/
├── app/                        # App Router
│   ├── layout.tsx             # 루트 레이아웃
│   ├── providers.tsx          # Provider 래퍼
│   ├── page.tsx               # 메인 페이지
│   ├── login/                 # 로그인
│   ├── register/              # 회원가입
│   ├── dashboard/             # 대시보드
│   ├── wallet/                # 지갑
│   └── games/                 # 게임 로비
│       ├── page.tsx           # 로비
│       └── [id]/              # 게임 상세
├── components/                 # 재사용 컴포넌트
│   ├── Layout.tsx             # 네비게이션 레이아웃
│   ├── Toast.tsx              # 토스트 알림
│   ├── Skeleton.tsx           # 로딩 스켈레톤
│   └── ProtectedRoute.tsx     # 인증 보호
├── contexts/                   # React Context
│   └── AuthContext.tsx        # 전역 인증 상태
└── package.json
```

## 설치 및 실행

### 설치

```bash
npm install
```

### 개발 서버 실행

```bash
npm run dev
```

http://localhost:3000 에서 실행됩니다.

### 빌드

```bash
npm run build
```

### 프로덕션 실행

```bash
npm start
```

## 주요 기능

### 1. 전역 인증 상태 (AuthContext)

```typescript
import { useAuth } from "@/contexts/AuthContext";

function Component() {
  const { user, loading, login, logout } = useAuth();
  
  // user: 현재 로그인한 사용자
  // loading: 로딩 상태
  // login(username, password): 로그인 함수
  // logout(): 로그아웃 함수
}
```

### 2. 토스트 알림

```typescript
import { useToast } from "@/components/Toast";

function Component() {
  const { showToast } = useToast();
  
  showToast("Success message", "success");
  showToast("Error message", "error");
  showToast("Warning message", "warning");
  showToast("Info message", "info");
}
```

### 3. Protected Route

```typescript
import ProtectedRoute from "@/components/ProtectedRoute";

export default function Page() {
  return (
    <ProtectedRoute>
      {/* 인증된 사용자만 접근 가능 */}
    </ProtectedRoute>
  );
}
```

### 4. 공통 레이아웃

```typescript
import Layout from "@/components/Layout";

export default function Page() {
  return (
    <Layout>
      {/* 네비게이션과 Logout 버튼 포함 */}
    </Layout>
  );
}
```

### 5. 로딩 스켈레톤

```typescript
import { WalletSkeleton, CardSkeleton, ListSkeleton } from "@/components/Skeleton";

{loading && <WalletSkeleton />}
```

## API 통신

### Fetch 설정

모든 API 요청에 `credentials: "include"` 설정:

```typescript
const response = await fetch("http://localhost:8080/api/endpoint", {
  credentials: "include",  // 세션 쿠키 포함
  method: "POST",
  headers: {
    "Content-Type": "application/json",
  },
  body: JSON.stringify(data),
});
```

### 에러 처리

```typescript
try {
  const response = await fetch(url, options);
  
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error || "Request failed");
  }
  
  const data = await response.json();
  return data;
} catch (err) {
  showToast(err.message, "error");
}
```

## 스타일링

### Tailwind CSS

유틸리티 클래스 사용:

```tsx
<div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
  <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
    Title
  </h1>
</div>
```

### 다크 모드

자동으로 시스템 설정 감지:

```css
/* 라이트 모드 */
.text-zinc-900

/* 다크 모드 */
.dark:text-zinc-50
```

## 라우팅

### App Router

```
/                    → 메인 페이지 (로그인/회원가입 링크)
/login               → 로그인
/register            → 회원가입
/dashboard           → 대시보드 (Protected)
/wallet              → 지갑 (Protected)
/games               → 게임 로비 (Protected)
/games/[id]          → 게임 상세 (Protected)
```

## 개발 가이드

### 새 페이지 추가

1. `app/new-page/page.tsx` 생성
2. "use client" 지시어 추가 (클라이언트 컴포넌트)
3. ProtectedRoute와 Layout 래핑

```typescript
"use client";

import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";

export default function NewPage() {
  return (
    <ProtectedRoute>
      <Layout>
        <div>New Page Content</div>
      </Layout>
    </ProtectedRoute>
  );
}
```

### API 호출 패턴

```typescript
const [data, setData] = useState(null);
const [loading, setLoading] = useState(true);

useEffect(() => {
  fetchData();
}, []);

const fetchData = async () => {
  try {
    const response = await fetch("http://localhost:8080/api/data", {
      credentials: "include",
    });
    
    if (!response.ok) throw new Error("Failed");
    
    const result = await response.json();
    setData(result);
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    setLoading(false);
  }
};
```

## 트러블슈팅

### CORS 에러

- 백엔드가 실행 중인지 확인
- SecurityConfig에서 CORS 설정 확인
- `credentials: "include"` 설정 확인

### 인증 상태가 유지되지 않음

- 쿠키 확인 (개발자 도구 → Application → Cookies)
- JSESSIONID 쿠키 존재 확인
- 도메인이 localhost인지 확인
