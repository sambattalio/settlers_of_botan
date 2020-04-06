package bot;

import soc.debug.D;
import soc.game.SOCGame;
import soc.game.SOCPlayingPiece;
import soc.game.SOCRoutePiece;
import soc.robot.*;

import java.util.*;

public class NDRobotDM extends SOCRobotDM {

    public NDRobotDM(SOCRobotBrain br) {
        super(br);
    }

    /**
     * A strategy that chooses to build roads to the nearest open settlement spot and then settlements at that location
     *
     * @param strategy an integer that determines which strategy is used
     */
    @Override
    public void planStuff(int strategy) {
        D.ebug_enable();
        D.ebugPrintln("!!! Planning our move !!!");

        // reset any building plan already in place
        buildingPlan.clear();

        // Grab all of bot's current board pieces
        Vector<SOCPlayingPiece> playerPieces = ourPlayerData.getPieces();
        Vector<Integer> bestPath = new Vector<>();
        Iterator<SOCPlayingPiece> i = playerPieces.iterator();
        while (i.hasNext()) {
            //TODO is there a reason this need to be a while loop or is a for-each more idiomatic?
            SOCPlayingPiece piece = i.next();
            // look for ways to build off of any placed roads using BFS and resource odds
            if (piece.getType() == SOCPlayingPiece.ROAD) {
                Vector<Integer> searchResult = BFS(this.game, piece.getCoordinates());
                //TODO refactor to pass in best path in some form so that even less is explored, and to take in nodes instead of edges

                // has pre-existing road first so we remove it
                if (searchResult.size() > 0) searchResult.removeElementAt(0);
                // Check if better than the best path
                if (comparePaths(this.game, searchResult, bestPath) > 0) {
                    bestPath = searchResult;
                }
            }
        }

        // if we have settlements left to place
        // TODO find settlements left constant
        if (ourPlayerData.getSettlements().size() < 5) {
            // change last coord to settlement as it will always be one from our BFS
            buildingPlan.push(new SOCPossibleSettlement(ourPlayerData, bestPath.lastElement(), null));
        } else {
            //try to upgrade if we run out of settlements
            //noinspection OptionalGetWithoutIsPresent
            int toUpgrade = ourPlayerData.getSettlements().stream().map(SOCPlayingPiece::getCoordinates).max(this::compareSettlements).get();
            buildingPlan.push(new SOCPossibleCity(ourPlayerData, toUpgrade));
        }
        for (int j = bestPath.size() - 2; j >= 0; j--)
            buildingPlan.push(new SOCPossibleRoad(ourPlayerData, bestPath.get(j), null));
    }

    /**
     * Returns if legally possible to hypothetically place a settlement at coord
     *
     * @param game
     * @param coord the node to check
     * @return if there is no settlement there or at any of the adjacent nodes
     */
    public static boolean canBuildSettlement(SOCGame game, final int coord) {
        if (!game.getBoard().isNodeOnLand(coord)) return false;

        if (game.getBoard().settlementAtNode(coord) != null) return false;

        for (int i = 0; i <= 2; i++) {
            if (game.getBoard().settlementAtNode(game.getBoard().getAdjacentNodeToNode(coord, i)) != null)
                return false;
        }
        return true;
    }

    /**
     * Returns if legally possible to hypothetically place a road at edgeCoord
     *
     * @param game
     * @param edgeCoord  the edge to check
     * @param sourceEdge the edge that we have gotten to this node from
     * @return if there is no road already there
     */
    public static boolean canBuildRoad(SOCGame game, final int edgeCoord, final int sourceEdge) {
        for (SOCRoutePiece r : game.getBoard().getRoadsAndShips()) {
            if (edgeCoord == r.getCoordinates()) {
                return false;
            }
        }

        // TODO replace with a 'node on edges' method call
        List<Integer> nodesOnEdgeOne = game.getBoard().getAdjacentNodesToEdge(edgeCoord);
        List<Integer> nodesOnEdgeTwo = game.getBoard().getAdjacentNodesToEdge(sourceEdge);
        Optional<Integer> connectingNode = nodesOnEdgeOne.stream().filter(nodesOnEdgeTwo::contains).findFirst();

        if (!connectingNode.isPresent()) return false; // edges do not touch

        // Cannot build a road through a settlement that is not ours
        SOCPlayingPiece settlement = game.getBoard().settlementAtNode(connectingNode.get());
        //if (settlement != null && !ourPlayerData.equals(settlement.getPlayer())) return false;

        return true;
    }

    /**
     * Returns an "optimal" path to a new settlement from the starting edge coordinate
     *
     * @param coord the edge to branch off of
     * @return a vector of coordinates starting with the existing road and ending with the optimal settlement
     * @see NDRobotDM::comparePath the comparison to use
     */
    public static Vector<Integer> BFS(SOCGame game, int coord) {
        Queue<Vector<Integer>> queue = new LinkedList<>();

        // push initial coord
        Vector<Integer> vec = new Vector<>();
        vec.add(coord);
        queue.add(vec);

        Vector<Integer> bestPath = new Vector<>();

        while (!queue.isEmpty()) {
            Vector<Integer> current = queue.poll();

            if (current.size() > bestPath.size() && bestPath.size() != 0) continue;

            // Check for possible settlements
            for (int node : game.getBoard().getAdjacentNodesToEdge(current.lastElement())) {
                if (canBuildSettlement(game, node)) {

                    Vector<Integer> next = new Vector<>(current);
                    // add the settlement to the end of the path leading here
                    next.add(node);
                    if (comparePaths(game, next, bestPath) > 0) {
                        bestPath = next;
                    }
                }
            }

            // Check for new roads to build to add to queue
            for (int edge : game.getBoard().getAdjacentEdgesToEdge(current.lastElement())) {
                boolean isLoop = current.contains(edge);
                if (canBuildRoad(game, edge, current.lastElement()) && !isLoop) {
                    Vector<Integer> newPath = new Vector<>(current);
                    newPath.add(edge);
                    queue.add(newPath);
                }
            }
        }

        return bestPath;
    }

    /**
     * Compares two paths (vectors of roads followed by a settlement) for size and then quality of destination
     * Does not check the validity of the settlements
     *
     * @param one the first path
     * @param two the second path
     * @return a positive if the first path is better, or a negative if the second path is better
     */
    public static int comparePaths(SOCGame game, Vector<Integer> one, Vector<Integer> two) {
        if (two.size() == 0) {
            return 4;
        } else if (one.size() == 0) {
            return -4;
        }

        if (one.size() < two.size()) {
            return 2;
        } else if (one.size() > two.size()) {
            return -2;
        }

        int x = compareSettlements(game, one.lastElement(), two.lastElement());
        if (x != 0) return x;

        return -1;
        // TODO could consider relative rarities, or relative need as well, ports, etc.
    }

    /**
     * Compares the potential location of two settlements based on the values of the hexes around them
     * Does not check the validity of the settlements
     *
     * @param one
     * @param two
     * @return
     */
    public static int compareSettlements(SOCGame game, int one, int two) {
        if (NDHelpers.totalProbabilityAtNode(game, one) > NDHelpers.totalProbabilityAtNode(game, two)) {
            return 1;
        } else if (NDHelpers.totalProbabilityAtNode(game, one) < NDHelpers.totalProbabilityAtNode(game, two)) {
            return -1;
        }

        return 0;
    }

    private int compareSettlements(int one, int two) {
        return compareSettlements(this.game, one, two);
    }


}
