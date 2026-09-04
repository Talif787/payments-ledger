# GCP Terraform (GKE Autopilot + Cloud SQL) — VALIDATE ONLY, DO NOT APPLY FOR FREE

## Read this first

Applying this configuration creates BILLABLE Google Cloud resources:

- A GKE Autopilot cluster (per-pod charges; only one cluster per billing account
  avoids the management fee, and pods still cost money).
- A Cloud SQL for PostgreSQL instance (no free running tier).
- A VPC, private services access, and an Artifact Registry repository.

Left running, this is on the order of tens of dollars per month at the smallest
tiers. There is no configuration of GKE + Cloud SQL that is free to run.

To keep everything free, use `deploy/terraform/local` (kind), which applies end to
end at zero cost. Treat this GCP configuration as a validated portfolio artifact
you apply only deliberately, for a short live demo, then destroy.

## Cost guard

`apply` is blocked until you explicitly accept charges. The GKE cluster has a
precondition on `var.accept_costs`; with the default `false`, plan/apply fails
with a clear message. You must set `accept_costs = true` to proceed. This is
intentional: it makes an accidental spend impossible.

## Free validation (no billing, no cluster)

    cd deploy/terraform/gcp
    terraform init         # downloads providers only, free
    terraform fmt -check
    terraform validate     # checks the configuration, free, no GCP calls

## If and only if you choose to spend (later, deliberately)

    gcloud auth application-default login
    cp terraform.tfvars.example terraform.tfvars   # set project_id
    # edit terraform.tfvars: accept_costs = true
    terraform plan                                 # review what will be created and cost
    terraform apply                                # BILLABLE from here

    # deploy the app: build+push images to Artifact Registry, get cluster creds,
    # then helm install with values-gcp.yaml (see the Phase 5b runbook).

    # ALWAYS tear down when done:
    terraform destroy

Kafka and Redis are self-hosted on the cluster via the Helm chart (there is no
cheap managed Kafka on GCP); only PostgreSQL is managed, via Cloud SQL.
