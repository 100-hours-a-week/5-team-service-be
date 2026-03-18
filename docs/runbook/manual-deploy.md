# Backend 수동 배포 가이드

서버(EC2)에서 직접 배포해야 할 때 사용하는 런북.

CI/CD 파이프라인(GitHub Actions)을 통한 배포가 정상이지만,
긴급 상황이나 파이프라인 장애 시 서버에서 직접 배포할 수 있다.

---

## 왜 `docker compose up -d`만 치면 안 되는가

서버의 `docker-compose.yml`은 이미지 경로에 `${ECR_REGISTRY}` 환경변수를 사용한다.
CI/CD에서는 SSM을 통해 `export ECR_REGISTRY=...`를 주입하지만,
SSH로 직접 접속하면 이 변수가 없어서 이미지 경로가 `/doktori/backend-api:develop`처럼 빈 값이 되고
`invalid reference format` 에러가 발생한다.

---

## Dev 환경 수동 배포

### 사전 조건
- EC2 인스턴스에 SSH 접속 (`ubuntu@<dev-server-ip>`)
- AWS CLI 설정 완료 (IAM Role 또는 credentials)
- ECR Registry 주소 확인 (예: `123456789012.dkr.ecr.ap-northeast-2.amazonaws.com`)

### 1. ECR 로그인

```bash
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <ECR_REGISTRY>
```

### 2. 환경변수 설정 + 배포
```bash
# ECR_REGISTRY 설정
export ECR_REGISTRY=<250857930609.dkr.ecr.ap-northeast-2.amazonaws.com/doktori/backend-chat>

# 이미지 pull & 재시작
docker compose -f /home/ubuntu/app/docker-compose.yml pull backend chat
docker compose -f /home/ubuntu/app/docker-compose.yml up -d --no-deps backend chat

# 미사용 이미지 정리
docker image prune -f
```

### 원라이너 (복사-붙여넣기용)

```bash
export ECR_REGISTRY=<ECR_REGISTRY> && \
  aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY && \
  docker compose -f /home/ubuntu/app/docker-compose.yml pull backend chat && \
  docker compose -f /home/ubuntu/app/docker-compose.yml up -d --no-deps backend chat && \
  docker image prune -f
```

> `<ECR_REGISTRY>`를 실제 ECR 주소로 치환해서 사용할 것.

---

## Prod 환경 수동 배포

Prod는 API 서버와 Chat 서버가 별도 인스턴스에서 동작한다.

### API 서버

```bash
export ECR_REGISTRY=<ECR_REGISTRY>
export TAG=<IMAGE_TAG>  # "develop", "sha-abc1234", "latest" 등

aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

docker pull $ECR_REGISTRY/doktori/backend-api:$TAG
docker stop backend-api 2>/dev/null || true
docker rm backend-api 2>/dev/null || true
docker run -d \
  --name backend-api \
  --restart unless-stopped \
  --env-file /home/ubuntu/app/.env \
  -p 8080:8080 \
  $ECR_REGISTRY/doktori/backend-api:$TAG

docker image prune -f
```

### Chat 서버

```bash
export ECR_REGISTRY=<ECR_REGISTRY>
export TAG=<IMAGE_TAG>

aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

docker pull $ECR_REGISTRY/doktori/backend-chat:$TAG
docker stop backend-chat 2>/dev/null || true
docker rm backend-chat 2>/dev/null || true
docker run -d \
  --name backend-chat \
  --restart unless-stopped \
  --env-file /home/ubuntu/app/.env \
  -p 8081:8081 \
  $ECR_REGISTRY/doktori/backend-chat:$TAG

docker image prune -f
```

---

## 배포 확인

```bash
# 컨테이너 상태 확인
docker ps

# 로그 확인
docker logs -f --tail 50 backend-api
docker logs -f --tail 50 backend-chat

# 헬스체크 (포트가 열려있는지)
curl -s http://localhost:8080/actuator/health || echo "API not responding"
curl -s http://localhost:8081/actuator/health || echo "Chat not responding"
```

---

## 롤백

이전 이미지 태그로 다시 배포하면 된다.

```bash
# ECR에서 사용 가능한 이미지 태그 조회
aws ecr describe-images \
  --repository-name doktori/backend-api \
  --query 'sort_by(imageDetails,&imagePushedAt)[-5:].imageTags' \
  --output table

# 이전 태그로 재배포 (위 배포 절차에서 TAG만 변경)
export TAG=<이전_태그>
```

---

## CI/CD를 통한 배포 (정상 경로)

수동 배포 대신 CI/CD를 트리거하는 것이 권장된다.

```bash
# GitHub CLI로 워크플로우 수동 트리거
gh workflow run "Backend CI/CD" --ref develop   # Dev 배포
gh workflow run "Backend CI/CD" --ref main      # Prod 배포

# 실행 상태 확인
gh run list --workflow="Backend CI/CD" --limit 5
```

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| `invalid reference format` | `ECR_REGISTRY` 미설정 | `export ECR_REGISTRY=...` 후 재시도 |
| `no basic credentials` | ECR 로그인 만료 | `aws ecr get-login-password ...` 재실행 |
| `manifest unknown` | 이미지 태그 없음 | `aws ecr describe-images`로 태그 확인 |
| 컨테이너 시작 후 바로 종료 | 앱 설정 오류 | `docker logs <container>` 확인, `.env` 점검 |
| SSM 명령 실패 | IAM 권한 부족 | AWS 콘솔에서 SSM 명령 이력 확인 |