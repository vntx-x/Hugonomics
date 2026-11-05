#!/usr/bin/env bash
set -euo pipefail

ROOT="$HOME/hugomarket"
APP="$ROOT/app"
ENV_FILE="$ROOT/.env"
API_BIND="${API_BIND:-127.0.0.1}"
API_PORT="${API_PORT:-8080}"

mkdir -p "$APP"
tar -xzf "$ROOT/backend.tar.gz" -C "$APP"
rm -f "$ROOT/backend.tar.gz"

if [ ! -f "$ENV_FILE" ]; then
  cat > "$ENV_FILE" <<EOF
POSTGRES_DB=hugomarket
POSTGRES_USER=hugomarket
POSTGRES_PASSWORD=$(openssl rand -hex 24)
PEPPER=$(openssl rand -hex 32)
MIN_CLIENT_VERSION=0.0.1
API_BIND=$API_BIND
API_PORT=$API_PORT
EOF
  chmod 600 "$ENV_FILE"
  echo "created $ENV_FILE"
fi

cd "$APP"
COMPOSE=(docker compose --project-name hugomarket --env-file "$ENV_FILE" -f docker-compose.yml)
if [ -n "${API_HOST:-}" ]; then
  COMPOSE+=(-f docker-compose.traefik.yml)
fi
"${COMPOSE[@]}" up -d --build --remove-orphans

for _ in $(seq 1 30); do
  if "${COMPOSE[@]}" exec -T api node -e "fetch('http://127.0.0.1:8080/healthz').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

"${COMPOSE[@]}" exec -T api node dist/db/migrate.js
"${COMPOSE[@]}" ps
echo "--- healthz"
curl -fsS "http://$API_BIND:$API_PORT/healthz"
echo
echo "--- meta"
curl -fsS "http://$API_BIND:$API_PORT/v1/meta" | sed 's/"pepper":"[^"]*"/"pepper":"***"/'
echo
