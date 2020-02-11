package bot;

import soc.game.SOCSettlement;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotDM;
import soc.game.SOCPlayingPiece;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;
import soc.debug.D;
import soc.game.SOCRoutePiece;

import java.util.*;

public class NDRobotDM extends SOCRobotDM {

    public NDRobotDM(SOCRobotBrain br) {
        super(br);
    }

    /**
     * A strategy that chooses to build roads to the nearest open settlement spot and then settlements at that location
     * @param strategy  an integer that determines which strategy is used
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
                Vector<Integer> searchResult = BFS(piece.getCoordinates());
                //TODO refactor to pass in best path in some form so that even less is explored, and to take in nodes instead of edges
                D.ebugPrintln("Best on road " + piece + " was " + searchResult);
                // has pre-existing road first so we remove it
                if (searchResult.size() > 0) searchResult.removeElementAt(0);
                // Check if better than the best path
                if (comparePaths(searchResult, bestPath) > 1) {
                    bestPath = searchResult;
                    D.ebugPrintln("Best path set:" + bestPath.toString());
                }
            }
        }

        D.ebugPrintln("Best at end" + bestPath.toString());
        // change last coord to settlement as it will always be one from our BFS
        buildingPlan.push(new SOCPossibleSettlement(ourPlayerData, bestPath.lastElement(), null));
        for (int j = bestPath.size() - 2; j >= 0; j--)
            buildingPlan.push(new SOCPossibleRoad(ourPlayerData, bestPath.get(j), null));
    }

    /**
     * Returns if legally possible to hypothetically place a settlement at coord
     *
     * @param coord the node to check
     * @return if there is no settlement there or at any of the adjacent nodes
     */
    public boolean canBuildSettlement(final int coord) {
        if (game.getBoard().settlementAtNode(coord) != null) return false;

        for (int i = 0; i <= 2; i++) {
            D.ebugPrintln("Trying to find adjacent nodes from node: " + coord + " in direction: " + i);
            if (game.getBoard().settlementAtNode(game.getBoard().getAdjacentNodeToNode(coord, i)) != null)
                return false;
        }
        return true;
    }

    /**
     * Returns if legally possible to hypothetically place a road at edgeCoord
     *
     * @param edgeCoord the edge to check
     * @param sourceEdge the edge that we have gotten to this node from
     * @return if there is no road already there
     */
    public boolean canBuildRoad(final int edgeCoord, final int sourceEdge) {
        for (SOCRoutePiece r : game.getBoard().getRoadsAndShips()) {
            if (edgeCoord == r.getCoordinates()) {
                return false;
            }
        }

        // TODO replace with a 'node on edges' method call
        List<Integer> nodesOnEdgeOne = game.getBoard().getAdjacentNodesToEdge(edgeCoord);
        List<Integer> nodesOnEdgeTwo = game.getBoard().getAdjacentNodesToEdge(sourceEdge);
        Optional<Integer> connectingNode = nodesOnEdgeOne.stream().filter(nodesOnEdgeTwo::contains).findFirst();

        if(!connectingNode.isPresent()) return false; // edges do not touch

        // Cannot build a road through a settlement that is not ours
        SOCPlayingPiece settlement = game.getBoard().settlementAtNode(connectingNode.get());
        if (settlement != null && !ourPlayerData.equals(settlement.getPlayer())) return false;

        return true;
    }

    /**
     * Returns the sum of the probabilities of the tiles surrounding a node
     *
     * @param nodeCoord the node to check
     * @return the total probability
     */
    public int totalProbabilityAtNode(final int nodeCoord) {
        int sum = 0;
        for (int hexCoord : game.getBoard().getAdjacentHexesToNode(nodeCoord)) {
            int diceNumber = game.getBoard().getNumberOnHexFromCoord(hexCoord);
            D.ebugPrintln("Value of Hex piece " + String.valueOf(hexCoord) + ": " + String.valueOf(diceNumber));
            // skip water
            if (diceNumber > 12 || diceNumber <= 0) continue;
            sum += (diceNumber > 7) ? 13 - diceNumber : diceNumber - 1;
        }
        D.ebugPrintln("Sum is: " + String.valueOf(sum));

        return sum;
    }

    /**
     * Returns an "optimal" path to a new settlement from the starting edge coordinate
     *
     * @param coord the edge to branch off of
     * @return a vector of coordinates starting with the existing road and ending with the optimal settlement
     */
    public Vector<Integer> BFS(int coord) {
        Queue<Vector<Integer>> stack = new LinkedList<>();

        // push initial coord
        Vector<Integer> vec = new Vector<>();
        vec.add(coord);
        stack.add(vec);

        Vector<Integer> bestPath = new Vector<>();

        while (!stack.isEmpty()) {
            Vector<Integer> current = stack.poll();

            if (current.size() > bestPath.size() && bestPath.size() != 0) continue;

            // Check for possible settlements
            for (int node : game.getBoard().getAdjacentNodesToEdge(current.lastElement())) {
                D.ebugPrintln("Considering node " + node + " onto " + current);
                if (canBuildSettlement(node)) {
                    D.ebugPrintln("Can build settlement at " + node);
                    D.ebugPrintln("Sum of probabilities: " + totalProbabilityAtNode(node));
                    if (comparePaths(current, bestPath) > 0) {
                        bestPath = new Vector<>(current);
                        // add the settlement to the end of the path leading here
                        D.ebugPrintln("Adding node " + String.valueOf(node) + " to " + bestPath);
                        bestPath.add(node);
                    }
                }
            }

            // Check for new roads to build to add to stack
            for (int edge : game.getBoard().getAdjacentEdgesToEdge(current.lastElement())) {
                D.ebugPrintln("Adjacent edge to " + current.lastElement() + ": " + edge);
                boolean isLoop = current.contains(edge);
                if (canBuildRoad(edge, current.lastElement()) && !isLoop) {
                    D.ebugPrintln("Can build road on " + edge);
                    Vector<Integer> newPath = new Vector<>(current);
                    newPath.add(edge);
                    stack.add(newPath);
                }
            }

        }

        return bestPath;
    }

    /**
     * Compares two paths (vectors of roads followed by a settlement) for size and then quality of destination
     *
     * @param one the first path
     * @param two the second path
     * @return a positive if the first path is better, or a negative if the second path is better
     */
    private int comparePaths(Vector<Integer> one, Vector<Integer> two) {
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

        if (totalProbabilityAtNode(one.lastElement()) > totalProbabilityAtNode(one.lastElement())) {
            return 1;
        } else if (totalProbabilityAtNode(one.lastElement()) < totalProbabilityAtNode(one.lastElement())) {
            return -1;
        }

        return 0;
        // TODO could consider relative rarities, or relative need as well
    }


}
