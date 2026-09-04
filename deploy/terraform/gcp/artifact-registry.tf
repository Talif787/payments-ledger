# Docker registry for the service images (push here, then Helm pulls from here).
resource "google_artifact_registry_repository" "images" {
  location      = var.region
  repository_id = var.cluster_name
  format        = "DOCKER"
  depends_on    = [google_project_service.enabled]
}
