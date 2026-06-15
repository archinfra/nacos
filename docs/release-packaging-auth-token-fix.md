# Nacos SkillHub K8s Auth Token Fix

Nacos requires `nacos.core.auth.plugin.nacos.token.secret.key` to be a Base64 string whose decoded original value is longer than 32 characters.

The K8s installer now always ensures `NACOS_AUTH_TOKEN` is valid:

- if `--auth-token` is empty, it generates a Base64 token automatically;
- if `--auth-token` is already valid Base64, it is used as-is;
- if `--auth-token` is a raw string longer than 32 characters, it is Base64-encoded automatically;
- otherwise installation fails early with a clear error.

The rendered Kubernetes manifest stores auth values in a Secret named `<release-name>-auth` and injects them into the Deployment with `secretKeyRef`.

Immediate manual fix for an existing Deployment:

```bash
RAW_SECRET='nacos-skillhub-token-change-me-please-20260615-xxxxxxxxxxxxxxxx'
TOKEN="$(printf '%s' "${RAW_SECRET}" | base64 | tr -d '\n')"

kubectl -n a11 set env deployment/nacos-skillhub \
  NACOS_AUTH_TOKEN="${TOKEN}" \
  NACOS_AUTH_IDENTITY_KEY=serverIdentity \
  NACOS_AUTH_IDENTITY_VALUE=security

kubectl -n a11 rollout restart deployment/nacos-skillhub
kubectl -n a11 rollout status deployment/nacos-skillhub
```
