variable "cluster_name" {
  description = "Name of the local kind cluster."
  type        = string
  default     = "payments-ledger"
}

variable "namespace" {
  description = "Kubernetes namespace for the release."
  type        = string
  default     = "payments-ledger"
}

variable "services" {
  description = "Service directories to build and load as local images."
  type        = list(string)
  default     = ["ledger-service", "reconciliation-service", "fraud-service"]
}

variable "image_tag" {
  description = "Local image tag (matches the chart's image.tag)."
  type        = string
  default     = "local"
}

variable "image_rebuild_token" {
  description = "Change this value to force a rebuild and reload of images on apply."
  type        = string
  default     = "1"
}
