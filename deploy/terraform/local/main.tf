# Local, zero-cost end-to-end deploy: provision a kind cluster, build and load the
# service images, and install the Helm chart. This applies for real and costs
# nothing, because everything runs in Docker on the local machine.

resource "kind_cluster" "this" {
  name           = var.cluster_name
  wait_for_ready = true
}

# The kind provider does not build or load images, so we shell out for that step.
# Rebuild is triggered by image_rebuild_token so `apply` is idempotent otherwise.
resource "null_resource" "build_and_load_images" {
  for_each = toset(var.services)

  triggers = {
    rebuild = var.image_rebuild_token
    cluster = kind_cluster.this.name
  }

  provisioner "local-exec" {
    command = <<-EOT
      docker build -t ${each.key}:${var.image_tag} ${path.module}/../../../${each.key}
      kind load docker-image ${each.key}:${var.image_tag} --name ${kind_cluster.this.name}
    EOT
  }

  depends_on = [kind_cluster.this]
}

provider "helm" {
  kubernetes {
    host                   = kind_cluster.this.endpoint
    client_certificate     = kind_cluster.this.client_certificate
    client_key             = kind_cluster.this.client_key
    cluster_ca_certificate = kind_cluster.this.cluster_ca_certificate
  }
}

resource "helm_release" "payments_ledger" {
  name             = "payments-ledger"
  chart            = "${path.module}/../../helm/payments-ledger"
  namespace        = var.namespace
  create_namespace = true
  wait             = true
  timeout          = 600

  depends_on = [null_resource.build_and_load_images]
}
