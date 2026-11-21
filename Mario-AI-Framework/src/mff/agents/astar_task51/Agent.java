package mff.agents.astar_task51;

import mff.agents.astarHelper.MarioAction;
import mff.agents.benchmark.IAgentBenchmark;
import mff.agents.benchmark.IAgentBenchmarkBacktrack;
import mff.agents.common.IMarioAgentMFF;
import mff.agents.common.MarioTimerSlim;
import mff.forwardmodel.slim.core.MarioForwardModelSlim;

import java.util.ArrayList;

public class Agent implements IMarioAgentMFF, IAgentBenchmark, IAgentBenchmarkBacktrack {

    private final int searchSteps;
    private final float timeToFinishWeight;

    private ArrayList<boolean[]> actionsList = new ArrayList<>();
    private float furthestDistance = -1;
    private boolean finished = false;
    private int totalSearchCalls = 0;
    private int totalNodesEvaluated = 0;
    private int mostBacktrackedNodes = 0;

    public Agent() {
        this(3, 1.1f);
    }

    public Agent(int searchSteps, float timeToFinishWeight) {
        if (searchSteps < 1) {
            throw new IllegalArgumentException("searchSteps must be positive.");
        }
        if (!Float.isFinite(timeToFinishWeight) || timeToFinishWeight < 0) {
            throw new IllegalArgumentException("timeToFinishWeight must be finite and non-negative.");
        }

        this.searchSteps = searchSteps;
        this.timeToFinishWeight = timeToFinishWeight;
    }

    @Override
    public void initialize(MarioForwardModelSlim model) {
        AStarTree.winFound = false;
        AStarTree.exitTileX = model.getWorld().level.exitTileX * 16;
    }

    @Override
    public boolean[] getActions(MarioForwardModelSlim model, MarioTimerSlim timer) {
        if (finished) {
            if (actionsList.isEmpty()) {
                return MarioAction.NO_ACTION.value;
            }
            return actionsList.remove(actionsList.size() - 1);
        }

        AStarTree tree = new AStarTree(model, searchSteps, timeToFinishWeight);
        ArrayList<boolean[]> newActionsList = tree.search(timer);

        totalSearchCalls++;
        totalNodesEvaluated += tree.nodesEvaluated;
        mostBacktrackedNodes = Math.max(mostBacktrackedNodes, tree.mostBacktrackedNodes);

        if (AStarTree.winFound) {
            actionsList = newActionsList;
            finished = true;
            return actionsList.remove(actionsList.size() - 1);
        }

        if (tree.furthestNodeDistance > furthestDistance) {
            furthestDistance = tree.furthestNodeDistance;
            actionsList = newActionsList;
        }

        if (actionsList.isEmpty()) {
            actionsList = newActionsList;
        }

        if (actionsList.isEmpty()) {
            return MarioAction.NO_ACTION.value;
        }

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