#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:9050}"
API_KEY="${API_KEY:-internal-zaytoun-key}"
IMG_DIR="${1:-src/test/resources/sample-images}"

for img in "$IMG_DIR"/*; do
  [ -f "$img" ] || continue
  batch="demo-$(basename "$img" | cut -d. -f1)"
  curl -sS -X POST "$BASE_URL/api/v1/dataset/images" \
    -H "X-API-Key: $API_KEY" \
    -F "file=@$img" \
    -F "batchId=$batch" \
    -F "cultivar=Demo" \
    -F "actualYieldPercent=18.5" >/dev/null
  echo "Loaded $batch"
done
