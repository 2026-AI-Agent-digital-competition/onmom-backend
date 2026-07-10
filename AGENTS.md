# 온맘(Onmom) 개발 지침

이 저장소에서 코드를 작성하거나 수정하기 전, 반드시 아래 문서를 먼저 읽고 따른다.

## 필수 참고 문서

- [API 설계 가이드](docs/onmom_api_guidelines.md)
- [DB 설계안](docs/onmom_db_design.md)
- [MySQL DDL](src/main/resources/db/migration/V1__create_onmom_schema.sql)

## 핵심 규칙

- 백엔드는 Spring Boot 기반으로 작성한다.
- Java 버전은 21을 기준으로 한다.
- 패키지는 도메인형 구조를 사용한다.
- 공통 응답은 `ApiResponse<T>`를 사용한다.
- 예외 처리는 `global.exception`에서 공통 처리한다.
- 목록 조회는 처음부터 cursor pagination을 사용한다.
- Controller는 Entity를 직접 반환하지 않는다.
- Controller는 요청/응답 변환에 집중하고, 비즈니스 로직은 Service에 둔다.
- 트랜잭션 경계는 Service에 둔다.
- DB는 `FOREIGN KEY`, `CHECK`, MySQL `ENUM` 없이 설계한다.
- DB 상태값은 `VARCHAR`로 저장하고 Java enum과 애플리케이션 검증으로 관리한다.
- DB 참조 무결성, 권한, 상태 전이는 Service 레이어에서 검증한다.
- React 웹은 카카오 인가 요청의 `state`를 검증한 뒤 인가 코드를 백엔드에 전달한다.
- 백엔드는 인가 코드를 카카오 access token으로 교환하고 사용자 정보 API를 호출해 카카오 회원번호를 확인한다.
- 카카오 client ID(REST API 키), client secret, redirect URI는 환경 변수 또는 외부 설정으로 주입하고, 카카오 access/refresh token은 클라이언트에 반환하거나 DB에 저장하지 않는다.
- 카카오 authorization URI는 React SDK가 관리하고, token/user-info endpoint는 `KakaoClient` 내부 상수로 관리한다.
- 카카오 외부 연동 장애 로그에는 실패 단계, HTTP 상태, 예외 종류만 남기고 인가 코드, 토큰, secret, 사용자 정보, 응답 본문을 기록하지 않는다.
- 백엔드 자체 access token은 JWT로 발급하고 `Authorization: Bearer {accessToken}` 헤더로 검증한다.
- CORS 허용 origin은 환경 변수로 주입하고 `/api/**`에만 적용하며, JWT 헤더 방식에서는 credentials를 허용하지 않는다.
- 가족 초대는 DB에 저장되는 6글자 초대 코드 방식으로 구현한다.
- 초대 코드는 pregnancy당 활성 코드 1개만 유지하고, 만료 전 여러 가족이 사용할 수 있다.
- JWT 서명 키와 카카오 client secret은 환경 변수 또는 외부 설정으로 주입하고 저장소에 커밋하지 않는다.
- 의존성 주입은 생성자 주입을 기본으로 하며, 특별한 이유 없이 필드 주입이나 `@Autowired`를 사용하지 않는다.

## 패키지 구조 기준

기준 루트 패키지는 `com.onmom`이다. 프로젝트 생성 초기 패키지가 이와 다르면, 도메인 구현을 늘리기 전에 이 기준으로 정리한다.

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

각 도메인은 필요에 따라 아래 하위 패키지를 가진다.

```text
controller
service
repository
domain
dto
client
```

## API 응답 규칙

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

## 작업 전 체크리스트

- 관련 도메인의 기존 구조를 먼저 확인한다.
- `docs/onmom_api_guidelines.md`의 API 규칙을 따른다.
- `docs/onmom_db_design.md`의 테이블과 상태값을 기준으로 구현한다.
- 새 API를 추가하면 요청 DTO, 응답 DTO, 에러 코드, 권한 조건을 함께 정리한다.
- 새 목록 조회 API는 offset/page 방식이 아니라 cursor 방식으로 구현한다.
- cursor는 클라이언트가 해석하지 않는 문자열로 취급하고, 기본 정렬은 `createdAt desc, id desc`를 사용한다.
- 단위 테스트는 DB와 외부 API를 직접 사용하지 않고 빠르게 실행되도록 작성한다.
- MySQL/Flyway/JPA 매핑 검증은 DB 구조가 안정화된 뒤 단위 테스트와 분리된 통합 테스트로 작성한다.
