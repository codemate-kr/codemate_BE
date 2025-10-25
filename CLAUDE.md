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

**Core Modules:**
- `auth/` - OAuth2 (Google) authentication with JWT tokens
- `member/` - User management and solved problem tracking (includes `verification/` for BOJ verification)
- `team/` - Team formation and management
- `problem/` - Algorithm problem data management
- `recommendation/` - Problem recommendation engine (manual and scheduled recommendations)
- `solvedac/` - Integration with solved.ac API for problem data
- `infrastructure/` - Technical concerns (mail, scheduler)
  - `mail/` - Email notification services
  - `scheduler/` - Scheduled tasks (problem recommendations, email sending)
- `common/` - Shared utilities (DTOs, exceptions, base entities)
- `config/` - Application configuration (Security, JWT, Redis, Swagger, CORS)

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
  - `MemberSolvedProblem` - Tracks individual solved problems
- `Team` - Team entity with recommendation settings (difficulty preset, day of week, status)
  - `TeamMember` - Team membership with roles (LEADER, MEMBER)
- `Problem` - Algorithm problem data from solved.ac (problem ID, title, tier, tags)
- `TeamRecommendation` - Batch recommendation records (manual or scheduled)
  - `TeamRecommendationProblem` - Individual recommended problems in a batch
- Role-based authorization system (ROLE_USER, ROLE_ADMIN)

**Key Architectural Patterns:**
- **Separation of Concerns**: 문제 추천 준비(새벽 2시 배치)와 이메일 발송(사용자 설정 요일/시간) 분리
  - `ProblemRecommendationScheduler`: 매일 새벽 2시 solved.ac API 호출 및 문제 추천 데이터 DB 저장
  - `EmailSendScheduler`: 팀별 설정된 요일/시간에 준비된 추천 데이터를 이메일로 발송
- **Facade Pattern**: BOJ 인증은 `BojVerificationFacade`가 조율 (hash 생성, solved.ac 검증, DB 업데이트)
- **JWT + Refresh Token**: Access token (짧은 유효기간) + Refresh token (Redis 저장, HttpOnly cookie)
- **Entity-DTO Separation**: Controller는 DTO만 사용, Entity는 Service 계층 내부에서만 사용

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
- 테스트 클래스명: `*Tests.java` (예: `MemberServiceTests`)
- `@SpringBootTest`는 필요시에만 사용, 단위 테스트 우선
- 비즈니스 로직의 의미 있는 커버리지 확보 목표

**커밋 및 PR 가이드라인:**
- 커밋 메시지: present-tense, imperative, 간결하게
- 기능 범위 prefix 사용 (예: `member: add repository method`, `auth: fix OAuth2 callback`)
- PR 포함 사항: 변경 목적/요약, 관련 이슈 링크(`Closes #123`), 테스트 방법
- PR 전 `./gradlew build` 성공 확인 필수