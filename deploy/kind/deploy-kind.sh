#!/usr/bin/env bash
# End-to-end local deploy: create a kind cluster, build the three service images,
# load them into the cluster, and install the Helm chart. Run from the repo root.
# Zero cloud cost. Requires docker, kind, kubectl, and helm.
set -euo pipefail

CLUSTER="${CLUSTER:-payments-ledger}"
NS="${NS:-payments-ledger}"
SERVICES=(ledger-service reconciliation-service fraud-service)

echo "== 1/4 cluster =="
if kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  echo "kind cluster '$CLUSTER' already exists"
else
  kind create cluster --config deploy/kind/kind-config.yaml
fi

echo "== 2/4 build images =="
for s in "${SERVICES[@]}"; do
  echo "building $s:local"
  docker build -t "$s:local" "./$s"
done

echo "== 3/4 load images into kind =="
for s in "${SERVICES[@]}"; do
  kind load docker-image "$s:local" --name "$CLUSTER"
done

echo "== 4/4 install chart =="
kubectl create namespace "$NS" --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install payments-ledger deploy/helm/payments-ledger -n "$NS" --wait --timeout 10m

echo ""
kubectl -n "$NS" get pods
echo ""
echo "All set. Port-forward and smoke test with the commands in RUNBOOK-phase5a-k8s.md."
