package mff.agents.benchmark;

import engine.core.MarioLevelModel;
import engine.core.MarioTimer;
import mff.agents.astar_task51.Agent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static mff.LevelLoader.getLevel;

public class AgentBenchmarkTask51 {

    private static final int ORIGINAL_LEVEL_TIMER_SECONDS = 200;
    private static final int GENERATED_LEVEL_TIMER_SECONDS = 30;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: AgentBenchmarkTask51 <searchSteps> <timeToFinishWeight>");
        }

        int searchSteps = Integer.parseInt(args[0]);
        float timeToFinishWeight = Float.parseFloat(args[1]);

        if (searchSteps < 1) {
            throw new IllegalArgumentException("searchSteps must be positive.");
        }
        if (!Float.isFinite(timeToFinishWeight) || timeToFinishWeight < 0) {
            throw new IllegalArgumentException("timeToFinishWeight must be finite and non-negative.");
        }

        Path outputDirectory = Path.of("agent-benchmark");
        Files.createDirectories(outputDirectory);

        String weightText = formatWeight(timeToFinishWeight);
        Path outputFile = outputDirectory.resolve(
                "astar-searchSteps-" + searchSteps
                        + "-timeToFinish-" + weightText + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("searchSteps,timeToFinishWeight,level,win/fail,% travelled,"
                    + "run time,game ticks,planning time,total plannings,"
                    + "nodes evaluated,most backtracked nodes\n");

            warmup(searchSteps, timeToFinishWeight);
            testOriginalLevels(writer, searchSteps, timeToFinishWeight);
            testKrysLevels(writer, searchSteps, timeToFinishWeight);
            testPatternCountLevels(writer, searchSteps, timeToFinishWeight);
        }

        System.out.println("Wrote " + outputFile);
    }

    private static void warmup(int searchSteps, float timeToFinishWeight) {
        String level = getLevel("./levels/original/lvl-1.txt");
        new AgentBenchmarkGame().runGame(
                new Agent(searchSteps, timeToFinishWeight),
                level,
                ORIGINAL_LEVEL_TIMER_SECONDS,
                0,
                false);
    }

    private static void testOriginalLevels(
            BufferedWriter writer,
            int searchSteps,
            float timeToFinishWeight) throws IOException {
        for (int i = 1; i <= 15; i++) {
            String level = getLevel("./levels/original/lvl-" + i + ".txt");
            runLevel(writer, searchSteps, timeToFinishWeight,
                    "original-" + i, level, ORIGINAL_LEVEL_TIMER_SECONDS);
        }
    }

    private static void testKrysLevels(
            BufferedWriter writer,
            int searchSteps,
            float timeToFinishWeight) throws IOException {
        for (int i = 1; i <= 15; i++) {
            String level = new levelGenerators.krys.LevelGenerator(i)
                    .getGeneratedLevel(
                            new MarioLevelModel(150, 16),
                            new MarioTimer(5L * 60 * 60 * 1000));

            runLevel(writer, searchSteps, timeToFinishWeight,
                    "krys-" + i, level, GENERATED_LEVEL_TIMER_SECONDS);
        }
    }

    private static void testPatternCountLevels(
            BufferedWriter writer,
            int searchSteps,
            float timeToFinishWeight) throws IOException {
        for (int i = 1; i <= 15; i++) {
            String level = getLevel("./levels/patternCount/lvl-" + i + ".txt");
            runLevel(writer, searchSteps, timeToFinishWeight,
                    "patternCount-" + i, level, GENERATED_LEVEL_TIMER_SECONDS);
        }
    }

    private static void runLevel(
            BufferedWriter writer,
            int searchSteps,
            float timeToFinishWeight,
            String levelName,
            String level,
            int gameTimerSeconds) throws IOException {
        System.out.println("astar-" + levelName);

        Agent agent = new Agent(searchSteps, timeToFinishWeight);
        AgentStats stats = new AgentBenchmarkGame().runGame(
                agent, level, gameTimerSeconds, 0, false);
        stats.level = levelName;

        writer.write(searchSteps + ","
                + formatWeight(timeToFinishWeight) + ","
                + stats.level + ","
                + stats.win + ","
                + Float.toString(stats.percentageTravelled) + ","
                + stats.runTime + ","
                + stats.totalGameTicks + ","
                + stats.totalPlanningTime + ","
                + stats.searchCalls + ","
                + stats.nodesEvaluated + ","
                + agent.getMostBacktrackedNodes() + "\n");
    }

    private static String formatWeight(float weight) {
        return String.format(Locale.ROOT, "%.1f", weight);
    }
}