# 온맘(Onmom) API 설계 가이드

Spring Boot 기반 온맘 백엔드 API 협업 가이드입니다. 팀원이 같은 규칙으로 Controller, DTO, 에러 응답, cursor pagination, 문서화를 작성하기 위한 기준입니다.

## 1. 기본 원칙

- API는 REST 스타일을 따른다.
- JSON 요청/응답을 기본으로 한다.
- URL은 명사 중심, 소문자, kebab-case를 사용한다.
- Java 필드와 JSON 필드는 `camelCase`를 사용한다.
- 상태값은 문자열 코드로 주고받는다.
- 서버 저장 시간은 UTC를 권장하고, API 응답은 ISO-8601 형식을 사용한다.
- 민감 정보, 토큰, 내부 예외 메시지는 응답에 노출하지 않는다.

## 2. Base URL

```text
Local: http://localhost:8080
Dev:   https://dev-api.onmom.example.com
Prod:  https://api.onmom.example.com
```

API 경로는 `/api/v1` prefix를 사용합니다.

## 3. HTTP 규칙

| Method | 용도 |
|---|---|
| `GET` | 조회 |
| `POST` | 생성, 명령성 액션 |
| `PATCH` | 일부 수정 |
| `PUT` | 전체 교체가 명확할 때만 사용 |
| `DELETE` | 삭제 요청. 실제 DB는 soft delete 가능 |

| Status | 의미 |
|---|---|
| `200 OK` | 조회/수정 성공 |
| `201 Created` | 생성 성공 |
| `204 No Content` | 응답 본문 없는 성공 |
| `400 Bad Request` | 요청 형식 또는 값 검증 실패 |
| `401 Unauthorized` | 인증 필요 또는 토큰 invalid |
| `403 Forbidden` | 권한 없음 |
| `404 Not Found` | 리소스 없음 |
| `409 Conflict` | 중복, 상태 충돌 |
| `422 Unprocessable Entity` | 비즈니스 규칙 위반 |
| `502 Bad Gateway` | 외부 API 연동 실패 |
| `500 Internal Server Error` | 서버 오류 |

## 4. URL 네이밍

```http
GET    /api/v1/emotion-records
GET    /api/v1/emotion-records/{emotionRecordId}
POST   /api/v1/emotion-records
PATCH  /api/v1/emotion-records/{emotionRecordId}
DELETE /api/v1/emotion-records/{emotionRecordId}
```

부모 리소스 맥락이 강한 경우에만 중첩합니다.

```http
GET  /api/v1/pregnancies/{pregnancyId}/emotion-records
POST /api/v1/pregnancies/{pregnancyId}/family-invite-codes
```

행동성 API는 짧은 동사형 suffix를 허용합니다.

```http
POST /api/v1/family-invite-codes/accept
POST /api/v1/calendar-events/{eventId}/confirm
POST /api/v1/notifications/{notificationId}/read
```

## 5. 공통 응답 형식

모든 API는 공통 `ApiResponse<T>` 형식으로 응답합니다.

권장 패키지:

```text
com.onmom.global.response
├── ApiResponse.java
└── ResultType.java

com.onmom.global.exception
├── GlobalExceptionHandler.java
├── BusinessException.java
├── ErrorCode.java
└── FieldErrorResponse.java
```

`ApiResponse` 예시:

```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final ResultType result;
    private final T data;
    private final String code;
    private final String message;

    public static <S> ApiResponse<S> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null, null);
    }

    public static <S> ApiResponse<S> error(String code, String message) {
        return new ApiResponse<>(ResultType.ERROR, null, code, message);
    }

    public static <S> ApiResponse<S> error(String code, String message, S data) {
        return new ApiResponse<>(ResultType.ERROR, data, code, message);
    }
}
```

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

필드 검증 실패:

```json
{
  "result": "ERROR",
  "data": [
    {
      "field": "moodScore",
      "message": "moodScore는 1에서 5 사이여야 합니다."
    }
  ],
  "code": "VALIDATION_FAILED",
  "message": "요청 값이 올바르지 않습니다."
}
```

## 6. Cursor Pagination

목록 조회는 처음부터 cursor 기반으로 설계합니다. Spring `Pageable`은 page/offset 기반이므로 기본 목록 API에 사용하지 않습니다.

클라이언트는 cursor를 해석하지 않는 opaque string으로 취급합니다. 서버는 기본적으로 마지막 항목의 `createdAt`과 `id`를 cursor에 담아 다음 페이지 조건을 계산합니다.

첫 페이지:

```http
GET /api/v1/notifications?size=20
```

다음 페이지:

```http
GET /api/v1/notifications?cursor=eyJjcmVhdGVkQXQiOiIyMDI2LTA3LTA5VDEwOjAwOjAwLjAwMFoiLCJpZCI6MTAwfQ&size=20
```

기본 규칙:

```text
size = 20
maxSize = 100
sort = createdAt desc, id desc
cursor = Base64URL encoded JSON containing { "createdAt": lastItem.createdAt, "id": lastItem.id }
```

첫 페이지는 `cursor`를 보내지 않습니다. 다음 페이지 요청 시 서버는 cursor를 복호화해 `cursorCreatedAt`, `cursorId`를 얻습니다.

기본 조회 조건:

```sql
WHERE (:cursorCreatedAt IS NULL)
   OR (created_at < :cursorCreatedAt)
   OR (created_at = :cursorCreatedAt AND id < :cursorId)
ORDER BY created_at DESC, id DESC
LIMIT :sizePlusOne
```

`sizePlusOne`은 `size + 1`입니다. 조회 결과가 `size + 1`개이면 `hasNext = true`로 판단하고, 응답 `content`에는 앞의 `size`개만 담습니다.

응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "content": [
      {
        "id": 100,
        "createdAt": "2026-07-09T10:00:00.000Z",
        "title": "태동 감소에 대한 불안 표현"
      }
    ],
    "page": {
      "nextCursor": "eyJjcmVhdGVkQXQiOiIyMDI2LTA3LTA5VDEwOjAwOjAwLjAwMFoiLCJpZCI6MTAwfQ",
      "size": 20,
      "hasNext": true
    }
  }
}
```

응답 `content`에는 최대 `size`개만 담고, 다음 페이지가 있으면 응답에 포함된 마지막 item 기준으로 `nextCursor`를 내려줍니다. 다음 페이지가 없으면 `nextCursor`는 `null`입니다.

도메인 요구상 정렬 기준이 `createdAt desc, id desc`가 아닌 경우에는 cursor에 정렬 기준 컬럼과 `id`를 함께 담아야 합니다. 예를 들어 일정 목록이 `startsAt asc, id asc` 정렬을 사용한다면 cursor는 `{ "startsAt": lastItem.startsAt, "id": lastItem.id }`처럼 구성하고, 조회 조건도 해당 정렬 방향에 맞춰 별도로 정의합니다.

잘못된 cursor 형식, 복호화 실패, 필수 값 누락은 `400 Bad Request`와 `INVALID_CURSOR` 에러 코드로 응답합니다.

## 7. 인증 정책

초기 버전에서는 Spring Security를 사용하지 않고 직접 인증 흐름을 구현합니다.

카카오 로그인 흐름:

1. React가 카카오 JavaScript SDK로 인가를 요청하고 무작위 `state`를 저장한다.
2. 카카오가 등록된 React callback URI로 `code`, `state`를 전달한다.
3. React는 `state`가 저장값과 일치하는지 검증한다.
4. React가 백엔드에 카카오 인가 코드와 신규 가입 역할을 전달한다.
5. 백엔드는 인가 코드로 카카오 토큰 API를 호출해 access token을 발급받는다.
6. 백엔드는 access token으로 `/v2/user/me`를 호출해 카카오 회원번호와 동의된 사용자 정보를 조회한다.
7. 회원번호를 `providerUserId`로 사용해 `oauth_accounts`에서 기존 사용자를 조회한다.
8. 계정이 없으면 카카오 이메일, 닉네임, 프로필 이미지로 `users`, `oauth_accounts`를 생성한다.
9. 백엔드는 자체 access token을 JWT로 발급한다.

현재 카카오 로그인 API:

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

`role`은 `MOTHER`, `FAMILY`만 허용합니다. 기존 카카오 계정이면 `oauth_accounts(provider, provider_user_id)`로 기존 사용자와 저장된 역할을 사용하고, 없으면 요청 `role`을 `users.primary_role`로 저장한 뒤 `users`, `oauth_accounts`를 생성합니다.

인가 코드는 일회용이며 React가 카카오 토큰을 직접 발급하거나 백엔드에 전달하지 않습니다. 백엔드는 카카오 client ID(REST API 키), client secret, redirect URI를 외부 설정으로 관리하고, 발급된 카카오 access/refresh token을 저장하거나 API 응답에 포함하지 않습니다. React 인가 요청의 callback URI, 카카오 개발자 콘솔 등록값, 백엔드의 redirect URI 설정은 정확히 같아야 합니다.

백엔드는 authorization URI를 사용하지 않습니다. React SDK가 인가 화면 이동을 담당합니다. 카카오 token endpoint `https://kauth.kakao.com/oauth/token`과 user-info endpoint `https://kapi.kakao.com/v2/user/me`는 `KakaoClient` 내부 상수로 관리하며 application 설정으로 노출하지 않습니다.

카카오 원격 호출은 DB 트랜잭션 밖에서 실행합니다. OAuth 계정 조회·생성은 별도 트랜잭션에서 처리하고 `oauth_accounts(provider, provider_user_id)` UNIQUE 충돌을 `saveAndFlush`로 확인합니다. 동시 요청 중 실패한 트랜잭션이 종료되면 먼저 생성된 기존 계정을 새 read-only 트랜잭션에서 재조회합니다.

카카오 token/user-info 외부 연동 장애는 `WARN`으로 기록하되 `stage`, HTTP status, 예외 종류만 포함합니다. token endpoint의 `400 Bad Request`는 만료·재사용·invalid 인가 코드로 처리하고 로그를 남기지 않습니다. token endpoint의 `401/403`은 OAuth client 설정 거절로, `429`와 그 밖의 4xx/5xx는 외부 연동 실패로 기록합니다. 인가 코드, access/refresh token, client secret, 이메일, 카카오 회원번호, 응답 본문은 모든 로그에서 제외합니다.

JWT 기본 정책:

```text
tokenType = Bearer
subject = userId
claims.userId = users.id
claims.role = users.primary_role
expiresIn = 1 hour
```

JWT 서명 키는 환경 변수 또는 외부 설정으로 주입하고, 코드나 저장소에 평문으로 커밋하지 않습니다. 초기 구현에서는 refresh token을 발급하지 않습니다. refresh token이 필요해지면 토큰 저장/폐기 정책과 DB 테이블을 별도 설계한 뒤 추가합니다.

현재 설정 키:

```text
onmom.jwt.secret=${ONMOM_JWT_SECRET:}
onmom.jwt.access-token-expiration=1h
onmom.cors.allowed-origins=${ONMOM_CORS_ALLOWED_ORIGINS:http://localhost:5173}
onmom.kakao.client-id=${ONMOM_KAKAO_CLIENT_ID:}
onmom.kakao.client-secret=${ONMOM_KAKAO_CLIENT_SECRET:}
onmom.kakao.redirect-uri=${ONMOM_KAKAO_REDIRECT_URI:}
```

CORS는 `/api/**`에만 적용하고 허용 origin을 환경별로 명시합니다. 여러 origin은 쉼표로 구분합니다. 인증은 쿠키가 아닌 `Authorization` 헤더를 사용하므로 credentials는 허용하지 않습니다.

로그인 이후 인증이 필요한 API는 아래 헤더를 사용합니다.

```http
Authorization: Bearer {accessToken}
```

Spring Security를 쓰지 않는 경우에도 인증이 필요한 API는 Controller 진입 전에 JWT 서명, 만료 시간, 필수 claim을 검증하고 현재 사용자 ID를 요청 컨텍스트에 저장합니다. 권한 검증은 Service 레이어에서 이 사용자 ID를 기준으로 수행합니다.

인증 실패 에러:

| 상황 | Status | Error code |
|---|---:|---|
| Authorization 헤더 없음 | 401 | `AUTHENTICATION_REQUIRED` |
| Bearer 형식 아님 | 401 | `INVALID_AUTHORIZATION_HEADER` |
| JWT 서명 invalid | 401 | `INVALID_TOKEN` |
| JWT 만료 | 401 | `EXPIRED_TOKEN` |
| JWT 필수 claim 누락 | 401 | `INVALID_TOKEN` |
| 카카오 인가 코드 만료, 재사용 또는 invalid | 401 | `KAKAO_AUTHORIZATION_CODE_INVALID` |
| 카카오 OAuth 설정 누락 | 500 | `KAKAO_OAUTH_NOT_CONFIGURED` |
| 카카오 OAuth client 인증 거절 | 500 | `KAKAO_OAUTH_CONFIGURATION_INVALID` |
| 카카오 rate limit, 기타 token/user-info 외부 연동 실패 | 502 | `KAKAO_LOGIN_FAILED` |
| 지원하지 않는 role | 400 | `INVALID_ROLE` |
| JWT 서명 키 미설정 | 500 | `JWT_SECRET_NOT_CONFIGURED` |
| 초대 코드 없음 또는 사용 불가 | 400 | `INVALID_INVITE_CODE` |
| 초대 코드 만료 | 400 | `EXPIRED_INVITE_CODE` |
| 본인 초대 코드 수락 | 400 | `CANNOT_ACCEPT_OWN_INVITE` |
| 임신 프로필 없음 | 404 | `PREGNANCY_NOT_FOUND` |
| 임신 프로필 권한 없음 | 403 | `PREGNANCY_ACCESS_DENIED` |

## 8. 권한 규칙

요청의 `pregnancyId`가 현재 사용자와 연결되어 있는지 항상 확인합니다.

```text
MOTHER: pregnancies.mother_user_id == currentUserId
FAMILY: family_connections.family_user_id == currentUserId
        AND family_connections.status == CONNECTED
```

## 9. 가족 초대 정책

초기 MVP에서는 산모가 발급한 6글자 초대 코드를 가족이 입력해 연결합니다. 초대 코드는 DB에 저장하고, 실제 연결 결과는 `family_connections`에 저장합니다.

초대 코드 정책:

```text
length = 6
alphabet = ABCDEFGHJKLMNPQRSTUVWXYZ23456789
expiresIn = 10 minutes
activeCode = one PENDING code per pregnancy
usage = multiple family users can accept the same code before expiration
```

새 코드를 발급하면 같은 pregnancy의 기존 `PENDING` 코드는 `REVOKED` 처리합니다. 만료 전 여러 가족이 같은 코드를 입력할 수 있으므로 코드 자체에는 `ACCEPTED` 상태를 사용하지 않습니다.

초대 생성 예시:

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

초대 수락 예시:

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

수락 시 서버는 코드 존재 여부, 상태, 만료 시간, 본인 초대 수락 여부, 가족 연결 중복 여부를 검증한 뒤 `family_connections`를 생성하거나 연결 상태를 갱신합니다.

초대 코드 취소 API, 초대 코드 목록 조회 API, 초대 사용 이력 테이블은 MVP 범위에서 제외합니다.

## 10. 도메인형 파일 구조

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

의존 방향:

- 도메인은 `global`에 의존할 수 있다.
- `global`은 개별 도메인에 의존하지 않는다.
- Controller는 Service에만 의존한다.
- Service는 Repository, domain, 외부 client에 의존한다.
- Entity를 Controller 응답으로 직접 반환하지 않는다.

## 11. DTO와 Validation

- 요청 DTO 이름은 `{Action}{Domain}Request` 형식을 사용합니다.
- 응답 DTO 이름은 `{Domain}Response` 또는 `{Action}{Domain}Response` 형식을 사용합니다.
- Entity를 Controller 응답으로 직접 반환하지 않습니다.
- 요청 DTO에는 Bean Validation을 사용합니다.
- 비즈니스 검증은 Service에서 처리합니다.

## 12. Controller 작성 규칙

- Controller는 요청/응답 변환과 HTTP 상태 코드 지정에 집중한다.
- 비즈니스 로직은 Service에 둔다.
- Entity를 직접 반환하지 않는다.
- `@RequestBody`에는 반드시 DTO를 사용한다.
- `@Valid`로 기본 검증을 수행한다.
- 의존성 주입은 생성자 주입을 기본으로 한다.
- 특별한 이유 없이 필드 주입이나 `@Autowired`를 사용하지 않는다.

예:

```java
@RestController
@RequestMapping("/api/v1/emotion-records")
@RequiredArgsConstructor
public class EmotionRecordController {

    private final EmotionRecordService emotionRecordService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmotionRecordResponse>> create(
            @Valid @RequestBody CreateEmotionRecordRequest request
    ) {
        EmotionRecordResponse response = emotionRecordService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
```

## 13. Service 작성 규칙

- 트랜잭션 경계는 Service에 둔다.
- 읽기 전용 조회에는 `@Transactional(readOnly = true)`를 사용한다.
- DB에 FK가 없으므로 참조 존재 여부를 Service에서 확인한다.
- 권한 검증은 Service 진입 초기에 수행한다.
- 목록 조회는 offset pagination 대신 cursor 조건을 사용한다.

## 14. 테스트 작성 규칙

- 단위 테스트는 DB, Docker, 외부 API에 직접 의존하지 않는다.
- Service 단위 테스트는 Repository, 외부 Client, JWT 발급기 등을 mock 또는 fake로 대체한다.
- Controller 테스트는 요청/응답 형식, validation, status code를 중심으로 검증한다.
- 초기 개발 단계에서는 모듈/단위 테스트를 우선 작성한다.
- MySQL DDL, Flyway migration, JPA 매핑 검증은 DB 구조가 안정화된 뒤 별도 통합 테스트로 분리한다.
- 통합 테스트는 단위 테스트보다 느리므로 필요한 범위를 명확히 한다.

## 15. 협업 체크리스트

API를 추가할 때 PR에 아래 내용을 포함합니다.

- API 목적
- HTTP method와 path
- 요청 DTO
- 응답 DTO
- 성공 status code
- 주요 에러 코드
- 권한 조건
- 테스트 케이스
- DB 변경 여부
