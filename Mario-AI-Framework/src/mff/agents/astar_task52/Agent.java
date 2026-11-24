package mff.agents.astar_task52;

import mff.agents.astarHelper.MarioAction;
import mff.agents.benchmark.IAgentBenchmark;
import mff.agents.benchmark.IAgentBenchmarkBacktrack;
import mff.agents.common.IMarioAgentMFF;
import mff.agents.common.MarioTimerSlim;
import mff.forwardmodel.slim.core.MarioForwardModelSlim;

import java.util.ArrayList;

public class Agent implements IMarioAgentMFF, IAgentBenchmark, IAgentBenchmarkBacktrack {

    private static final int SEARCH_STEPS = 3;
    private static final int DEFAULT_WINDOW_WIDTH_PIXELS = 176;

    private final int windowWidthPixels;
    private final int horizonFrames;
    private ArrayList<boolean[]> actionsList = new ArrayList<>();
    private float furthestDistance = -1;
    private boolean finished = false;
    private int totalSearchCalls = 0;
    private int totalNodesEvaluated = 0;
    private int mostBacktrackedNodes = 0;

    public Agent() {
        this(DEFAULT_WINDOW_WIDTH_PIXELS, Integer.MAX_VALUE);
    }

    public Agent(int windowWidthPixels, int horizonFrames) {
        if (windowWidthPixels < 1) {
            throw new IllegalArgumentException("windowWidthPixels must be positive.");
        }
        if (horizonFrames != Integer.MAX_VALUE
                && (horizonFrames < 1 || horizonFrames % SEARCH_STEPS != 0)) {
            throw new IllegalArgumentException(
                    "horizonFrames must be positive and divisible by searchSteps.");
        }

        this.windowWidthPixels = windowWidthPixels;
        this.horizonFrames = horizonFrames;
    }

    @Override
    public void initialize(MarioForwardModelSlim model) {
        AStarTree.winFound = false;
        AStarTree.exitTileX = model.getWorld().level.exitTileX * 16;
    }

    @Override
    public boolean[] getActions(MarioForwardModelSlim model, MarioTimerSlim timer) {
        if (finished) {
            if (actionsList.size() == 0)
                return MarioAction.NO_ACTION.value;
            else
                return actionsList.remove(actionsList.size() - 1);
        }

        AStarTree tree = new AStarTree(model, SEARCH_STEPS, windowWidthPixels, horizonFrames);
        ArrayList<boolean[]> newActionsList = tree.search(timer);
        totalSearchCalls++;
        this.totalNodesEvaluated += tree.nodesEvaluated;
        mostBacktrackedNodes = Math.max(mostBacktrackedNodes, tree.getMostBacktrackedNodes());

        if (AStarTree.winFound) {
            actionsList = newActionsList;
            finished = true;
            return actionsList.remove(actionsList.size() - 1);
        }

        // we need planning to furthest state to stick with the plan for some time, else the agent sometimes spins in place
        if (tree.furthestNodeDistance > furthestDistance) {
            furthestDistance = tree.furthestNodeDistance;
            actionsList = newActionsList;
        }

        if (actionsList.size() == 0) // agent failed
            return MarioAction.NO_ACTION.value;

        return actionsList.remove(actionsList.size() - 1);
    }

    @Override
    public int getSearchCalls() {
        return totalSearchCalls;
    }

    @Override
    public int getNodesEvaluated() {
        return totalNodesEvaluated;
    }

    @Override
    public int getMostBacktrackedNodes() {
        return mostBacktrackedNodes;
    }

    @Override
    public String getAgentName() {
        return "MFF AStar Agent";
    }
}
