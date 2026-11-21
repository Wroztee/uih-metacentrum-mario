#!/usr/bin/env python3
"""Aggregate task 5.1 result CSVs and save a parameter heatmap."""

import argparse
import csv
import math
from collections import defaultdict
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np


METRICS = {
    "win_rate": ("win/fail", "Win rate (%)", 100.0),
    "percentage_travelled": ("% travelled", "Mean distance travelled (%)", 100.0),
    "run_time": ("run time", "Mean run time (ms)", 1.0),
    "game_ticks": ("game ticks", "Mean game ticks", 1.0),
    "planning_time": ("planning time", "Mean planning time (ms)", 1.0),
    "search_calls": ("total plannings", "Mean search calls", 1.0),
    "nodes_evaluated": ("nodes evaluated", "Mean nodes evaluated", 1.0),
    "most_backtracked_nodes": ("most backtracked nodes", "Mean most-backtracked nodes", 1.0),
}

REQUIRED_COLUMNS = {
    "searchSteps",
    "timeToFinishWeight",
    "win/fail",
    "% travelled",
    "run time",
    "game ticks",
    "planning time",
    "total plannings",
    "nodes evaluated",
    "most backtracked nodes",
}


def parse_metric_value(raw_value, metric, csv_path, line_number):
    if metric == "win_rate":
        value = raw_value.strip().lower()
        if value not in {"true", "false"}:
            raise ValueError(
                f"{csv_path}:{line_number}: expected true/false for win/fail, got {raw_value!r}"
            )
        return 1.0 if value == "true" else 0.0

    try:
        value = float(raw_value)
    except (TypeError, ValueError) as error:
        raise ValueError(
            f"{csv_path}:{line_number}: invalid value for {METRICS[metric][0]!r}: {raw_value!r}"
        ) from error

    if not math.isfinite(value):
        raise ValueError(
            f"{csv_path}:{line_number}: non-finite value for {METRICS[metric][0]!r}"
        )
    return value


def read_aggregates(folder, metric):
    csv_files = sorted(folder.glob("*.csv"))
    if not csv_files:
        raise ValueError(f"No CSV files found directly inside {folder}")

    totals = defaultdict(float)
    counts = defaultdict(int)

    for csv_path in csv_files:
        with csv_path.open("r", newline="", encoding="utf-8-sig") as file:
            reader = csv.DictReader(file)
            missing = REQUIRED_COLUMNS.difference(reader.fieldnames or [])
            if missing:
                missing_columns = ", ".join(sorted(missing))
                raise ValueError(f"{csv_path}: missing required columns: {missing_columns}")

            for line_number, row in enumerate(reader, start=2):
                if not row or not any(value and value.strip() for value in row.values()):
                    continue
                try:
                    search_steps = int(row["searchSteps"])
                    weight = float(row["timeToFinishWeight"])
                except (TypeError, ValueError) as error:
                    raise ValueError(
                        f"{csv_path}:{line_number}: invalid parameter values"
                    ) from error

                if search_steps < 1 or not math.isfinite(weight):
                    raise ValueError(
                        f"{csv_path}:{line_number}: invalid parameter values"
                    )

                key = (search_steps, weight)
                totals[key] += parse_metric_value(
                    row[METRICS[metric][0]], metric, csv_path, line_number
                )
                counts[key] += 1

    if not totals:
        raise ValueError(f"No result rows found in CSV files under {folder}")

    means = {
        key: (total / counts[key]) * METRICS[metric][2]
        for key, total in totals.items()
    }
    return means, counts


def save_heatmap(means, counts, metric, output_path):
    search_steps = sorted({key[0] for key in means})
    weights = sorted({key[1] for key in means})

    values = np.full((len(weights), len(search_steps)), np.nan)
    for row_index, weight in enumerate(weights):
        for column_index, steps in enumerate(search_steps):
            key = (steps, weight)
            if key in means:
                values[row_index, column_index] = means[key]

    masked_values = np.ma.masked_invalid(values)
    color_map = plt.get_cmap("viridis").copy()
    color_map.set_bad("#d9d9d9")

    figure_width = max(9.0, 0.85 * len(search_steps) + 3.0)
    figure_height = max(6.0, 0.65 * len(weights) + 2.0)
    figure, axis = plt.subplots(figsize=(figure_width, figure_height))

    image = axis.imshow(masked_values, cmap=color_map, origin="lower", aspect="auto")
    colorbar = figure.colorbar(image, ax=axis)
    colorbar.set_label(METRICS[metric][1])

    axis.set_xticks(range(len(search_steps)), labels=[str(value) for value in search_steps])
    axis.set_yticks(range(len(weights)), labels=[f"{value:g}" for value in weights])
    axis.set_xlabel("searchSteps")
    axis.set_ylabel("timeToFinish weight")
    axis.set_title(f"{METRICS[metric][1]} by task 5.1 parameters")

    for row_index, weight in enumerate(weights):
        for column_index, steps in enumerate(search_steps):
            key = (steps, weight)
            if key in means:
                axis.text(
                    column_index,
                    row_index,
                    f"{means[key]:.1f}",
                    ha="center",
                    va="center",
                    color="white",
                    fontsize=8,
                )

    figure.tight_layout()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(output_path, dpi=200, bbox_inches="tight")
    plt.close(figure)

    row_count = sum(counts.values())
    print(
        f"Saved {output_path} ({len(means)} parameter combinations, "
        f"{row_count} result rows)."
    )


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Aggregate task 5.1 CSV results by searchSteps and timeToFinishWeight, "
            "then save a matplotlib heatmap."
        )
    )
    parser.add_argument("folder", type=Path, help="Folder containing result CSV files")
    parser.add_argument("metric", choices=METRICS, help="Metric to average across result rows")
    parser.add_argument("output", type=Path, help="Output image path, e.g. heatmaps/win-rate.png")
    args = parser.parse_args()

    if not args.folder.is_dir():
        parser.error(f"not a directory: {args.folder}")

    try:
        means, counts = read_aggregates(args.folder, args.metric)
        save_heatmap(means, counts, args.metric, args.output)
    except (OSError, ValueError, csv.Error) as error:
        parser.error(str(error))


if __name__ == "__main__":
    main()
