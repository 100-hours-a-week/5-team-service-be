variable "REGISTRY" {
  default = ""
}

variable "TAG" {
  default = "develop"
}

group "default" {
  targets = ["api", "chat"]
}

target "api" {
  context    = "."
  target     = "api"
  platforms  = ["linux/arm64"]
  tags = TAG == "develop" ? [
    "${REGISTRY}/doktori/backend-api:develop"
  ] : [
    "${REGISTRY}/doktori/backend-api:${TAG}",
    "${REGISTRY}/doktori/backend-api:latest"
  ]
}

target "chat" {
  context    = "."
  target     = "chat"
  platforms  = ["linux/arm64"]
  tags = TAG == "develop" ? [
    "${REGISTRY}/doktori/backend-chat:develop"
  ] : [
    "${REGISTRY}/doktori/backend-chat:${TAG}",
    "${REGISTRY}/doktori/backend-chat:latest"
  ]
}
