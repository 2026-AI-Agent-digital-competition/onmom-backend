# 온맘(Onmom)

AI agent를 활용한 산모-가족 연결 서비스입니다.  
산모가 자신의 감정과 상태를 기록하면 AI가 이를 해석하고, 가족이 이해하기 쉬운 메시지와 알림으로 연결해주는 것을 목표로 합니다.

## 프로젝트 개요

온맘은 임신 중 산모의 감정 변화, 일정, 위험 신호를 가족과 더 부드럽게 공유하기 위한 서비스입니다.

주요 흐름:

- 산모/가족 역할 선택
- 카카오 로그인
- 산모 정보와 임신 주수 등록
- QR/초대 링크 기반 가족 연결
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
- Lombok
- Docker Compose Support
- Testcontainers

카카오 로그인은 백엔드에서 카카오 사용자 정보 API를 직접 호출하는 방식으로 구현합니다. 로그인 성공 후 백엔드는 자체 access token을 JWT로 발급합니다.

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
