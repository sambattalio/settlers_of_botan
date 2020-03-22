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

public class NDOpeningBuildStrategy extends OpeningBuildStrategy {

    protected SOCResourceSet firstResources;

    private final int MIN_LENGTH_FOR_LONGEST = 5;

    private final static int[] NODE_2_AWAY = { -9, 0x02, 0x22, 0x20, -0x02, -0x22, -0x20 };

    private final static int[] NODE_3_AWAY = { -9, 0x03, 0x33, 0x30, -0x03, -0x33, -0x30 };

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

        ArrayList<Pair<Integer, Integer>> nodeStats = new ArrayList<Pair<Integer, Integer>>();

        for (int node : this.ourPlayerData.getPotentialSettlements()) {
            int probability = NDRobotDM.totalProbabilityAtNode(this.game, node);
            nodeStats.add(new Pair<Integer, Integer>(node, probability));
        }

        // sort and yeet out top N
        Collections.sort(nodeStats, Comparator.comparing(p -> -p.getValue()));

        return nodeStats.subList(0, n); 
    }

    public int planInitialSettlements() {
        firstSettlement = 35;  

        // the plan for the first one... find the BEST node possible
        firstSettlement = getBestNode();

        firstResources = NDHelpers.findResourcesFromCoord(this.game, firstSettlement);


        return firstSettlement;
    }

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
        Vector<Integer> path = NDHelpers.BFSToCoord(this.game, settlementNode, plannedRoadDestinationNode);

        // return first road from BFS to coordinate
        if (path.size() < 2) return -1;

        D.ebugPrintln("Here..." + path.get(1));
        return path.get(1);        
    }


    // copied from SOCBoard and modified
    public boolean isNodeMoreThan4AwayFromNode(final int n1, final int n2)
    {
        final int d = n2 - n1;
        D.ebugPrintln("D: " + d + " " + d / 10 + " " + d % 10);

        if (d / 10 > 3 || d % 10 > 3) return true;
        return false;
    }


    public int bestSecondSettlement() {
        // look at 5 "best" nodes left over, and compare resources to determine where to go
        // try to maximize road building first, but if can't maximize development cards, ... expansion
        List<Pair<Integer, Integer>> nodes = getTopNNodes(3);


        Vector<Pair<Integer, Integer>> longRoads = new Vector<Pair<Integer, Integer>>();
        Vector<Pair<Integer, Integer>> develCoords = new Vector<Pair<Integer, Integer>>();
        // find distance to top 5, keeping those at or above minimium distance
        for (Pair<Integer, Integer> node : nodes) {
            SOCResourceSet resourcesAtCoord = NDHelpers.findResourcesFromCoord(this.game, node.getKey());

            // add in first node's resources
            resourcesAtCoord.add(firstResources);
            D.ebugPrintln("potential resources: " + resourcesAtCoord.toString());
            if (isNodeMoreThan4AwayFromNode(firstSettlement, node.getKey()) && resourcesAtCoord.contains(NDHelpers.ROAD_SET)) {
                longRoads.add(node);
            }
            if (resourcesAtCoord.contains(NDHelpers.DEVEL_SET)) {
                develCoords.add(node);
            }
        }

        // Pick either the best (first b/c sorted on probability) or search for development card locations
        if (longRoads.size() > 0) {
            D.ebugPrintln("Placing second for longest road");
            secondSettlement = longRoads.get(0).getKey();
        } else if (develCoords.size() > 0) {
            D.ebugPrintln("Placing second for development cards");
            secondSettlement = develCoords.get(0).getKey();
        } else {
            D.ebugPrintln("Placing second for best resources");
            secondSettlement = nodes.get(0).getKey();
        }
        return secondSettlement;
    }

    public int planSecondSettlement() { 
        return bestSecondSettlement();
    }

}

