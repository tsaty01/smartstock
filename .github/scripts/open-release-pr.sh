#!/usr/bin/env bash
# After a dev prerelease, open (or retitle) the dev -> main pull request.
set -euo pipefail

branch="${1:?branch is required}"
version="${2:?version is required}"
[[ "$branch" == dev ]] || exit 0

title="chore: merge dev to main (${version})"
if [[ -n "$(gh pr list --base main --head dev --state open --json number --jq '.[].number')" ]]; then
    gh pr edit dev --title "$title"
else
    gh pr create --base main --head dev --title "$title" \
        --body "Releasable changes on \`dev\` up to ${version}. Merge with a merge commit to publish a stable release."
fi
