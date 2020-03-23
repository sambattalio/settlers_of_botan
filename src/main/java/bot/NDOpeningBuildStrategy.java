package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCBoard;
import soc.game.SOCSettlement;
import soc.game.SOCResourceSet;
import soc.robot.OpeningBuildStrategy;
import soc.robot.SOCRobotBrain;
import bot.NDHelpers;
import bot.NDRobotDM;
import soc.debug.D;

import java.util.Vector;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class NDOpeningBuildStrategy extends OpeningBuildStrategy {

    protected SOCResourceSet firstResources;

    private final int MIN_LENGTH_FOR_LONGEST = 5;

    private final static int[] NODE_2_AWAY = { -9, 0x02, 0x22, 0x20, -0x02, -0x22, -0x20 };
    private final static int[] NODE_3_AWAY = { -9, 0x03, 0x33, 0x30, -0x03, -0x33, -0x30 };
    private final static int[] NODE_4_AWAY = { -9, 0x04, 0x44, 0x40, -0x04, -0x44, -0x40 };

    private int roadToBuild;

    /**
     * Create an OpeningBuildStrategy for a {@link NDRobotBrain}'s player.
     *
     * @param ga Our game
     * @param pl Our player data in <tt>ga</tt>
     */
    public NDOpeningBuildStrategy(SOCGame ga, SOCPlayer pl) {
        super(ga, pl);
    }

    /**
     * Pick the best possible node for opener
     *
     * @return coord of best possible opener node based on current board state.
     *
     */
    private int getBestNode() {
        int bestNode = -1;
        int bestNodeProb = -1;

        for (int node : this.ourPlayerData.getPotentialSettlements()) {
            int probability = NDRobotDM.totalProbabilityAtNode(this.game, node);

            if (probability > bestNodeProb) {
                bestNodeProb = probability;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private List<Pair<Integer, Integer>> getTopNNodes(int n) {
        int bestNode = -1;
        int bestNodeProb = -1;
        D.ebugPrintln("start topnnodes");

        ArrayList<Pair<Integer, Integer>> nodeStats = new ArrayList<Pair<Integer, Integer>>();

        for (int node : this.ourPlayerData.getPotentialSettlements()) {
            int probability = NDRobotDM.totalProbabilityAtNode(this.game, node);
            nodeStats.add(new Pair<Integer, Integer>(node, probability));
        }

        // sort and yeet out top N
        Collections.sort(nodeStats, Comparator.comparing(p -> -p.getValue()));

        D.ebugPrintln("end topnnodes");
        return nodeStats.subList(0, n); 
    }

    public int planInitialSettlements() {
        firstSettlement = 35;  

        // the plan for the first one... find the BEST node possible
        firstSettlement = getBestNode();

        firstResources = NDHelpers.findResourcesFromCoord(this.game, firstSettlement);


        return firstSettlement;
    }
    /*
    public int planInitRoad() {
        int settlementNode = ourPlayerData.getLastSettlementCoord();
        int nextNode;

        Vector<SOCSettlement> settlements = ourPlayerData.getSettlements();

        if (ourPlayerData.getSettlements().size() > 1) {
            // set destination node to not the one just placed
            nextNode = settlements.get(0).getCoordinates() == settlementNode ? settlements.get(1).getCoordinates() : settlements.get(0).getCoordinates();
        } else {
            nextNode = bestSecondSettlement();
        }

        // develop path to hypothetical next node... and place road on first option 
        plannedRoadDestinationNode = nextNode;

        // todo do facing / coordinate bitmapping to do this instead of BFS
        //Vector<Integer> path = NDHelpers.BFSToCoord(this.game, settlementNode, plannedRoadDestinationNode);
        //return path.get(1);
        return game.getBoard().getAdjacentEdgesToNode(settlementNode).get(0);
    }
    */

    public int planInitRoad() {
        int settlementNode = ourPlayerData.getLastSettlementCoord();
        return game.getBoard().getAdjacentEdgesToNode(settlementNode).get(0);
    }

    public boolean isNode234AwayFromNode(final int n1, final int n2)
    {
        final int d = n2 - n1;
        for (int facing = 1; facing <= 6; ++facing)
        {
            if (d == NODE_2_AWAY[facing] || d == NODE_3_AWAY[facing] || d == NODE_4_AWAY[facing])
                return true;
        }
        return false;
    }

    public int bestSecondSettlement() {
        // look at 5 "best" nodes left over, and compare resources to determine where to go
        // try to maximize road building first, but if can't maximize development cards, ... expansion
        List<Pair<Integer, Integer>> nodes = getTopNNodes(3);


        Vector<Pair<Integer, Integer>> longRoads = new Vector<Pair<Integer, Integer>>();
        Vector<Pair<Integer, Integer>> develCoords = new Vector<Pair<Integer, Integer>>();
        Vector<Pair<Integer, Integer>> leftoverAdjusted = new Vector<Pair<Integer, Integer>>();
        // find distance to top 5, keeping those at or above minimium distance
        for (Pair<Integer, Integer> node : nodes) {
            SOCResourceSet resourcesAtCoord = NDHelpers.findResourcesFromCoord(this.game, node.getKey());

            // add in first node's resources
            resourcesAtCoord.add(firstResources);
            D.ebugPrintln("potential resources: " + resourcesAtCoord.toString());
            Vector<Integer> shortestPath = NDHelpers.BFSToCoord(game, firstSettlement, node.getKey());
            if (shortestPath.size() >= MIN_LENGTH_FOR_LONGEST && resourcesAtCoord.contains(NDHelpers.ROAD_SET)) {
                longRoads.add(new Pair<Integer, Integer>(node.getKey(), shortestPath.get(1)));
            }
            if (resourcesAtCoord.contains(NDHelpers.DEVEL_SET)) {
                develCoords.add(new Pair<Integer, Integer>(node.getKey(), shortestPath.get(1)));
            }
            leftoverAdjusted.add(new Pair<Integer, Integer>(node.getKey(), shortestPath.get(1)));
        }

        // Pick either the best (first b/c sorted on probability) or search for development card locations
        if (longRoads.size() > 0) {
            D.ebugPrintln("Placing second for longest road");
            secondSettlement = longRoads.get(0).getKey();
            roadToBuild = longRoads.get(0).getValue();
        } else if (develCoords.size() > 0) {
            D.ebugPrintln("Placing second for development cards");
            secondSettlement = develCoords.get(0).getKey();
            roadToBuild = develCoords.get(0).getValue();
        } else {
            D.ebugPrintln("Placing second for best resources");
            secondSettlement = leftoverAdjusted.get(0).getKey();
            roadToBuild = leftoverAdjusted.get(0).getValue();
        }
        return secondSettlement;
    }

    public int planSecondSettlement() { 
        return bestSecondSettlement();
    }

}

