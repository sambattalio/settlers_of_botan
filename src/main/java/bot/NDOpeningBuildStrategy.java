package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCResourceSet;
import soc.game.SOCSettlement;
import soc.robot.OpeningBuildStrategy;
import soc.debug.D;

import java.util.Vector;
import javafx.util.Pair;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

public class NDOpeningBuildStrategy extends OpeningBuildStrategy {

    protected SOCResourceSet firstResources;

    private final int MIN_LENGTH_FOR_LONGEST = 5;

    private final static int[] NODE_2_AWAY = { -9, 0x02, 0x22, 0x20, -0x02, -0x22, -0x20 };
    private final static int[] NODE_3_AWAY = { -9, 0x03, 0x33, 0x30, -0x03, -0x33, -0x30 };
    private final static int[] NODE_4_AWAY = { -9, 0x04, 0x44, 0x40, -0x04, -0x44, -0x40 };

    public static HashMap<Integer, Integer> probMap = new HashMap<>();

    private Vector<Integer> roadsToBuild;

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
        return this.ourPlayerData.getPotentialSettlements().stream()
                .max(Comparator.comparing(NDHelpers.totalProbabilityAtNodeMapping(game.getBoard()))).orElseThrow(() -> new IllegalStateException("No best settlement"));
    }

    private List<Integer> getTopNNodes(int n) {
        return this.ourPlayerData.getPotentialSettlements().stream()
                .sorted(Comparator.comparing(NDHelpers.totalProbabilityAtNodeMapping(game.getBoard())).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    public int closestRoadToNode(int source, int target) {
        Comparator<Integer> comparator = Comparator.comparing(edge -> Math.abs(game.getBoard().getAdjacentNodeFarEndOfEdge(edge, target) - target));
        return game.getBoard().getAdjacentEdgesToNode(source).stream()
                   .min(comparator).orElse(-1);
    }


    public int planInitialSettlements() {
        D.ebugPrintln("Placing first settlement");
        // the plan for the first one... find the BEST node possible
        firstSettlement = getBestNode();

        firstResources = NDHelpers.findResourcesFromCoord(this.game, firstSettlement);

        return firstSettlement;
    }

    public int planInitRoad() {
        int settlementNode = ourPlayerData.getLastSettlementCoord();
        int nextNode;

        int newRoad;

        Vector<SOCSettlement> settlements = ourPlayerData.getSettlements();

        if (ourPlayerData.getSettlements().size() > 1) {
            // set destination node to not the one just placed
            nextNode = settlements.get(0).getCoordinates() == settlementNode ? settlements.get(1).getCoordinates() : settlements.get(0).getCoordinates();
        } else {
            nextNode = bestSecondSettlement();
        }

        // develop path to hypothetical next node... and place road on first option 
        plannedRoadDestinationNode = nextNode;

        return closestRoadToNode(settlementNode, nextNode);
    }

    public int bestSecondSettlement() {
        // look at 5 "best" nodes left over, and compare resources to determine where to go
        // try to maximize road building first, but if can't maximize development cards, ... expansion
        List<Integer> nodes = getTopNNodes(5);

        Vector<Integer> longRoads = new Vector<>();
        Vector<Integer> develCoords = new Vector<>();
        // find distance to top 5, keeping those at or above minimium distance
        for (int node : nodes) {
            SOCResourceSet resourcesAtCoord = NDHelpers.findResourcesFromCoord(this.game, node);

            resourcesAtCoord.add(firstResources);
            if (resourcesAtCoord.contains(NDHelpers.ROAD_SET)) {
                longRoads.add(node);
            }
            if (resourcesAtCoord.contains(NDHelpers.DEVEL_SET)) {
                develCoords.add(node);
            }
        }

        // Pick either the best (first b/c sorted on probability) or search for development card locations
        if (longRoads.size() > 0) {
            D.ebugPrintln("Placing second settlement for longest road");
            secondSettlement = longRoads.get(0);
        } else if (develCoords.size() > 0) {
            D.ebugPrintln("Placing second settlement for development cards");
            secondSettlement = develCoords.get(0);
        } else {
            D.ebugPrintln("Placing second settlement for best resources");
            secondSettlement = nodes.get(0);
        }
        return secondSettlement;
    }

    public int planSecondSettlement() { 
        return bestSecondSettlement();
    }

}

