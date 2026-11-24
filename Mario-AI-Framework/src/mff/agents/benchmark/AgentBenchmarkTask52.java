package mff.agents.benchmark;

import engine.core.MarioLevelModel;
import engine.core.MarioTimer;
import mff.agents.astar_task52.Agent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static mff.LevelLoader.getLevel;

public class AgentBenchmarkTask52 {

    private static final int SEARCH_STEPS = 3;
    private static final int ORIGINAL_LEVEL_TIMER_SECONDS = 200;
    private static final int GENERATED_LEVEL_TIMER_SECONDS = 30;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: AgentBenchmarkTask52 <windowWidthPixels> <horizonFrames>");
        }

        int windowWidthPixels = Integer.parseInt(args[0]);
        int horizonFrames = Integer.parseInt(args[1]);
        if (windowWidthPixels < 1) {
            throw new IllegalArgumentException("windowWidthPixels must be positive.");
        }
        if (horizonFrames < 1 || horizonFrames % SEARCH_STEPS != 0) {
            throw new IllegalArgumentException(
                    "horizonFrames must be positive and divisible by searchSteps.");
        }

        Path outputDirectory = Path.of("agent-benchmark");
        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve(
                "astarWindow-window-" + windowWidthPixels
                        + "-horizon-" + horizonFrames + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("windowWidthPixels,horizonFrames,level,win/fail,% travelled,"
                    + "run time,game ticks,planning time,total plannings,"
                    + "nodes evaluated,most backtracked nodes\n");
            writer.flush();

            warmup(windowWidthPixels, horizonFrames);

            for (int i = 1; i <= 15; i++) {
                String levelName = "original-" + i;
                String level = getLevel("./levels/original/lvl-" + i + ".txt");
                runLevel(writer, windowWidthPixels, horizonFrames,
                        levelName, level, ORIGINAL_LEVEL_TIMER_SECONDS);
            }

            for (int i = 1; i <= 100; i++) {
                String levelName = "krys-" + i;
                String level = getLevel("./levels/krys/lvl-" + i + ".txt");
                runLevel(writer, windowWidthPixels, horizonFrames,
                        levelName, level, GENERATED_LEVEL_TIMER_SECONDS);
            }

            for (int i = 1; i <= 100; i++) {
                String levelName = "patternCount-" + i;
                String level = getLevel("./levels/patternCount/lvl-" + i + ".txt");
                runLevel(writer, windowWidthPixels, horizonFrames,
                        levelName, level, GENERATED_LEVEL_TIMER_SECONDS);
            }
        }

        System.out.println("Wrote " + outputFile);
    }

    private static void warmup(int windowWidthPixels, int horizonFrames) {
        String level = getLevel("./levels/original/lvl-1.txt");
        new AgentBenchmarkGame().runGame(
                new Agent(windowWidthPixels, horizonFrames),
                level,
                ORIGINAL_LEVEL_TIMER_SECONDS,
                0,
                false);
    }

    private static void runLevel(
            BufferedWriter writer,
            int windowWidthPixels,
            int horizonFrames,
            String levelName,
            String level,
            int gameTimerSeconds) throws IOException {
        System.out.println("astarWindow-" + levelName);

        Agent agent = new Agent(windowWidthPixels, horizonFrames);
        AgentStats stats = new AgentBenchmarkGame().runGame(
                agent, level, gameTimerSeconds, 0, false);
        stats.level = levelName;

        writer.write(windowWidthPixels + ","
                + horizonFrames + ","
                + stats.level + ","
                + stats.win + ","
                + Float.toString(stats.percentageTravelled) + ","
                + stats.runTime + ","
                + stats.totalGameTicks + ","
                + stats.totalPlanningTime + ","
                + stats.searchCalls + ","
                + stats.nodesEvaluated + ","
                + agent.getMostBacktrackedNodes() + "\n");
        writer.flush();
    }
}
