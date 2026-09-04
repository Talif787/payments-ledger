output "cluster_name" {
  value = google_container_cluster.primary.name
}

output "region" {
  value = var.region
}

output "cloudsql_private_ip" {
  value       = google_sql_database_instance.postgres.private_ip_address
  description = "Private IP for the app's DB_URL / postgres.host Helm value."
}

output "db_password" {
  value       = random_password.db.result
  sensitive   = true
  description = "Generated DB password (terraform output -raw db_password)."
}

output "artifact_registry" {
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.images.repository_id}"
  description = "Image registry prefix for the chart's image.registry value."
}

output "get_credentials" {
  value = "gcloud container clusters get-credentials ${var.cluster_name} --region ${var.region} --project ${var.project_id}"
}
