#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
steps_values=(1 2 3 4 5 6 7 8 9 10 20)
weight_values=(0.2 0.4 0.6 0.8 1.0 1.2 1.4 1.6 1.8 2.0)

for steps in "${steps_values[@]}"; do
  for weight in "${weight_values[@]}"; do
    qsub -v "SEARCH_STEPS=${steps},TTFW=${weight}" \
      "$SCRIPT_DIR/script-astar-task51.sh"
  done
done