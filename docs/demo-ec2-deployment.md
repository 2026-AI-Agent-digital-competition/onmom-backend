# Docker Hub 이미지로 Ubuntu EC2에 시연 배포하기

이 가이드는 도메인, HTTPS, CI/CD, ECR 없이 Docker Hub 이미지를 Ubuntu EC2에 직접 배포하는 실습입니다.
최종 접속 주소는 `http://EC2_공인_IP:8080`입니다. 시연이 끝나면 EC2를 중지하거나 종료해 비용을 관리하세요.

> 이미 Amazon Linux EC2를 만들었다면 Ubuntu로 운영체제를 바꿀 수는 없습니다. 새 Ubuntu EC2를 만든 뒤,
> 정상 동작을 확인한 다음 기존 인스턴스를 종료하세요.

## 준비물

- Docker Desktop이 실행 중인 내 컴퓨터
- Docker Hub 계정과 public repository `내_DockerHub_ID/onmom-backend`
- Ubuntu Server 24.04 LTS, `t3.micro` EC2와 `.pem` 키 파일
- EC2 보안 그룹: SSH(22)는 **내 IP**, 사용자 접속용 TCP 8080은 `0.0.0.0/0`

도메인과 HTTPS는 필요 없습니다. 카카오 실로그인도 지금은 연결하지 않고, 시연용 개발 토큰 API를 사용합니다.

## 1. Docker Hub repository 만들기

1. [Docker Hub](https://hub.docker.com/)에 로그인한다.
2. **My Hub → Repositories → Create repository**를 누른다.
3. Repository name에 `onmom-backend`를 입력한다.
4. Visibility는 **Public**을 선택하고 생성한다.

이 문서에서 `내_DockerHub_ID`는 Docker Hub 프로필에 보이는 사용자 ID입니다.

## 2. Ubuntu EC2 만들기

AWS 콘솔에서 리전을 **서울(`ap-northeast-2`)**로 선택한 뒤 **EC2 → Instances → Launch instance**를 누릅니다.

- Name: `onmom-demo-ubuntu`
- AMI: **Ubuntu Server 24.04 LTS (HVM), SSD Volume Type**, 64-bit(x86)
- Instance type: **t3.micro**
- Key pair: 새 key pair를 만들고 `.pem` 파일을 안전한 곳에 저장
- Network security group:
  - SSH / TCP 22 / Source: **My IP**
  - Custom TCP / TCP 8080 / Source: `0.0.0.0/0`
- Storage: gp3 20GiB
- Advanced details: 기본값 유지, IAM instance profile 선택하지 않음

인스턴스가 Running 상태가 되면 Public IPv4 address를 기록합니다. Docker Hub public image를 사용하므로
AWS IAM 역할이나 AWS CLI 설정은 필요 없습니다.

## 3. 내 컴퓨터에서 이미지 만들고 Docker Hub에 올리기

프로젝트 루트에서 `내_DockerHub_ID`를 실제 Docker Hub 사용자 ID로 바꿔 실행합니다.

```bash
docker login
docker build --platform linux/amd64 -t 내_DockerHub_ID/onmom-backend:demo .
docker push 내_DockerHub_ID/onmom-backend:demo
```

Docker Hub repository의 Tags에 `demo`가 보이면 업로드가 완료된 것입니다. Apple Silicon Mac도 `t3.micro`의
x86_64 CPU에서 실행할 수 있도록 `--platform linux/amd64`를 반드시 붙입니다.

## 4. Ubuntu EC2에 Docker 설치하기

키 파일 권한을 제한하고 Ubuntu 사용자로 SSH 접속합니다. Ubuntu AMI의 기본 사용자는 `ubuntu`입니다.

```bash
chmod 400 ~/Downloads/onmom-demo-key.pem
ssh -i ~/Downloads/onmom-demo-key.pem ubuntu@EC2_공인_IP
```

EC2에서 아래 명령을 **위에서 아래 순서대로** 실행합니다. Docker 공식 apt repository를 추가하고 Docker Engine과
Docker Compose plugin을 설치하는 명령입니다.

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
exit
```

다시 SSH로 접속한 뒤 설치를 확인합니다.

```bash
ssh -i ~/Downloads/onmom-demo-key.pem ubuntu@EC2_공인_IP
docker run hello-world
docker compose version
```

Docker의 Ubuntu 설치 방식은 공식 문서를 따릅니다. [Docker Ubuntu 설치 문서](https://docs.docker.com/engine/install/ubuntu/)

## 5. Compose 파일과 환경 변수 전송하기

내 컴퓨터에서 EC2 폴더를 만든 뒤, Compose 파일과 예시 환경 파일을 전송합니다.

```bash
ssh -i ~/Downloads/onmom-demo-key.pem ubuntu@EC2_공인_IP 'mkdir -p ~/onmom'
scp -i ~/Downloads/onmom-demo-key.pem compose.yaml demo.env.example ubuntu@EC2_공인_IP:~/onmom/
```

EC2에 접속해 실제 환경 파일을 만들고 값을 채웁니다.

```bash
cd ~/onmom
cp demo.env.example .env
nano .env
```

`APP_IMAGE`의 `YOUR_DOCKERHUB_ID`를 실제 Docker Hub ID로 바꿉니다. 아래 세 값은 각각 다른
랜덤 문자열로 바꿉니다.

```bash
openssl rand -hex 32
```

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `ONMOM_JWT_SECRET`

## 6. EC2에서 실행하고 확인하기

```bash
docker compose pull
docker compose up -d
docker compose ps
docker compose logs -f app
```

브라우저에서 다음 주소를 엽니다.

```text
http://EC2_공인_IP:8080/swagger-ui/index.html
```

`app`이 `running`이면 배포 성공입니다. MySQL은 외부에 포트를 열지 않아 EC2 밖에서 접근할 수 없습니다.

카카오 로그인 없이 토큰이 필요하면 아래 요청을 한 번 보냅니다. 빈 DB라면 데모 산모 사용자를 만들고,
이미 있으면 같은 사용자의 토큰을 다시 발급합니다.

```bash
curl -X POST http://EC2_공인_IP:8080/api/v1/dev/auth/demo-login
```

응답의 `data.accessToken`만 Swagger의 **Authorize**에 입력합니다. Swagger가 `Bearer ` 접두어를 자동으로 붙입니다.
이 엔드포인트는 `.env`의 `ONMOM_DEV_TOKEN_ENABLED=true`일 때만 존재하며, 실제 서비스 배포에서는 반드시 `false`로 바꿔야 합니다.

## 7. React 프런트엔드 연결하기

프런트를 각 팀원의 컴퓨터에서 Vite로 실행한다면 프런트 프로젝트의 `.env`에 EC2 API 주소를 넣습니다.

```text
VITE_API_BASE_URL=http://EC2_공인_IP:8080
```

API 호출 코드는 이 값을 base URL로 사용합니다. 예를 들어 Axios를 사용한다면 다음과 같습니다.

```javascript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

const { data } = await api.post('/api/v1/dev/auth/demo-login');
const accessToken = data.data.accessToken;

await api.post('/api/v1/pregnancies', requestBody, {
  headers: { Authorization: `Bearer ${accessToken}` },
});
```

EC2의 `~/onmom/.env`에는 `ONMOM_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173`를
유지합니다. 값을 바꿨다면 EC2에서 `docker compose up -d --force-recreate app`을 실행합니다.

현재 백엔드는 HTTP이고 도메인이 없으므로 프런트도 로컬 HTTP 개발 서버로 실행해야 합니다. Vercel처럼 HTTPS로
배포한 프런트는 HTTP API를 호출할 수 없습니다(브라우저 mixed content 차단). 도메인과 HTTPS를 추가한 뒤에야
프런트도 외부에 배포할 수 있습니다.

## 8. 코드 변경 후 다시 배포하기

내 컴퓨터에서 3단계의 `docker build`, `docker push` 명령을 다시 실행합니다.

EC2에서는 다음만 실행합니다.

```bash
cd ~/onmom
docker compose pull app
docker compose up -d --force-recreate app
```

MySQL 데이터는 Docker volume에 남아 있으므로 `docker compose down -v`는 실행하지 마세요. 이 명령은
데이터베이스까지 삭제합니다.
