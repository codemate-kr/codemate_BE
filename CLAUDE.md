# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

**Build and Run:**
```bash
./gradlew build          # Build the application
./gradlew bootRun        # Run the Spring Boot application
./gradlew test           # Run tests
```

**Local Development with Docker:**
```bash
cd scripts
docker-compose up -d     # Start MySQL and Redis services
docker-compose stop      # Stop services (preserves data)
docker-compose down      # Stop and remove everything (data loss)
```

**Database Access:**
- MySQL: `root` / `root` at localhost:3306
- Adminer web client: http://localhost:18080
- Direct connection: `docker exec -it docker-container mysql -uroot -p`

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
- Uses `.env` file for environment variables (not committed)
- Required env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, `REDIS_HOST`, `REDIS_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- API endpoints follow `/api/**` pattern
- JPA auditing enabled for entity timestamps

**Domain Model:**
- `Member` - User entity with OAuth provider info
  - `MemberSolvedProblem` - Tracks individual solved problems
- `Team` - Team entity with recommendation settings
  - `TeamMember` - Team membership with roles (LEADER, MEMBER)
- `Problem` - Algorithm problem data from solved.ac
- `TeamRecommendation` - Batch recommendation records (manual or scheduled)
  - `TeamRecommendationProblem` - Individual recommended problems
- Role-based authorization system (ROLE_USER, ROLE_ADMIN)

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