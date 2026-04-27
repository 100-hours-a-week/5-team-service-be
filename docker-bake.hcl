variable "REGISTRY" {
  default = ""
}

variable "TAG" {
  default = "dev-develop"
}

group "default" {
  targets = ["api", "chat"]
}

# ECR repo는 dev/prod를 repo로 나누지 않고 tag prefix로 구분한다.
# Terraform ECR lifecycle policy는 prod-* / dev-* 태그를 보존 기준으로 사용한다.
target "api" {
  context    = "."
  target     = "api"
  platforms  = ["linux/arm64"]
  tags       = ["${REGISTRY}/doktori/backend-api:${TAG}"]
}

target "chat" {
  context    = "."
  target     = "chat"
  platforms  = ["linux/arm64"]
  tags       = ["${REGISTRY}/doktori/backend-chat:${TAG}"]
}
