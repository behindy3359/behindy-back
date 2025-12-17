# Behindy Backend

서울 지하철 역을 배경으로 한 텍스트 어드벤처 게임의 백엔드 API 서버입니다. JWT 기반 인증, 게임 로직, AI 스토리 생성 연동, 커뮤니티 기능, 실시간 WebSocket 통신을 제공합니다.

## 기술 스택

- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21
- **Build Tool**: Gradle 8.x
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Security**: Spring Security 6 + JWT (jjwt 0.11.5)
- **WebSocket**: Spring WebSocket + STOMP
- **API Documentation**: SpringDoc OpenAPI 2.7
- **ORM**: Spring Data JPA + Hibernate

## 주요 기능

### 인증 (Auth)
- JWT 기반 인증 시스템
- Access Token (15분) + Refresh Token (7일)
- BCrypt 비밀번호 해싱
- HttpOnly Cookie

### 게임 (Game)
- 지하철 역 및 호선 기반 게임 시작
- 캐릭터 생성 및 관리
- 선택지에 따른 스토리 분기 처리
- 게임 상태 추적 및 히스토리 관리
- 멀티플레이어 룸 생성 및 참여
- WebSocket 기반 실시간 채팅 및 상태 동기화

### 커뮤니티 (Community)
- 게시글 CRUD (작성, 조회, 수정, 삭제)
- 댓글 작성 및 좋아요
- HTML Sanitization (XSS 방지)
- 조회수 및 통계 관리

### AI 스토리 생성
- LLM Server와 WebClient 연동
- 역 정보 기반 스토리 자동 생성
- 배치 작업 스케줄링

### 지하철 실시간 정보
- 서울 열린데이터광장 API 연동
- 실시간 열차 위치 정보 조회
- Redis 기반 캐싱 (API 호출 최소화)
- 정기 데이터 갱신 스케줄러

### 보안 및 암호화
- AES256 필드 레벨 암호화
- 테이블 레벨 암호화
- Internal API Key 인증 (서비스 간 통신)

## API 엔드포인트

### 인증
```
POST   /api/auth/signup          # 회원가입
POST   /api/auth/login           # 로그인
POST   /api/auth/token/refresh   # 토큰 갱신
POST   /api/auth/logout          # 로그아웃
```

### 게임
```
GET    /api/game/status                                # 게임 상태 조회
POST   /api/game/enter/station/{name}/line/{number}   # 역 기반 게임 시작
POST   /api/game/choice/{optionId}                    # 선택지 선택
GET    /api/game/history                              # 게임 히스토리
```

### 커뮤니티
```
GET    /api/posts           # 게시글 목록
POST   /api/posts           # 게시글 작성
GET    /api/posts/{id}      # 게시글 조회
PUT    /api/posts/{id}      # 게시글 수정
DELETE /api/posts/{id}      # 게시글 삭제
```

## 환경 변수

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/behindy
SPRING_DATASOURCE_USERNAME=behindy
SPRING_DATASOURCE_PASSWORD=your_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your_redis_password

# JWT
JWT_SECRET=your_jwt_secret_key
JWT_ACCESS_VALIDITY=900000      # 15분
JWT_REFRESH_VALIDITY=604800000  # 7일

# Encryption
FIELD_KEY=your_field_encryption_key
TABLE_KEY=your_table_encryption_key

# AI Server
AI_SERVER_URL=http://localhost:8000
AI_SERVER_ENABLED=true
AI_SERVER_INTERNAL_API_KEY=your_internal_api_key

# Seoul Metro API
SEOUL_METRO_API_KEY=your_seoul_metro_api_key
SEOUL_METRO_API_ENABLED=true
```

## 로컬 개발

### 요구사항
- Java 21
- Gradle 8.x
- PostgreSQL 15
- Redis 7

### 실행
```bash
# 의존성 설치 및 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```

서버는 `http://localhost:8080`에서 실행됩니다.

## Docker 빌드

```bash
# 이미지 빌드
docker build -t behindy-backend .

# 컨테이너 실행
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/behindy \
  -e SPRING_DATASOURCE_USERNAME=behindy \
  -e SPRING_DATASOURCE_PASSWORD=password \
  behindy-backend
```

## 배포

GitHub Actions를 통한 자동 배포:

1. `main` 브랜치에 push
2. 자동으로 Docker 이미지 빌드
3. EC2 서버에 배포 및 재시작

자세한 내용은 `.github/workflows/deploy.yml` 참조

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/behindy/
│   │   ├── controller/      # REST API 컨트롤러
│   │   ├── service/         # 비즈니스 로직
│   │   ├── repository/      # JPA Repository
│   │   ├── entity/          # JPA 엔티티
│   │   ├── dto/             # DTO 클래스
│   │   ├── security/        # 보안 설정
│   │   ├── config/          # 설정 클래스
│   │   └── exception/       # 예외 처리
│   └── resources/
│       └── application.yml  # 애플리케이션 설정
└── test/                    # 테스트 코드
```

## 데이터베이스 스키마

### 주요 테이블
- `users`: 사용자 정보 (암호화된 이메일, BCrypt 해싱된 비밀번호)
- `refresh_token`: JWT Refresh Token 관리
- `char`: 게임 캐릭터 (이름, HP, Sanity)
- `sto`: 스토리 메타데이터
- `page`: 스토리 페이지 내용
- `options`: 페이지별 선택지
- `now`: 현재 게임 진행 상태
- `post`: 커뮤니티 게시글
- `post_stats`: 게시글 통계 (조회수, 좋아요 등)
- `comment`: 댓글
- `comment_like`: 댓글 좋아요
- `station`: 지하철 역 정보
- `stations_tr`: 역 번역 정보
- `log_e`, `log_o`: 에러 및 운영 로그
- `ops_log_a`, `ops_log_b`, `ops_log_d`, `ops_log_x`: 운영 로그 (타입별)

## 보안

- JWT 토큰 기반 인증
- BCrypt 비밀번호 해싱
- AES256 민감정보 암호화
- SQL Injection 방지 (JPA)
- XSS 방지 (HTML Sanitization)
- CORS 설정

## 아키텍처

이 프로젝트는 멀티레포지토리 아키텍처를 사용합니다.

- Frontend: Next.js 기반 UI 레이어
- Backend (이 레포): Spring Boot 기반 비즈니스 로직 및 API
- Story (LLM Server): FastAPI 기반 AI 스토리 생성 서버
- Ops: Docker Compose 기반 인프라 관리

## 관련 레포지토리

- [behindy-front](https://github.com/behindy3359/behindy-front) - Next.js 프론트엔드
- [behindy-story](https://github.com/behindy3359/behindy-story) - FastAPI AI 스토리 생성 서버
- [behindy-ops](https://github.com/behindy3359/behindy-ops) - 인프라 관리 (PostgreSQL, Redis, Nginx)

## 라이선스

MIT License
