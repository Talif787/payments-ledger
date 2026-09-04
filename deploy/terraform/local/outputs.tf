output "cluster_name" {
  value       = kind_cluster.this.name
  description = "The kind cluster name (use with kubectl and kind)."
}

output "namespace" {
  value       = var.namespace
  description = "Namespace the release is installed into."
}

output "next_steps" {
  value = <<-EOT
    Deployed locally at no cost.
    Point kubectl at the cluster:  kubectl config use-context kind-${var.cluster_name}
    Check pods:                    kubectl -n ${var.namespace} get pods
    Port-forward and smoke test as in RUNBOOK-phase5b-terraform.md.
  EOT
}
