# ============================================================================
# COST WARNING: applying this configuration creates BILLABLE resources
# (a GKE Autopilot cluster and a Cloud SQL Postgres instance). It is NOT free.
# apply is blocked until you set accept_costs=true. See README.md.
# ============================================================================

variable "accept_costs" {
  description = "Set to true to acknowledge that GKE and Cloud SQL incur charges. apply is blocked while false."
  type        = bool
  default     = false
}

variable "project_id" {
  description = "GCP project id."
  type        = string
}

variable "region" {
  description = "GCP region."
  type        = string
  default     = "us-central1"
}

variable "cluster_name" {
  description = "GKE Autopilot cluster name."
  type        = string
  default     = "payments-ledger"
}

variable "db_tier" {
  description = "Cloud SQL machine tier. db-f1-micro is the smallest and cheapest."
  type        = string
  default     = "db-f1-micro"
}

variable "db_user" {
  description = "Cloud SQL application user."
  type        = string
  default     = "ledger"
}
