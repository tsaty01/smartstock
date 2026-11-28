#!/usr/bin/env bash
# Build the server image for a release and push it as <version> plus latest (stable) or dev (prerelease).
set -euo pipefail

version="${1:?semantic-release version is required}"
image="ghcr.io/${GITHUB_REPOSITORY,,}-server"
moving_tag=latest
[[ "$version" == *-* ]] && moving_tag=dev

printf '%s' "${GITHUB_TOKEN:?}" | docker login ghcr.io --username "${GITHUB_ACTOR:?}" --password-stdin
trap 'docker logout ghcr.io >/dev/null 2>&1 || true' EXIT

docker buildx build server \
    --push \
    --label "org.opencontainers.image.source=${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}" \
    --label "org.opencontainers.image.version=${version}" \
    --label "org.opencontainers.image.revision=$(git rev-parse HEAD)" \
    --tag "${image}:${version}" \
    --tag "${image}:${moving_tag}"

if [[ "$version" == *-dev.* ]]; then
    body='{"ref":"refs/heads/dev"}'
    signature=$(printf '%s' "$body" | openssl dgst -sha256 -hmac "${KOMODO_WEBHOOK_SECRET:?}" -hex | cut -d ' ' -f 2)
    curl --fail-with-body --retry 3 --retry-all-errors \
        -H 'Content-Type: application/json' \
        -H "X-Hub-Signature-256: sha256=$signature" \
        --data "$body" "${KOMODO_URL:?}/listener/github/action/smartstock-update/dev"
fi
