#!/bin/bash
#PBS -l walltime=10:00:00
#PBS -l select=1:ncpus=1:mem=4gb:scratch_local=1gb:cluster=elmo4
#PBS -e /storage/praha1/home/jirakad1/AI_for_games/job_logs
#PBS -o /storage/praha1/home/jirakad1/AI_for_games/job_logs

set -euo pipefail

DATADIR=/storage/praha1/home/jirakad1/AI_for_games/super-mario-astar
RESULTDIR=/storage/praha1/home/jirakad1/AI_for_games/results/astar-task51

module add openjdk-17
: "${SCRATCHDIR:?SCRATCHDIR is not set}"
: "${SEARCH_STEPS:?SEARCH_STEPS was not passed to the job}"
: "${TTFW:?TTFW was not passed to the job}"

trap clean_scratch EXIT

cp -R "$DATADIR/Mario-AI-Framework" "$SCRATCHDIR/"
cd "$SCRATCHDIR/Mario-AI-Framework"

javac -cp src src/mff/agents/benchmark/AgentBenchmarkTask51.java
java -cp src mff.agents.benchmark.AgentBenchmarkTask51 \
  "$SEARCH_STEPS" "$TTFW"

result="agent-benchmark/astar-searchSteps-${SEARCH_STEPS}-timeToFinish-${TTFW}.csv"
mkdir -p "$RESULTDIR"
cp "$result" "$RESULTDIR/"