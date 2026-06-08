#!/usr/bin/env bash
set -euo pipefail

# ── Config ─────────────────────────────────────────────────────────────────────
SERVER_HOST="${DEPLOY_HOST:?set DEPLOY_HOST, e.g. DEPLOY_HOST=10.0.0.5 ./deploy.sh}"
SERVER_USER="${DEPLOY_USER:-YOUR_SSH_USER}"   # override: DEPLOY_USER=eric ./deploy.sh
REMOTE_DIR="/opt/hexon"                       # directory on the server
REPO_URL="https://github.com/Erkko68/HexonKMP.git"  # change to your repo URL
BRANCH="main"

# ── Deploy ─────────────────────────────────────────────────────────────────────
echo "→ Deploying to ${SERVER_USER}@${SERVER_HOST}:${REMOTE_DIR}"

ssh "${SERVER_USER}@${SERVER_HOST}" bash <<EOF
set -euo pipefail

# Clone the repo on first deploy, pull on subsequent ones.
if [ -d "${REMOTE_DIR}/.git" ]; then
  echo "Pulling latest changes…"
  git -C "${REMOTE_DIR}" fetch origin "${BRANCH}"
  git -C "${REMOTE_DIR}" reset --hard "origin/${BRANCH}"
else
  echo "Cloning repo…"
  git clone --branch "${BRANCH}" "${REPO_URL}" "${REMOTE_DIR}"
fi

cd "${REMOTE_DIR}"

echo "Building and restarting containers…"
docker compose up -d --build --remove-orphans

echo "Done. Running containers:"
docker compose ps
EOF

echo "✓ Deploy complete."
