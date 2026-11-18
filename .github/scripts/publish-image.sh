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
    --tag "${image}:${version}" \
    --tag "${image}:${moving_tag}"
