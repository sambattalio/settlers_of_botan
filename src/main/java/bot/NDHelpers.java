package bot;

import soc.debug.D;
import soc.game.*;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCPossibleSettlement;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        if (bestPlayer == null)
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
        if (bestPlayer == null || bestPlayer.getPlayerNumber() == playerNo) return true;

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
     * <p>
     * TODO maybe ports too?
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
     * <p>
     * TODO maybe ports too?
     *
     * @param game
     * @param playerNo
     * @param resources: int array where integers are SOCResourceConstant resource types
     * @return coords vector
     */
    public static Vector<Integer> findPotentialSettlementsFor(SOCGame game, int playerNo, List<Integer> resources) {
        if (resources.isEmpty()) {
            return new Vector<>(game.getPlayer(playerNo).getPotentialSettlements());
        }

        Vector<Integer> nodes = new Vector<>();

        for (int node : game.getPlayer(playerNo).getPotentialSettlements()) {

            Set<Integer> resourceSet = new HashSet<>(resources);

            for (int hex : game.getBoard().getAdjacentHexesToNode(node)) {
                if (resourceSet.contains(game.getBoard().getHexTypeFromCoord(hex))) {
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
     * @return best settlement
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
     * Finds possible roads that can be built from given edge coord
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
        // TODO refactor
        //check if the roads of our first settlement can connect to the roads of our second settlement
        Set<Integer> notOurRoads = game.getBoard().getRoadsAndShips().stream()
                .filter(road -> !road.getPlayer().equals(player))
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        Set<Set<Integer>> roadNetworks = getRoadNetworks(game, player);
        if (roadNetworks.size() == 2) {
            Iterator<Set<Integer>> iterator = roadNetworks.iterator();
            Set<Integer> firstBranching = iterator.next().stream()
                    .flatMap(edge -> game.getBoard().getAdjacentEdgesToEdge(edge).stream())
                    .filter(edge -> !notOurRoads.contains(edge))
                    .collect(Collectors.toSet());
            Set<Integer> secondBranching = iterator.next().stream()
                    .flatMap(edge -> game.getBoard().getAdjacentEdgesToEdge(edge).stream())
                    .filter(edge -> !notOurRoads.contains(edge))
                    .collect(Collectors.toSet());
            TreeSet<Integer> union = new TreeSet<>(firstBranching);
            union.retainAll(secondBranching);
            if(union.size() > 0) {
                return new SOCPossibleRoad(player, union.first(), null);
            }
        }

        // for now the strat is to try to build off of the longest road
        // of the player
        player.calcLongestRoad2();
        Optional<SOCLRPathData> pathData = player.getLRPaths().stream().max(Comparator.comparing(SOCLRPathData::getLength));

        if (pathData.isPresent()) {
            SOCLRPathData path = pathData.get();
            // check if can build off beginning

            //TODO findPossibleRoads(game, path.getBeginning()) vs game.getBoard().getAdjacentEdgesToNode(
            //TODO snake in direction of other settlement and good open areas / nodes
            List<Integer> possibleFront = game.getBoard().getAdjacentEdgesToNode(path.getBeginning()).stream()
                    .filter(player::isPotentialRoad)
                    .collect(Collectors.toList());
            // for now just return the first possible... later we need to prolly
            // search this shizz our
            if (possibleFront.size() != 0) return new SOCPossibleRoad(player, possibleFront.get(0), null);

            // same but end...
            List<Integer> possibleEnd = game.getBoard().getAdjacentEdgesToNode(path.getEnd()).stream()
                    .filter(player::isPotentialRoad)
                    .collect(Collectors.toList());
            // for now just return the first possible... later we need to prolly
            // search this shizz our
            if (possibleEnd.size() != 0) return new SOCPossibleRoad(player, possibleEnd.get(0), null);
        }

        return null;
    }

    private static Set<Set<Integer>> getRoadNetworks(SOCGame game, SOCPlayer player) {
        return Stream.concat(player.getSettlements().stream(), player.getCities().stream())
                .map(SOCPlayingPiece::getCoordinates)
                .map(coord -> getAllRoadsConnected(coord, game, player))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static Set<Integer> getAllRoadsConnected(int nodeCoord, SOCGame game, SOCPlayer player) {
        Set<Integer> connected = new HashSet<>();
        TreeSet<Integer> frontier = game.getBoard().getAdjacentEdgesToNode(nodeCoord).stream()
                .filter(edge -> player.getRoadsAndShips().stream().anyMatch(road -> road.getCoordinates() == edge))
                .collect(Collectors.toCollection(TreeSet::new));
        while(!frontier.isEmpty()) {
            Integer consideredEdge = frontier.first();
            frontier.remove(consideredEdge);
            connected.add(consideredEdge);
            frontier.addAll(
                    game.getBoard().getAdjacentEdgesToEdge(consideredEdge).stream()
                            .filter(edge -> player.getRoadsAndShips().stream().anyMatch(road -> road.getCoordinates() == edge))
                            .filter(edge -> !connected.contains(edge) && !frontier.contains(edge))
                            .collect(Collectors.toSet())
            );
        }
        return connected;
    }

    /**
     * Returns road distance from start to end
     *
     * @param game
     * @param player
     * @param start  coord
     * @param end    coord
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
                if (node == end) {

                    Vector<Integer> next = new Vector<>(current);
                    // add the settlement to the end of the path leading here
                    next.add(node);
                    if (NDRobotDM.comparePaths(game, next, bestPath) > 0) {
                        bestPath = next;
                    }
                }
            }

            // Check for new roads to build to add to queue
            for (int edge : game.getBoard().getAdjacentEdgesToEdge(current.lastElement())) {
                boolean isLoop = current.contains(edge);
                if (NDRobotDM.canBuildRoad(game, edge, current.lastElement()) && !isLoop) {
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
        if(game.getPlayer(playerNo).getPieces().stream().filter(piece -> piece instanceof SOCRoad).count() == SOCPlayer.ROAD_COUNT) {
            return false;
        }
        return isCompetitiveForLongestRoad(game, playerNo);

        // here we know we are competitive... now lets see if we can reach
        //TODO try to build off getLRPaths()????
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

        return connectingNode.isPresent(); // edges do not touch
    }

    /**
     * Returns the sum of the probabilities of the tiles surrounding a node
     * Memoizes since the value will not change
     *
     * @param game      the game board
     * @param nodeCoord the node to check
     * @return the total probability
     */
    public static int totalProbabilityAtNode(SOCGame game, final int nodeCoord) {
        if (totalProbabilityAtNodeGame != game) {
            totalProbabilityAtNodeCache = new HashMap<>();
            totalProbabilityAtNodeGame = game;
        }
        if (!totalProbabilityAtNodeCache.containsKey(nodeCoord)) {
            int sum = 0;
            for (int hexCoord : game.getBoard().getAdjacentHexesToNode(nodeCoord)) {
                int diceNumber = game.getBoard().getNumberOnHexFromCoord(hexCoord);
                // skip water
                if (diceNumber > 12 || diceNumber <= 0) continue;
                sum += (diceNumber > 7) ? 13 - diceNumber : diceNumber - 1;
            }

            totalProbabilityAtNodeCache.put(nodeCoord, sum);
        }
        return totalProbabilityAtNodeCache.get(nodeCoord);
    }
}
