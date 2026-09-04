# GKE Autopilot cluster. BILLABLE (per-pod resource charges, plus any cluster fee
# beyond the one free cluster per billing account). apply is gated on accept_costs.

resource "google_container_cluster" "primary" {
  name     = var.cluster_name
  location = var.region

  enable_autopilot = true
  network          = google_compute_network.vpc.id
  subnetwork       = google_compute_subnetwork.subnet.id

  ip_allocation_policy {
    cluster_secondary_range_name  = "pods"
    services_secondary_range_name = "services"
  }

  deletion_protection = false # demo: allow terraform destroy

  lifecycle {
    precondition {
      condition     = var.accept_costs
      error_message = "Refusing to create billable GKE/Cloud SQL resources. Set accept_costs=true to acknowledge charges and proceed. The free path is deploy/terraform/local."
    }
  }

  depends_on = [google_project_service.enabled]
}
