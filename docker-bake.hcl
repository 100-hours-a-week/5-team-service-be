variable "REGISTRY" {
  default = ""
}

variable "TAG" {
  default = "develop"
}

group "default" {
  targets = ["api", "chat"]
}

# dev: doktori/backend-api, prod: doktori/prod-backend-api
# ECR 레포 분리 — dev lifecycle policy가 prod 이미지를 밀어내는 문제 방지
target "api" {
  context    = "."
  target     = "api"
  platforms  = ["linux/arm64"]
  tags = TAG == "develop" ? [
    "${REGISTRY}/doktori/backend-api:develop"
  ] : [
    "${REGISTRY}/doktori/prod-backend-api:${TAG}",
  ]
}

target "chat" {
  context    = "."
  target     = "chat"
  platforms  = ["linux/arm64"]
  tags = TAG == "develop" ? [
    "${REGISTRY}/doktori/backend-chat:develop"
  ] : [
    "${REGISTRY}/doktori/prod-backend-chat:${TAG}",
  ]
}