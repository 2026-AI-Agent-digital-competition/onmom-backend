# 온맘 Postman 로컬 테스트 가이드

## 1. Import

Postman에서 아래 두 파일을 모두 import합니다.

- `docs/postman/onmom-local.postman_collection.json`
- `docs/postman/onmom-local.postman_environment.json`

요청을 보내기 전에 Postman 오른쪽 위 Environment에서 `Onmom Local`을 선택합니다.

## 2. 서버 실행

개발용 토큰으로 로컬 테스트를 할 때는 아래 명령으로 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local --onmom.dev-token.enabled=true'
```

실제 카카오 로그인 테스트만 할 때는 아래처럼 실행해도 됩니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 3. 개발용 토큰 테스트 유저 준비

개발용 토큰 발급 API는 `users` 테이블에 이미 존재하는 사용자에게만 JWT를 발급합니다.

```sql
USE `onmom-local`;

INSERT INTO users (nickname, primary_role, status)
VALUES ('test-mother', 'MOTHER', 'ACTIVE'),
       ('test-family', 'FAMILY', 'ACTIVE');

SELECT id, nickname, primary_role, status
FROM users
ORDER BY id DESC
LIMIT 10;
```

조회된 사용자 id를 Postman environment 변수에 넣습니다.

- `motherUserId`
- `familyUserId`

## 4. 권장 요청 순서

아래 순서대로 요청을 실행합니다.

1. `00 Auth / Dev Auth Status`
2. `00 Auth / Create Mother Dev Token`
3. `00 Auth / Create Family Dev Token`
4. `01 Pregnancy / Create Pregnancy`
5. `02 Family Invite / Issue Family Invite Code`
6. `02 Family Invite / Accept Family Invite Code`
7. `03 Emotion Records / Create Or Update Emotion Record`
8. `03 Emotion Records / Get Daily Emotion Record`
9. `03 Emotion Records / Get Emotion Calendar`
10. `04 Chat / Create Chat Message`
11. `04 Chat / List Chat Session Messages`
12. `05 Family Insight And Messages / Create Family Insight`
13. `05 Family Insight And Messages / List Received Family Messages`

## 5. 카카오 로그인

브라우저에서 1회용 카카오 authorization code를 받은 뒤 Postman environment의 `kakaoAuthorizationCode`에 넣습니다.
그리고 `loginRole`을 `MOTHER` 또는 `FAMILY`로 설정한 뒤 아래 요청을 실행합니다.

```text
00 Auth / Kakao Login
```

컬렉션은 응답의 role에 따라 반환된 JWT를 `motherToken` 또는 `familyToken`에 자동 저장합니다.

## 6. AI API 참고

아래 요청들은 백엔드에서 Gemini를 호출하므로 `.env`에 `GEMINI_API_KEY`가 필요합니다.

- `03 Emotion Records / Create Or Update Emotion Record`
- `03 Emotion Records / Get Emotion AI Report`
- `04 Chat / Create Chat Message`
- `05 Family Insight And Messages / Create Family Insight`

`GEMINI_API_KEY`가 없으면 백엔드는 AI API 키 설정 오류를 반환합니다.
