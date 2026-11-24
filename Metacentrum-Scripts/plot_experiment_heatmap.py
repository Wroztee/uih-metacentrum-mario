#!/usr/bin/env python3
"""Aggregate task 5.1 or 5.2 result CSVs and save a parameter heatmap."""

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

PARAMETER_COLUMNS = {
    ("searchSteps", "timeToFinishWeight"): (
        "searchSteps",
        "timeToFinish weight",
        "task 5.1",
    ),
    ("windowWidthPixels", "horizonFrames"): (
        "Window width (pixels)",
        "Horizon (simulated frames)",
        "task 5.2",
    ),
}

REQUIRED_METRIC_COLUMNS = {
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


def detect_parameters(fieldnames, csv_path):
    columns = set(fieldnames or [])
    for parameter_columns, labels in PARAMETER_COLUMNS.items():
        if set(parameter_columns).issubset(columns):
            return parameter_columns, labels

    supported = " or ".join(
        " + ".join(parameter_columns) for parameter_columns in PARAMETER_COLUMNS
    )
    raise ValueError(
        f"{csv_path}: could not identify parameter columns; expected {supported}"
    )


def read_aggregates(folder, metric):
    csv_files = sorted(folder.glob("*.csv"))
    if not csv_files:
        raise ValueError(f"No CSV files found directly inside {folder}")

    totals = defaultdict(float)
    counts = defaultdict(int)
    parameter_columns = None
    parameter_labels = None

    for csv_path in csv_files:
        with csv_path.open("r", newline="", encoding="utf-8-sig") as file:
            reader = csv.DictReader(file)
            file_parameter_columns, file_parameter_labels = detect_parameters(
                reader.fieldnames, csv_path
            )
            if parameter_columns is None:
                parameter_columns = file_parameter_columns
                parameter_labels = file_parameter_labels
            elif file_parameter_columns != parameter_columns:
                raise ValueError(
                    f"{csv_path}: parameter columns do not match the other CSV files"
                )

            required_columns = set(parameter_columns) | REQUIRED_METRIC_COLUMNS
            missing = required_columns.difference(reader.fieldnames or [])
            if missing:
                missing_columns = ", ".join(sorted(missing))
                raise ValueError(f"{csv_path}: missing required columns: {missing_columns}")

            for line_number, row in enumerate(reader, start=2):
                if not row or not any(value and value.strip() for value in row.values()):
                    continue
                try:
                    parameter_x = float(row[parameter_columns[0]])
                    parameter_y = float(row[parameter_columns[1]])
                except (TypeError, ValueError) as error:
                    raise ValueError(
                        f"{csv_path}:{line_number}: invalid parameter values"
                    ) from error

                if (
                    not math.isfinite(parameter_x)
                    or not math.isfinite(parameter_y)
                    or parameter_x <= 0
                    or parameter_y <= 0
                ):
                    raise ValueError(
                        f"{csv_path}:{line_number}: invalid parameter values"
                    )

                key = (parameter_x, parameter_y)
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
    return means, counts, parameter_columns, parameter_labels


def format_parameter(value):
    return str(int(value)) if value.is_integer() else f"{value:g}"


def save_heatmap(
    means,
    counts,
    metric,
    output_path,
    parameter_labels,
):
    parameter_x_values = sorted({key[0] for key in means})
    parameter_y_values = sorted({key[1] for key in means})

    values = np.full((len(parameter_y_values), len(parameter_x_values)), np.nan)
    for row_index, parameter_y in enumerate(parameter_y_values):
        for column_index, parameter_x in enumerate(parameter_x_values):
            key = (parameter_x, parameter_y)
            if key in means:
                values[row_index, column_index] = means[key]

    figure_width = max(9.0, 0.85 * len(parameter_x_values) + 3.0)
    figure_height = max(6.0, 0.65 * len(parameter_y_values) + 2.0)
    figure, axis = plt.subplots(figsize=(figure_width, figure_height))

    image = axis.imshow(values, cmap="viridis", origin="lower", aspect="auto")
    colorbar = figure.colorbar(image, ax=axis)
    colorbar.set_label(METRICS[metric][1])

    axis.set_xticks(
        range(len(parameter_x_values)),
        labels=[format_parameter(value) for value in parameter_x_values],
    )
    axis.set_yticks(
        range(len(parameter_y_values)),
        labels=[format_parameter(value) for value in parameter_y_values],
    )
    axis.set_xlabel(parameter_labels[0])
    axis.set_ylabel(parameter_labels[1])
    axis.set_title(f"{METRICS[metric][1]} by {parameter_labels[2]} parameters")

    for row_index, parameter_y in enumerate(parameter_y_values):
        for column_index, parameter_x in enumerate(parameter_x_values):
            key = (parameter_x, parameter_y)
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
            "Aggregate task 5.1 or 5.2 CSV results by their two parameters, "
            "then save a matplotlib heatmap."
        )
    )
    parser.add_argument("folder", type=Path, help="Folder containing one experiment's CSV files")
    parser.add_argument("metric", choices=METRICS, help="Metric to average across result rows")
    parser.add_argument("output", type=Path, help="Output image path, e.g. heatmaps/win-rate.png")
    args = parser.parse_args()

    if not args.folder.is_dir():
        parser.error(f"not a directory: {args.folder}")

    try:
        means, counts, _, parameter_labels = read_aggregates(args.folder, args.metric)
        save_heatmap(means, counts, args.metric, args.output, parameter_labels)
    except (OSError, ValueError, csv.Error) as error:
        parser.error(str(error))


if __name__ == "__main__":
    main()
