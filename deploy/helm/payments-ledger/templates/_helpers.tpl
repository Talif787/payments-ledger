{{- define "pl.labels" -}}
app.kubernetes.io/part-of: payments-ledger
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/* Image reference: "<registry><name>:<tag>" */}}
{{- define "pl.image" -}}
{{- printf "%s%s:%s" .root.Values.image.registry .name .root.Values.image.tag -}}
{{- end -}}
