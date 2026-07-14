# Cloudtype 배포 가이드

이 서비스는 프로젝트 루트의 `Dockerfile`로 Cloudtype에 배포합니다. 애플리케이션과 MariaDB는 반드시 같은 Cloudtype 프로젝트와 배포환경에 생성합니다.

> Cloudtype 프리 티어는 테스트·시연용입니다. 앱과 DB가 총 1GB 메모리를 나눠 쓰고, 데이터베이스는 임시 디스크를 사용하므로 매일 한 번 중지되며 DB 데이터도 초기화될 수 있습니다. 운영 데이터는 프리 티어 DB에 저장하지 않습니다.

## 1. MariaDB 생성

Cloudtype에서 `MariaDB` 서비스를 먼저 생성합니다. 프리 티어에서는 이 서비스에 **512MB**를 할당합니다. DB 이름, 사용자 이름, 비밀번호를 기록하고, 서비스의 내부 호스트 이름(서비스명)을 확인합니다. 같은 프로젝트 안에서 백엔드가 DB에 접속할 때는 외부 TCP 주소가 아니라 이 내부 호스트와 포트 `3306`을 사용합니다.

## 2. Spring Boot 서비스 생성

GitHub 저장소를 연결한 뒤 `Dockerfile` 템플릿을 선택합니다.

- Dockerfile 경로: `Dockerfile`
- 포트: `8080`
- 브랜치: 배포할 브랜치(기본 `main`)
- 프리 티어 메모리: **512MB**

Dockerfile은 이 할당에 맞춰 JVM 최대 힙을 256MB로 제한합니다. 앱과 MariaDB 이외의 동적 서비스는 프리 티어 공간에 추가하지 않습니다.

## 3. 환경 변수

Cloudtype 프로젝트 설정의 시크릿으로 DB 비밀번호, JWT 서명 키, 카카오 client secret, Gemini API 키를 등록합니다. 다음 키를 백엔드 서비스에 연결합니다.

```text
SPRING_PROFILES_ACTIVE=prod

DB_HOST=<MariaDB 내부 서비스명>
DB_PORT=3306
DB_NAME=onmom
DB_USERNAME=<MariaDB 사용자>
DB_PASSWORD=<MariaDB 비밀번호>

ONMOM_JWT_SECRET=<32바이트 이상 랜덤 값>
ONMOM_CORS_ALLOWED_ORIGINS=https://<프론트엔드 도메인>

ONMOM_KAKAO_CLIENT_ID=<카카오 REST API 키>
ONMOM_KAKAO_CLIENT_SECRET=<카카오 Client Secret>
ONMOM_KAKAO_REDIRECT_URI=https://<프론트엔드 카카오 콜백 URL>

GEMINI_API_KEY=<Gemini API 키>
ONMOM_DEV_TOKEN_ENABLED=false
```

`DB_PASSWORD`, `ONMOM_JWT_SECRET`, `ONMOM_KAKAO_CLIENT_SECRET`, `GEMINI_API_KEY`는 반드시 Cloudtype 시크릿으로 관리하며 GitHub나 `.env`에 커밋하지 않습니다. `ONMOM_CORS_ALLOWED_ORIGINS`에는 경로 없이 origin만 넣고, 여러 개면 쉼표로 구분합니다.

`SPRING_PROFILES_ACTIVE=prod`가 설정되면 로컬 전용 `local` 프로필이 활성화되지 않고, `DB_HOST` 등의 일반 배포용 데이터소스 변수를 사용합니다. 애플리케이션 시작 시 Flyway가 `V1__create_onmom_schema.sql`을 적용합니다.

## 4. 배포 후 확인

배포 로그에서 Spring Boot 시작과 Flyway 성공 여부를 확인하고 다음 주소를 엽니다.

```text
https://<Cloudtype 서비스 도메인>/swagger-ui/index.html
```

카카오 로그인을 사용하려면 카카오 개발자 콘솔의 Redirect URI와 `ONMOM_KAKAO_REDIRECT_URI`를 프런트엔드의 콜백 URL로 정확히 일치시킵니다. Cloudtype 백엔드 도메인은 카카오 redirect URI가 아니라 프런트엔드가 호출할 API base URL입니다.

## 장애 점검

- `503`: Cloudtype 서비스 포트가 `8080`인지 확인합니다.
- 하루에 한 번 또는 임의 시점에 앱/DB가 중지되거나 초기화될 수 있습니다. 프리 티어의 정상 제약이므로, 데모 데이터는 재생성 가능한 데이터만 사용합니다.
- DB 연결 실패: `DB_HOST`가 Cloudtype MariaDB의 **내부 서비스명**인지, 포트가 `3306`인지 확인합니다.
- Flyway 실패: 빈 DB인지 또는 이미 적용된 migration 이력과 스키마가 일치하는지 확인합니다.
- 브라우저 CORS 오류: 프런트의 실제 origin이 `ONMOM_CORS_ALLOWED_ORIGINS`에 정확히 포함됐는지 확인합니다.
