# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- Direct MySQL CLI: `docker exec -it codemate-mysql mysql -uroot -proot`

## Architecture Overview

This is a Spring Boot 3.2 application providing study helper services with algorithm problem recommendations.

### 도메인 역할 및 책임 (Domain Responsibilities)

#### 코어 도메인 (Core Domains)

**1. Auth 도메인** (`auth/`)
- **책임**: OAuth2 + JWT 기반 인증/인가 관리
- **주요 기능**:
  - OAuth2 Google 로그인 처리 (Spring Security)
  - JWT Access Token 발급/검증
  - Refresh Token 관리 (Redis 저장)
  - 로그아웃 처리
- **API 엔드포인트**: `/api/auth`
  - `POST /refresh` - 액세스 토큰 재발급
  - `POST /logout` - 로그아웃
- **주요 컴포넌트**: `AuthController`, `AuthService`, `JwtTokenProvider`

**2. Member 도메인** (`member/`)
- **책임**: 회원 프로필 관리 및 개인 학습 활동 추적
- **주요 기능**:
  - 회원 프로필 조회/수정
  - BOJ 핸들 인증 (solved.ac 연동)
  - 이메일 인증 및 변경
  - 문제 해결 인증 (solved.ac API로 검증)
- **API 엔드포인트**: `/api/member`
  - `GET /me` - 내 프로필 조회
  - `GET /{id}` - 멤버 공개 정보 조회
  - `GET /search` - 핸들로 멤버 검색
  - `POST /me/verify-solvedac` - BOJ 핸들 인증
  - `POST /me/problems/{problemId}/verify-solved` - 문제 해결 인증
- **주요 엔티티**:
  - `Member` - 회원 정보 (OAuth, BOJ 핸들)
  - `MemberSolvedProblem` - 개인이 푼 문제 기록 (UNIQUE: member_id, problem_id)
- **주요 컴포넌트**: `MemberController`, `MemberService`

**3. Team 도메인** (`team/`)
- **책임**: 팀 생성/관리 및 팀 단위 추천 설정
- **주요 기능**:
  - 팀 생성/삭제
  - 팀원 초대/탈퇴
  - 팀 추천 설정 관리 (요일, 난이도 프리셋)
  - 팀 통계 조회
- **API 엔드포인트**: `/api/teams`
  - `GET /my` - 내가 속한 팀 목록
  - `POST /` - 팀 생성
  - `GET /{teamId}/members` - 팀 멤버 조회
  - `POST /{teamId}/invite` - 멤버 초대
  - `PUT /{teamId}/recommendation-settings` - 추천 설정 업데이트
  - `DELETE /{teamId}/recommendation-settings` - 추천 비활성화
  - `POST /{teamId}/leaveTeam` - 팀 탈퇴
  - `POST /{teamId}/deleteTeam` - 팀 삭제
  - `GET /{teamId}/stats/today` - 팀 오늘의 현황 (리팩토링 예정)
  - `GET /{teamId}/stats/weekly` - 팀 주간 리포트 (리팩토링 예정)
- **주요 엔티티**:
  - `Team` - 팀 정보 (추천 설정 포함: 요일, 난이도, 상태)
  - `TeamMember` - 팀-멤버 연결 (역할: LEADER, MEMBER)
- **주요 컴포넌트**: `TeamController`, `TeamService`

**4. Recommendation 도메인** (`recommendation/`)
- **책임**: 팀 단위 문제 추천 생성 및 관리
- **주요 기능**:
  - 팀별 문제 추천 생성 (스케줄러 기반)
  - 수동 추천 생성 (팀장)
  - 이메일 발송 관리 (개인별 발송 상태 추적)
- **API 엔드포인트**: `/api/recommendation`
  - `POST /team/{teamId}/manual` - 수동 추천 생성
  - `GET /team/{teamId}/today-problem` - 팀 오늘의 문제 (로그인 시 해결 여부 포함)
  - `GET /health` - 추천 시스템 상태 확인
- **주요 엔티티**:
  - `Recommendation` - 추천 배치 (팀 독립적, DB FK 없음)
  - `RecommendationProblem` - 추천에 포함된 문제들 (ORDER BY id로 순서 보장)
  - `MemberRecommendation` - 개인-추천 연결 (teamId, teamName 스냅샷, 이메일 발송 상태)
- **스케줄러**:
  - `ProblemRecommendationScheduler` - 매일 새벽 2시 추천 준비
  - `EmailSendScheduler` - 매일 오전 9시 이메일 발송
- **주요 컴포넌트**: `RecommendationController`, `RecommendationService`

**5. Problem 도메인** (`problem/`)
- **책임**: 알고리즘 문제 메타데이터 저장소 (Repository 역할)
- **주요 기능**:
  - 문제 정보 저장 (solved.ac에서 동기화)
  - 문제 조회 (ID, 난이도, 태그로 검색)
  - 문제 메타데이터 업데이트
  - ~~문제 추천 알고리즘~~ (레거시, 삭제 예정 - Recommendation 도메인으로 통합)
- **API 엔드포인트**: ~~`/api/problem`~~ (레거시 추천 API 삭제 예정)
- **주요 엔티티**:
  - `Problem` - 알고리즘 문제 정보 (문제 ID, 제목, 난이도, 태그, URL)
- **주요 컴포넌트**: `ProblemService` (내부 서비스로만 사용), `ProblemRepository`
- **참고**: 문제 추천 로직은 `RecommendationService`에서 `SolvedAcService`를 직접 호출

#### 서포트 도메인 (Support Domains)

**6. SolvedAc 도메인** (`solvedac/`)
- **책임**: solved.ac API 연동 레이어 (외부 API 어댑터)
- **주요 기능**:
  - 유저 정보 조회 (BOJ 핸들 검증)
  - 풀지 않은 문제 추천 알고리즘
  - 문제 메타데이터 동기화
  - 사용자가 푼 문제 목록 조회
  - 특정 문제 해결 여부 확인 (`hasUserSolvedProblem`)
- **API 엔드포인트**: 없음 (내부 서비스)
- **사용처**: `RecommendationService`, `MemberService` 내부에서 사용
- **주요 컴포넌트**: `SolvedAcService`, `SolvedAcApiClient`

**7. Infrastructure 도메인** (`infrastructure/`)
- **책임**: 기술적 관심사 (횡단 관심사, Cross-cutting Concerns)
- **하위 모듈**:
  - `mail/` - 이메일 발송 서비스 (Thymeleaf 템플릿)
  - `scheduler/` - 스케줄링 작업 관리
- **특징**: 비즈니스 로직 없음, 순수 기술 레이어
- **주요 컴포넌트**: `MailSendService`, `ProblemRecommendationScheduler`, `EmailSendScheduler`

#### 공통 모듈 (Common Modules)

**8. Common** (`common/`)
- **책임**: 공유 유틸리티 및 기본 구조
- **포함 요소**:
  - `dto/` - 공통 응답 구조 (`ApiResponse`)
  - `enums/` - 공통 Enum (`CustomResponseStatus`)
  - `exception/` - 커스텀 예외 클래스
  - `entity/` - `BaseEntity` (생성/수정 시각, soft delete)

**9. Config** (`config/`)
- **책임**: 애플리케이션 설정 및 인프라 구성
- **포함 요소**:
  - `SecurityConfig` - Spring Security + OAuth2 설정
  - `JwtConfig` - JWT 토큰 설정
  - `RedisConfig` - Redis 연결 설정
  - `SwaggerConfig` - API 문서 설정
  - `CorsConfig` - CORS 정책 설정
  - `JpaAuditingConfig` - JPA Auditing 활성화

**Key Technologies:**
- Spring Boot 3.2 with Java 17
- Spring Security + OAuth2 + JWT
- MySQL with JPA/Hibernate
- Redis for session/token storage
- Thymeleaf for email templates
- Swagger/OpenAPI documentation at `/swagger-ui`

**Configuration:**
- Uses `.env` file for environment variables (loaded by `java-dotenv`, not committed)
- Required env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, `REDIS_HOST`, `REDIS_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- Profiles: `local` (default), `test`, `prod` - configured in `application-{profile}.yml`
- API endpoints follow `/api/**` pattern
- JPA auditing enabled for entity timestamps
- OAuth2 redirect URI: `${BASE_URL}/login/oauth2/code/google`

**Domain Model:**
- `Member` - User entity with OAuth provider info, BOJ handle, email verification
  - `MemberSolvedProblem` - Tracks individual solved problems (UNIQUE: member_id, problem_id)
- `Team` - Team entity with recommendation settings (difficulty preset, day of week, status)
  - `TeamMember` - Team membership with roles (LEADER, MEMBER)
- `Problem` - Algorithm problem data from solved.ac (problem ID, title, tier, tags)
- **Recommendation System (New Schema):**
  - `Recommendation` - Recommendation batch (team-independent, no DB FK to team)
    - `RecommendationProblem` - Problems in a recommendation batch (order guaranteed by id)
  - `MemberRecommendation` - Member-recommendation connection (email send status management)
    - Denormalized fields: `teamId`, `teamName` (preserved after team deletion)
    - Email status tracking: `emailSendStatus`, `emailSentAt`
- `TeamRecommendation` (Legacy) - Old batch recommendation records (being phased out)
  - `TeamRecommendationProblem` - Individual recommended problems in legacy schema
- Role-based authorization system (ROLE_USER, ROLE_ADMIN)

**Key Architectural Patterns:**
- **Separation of Concerns**: 문제 추천 준비(새벽 2시 배치)와 이메일 발송(오전 9시) 분리
  - `ProblemRecommendationScheduler`: 매일 새벽 2시 solved.ac API 호출 및 문제 추천 데이터 DB 저장
  - `EmailSendScheduler`: 매일 오전 9시 PENDING 상태의 이메일 발송
- **Denormalization for Data Preservation**: `MemberRecommendation`에 `teamId`, `teamName` 저장
  - DB FK 없이 애플리케이션 레벨에서만 참조
  - 팀 삭제 후에도 개인의 추천 이력 보존
- **No Cascade Strategy**: 엔티티 저장 시 명시적으로 Repository.save() 호출
  - `RecommendationProblem`을 직접 저장 (cascade 없이)
  - 동작 예측 가능성 향상
- **Problem Order Guarantee**: `RecommendationProblem.id` 순서로 문제 순서 보장 (별도 order 컬럼 없음)
- **Facade Pattern**: BOJ 인증은 `BojVerificationFacade`가 조율 (hash 생성, solved.ac 검증, DB 업데이트)
- **JWT + Refresh Token**: Access token (짧은 유효기간) + Refresh token (Redis 저장, HttpOnly cookie)
- **Entity-DTO Separation**: Controller는 DTO만 사용, Entity는 Service 계층 내부에서만 사용

## Recommendation System Deep Dive

**새 추천 시스템 아키텍처 (2025-11 리팩토링):**

```
Recommendation (추천 배치, 팀과 독립)
  ├─ team_id (Long, DB FK 없음)
  └─ RecommendationProblem (1:N)
       └─ problem_id (순서는 id로 보장)

MemberRecommendation (개인-추천 연결, 이메일 발송 관리)
  ├─ member_id (FK → Member)
  ├─ recommendation_id (FK → Recommendation)
  ├─ team_id (denormalized, DB FK 없음)
  ├─ team_name (denormalized, 팀 삭제 후에도 표시)
  ├─ email_send_status (PENDING/SENT/FAILED)
  └─ email_sent_at (nullable)

MemberSolvedProblem (문제 해결 추적, 별도)
  ├─ member_id (FK → Member)
  ├─ problem_id (FK → Problem)
  └─ solved_at (UNIQUE 제약: member_id + problem_id)
```

**주요 설계 결정:**
1. **팀 독립적 데이터 보존**: `Recommendation.teamId`는 DB FK 없이 애플리케이션 레벨만 참조 → 팀 삭제 시에도 추천 이력 보존
2. **Denormalization**: `MemberRecommendation`에 `teamId`, `teamName` 직접 저장 → 팀 삭제 후에도 이력 조회 가능
3. **문제 해결 추적 분리**: `MemberSolvedProblem`을 별도 테이블로 관리 → UNIQUE 제약으로 중복 방지
4. **Cascade 제거**: 모든 엔티티를 명시적으로 저장 → 동작 예측성 향상
5. **문제 순서**: `problem_order` 컬럼 없이 `id` 순서로 보장 → Repository에서 `ORDER BY id ASC` 필수

**데이터 흐름 (팀원 3명 × 문제 3개 예시):**
```
1. Recommendation 생성 (추천 배치)
2. RecommendationProblem 3개 생성 (문제1, 문제2, 문제3) - 직접 저장
3. MemberRecommendation 3개 생성 (팀원별) - teamId, teamName 스냅샷
4. 이메일 발송 시 emailSendStatus 업데이트
```

**팀 삭제 시 동작:**
- `TeamMember` cascade 삭제됨
- `Recommendation`, `MemberRecommendation` 보존됨 (FK 없음)
- 사용자는 여전히 "2024-01-15에 'XX팀'에서 받은 추천" 조회 가능 (teamName 스냅샷)

**참고 문서:** `RECOMMENDATION_REFACTORING_PROGRESS.md` - 진행 현황 및 체크리스트

## 코딩 가이드라인

**언어 및 설명:**
- 모든 응답과 주석은 한국어로 작성
- 코드의 의도와 목적을 명확히 설명
- 비즈니스 로직의 배경과 이유를 문서화

**코드 품질 원칙:**
- 유지보수성을 최우선으로 고려한 코드 작성
- 가독성이 높고 직관적인 변수명과 메서드명 사용
- 확장 가능한 구조로 설계 (SOLID 원칙 준수)
- 코드 중복 최소화 및 재사용성 극대화

**아키텍처 및 구조:**
- 각 클래스와 메서드의 역할과 책임을 명확히 정의
- 관심사의 분리 (Separation of Concerns) 철저히 적용
- 기능별로 적절한 파일 분리 및 패키지 구조 유지
- Controller - Service - Repository 레이어 구조 준수
- DTO/Entity 분리하여 계층 간 데이터 전달 최적화

**패키지 구조 규칙:**
- 도메인별 패키지 분리 (auth, member, team, problem, recommendation 등)
- 각 도메인 내에서 Controller, Service, Repository, Domain, DTO 분리
- 공통 기능은 common 패키지로 분리 (예외, 기본 엔티티, 공통 DTO)
- 설정 클래스는 config 패키지에 집중화 (Security, JWT, Redis, Swagger)
- 기술적 관심사는 infrastructure 패키지로 분리 (메일, 스케줄러)

**코딩 스타일 및 네이밍:**
- Java 17 기준, 4-space 들여쓰기, UTF-8 인코딩
- 패키지명: lowercase (예: `com.ryu.studyhelper.member`)
- 클래스명: UpperCamelCase (예: `MemberService`)
- 메서드/필드명: lowerCamelCase (예: `findMemberById`)
- Spring 스테레오타입 suffix 사용: `*Controller`, `*Service`, `*Repository`
- Lombok 활용: 생성자 주입(`@RequiredArgsConstructor`), 로깅(`@Slf4j`)

**테스트 작성:**
- JUnit 5 (`spring-boot-starter-test`) 사용
- 테스트 파일 위치: `src/test/java/...` (main 패키지 구조와 동일)
- 테스트 클래스명: `*Test.java` 또는 `*Tests.java`
- `@SpringBootTest`는 필요시에만 사용, 단위 테스트 우선
- **통합 테스트**: `@SpringBootTest` + `@ActiveProfiles("test")` 사용하여 H2 인메모리 DB로 테스트 (로컬 MySQL과 격리)
- 비즈니스 로직의 의미 있는 커버리지 확보 목표

**커밋 및 PR 가이드라인:**
- 커밋 메시지: present-tense, imperative, 간결하게
- 기능 범위 prefix 사용 (예: `member: add repository method`, `auth: fix OAuth2 callback`)
- PR 포함 사항: 변경 목적/요약, 관련 이슈 링크(`Closes #123`), 테스트 방법
- PR 전 `./gradlew build` 성공 확인 필수