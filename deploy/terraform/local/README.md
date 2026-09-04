# Local Terraform (free, applies end to end)

Provisions a local kind cluster and installs the Helm chart with real Terraform
providers. Costs nothing: everything runs in Docker locally.

## Use

    cd deploy/terraform/local
    terraform init
    terraform apply            # creates the kind cluster, builds/loads images, installs the chart

    # tear it all down
    terraform destroy

Requires docker, kind, kubectl, and helm on PATH (the same tools as the kind
deploy script). To rebuild images after code changes, bump image_rebuild_token:

    terraform apply -var image_rebuild_token=$(date +%s)
