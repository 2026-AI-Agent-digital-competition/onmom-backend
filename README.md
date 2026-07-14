# 온맘(Onmom)

AI agent를 활용한 산모-가족 연결 서비스입니다.  
산모가 자신의 감정과 상태를 기록하면 AI가 이를 해석하고, 가족이 이해하기 쉬운 메시지와 알림으로 연결해주는 것을 목표로 합니다.

## 프로젝트 개요

온맘은 임신 중 산모의 감정 변화, 일정, 위험 신호를 가족과 더 부드럽게 공유하기 위한 서비스입니다.

주요 흐름:

- 산모/가족 역할 선택
- 카카오 로그인
- 산모 정보와 임신 주수 등록
- 6글자 초대 코드 기반 가족 연결
- 감정 기록과 AI 리포트
- 온맘 AI 채팅
- 병원 예약 일정 저장
- 감정 번역 및 가족 메시지
- 태동 감소, 출혈, 심한 복통 등 긴급 알림

## 기술 스택

초기 백엔드는 아래 구성을 기준으로 합니다.

- Java 21
- Spring Boot 4.1.0
- Gradle
- Spring Web
- Spring Data JPA
- MySQL 8
- Flyway
- Validation
- Jackson
- Lombok

카카오 로그인은 React가 callback에서 받은 인가 코드를 백엔드에 전달하고, 백엔드가 카카오 access token 발급과 사용자 정보 조회를 수행하는 Authorization Code 방식으로 구현합니다. 로그인 성공 후 백엔드는 자체 access token을 JWT로 발급합니다.

JWT 서명 키와 카카오 OAuth 설정은 환경 변수 또는 외부 설정으로 주입합니다. 저장소에는 운영용 secret을 커밋하지 않습니다.

```text
ONMOM_JWT_SECRET
ONMOM_CORS_ALLOWED_ORIGINS
ONMOM_KAKAO_CLIENT_ID
ONMOM_KAKAO_CLIENT_SECRET
ONMOM_KAKAO_REDIRECT_URI
```

`ONMOM_KAKAO_CLIENT_ID`에는 카카오 REST API 키를 설정합니다. React의 callback URI, 카카오 개발자 콘솔에 등록한 redirect URI, `ONMOM_KAKAO_REDIRECT_URI`는 동일해야 합니다. React는 인가 요청 전에 생성한 `state`를 callback에서 검증한 뒤 인가 코드를 백엔드로 전달합니다.

React와 백엔드가 다른 origin이면 `ONMOM_CORS_ALLOWED_ORIGINS`에 허용할 React origin을 쉼표로 구분해 설정합니다. 기본 개발 origin은 `http://localhost:5173`이며 CORS는 `/api/**`에만 적용됩니다.

### 로컬 실행

로컬 MySQL을 직접 사용할 때는 아래 명령으로 실행합니다.

```bash
./gradlew run
```

`run` 태스크는 `local` 프로필로 Spring Boot를 실행합니다. 기본 로컬 DB 설정은 다음 환경 변수로 덮어쓸 수 있습니다.

```text
ONMOM_DATASOURCE_URL
ONMOM_DATASOURCE_USERNAME
ONMOM_DATASOURCE_PASSWORD
```

Spring Boot 표준 환경 변수인 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`도 사용할 수 있습니다.

프로젝트 루트의 `.env` 파일도 `local` 프로필에서 자동으로 읽습니다. `.env`는 git에 포함하지 않습니다.

### EC2 배포

시연용 Docker Hub → Ubuntu EC2 수동 배포는 [EC2 시연 배포 가이드](docs/demo-ec2-deployment.md)를 따릅니다.
도메인, HTTPS, CI/CD 없이 `http://EC2_IP:8080`에서 앱을 확인하는 최소 구성입니다.

### Cloudtype 배포

Cloudtype의 Dockerfile 서비스와 같은 프로젝트의 MariaDB를 사용하는 배포는
[Cloudtype 배포 가이드](docs/cloudtype-deployment.md)를 따릅니다.

## 문서

개발 전 아래 문서를 먼저 읽어주세요.

- [개발 지침](AGENTS.md)
- [API 설계 가이드](docs/onmom_api_guidelines.md)
- [DB 설계안](docs/onmom_db_design.md)
- [MySQL DDL](src/main/resources/db/migration/V1__create_onmom_schema.sql)

## 핵심 개발 규칙

- 패키지는 도메인형 구조를 사용합니다.
- 공통 응답은 `ApiResponse<T>`를 사용합니다.
- 예외 처리는 `global.exception`에서 공통 처리합니다.
- 목록 조회는 처음부터 cursor pagination을 사용합니다.
- Controller는 Entity를 직접 반환하지 않습니다.
- Controller에는 HTTP 요청/응답 처리만 두고, 비즈니스 로직은 Service에 둡니다.
- 트랜잭션 경계는 Service에 둡니다.
- DB는 `FOREIGN KEY`, `CHECK`, MySQL `ENUM` 없이 설계합니다.
- DB 상태값은 `VARCHAR`로 저장하고 Java enum과 애플리케이션 검증으로 관리합니다.
- 자체 access token은 JWT로 발급하고 `Authorization: Bearer {accessToken}` 헤더로 검증합니다.
- 의존성 주입은 생성자 주입을 기본으로 합니다.

## 현재 구현된 API

### 카카오 로그인

```http
POST /api/v1/auth/kakao-login
Content-Type: application/json
```

요청:

```json
{
  "authorizationCode": "single-use-kakao-code",
  "role": "MOTHER"
}
```

응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "userId": 1,
    "nickname": "온맘",
    "profileImageUrl": "https://example.com/profile.png",
    "role": "MOTHER",
    "tokenType": "Bearer",
    "accessToken": "jwt-access-token",
    "expiresIn": 3600
  }
}
```

백엔드는 인가 코드를 카카오 access token으로 교환한 뒤 `/v2/user/me`를 호출합니다. 응답의 카카오 회원번호 `id`를 사용자 식별자로 사용하며 카카오 access/refresh token은 저장하거나 프론트에 반환하지 않습니다. token/user-info endpoint는 설정으로 노출하지 않고 `KakaoClient` 내부 상수로 관리합니다.

카카오 외부 연동 장애는 실패 단계와 HTTP 상태 또는 예외 종류만 warning 로그로 기록합니다. 인가 코드, 카카오 토큰, client secret, 사용자 정보와 외부 응답 본문은 로그에 남기지 않습니다.

`role`은 `MOTHER`, `FAMILY`를 사용합니다. 기존 카카오 계정이면 저장된 사용자와 역할을 사용하고, 없으면 요청 `role`을 `users.primary_role`로 저장한 뒤 `users`, `oauth_accounts`를 생성합니다. 동일 카카오 계정의 동시 로그인에서 OAuth UNIQUE 충돌이 발생하면 실패한 생성 트랜잭션을 종료한 후 먼저 생성된 계정을 재조회합니다.

초기 등록은 역할별 도메인 데이터로 판단합니다. `MOTHER`는 활성 임신 프로필을 생성하면 완료되며 초대 코드 발급은 선택입니다. `FAMILY`는 초기 등록 과정에서 산모의 초대 코드를 수락해 `CONNECTED` 관계가 생성되어야 완료됩니다.

### 임신 프로필 생성

```http
POST /api/v1/pregnancies
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "motherDisplayName": "온맘",
  "babyNickname": "튼튼이",
  "pregnancyWeekStart": 12,
  "pregnancyWeekEnd": 13,
  "dueDate": "2027-01-01"
}
```

응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "id": 1,
    "motherDisplayName": "온맘",
    "babyNickname": "튼튼이",
    "pregnancyWeekStart": 12,
    "pregnancyWeekEnd": 13,
    "dueDate": "2027-01-01",
    "status": "ACTIVE"
  }
}
```

산모당 `ACTIVE` 임신 프로필은 하나만 허용합니다. 임신 주차는 둘 다 생략하거나 0~42 범위에서 함께 입력하며 시작 주차가 끝 주차보다 클 수 없습니다.

### 가족 초대 코드 발급

```http
POST /api/v1/pregnancies/{pregnancyId}/family-invite-codes
Authorization: Bearer {accessToken}
```

응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "code": "A7K2Q9",
    "expiresAt": "2026-07-09T12:10:00.000"
  }
}
```

### 가족 초대 코드 수락

```http
POST /api/v1/family-invite-codes/accept
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "code": "A7K2Q9"
}
```

응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "pregnancyId": 1,
    "connectionId": 10,
    "status": "CONNECTED"
  }
}
```

## 권장 패키지 구조

기준 루트 패키지는 `com.onmom`입니다. 프로젝트 생성 초기 패키지가 이와 다르면, 도메인 구현을 늘리기 전에 이 기준으로 정리합니다.

```text
com.onmom
├── global
│   ├── config
│   ├── exception
│   ├── response
│   ├── auth
│   └── common
├── auth
├── user
├── pregnancy
├── family
├── emotion
├── chat
├── calendar
├── hospital
├── notification
└── ai
```

각 도메인은 필요에 따라 아래 하위 패키지를 가집니다.

```text
controller
service
repository
domain
dto
client
```

## API 응답 형식

성공 응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "id": 1
  }
}
```

에러 응답:

```json
{
  "result": "ERROR",
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

Cursor 목록 응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "content": [],
    "page": {
      "nextCursor": null,
      "size": 20,
      "hasNext": false
    }
  }
}
```

## Cursor Pagination

목록 조회는 page/offset 방식 대신 cursor 방식을 기본으로 합니다.

첫 페이지:

```http
GET /api/v1/notifications?size=20
```

다음 페이지:

```http
GET /api/v1/notifications?cursor=eyJjcmVhdGVkQXQiOiIyMDI2LTA3LTA5VDEwOjAwOjAwLjAwMFoiLCJpZCI6MTAwfQ&size=20
```

기본 규칙:

- `size` 기본값은 20입니다.
- 최대 `size`는 100을 넘지 않습니다.
- 정렬은 기본적으로 `createdAt desc, id desc`를 사용합니다.
- `cursor`는 클라이언트가 해석하지 않는 opaque string입니다.
- 서버는 cursor 내부에 마지막 항목의 `createdAt`과 `id`를 담습니다.
- 다음 페이지가 있으면 `nextCursor`를 문자열로 내려줍니다.
- 다음 페이지 조회 조건은 `(createdAt < cursorCreatedAt) OR (createdAt = cursorCreatedAt AND id < cursorId)`입니다.

## DB 정책

초기 DB 설계는 MySQL 8 기준입니다.

- PK, UNIQUE, INDEX는 유지합니다.
- `FOREIGN KEY`, `CHECK`, MySQL `ENUM`은 사용하지 않습니다.
- 참조 무결성은 Service 레이어에서 검증합니다.
- 상태값은 DB에 `VARCHAR`로 저장합니다.
- Java 코드에서는 enum으로 상태값을 관리합니다.

## CLI/AI 코딩 도구 사용법

Codex CLI 또는 다른 AI 코딩 도구에 작업을 맡길 때는 아래처럼 요청합니다.

```text
AGENTS.md를 먼저 읽고, 그 지침대로 emotion 도메인의 감정 기록 생성 API를 구현해줘.
```

또는:

```text
docs/onmom_api_guidelines.md와 docs/onmom_db_design.md를 읽고,
온맘 백엔드 규칙에 맞게 notification 도메인의 cursor 목록 조회 API를 구현해줘.
```

## 작업 체크리스트

새 API를 추가할 때는 아래 내용을 함께 정리합니다.

- API 목적
- HTTP method와 path
- 요청 DTO
- 응답 DTO
- 성공 status code
- 주요 에러 코드
- 권한 조건
- 테스트 케이스
- DB 변경 여부

## 테스트 정책

- 단위 테스트는 DB와 외부 API에 직접 의존하지 않도록 작성합니다.
- 현재 단계에서는 모듈/단위 테스트를 우선 작성합니다.
- MySQL DDL, Flyway, JPA 매핑 검증은 DB 구조가 안정화된 뒤 별도 통합 테스트로 분리합니다.

## 현재 산출물

```text
AGENTS.md
README.md
docs/
├── onmom_api_guidelines.md
└── onmom_db_design.md
src/main/resources/db/migration/
└── V1__create_onmom_schema.sql
```
