# Cloud SQL for PostgreSQL with the three application databases on a private IP.
# BILLABLE. There is no free running tier for Cloud SQL.

resource "random_password" "db" {
  length  = 24
  special = false
}

resource "google_sql_database_instance" "postgres" {
  name                = "${var.cluster_name}-pg"
  database_version    = "POSTGRES_16"
  region              = var.region
  deletion_protection = false # demo: allow terraform destroy to remove it

  depends_on = [google_service_networking_connection.psa]

  settings {
    tier              = var.db_tier
    availability_type = "ZONAL"
    disk_size         = 10

    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.vpc.id
    }
  }
}

resource "google_sql_database" "ledger" {
  name     = "ledger"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_database" "reconciliation" {
  name     = "reconciliation"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_database" "fraud" {
  name     = "fraud"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "app" {
  name     = var.db_user
  instance = google_sql_database_instance.postgres.name
  password = random_password.db.result
}
