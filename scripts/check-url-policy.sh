#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
cd "$repo_root"
mkdir -p .private
check_dir=$(mktemp -d "$repo_root/.private/url-check-XXXXXX")
trap 'rm -rf -- "$check_dir"' EXIT
rtk javac -d "$check_dir" app/src/main/java/dev/mero/tv/UrlPolicy.java app/src/test/java/dev/mero/tv/UrlPolicyCheck.java
rtk java -cp "$check_dir" dev.mero.tv.UrlPolicyCheck
