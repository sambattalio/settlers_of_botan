package bot;

import soc.debug.D;
import soc.game.*;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCRobotBrain;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Predicate;


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
    public static boolean canBuildSettlement(int playerNo, NDRobotBrain brain) {
        return brain.getGame().getPlayer(playerNo).getSettlements().size() < 5;
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
        if(possible_nodes.isEmpty()) {
        	possible_nodes = findPotentialSettlementsFor(game, playerNo, Collections.emptyList());
        }

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
    public static SOCPossiblePiece bestPossibleLongRoad(SOCGame game, SOCPlayer player) {
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
    
    public static ResourceSet getPlayerResources(NDRobotBrain brain) {
        return brain.getOurPlayerData().getResources();
    }

    public static boolean haveResourcesForRoadAndSettlement(NDRobotBrain brain) {
        if(brain.getOurPlayerData().getPieces().stream().filter(piece -> piece instanceof SOCRoad).count() == SOCPlayer.ROAD_COUNT ||
        		brain.getOurPlayerData().getPieces().stream().filter(piece -> piece instanceof SOCSettlement).count() == SOCPlayer.SETTLEMENT_COUNT
        ) {
            return false;
        }
        ResourceSet set = brain.getOurPlayerData().getResources();
        return set.getAmount(SOCResourceConstants.CLAY) >= 2 &&
                set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                set.getAmount(SOCResourceConstants.WHEAT) >= 1 &&
                set.getAmount(SOCResourceConstants.WOOD) >= 2;
    }
    
    public static SOCResourceSet getResourcesFor(int type) {
        SOCResourceSet set = new SOCResourceSet();
        switch (type) {
            case SOCPossiblePiece.ROAD: {
                set.add(1, SOCResourceConstants.CLAY);
                set.add(1, SOCResourceConstants.WOOD);
                return set;
            }
            case SOCPossiblePiece.SETTLEMENT: {
                set.add(1, SOCResourceConstants.CLAY);
                set.add(1, SOCResourceConstants.WOOD);
                set.add(1, SOCResourceConstants.SHEEP);
                set.add(1, SOCResourceConstants.WHEAT);
                return set;
            }
            case SOCPossiblePiece.CITY: {
                set.add(3, SOCResourceConstants.ORE);
                set.add(2, SOCResourceConstants.WHEAT);
                return set;
            }
            case SOCPossiblePiece.CARD: {
                //TODO check if cards are left
                set.add(1, SOCResourceConstants.ORE);
                set.add(1, SOCResourceConstants.WHEAT);
                set.add(1, SOCResourceConstants.SHEEP);
                return set;
            }
        }
        return set;
    }

    public static boolean haveResourcesFor(int type, SOCRobotBrain brain) {
        return haveResourcesFor(type, brain, brain.getOurPlayerData().getResources());
    }

    public static boolean haveResourcesFor(int type, SOCRobotBrain brain, ResourceSet set) {
        D.ebugPrintln("Brain thinks bot has: " + set);
    
        switch (type) {
            case SOCPossiblePiece.ROAD: {
                if(brain.getOurPlayerData().getPieces().stream().filter(piece -> piece instanceof SOCRoad).count() == SOCPlayer.ROAD_COUNT) {
                    return false;
                }
                return set.getAmount(SOCResourceConstants.CLAY) >= 1 &&
                        set.getAmount(SOCResourceConstants.WOOD) >= 1;
            }
            case SOCPossiblePiece.SETTLEMENT: {
                if(brain.getOurPlayerData().getPieces().stream().filter(piece -> piece instanceof SOCSettlement).count() == SOCPlayer.SETTLEMENT_COUNT) {
                    return false;
                }
                return set.getAmount(SOCResourceConstants.CLAY) >= 1 &&
                        set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                        set.getAmount(SOCResourceConstants.WHEAT) >= 1 &&
                        set.getAmount(SOCResourceConstants.WOOD) >= 1;
            }
            case SOCPossiblePiece.CITY: {
                if(brain.getOurPlayerData().getPieces().stream().filter(piece -> piece instanceof SOCCity).count() == SOCPlayer.CITY_COUNT) {
                    return false;
                }
                return set.getAmount(SOCResourceConstants.ORE) >= 3 &&
                        set.getAmount(SOCResourceConstants.WHEAT) >= 2;
            }
            case SOCPossiblePiece.CARD: {
                //TODO check if cards are left
                return set.getAmount(SOCResourceConstants.ORE) >= 1 &&
                        set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                        set.getAmount(SOCResourceConstants.WHEAT) >= 1;
            }
        }
        return false;
    }

    public static Optional<SOCPossibleSettlement> findQualitySettlementFor(List<Integer> resources, NDRobotBrain brain) {
        D.ebugPrintln("Finding quality settlement");
        return Optional.ofNullable(bestPossibleSettlement(brain.getGame(), brain.getOurPlayerData(), resources));
    }

    public static Optional<SOCPossibleCity> findQualityCityFor(List<Integer> resources, NDRobotBrain brain) {
        D.ebugPrintln("Finding quality city");
        SOCGame game = brain.getGame();
        Vector<SOCSettlement> ourSettlements = brain.getOurPlayerData().getSettlements();
        if (resources.isEmpty()) {
            return ourSettlements.stream()
                    .map(SOCPlayingPiece::getCoordinates)
                    .sorted(Comparator.comparing(node -> totalProbabilityAtNode(game, node)))
                    .map(node -> new SOCPossibleCity(brain.getOurPlayerData(), node))
                    .findFirst();
        }
        return ourSettlements.stream()
                .filter(settlement -> settlement.getAdjacentHexes().stream()
                        .anyMatch(hex -> resources.contains(game.getBoard().getHexTypeFromCoord(hex)))
                )
                .map(SOCPlayingPiece::getCoordinates)
                .sorted(Comparator.comparing(node -> totalProbabilityAtNode(game, node)))
                .map(node -> new SOCPossibleCity(brain.getOurPlayerData(), node))
                .findFirst();
    }

    public static Optional<SOCPossibleSettlement> findQualitySettlement(NDRobotBrain brain) {
        return findQualitySettlementFor(Collections.emptyList(), brain);
    }

    public static Optional<SOCPossibleCity> findQualityCity(NDRobotBrain brain) {
        return findQualityCityFor(Collections.emptyList(), brain);
    }

    public static Optional<SOCPossiblePiece> findQualityRoadForLongestRoad(NDRobotBrain brain) {
        D.ebugPrintln("Finding quality road for longest road");
        return Optional.ofNullable(bestPossibleLongRoad(brain.getGame(), brain.getOurPlayerData()));
    }

    public static Optional<SOCPossiblePiece> findQualityRoadForExpansion(NDRobotBrain brain) {
        D.ebugPrintln("Finding quality road for expansion");
        SOCGame game = brain.getGame();
        Set<Integer> othersSettlements = game.getBoard().getSettlements().stream()
                .filter(socSettlement -> socSettlement.getPlayerNumber() != brain.getOurPlayerNumber())
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        Set<Integer> invalidSettlements = othersSettlements.stream()
                .flatMap(node -> Stream.concat(Stream.of(node), game.getBoard().getAdjacentNodesToNode(node).stream()))
                .collect(Collectors.toSet());
        Set<Integer> occupiedRoads = game.getBoard().getRoadsAndShips().stream()
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        HashSet<Integer> visitedNodes = new HashSet<>();
        // stores the road that was used to get to a node
        HashMap<Integer, Integer> getTo = new HashMap<>();

        // start frontier as all the nodes that are one buildable road away from where we already have roads
        // and set all getTo
        HashSet<Integer> frontier = brain.getOurPlayerData().getRoadNodes().stream()
                .filter(node -> !othersSettlements.contains(node))
                .flatMap(node -> game.getBoard().getAdjacentEdgesToNode(node).stream()
                        .filter(edge -> !occupiedRoads.contains(edge))
                        // set getTo if not already set
                        .peek(edge -> getTo.put(game.getBoard().getAdjacentNodeFarEndOfEdge(edge, node), getTo.getOrDefault(game.getBoard().getAdjacentNodeFarEndOfEdge(edge, node), edge)))
                        .map(edge -> game.getBoard().getAdjacentNodeFarEndOfEdge(edge, node))
                )
                .filter(node -> !othersSettlements.contains(node))
                .collect(Collectors.toCollection(HashSet::new));

        Predicate<Integer> isPossibleSettlement = node -> !invalidSettlements.contains(node);
        //TODO choose to take a further settlement if it is much better than closer one
        for(int length = 1; length < 5; length++) {
            if(frontier.stream().anyMatch(isPossibleSettlement)) {
                return frontier.stream()
                        .filter(isPossibleSettlement)
                        .sorted() //TODO add value comparison, and consider long term growth
                        .findFirst()
                        .map(node -> new SOCPossibleRoad(brain.getOurPlayerData(), getTo.get(node), null));
            }
            visitedNodes.addAll(frontier);
            frontier = frontier.stream()
                    .flatMap(node -> game.getBoard().getAdjacentEdgesToNode(node).stream()
                            .filter(edge -> !occupiedRoads.contains(edge))
                            .peek(edge -> getTo.put(game.getBoard().getAdjacentNodeFarEndOfEdge(edge, node), getTo.getOrDefault(game.getBoard().getAdjacentNodeFarEndOfEdge(edge, node), getTo.get(node))))
                            .map(edge -> game.getBoard().getAdjacentNodeFarEndOfEdge(edge, node))
                    )
                    .filter(node -> !othersSettlements.contains(node) && !visitedNodes.contains(node))
                    .collect(Collectors.toCollection(HashSet::new));
        }
        return Optional.empty(); //TODO add quality road search based on resources like with settlements & cities
    }


    public static SOCResourceSet getExtantResources(SOCRobotBrain brain) {
        return Arrays.stream(brain.getGame().getPlayers())
                .map(SOCPlayer::getResources)
                .collect(SOCResourceSet::new, SOCResourceSet::add, SOCResourceSet::add);
    }

    public static Map<Integer, Integer> getProbabilityForResource(SOCRobotBrain brain) {
        SOCBoard board = brain.getGame().getBoard();
        //get all the hexes around a settlement
        Stream<Integer> settlementHexes = brain.getOurPlayerData().getSettlements().stream()
                .map(SOCSettlement::getAdjacentHexes)
                .flatMap(Collection::stream);

        //get all hexes around a city
        Stream<Integer> cityHexes = brain.getOurPlayerData().getCities().stream()
                .map(SOCCity::getAdjacentHexes)
                .flatMap(Collection::stream);
        //double city hexes since cities give double
        cityHexes = Stream.concat(cityHexes, cityHexes);

        //create a map of resource type to the total probability
        return Stream.concat(settlementHexes, cityHexes)
                .collect(Collectors.groupingBy(board::getHexTypeFromCoord, Collectors.summingInt(board::getHexNumFromCoord)));
    }

}
