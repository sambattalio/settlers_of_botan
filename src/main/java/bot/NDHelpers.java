package bot;

import soc.game.*;
import soc.robot.*;
import soc.disableDebug.D;

import soc.game.SOCResourceSet;
import java.util.*;


import java.util.Vector;
import static java.lang.Math.abs;

public class NDHelpers {

    static final int MAX_ROAD_DIFF = 3;
    static final int MAX_ARMY_DIFF = 2;

    static final SOCResourceSet ROAD_SET = new SOCResourceSet(1, 0, 0, 0, 1, 0);
    static final SOCResourceSet DEVEL_SET = new SOCResourceSet(0, 1, 1, 1, 0, 0);
    private static SOCGame totalProbabilityAtNodeGame;
    private static HashMap<Integer, Integer> totalProbabilityAtNodeCache;

    /* Returns true if input player has longest or is within 3 roads */
    public static boolean isCompetitiveForLongestRoad(SOCGame game, int playerNo) {
        SOCPlayer bestPlayer = game.getPlayerWithLongestRoad();

        /* No one has it yet */
        if(bestPlayer == null)
            return true;

        /* Of course i know him... he's me */
        if (bestPlayer.getPlayerNumber() == playerNo) return true;

        /* Check diff in length */
        SOCPlayer ndBot = game.getPlayer(playerNo);
        return abs(ndBot.getLongestRoadLength() - bestPlayer.getLongestRoadLength()) <= MAX_ROAD_DIFF;
    }

    /**
     * Returns if playerNo is deemed competitive for largest army.
     *
     * @param game
     * @param playerNo
     * @return true if army count - best army count < MAX_ARMY_DIFF
     */
    public static boolean isCompetitiveForLargestArmy(SOCGame game, int playerNo) {
        SOCPlayer bestPlayer = game.getPlayerWithLargestArmy();
        if (bestPlayer.getPlayerNumber() == playerNo) return true;

        /* Check diff in length */
        SOCPlayer ndBot = game.getPlayer(playerNo);

        return game.getNumDevCards() != 0 && abs(ndBot.getNumKnights() - bestPlayer.getNumKnights()) <= MAX_ARMY_DIFF;
    }

    /**
     * Returns if a player can build a settlement
     *
     * @param game
     * @param playerNo
     * @return true if possible to build a settlement
     */
    public static boolean canBuildSettlement(SOCGame game, int playerNo) {
        return game.getPlayer(playerNo).getPotentialSettlements().size() != 0;
    }


    /**
     * Returns if there exists a settlement that will yield the resources in the set
     *
     * TODO maybe ports too?
     * Right now only handles arrays up to size 3
     *
     * @param game
     * @param playerNo
     * @param resources: int array where integers are SOCResourceConstant resource types
     * @return if there is a settlement that can yield these resources
     */
    public static boolean existsQualitySettlementFor(SOCGame game, int playerNo, List<Integer> resources) {
        return !findPotentialSettlementsFor(game, playerNo, resources).isEmpty();       
    }

    /**
     * Returns coords of settlements that will yield the resources in the set
     *
     * TODO maybe ports too?
     * Right now only handles arrays up to size 3
     *
     * @param game
     * @param playerNo
     * @param resources: int array where integers are SOCResourceConstant resource types
     * @return coords vector 
     */
    public static Vector<Integer> findPotentialSettlementsFor(SOCGame game, int playerNo, List<Integer> resources) {
        if(resources.isEmpty()) {
            return new Vector<>(game.getPlayer(playerNo).getPotentialSettlements());
        }

        Vector<Integer> nodes = new Vector<>();

        for (int node : game.getPlayer(playerNo).getPotentialSettlements()) {

            Set<Integer> resourceSet = new HashSet<>(resources);

            for (int hex : game.getBoard().getAdjacentHexesToNode(node)) {
                if(resourceSet.contains(game.getBoard().getHexTypeFromCoord(hex))) {
                    nodes.add(node);
                    break;
                }
            }
        }

        return nodes;
    }

    /**
     * Returns resources yielded from a roll @ coord
     *
     * @param game
     * @param coord
     * @return vect of resource types
     */
    public static SOCResourceSet findResourcesFromCoord(SOCGame game, int coord) {
        SOCResourceSet resources = new SOCResourceSet();

        for (int hex : game.getBoard().getAdjacentHexesToNode(coord)) {
            int type = game.getBoard().getHexTypeFromCoord(hex);
            if (type < 0 || type > 6) continue;
            resources.add(1, type);
        }

        return resources;
    }


    /**
     * Returns *coord* of the best possible settlement for given resources
     *
     * @param game
     * @param player
     * @param resources
     *
     * @return best settlement
     *
     */
    public static SOCPossibleSettlement bestPossibleSettlement(SOCGame game, SOCPlayer player, List<Integer> resources) {
        int playerNo = player.getPlayerNumber();
        Vector<Integer> possible_nodes = findPotentialSettlementsFor(game, playerNo, resources);

        int bestNode = possible_nodes.firstElement();

        for (int node : possible_nodes) {
            if (totalProbabilityAtNode(game, bestNode) > totalProbabilityAtNode(game, node)) {
                bestNode = node;
            }
        }

        return new SOCPossibleSettlement(player, bestNode, null); //TODO add potential road list
    }


    /**
     * Finds possible roads that can be built from given coord
     *
     * @param game
     * @param edgeCoord edge to build off of
     * @return Vector of coords
     */
    public static Vector<Integer> findPossibleRoads(SOCGame game, final int edgeCoord) {

        Vector<Integer> possibleRoads = new Vector<>();

        for (int edge : game.getBoard().getAdjacentEdgesToEdge(edgeCoord)) {
            if (canBuildRoadTwo(game, edge, edgeCoord)) {
                possibleRoads.add(edge);
            } 
        }

        return possibleRoads;
    }

    /**
     * Returns coord of best possible road to place to maximize length
     *
     * @param game
     * @param player
     * @return best road to build
     */
    public static SOCPossibleRoad bestPossibleLongRoad(SOCGame game, SOCPlayer player) {
        // for now the strat is to try to build off of the longest road(s)
        // of the player
        // TODO maybe import ?
        Vector<SOCLRPathData> pathData = game.getPlayer(player.getPlayerNumber()).getLRPaths();

        for (SOCLRPathData path : pathData) {
            // check if can build off beginning

            Vector<Integer> possibleFront = findPossibleRoads(game, path.getBeginning());
            // for now just return the first possible... later we need to prolly
            // search this shizz our
            if (possibleFront.size() != 0) return new SOCPossibleRoad(player, possibleFront.get(0), null);

            // same but end...
            Vector<Integer> possibleEnd = findPossibleRoads(game, path.getEnd());
            // for now just return the first possible... later we need to prolly
            // search this shizz our
            if (possibleEnd.size() != 0) return new SOCPossibleRoad(player, possibleEnd.get(0), null);
        }

        return null;
    }

    /**
     * Returns road distance from start to end
     *
     * @param game
     * @param player 
     * @param start coord
     * @param end coord
     * @return integer road length
     */
    public static int distanceBetweenNodes(SOCGame game, SOCPlayer player, int start, int end) {
        return BFSToCoord(game, start, end).size();
    }


    public static Vector<Integer> BFSToCoord(SOCGame game, int start, int end) {
        Queue<Vector<Integer>> queue = new LinkedList<>();

        // push initial coord
        Vector<Integer> vec = new Vector<>();
        vec.add(start);
        queue.add(vec);

        Vector<Integer> bestPath = new Vector<>();

        while (!queue.isEmpty()) {
            Vector<Integer> current = queue.poll();

            if (current.size() > bestPath.size() && bestPath.size() != 0) continue;

            // Check for possible settlements
            for (int node : game.getBoard().getAdjacentNodesToEdge(current.lastElement())) {
                D.ebugPrintln("Considering node " + node + " onto " + current);
                if (node == end) {
                    D.ebugPrintln("Can build settlement at " + node);

                    Vector<Integer> next = new Vector<>(current);
                    // add the settlement to the end of the path leading here
                    next.add(node);
                    if (NDRobotDM.comparePaths(game, next, bestPath) > 0) {
                        bestPath = next;
                        D.ebugPrintln("Best so far was: " + bestPath.toString());
                    }
                }
            }

            // Check for new roads to build to add to queue
            for (int edge : game.getBoard().getAdjacentEdgesToEdge(current.lastElement())) {
                D.ebugPrintln("Adjacent edge to " + current.lastElement() + ": " + edge);
                boolean isLoop = current.contains(edge);
                if (NDRobotDM.canBuildRoad(game, edge, current.lastElement()) && !isLoop) {
                    D.ebugPrintln("Can build road on " + edge);
                    Vector<Integer> newPath = new Vector<>(current);
                    newPath.add(edge);
                    queue.add(newPath);
                }
            }

        }

        return bestPath;
    }



    /**
     * Try to trade for resources
     *
     * @param game
     * @param playerNo
     * @param give
     * @param get
     * @return void
     */
    /*public static void setPlayersOffer(SOCGame game, int playerNo, SOCResourceSet give, SOCResourceSet get) {
      // make an array to yeet trade to everyone
      boolean[] players_to_offer = new boolean[game.maxPlayers];
      Arrays.fill(players_to_offer, true);
      players_to_offer[playerNo] = false; // don't offer self

      game.getPlayer(playerNo).setCurrentOffer(SOCTradeOffer(game, playerNo, players_to_offer, give, get));
    }*/

    /**
     * Clears current offer
     *
     * @param game
     * @param playerNo
     * @return void
     */
    public static void clearPlayersOffer(SOCGame game, int playerNo) {
        game.getPlayer(playerNo).setCurrentOffer(null);
    }

    /**
     * Returns if longest road is possible to build
     *
     * @param game
     * @param playerNo
     * @return true if can build to longest road
     */
    public static boolean isLongestRoadPossible(SOCGame game, int playerNo) {
        if (!isCompetitiveForLongestRoad(game, playerNo)) return false;

        // here we know we are competitive... now lets see if we can reach
        //TODO try to build off getLRPaths()????

        return true;
    }

    public static boolean canBuildRoadTwo(SOCGame game, final int edgeCoord, final int sourceEdge) {
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

        return true;
    }

    /**
     * Returns the sum of the probabilities of the tiles surrounding a node
     * Memoizes since the value will not change
     *
     * @param game the game board
     * @param nodeCoord the node to check
     * @return the total probability
     */
    public static int totalProbabilityAtNode(SOCGame game, final int nodeCoord) {
        if(totalProbabilityAtNodeGame != game) {
            totalProbabilityAtNodeCache = new HashMap<>();
            totalProbabilityAtNodeGame = game;
        }
        if(!totalProbabilityAtNodeCache.containsKey(nodeCoord)) {
            int sum = 0;
            for (int hexCoord : game.getBoard().getAdjacentHexesToNode(nodeCoord)) {
                int diceNumber = game.getBoard().getNumberOnHexFromCoord(hexCoord);
                // skip water
                if (diceNumber > 12 || diceNumber <= 0) continue;
                sum += (diceNumber > 7) ? 13 - diceNumber : diceNumber - 1;
            }
            soc.debug.D.ebugPrintln("Sum at node " + nodeCoord + " was " + sum);

            totalProbabilityAtNodeCache.put(nodeCoord, sum);
        }
        return totalProbabilityAtNodeCache.get(nodeCoord);
    }
}
