#!/usr/bin/env bash
# After a dev prerelease, open the dev -> main pull request unless one is already open.
set -euo pipefail

branch="${1:?branch is required}"
[[ "$branch" == dev ]] || exit 0

if [[ -z "$(gh pr list --base main --head dev --state open --json number --jq '.[].number')" ]]; then
    gh pr create --base main --head dev --title 'chore: Merge branch `dev` to `main`' \
        --body 'This pull request will merge branch `dev` to `main`.'
fi
