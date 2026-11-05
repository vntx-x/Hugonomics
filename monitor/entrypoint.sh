#!/usr/bin/env bash
set -euo pipefail

MAIN=/home/mc/data/main
WORK=/home/mc/data/work
mkdir -p "$MAIN" "$WORK/mods" "$WORK/config/hugomarket"

if [ ! -f "$MAIN/portablemc_auth.json" ]; then
  echo "missing $MAIN/portablemc_auth.json (run 'portablemc login' on a desktop and copy the file)" >&2
  exit 1
fi
if [ -z "$MC_LOGIN" ]; then
  echo "MC_LOGIN is empty" >&2
  exit 1
fi

cp "$MAIN/portablemc_auth.json" "$WORK/portablemc_auth.json"
cp -n /home/mc/options.txt "$WORK/options.txt" || true
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99
cp /home/mc/mods/*.jar "$WORK/mods/" 2>/dev/null || true
if [ -f /home/mc/config.json ]; then
  cp /home/mc/config.json "$WORK/config/hugomarket/config.json"
fi

export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe
export LP_NUM_THREADS=2
Xvfb :99 -screen 0 640x360x24 -nolisten tcp &
sleep 2

exec portablemc --main-dir "$MAIN" --work-dir "$WORK" start "fabric:$MC_VERSION" \
  --login "$MC_LOGIN" --server "$MC_SERVER" --resolution 640x360 \
  --jvm-args="$JAVA_OPTS"
