# CLAUDE.md

Claude Code가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## Development Commands

**Build and Run:**
```bash
./gradlew build          # Build the application (compile + test)
./gradlew bootRun        # Run Spring Boot app (default: http://localhost:8080)
./gradlew test           # Run all tests (JUnit 5)
./gradlew test --tests "ClassName"  # Run specific test class
```

**Local Development with Docker:**
```bash
cd scripts
docker-compose up -d     # Start MySQL, Redis, phpMyAdmin, RedisInsight
docker-compose stop      # Stop services (preserves data volumes)
docker-compose down      # Stop and remove containers (data persists in volumes)
```

**Database and Cache Access:**
- MySQL: `root` / `root` at localhost:3306 (database: `codemate`)
- phpMyAdmin: http://localhost:18080
- Redis: localhost:6379
- RedisInsight: http://localhost:18081

## Architecture Overview

Spring Boot 3.2 + Java 17 기반 알고리즘 문제 추천 서비스입니다.

### 도메인 구조

| 도메인 | 책임 | 주요 API |
|--------|------|----------|
| **Auth** | OAuth2 + JWT 인증 | `/api/auth` |
| **Member** | 회원 관리, BOJ 핸들 인증 | `/api/member` |
| **Team** | 팀 관리, 추천 설정 | `/api/teams` |
| **Recommendation** | 문제 추천, 이메일 발송 | `/api/recommendation` |
| **Problem** | 문제 메타데이터 저장소 | 내부 서비스 |
| **SolvedAc** | solved.ac API 연동 | 내부 서비스 |
| **Infrastructure** | 메일, 스케줄러 | - |

### 핵심 스케줄러

- `ProblemRecommendationScheduler`: 매일 새벽 6시 추천 생성
- `EmailSendScheduler`: 매일 오전 9시 이메일 발송

### 주요 설계 패턴

- **Denormalization**: `MemberRecommendation`에 `teamId`, `teamName` 저장 (팀 삭제 후에도 이력 보존)
- **No Cascade**: 엔티티 저장 시 명시적 `Repository.save()` 호출
- **Entity-DTO Separation**: Controller는 DTO만 사용

## 코딩 가이드라인

**언어:**
- 모든 응답과 주석은 한국어로 작성

**패키지 구조:**
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

**네이밍:**
- 패키지명: lowercase (`com.ryu.studyhelper.member`)
- 클래스명: UpperCamelCase (`MemberService`)
- 메서드/필드명: lowerCamelCase (`findMemberById`)
- Spring suffix: `*Controller`, `*Service`, `*Repository`

**테스트:**
- JUnit 5 사용, 테스트 클래스명: `*Test.java`
- 통합 테스트: `@SpringBootTest` + `@ActiveProfiles("test")` (H2 인메모리 DB)

**커밋:**
- present-tense, imperative, 간결하게
- prefix 사용: `member: add repository method`, `auth: fix OAuth2 callback`