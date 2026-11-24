#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
window_widths=(96 112 128 144 160 176 192 208 224 240 256)
horizons=(12 24 36 48 60 72 84 96 108 120 132)

for window_width in "${window_widths[@]}"; do
  for horizon in "${horizons[@]}"; do
    qsub -v "WINDOW_WIDTH=${window_width},HORIZON_FRAMES=${horizon}" \
      "$SCRIPT_DIR/script-astar-task52.sh"
  done
done
