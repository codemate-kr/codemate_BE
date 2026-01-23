# Codemate

> 팀 기반 알고리즘 문제 추천 서비스

Codemate는 스터디 팀원들에게 매일 맞춤형 알고리즘 문제를 추천하고, 이메일로 발송하는 서비스입니다. [solved.ac](https://solved.ac) API와 연동하여 각 팀원이 아직 풀지 않은 문제 중 적절한 난이도의 문제를 선별합니다.

## 주요 기능

- **OAuth2 Google 로그인**: 간편한 소셜 로그인
- **팀 관리**: 팀 생성, 멤버 초대/수락/거절, 역할 관리
- **문제 추천 설정**: 난이도 프리셋, 요일별 추천 활성화
- **일일 문제 추천**: 매일 새벽 6시 자동 추천 생성
- **이메일 발송**: 오전 9시 추천 문제 이메일 알림
- **문제 해결 인증**: solved.ac API로 실시간 해결 여부 확인

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Backend** | Spring Boot 3.2, Java 17 |
| **Database** | MySQL 8.0, Redis 7 |
| **Auth** | Spring Security, OAuth2, JWT |
| **Infra** | AWS EC2, Docker, Nginx |
| **CI/CD** | GitHub Actions, Docker Hub |
| **Monitoring** | Sentry |
| **External API** | solved.ac, Google OAuth2, AWS SES |

## 아키텍처

### 인프라 구성

```mermaid
graph LR
    subgraph Client
        User[사용자] --> FE[Frontend<br/>Vercel]
    end

    subgraph EC2[AWS EC2]
        Nginx[Nginx :80/:443]
        Blue[Spring Blue :8081]
        Green[Spring Green :8082]
        MySQL[(MySQL 8.0)]
        Redis[(Redis 7)]
    end

    subgraph External[External Services]
        Google[Google OAuth2]
        SolvedAc[solved.ac API]
        SES[AWS SES]
    end

    FE --> Nginx
    Nginx -.->|active| Blue
    Nginx -.->|standby| Green
    Blue & Green --> MySQL
    Blue & Green --> Redis
    Blue & Green --> External
```

> 📦 Spring Blue, Spring Green, MySQL, Redis는 각각 독립된 Docker 컨테이너로 배포됩니다.

### 블루-그린 무중단 배포

```mermaid
sequenceDiagram
    participant GH as GitHub Actions
    participant EC2 as EC2 Server
    participant Nginx
    participant Active as Active Container (Blue)
    participant Standby as Standby Container (Green)

    GH->>EC2: 새 이미지 배포
    EC2->>Standby: 새 버전 시작
    Standby-->>EC2: 헬스체크 OK
    EC2->>Nginx: upstream 전환
    Nginx->>Standby: 트래픽 전환
    EC2->>Active: Graceful Shutdown
    Note over Nginx,Standby: 다운타임 0초
```

### 도메인 구조

```mermaid
graph TB
    subgraph Auth[Auth]
        A[인증/인가<br/>JWT, OAuth2]
    end

    subgraph Core[Core Domains]
        direction LR
        Member[Member<br/>회원 관리] <--> Team[Team<br/>팀 관리]
        Team <--> Recommendation[Recommendation<br/>문제 추천]
        Recommendation <--> Problem[Problem<br/>문제 저장소]
    end

    subgraph Support[Support Domains]
        direction LR
        SolvedAc[SolvedAc<br/>외부 API 연동] ~~~ Infra[Infrastructure<br/>메일, 스케줄러]
    end

    Auth --> Core
    Core <--> Support
```

| 도메인 | 책임 |
|--------|------|
| **Auth** | OAuth2 + JWT 인증, 토큰 관리 |
| **Member** | 회원 프로필, BOJ 핸들 인증, 문제 해결 추적 |
| **Team** | 팀 생성/삭제, 멤버 초대, 추천 설정 관리 |
| **Recommendation** | 문제 추천 생성, 이메일 발송 관리 |
| **Problem** | 알고리즘 문제 메타데이터 저장소 |
| **SolvedAc** | solved.ac API 연동 레이어 |
| **Infrastructure** | 메일 발송, 스케줄링 |

### 프로젝트 구조

```
src/main/java/com/ryu/studyhelper/
├── auth/                 # 인증/인가
├── member/               # 회원 관리
├── team/                 # 팀 관리
├── recommendation/       # 문제 추천
├── problem/              # 문제 저장소
├── solvedac/             # solved.ac 연동
├── infrastructure/       # 메일, 스케줄러
├── common/               # 공통 유틸리티
└── config/               # 설정 클래스
```

### 추천 시스템 데이터 흐름

```mermaid
sequenceDiagram
    participant Scheduler as 스케줄러
    participant RS as RecommendationService
    participant SA as solved.ac API
    participant DB as Database
    participant Mail as MailService

    Note over Scheduler: 매일 06:00
    Scheduler->>RS: 추천 생성 시작
    RS->>DB: 활성화된 팀 조회

    loop 각 팀별
        RS->>SA: 팀원들이 안 푼 문제 조회
        SA-->>RS: 추천 문제 목록
        RS->>DB: Recommendation 저장
        RS->>DB: MemberRecommendation 저장
    end

    Note over Scheduler: 매일 09:00
    Scheduler->>DB: PENDING 상태 조회
    Scheduler->>Mail: 이메일 발송
    Mail-->>DB: 상태 업데이트 (SENT)
```

## 배포

### CI/CD 파이프라인

```
GitHub Push → GitHub Actions → Docker Build/Push → EC2 SSH → deploy-blue-green.sh 실행
```

1. `main` 브랜치에 PR 머지
2. GitHub Actions가 Docker 이미지 빌드 및 Docker Hub 푸시
3. GitHub Actions가 EC2에 SSH 접속하여 블루-그린 배포 스크립트 실행

## 라이선스

Apache License 2.0