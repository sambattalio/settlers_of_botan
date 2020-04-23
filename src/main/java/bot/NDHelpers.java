package bot;

import bot.decision.StrategyConstants;
import soc.debug.D;
import soc.game.*;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCRobotBrain;

import soc.robot.SOCPossibleCity;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


import static java.lang.Math.abs;

public class NDHelpers {

    static final SOCResourceSet ROAD_SET = new SOCResourceSet(1, 0, 0, 0, 1, 0);
    static final SOCResourceSet DEVEL_SET = new SOCResourceSet(0, 1, 1, 1, 0, 0);
    private static SOCBoard totalProbabilityAtNodeBoard;
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
        
        return abs(ndBot.getLongestRoadLength() - bestPlayer.getLongestRoadLength()) <= StrategyConstants.MAX_ROAD_DIFF;
    }
    
    /* Returns true if we have longest road and are ahead of everyone else by 2 */
    public static boolean canSwitchFromLongestRoad(SOCGame game, int playerNo) {
        SOCPlayer bestPlayer = game.getPlayerWithLongestRoad();
        if(bestPlayer == null) {
        	return false;
        }

        if (bestPlayer.getPlayerNumber() == playerNo) {
        	int secondBest = -1;
    		SOCPlayer secondBestPlayer = null;
    		
        	for (int i = 0; i < game.maxPlayers; i++) {
        		SOCPlayer p = game.getPlayer(i);
        		if(p != bestPlayer && p.getLongestRoadLength() > secondBest) {
    				secondBest = p.getLongestRoadLength();
    				secondBestPlayer = p;
    			}
        	}
    		
        	return (bestPlayer.getRoadsAndShips().size() >= 15) || ((bestPlayer.getLongestRoadLength() >= 5) && ((bestPlayer.getLongestRoadLength() - secondBestPlayer.getLongestRoadLength()) >= StrategyConstants.ROAD_SWITCH));
        } 
        
        return false;
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

        return game.getNumDevCards() != 0 && abs(ndBot.getNumKnights() - bestPlayer.getNumKnights()) <= StrategyConstants.MAX_ARMY_DIFF;
    }
    
    /**
     * Returns if playerNo is ahead in largest army.
     *
     * @param game
     * @param playerNo
     * @return true if army count - best army count < MAX_ARMY_DIFF
     */
    public static boolean canSwitchFromLargestArmy(SOCGame game, int playerNo) {
        SOCPlayer bestPlayer = game.getPlayerWithLargestArmy();
        if(bestPlayer == null) {
        	return false;
        }
 
        if (bestPlayer.getPlayerNumber() == playerNo) {
        	int secondBest = -1;
    		SOCPlayer secondBestPlayer = null;
    		
        	for (int i = 0; i < game.maxPlayers; i++) {
        		SOCPlayer p = game.getPlayer(i);
        		if(p != bestPlayer && p.getNumKnights() > secondBest) {
    				secondBest = p.getNumKnights();
    				secondBestPlayer = p;
    			}
        	}
    		
    		return (game.getNumDevCards() == 0) || ((bestPlayer.getNumKnights() > 3) && (bestPlayer.getNumKnights() - secondBestPlayer.getNumKnights() >= StrategyConstants.ARMY_SWITCH));
        }
        
        return false;
    }

    /**
     * Returns if a player can build a settlement
     *
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
    public static List<Integer> findPotentialSettlementsFor(SOCGame game, int playerNo, List<Integer> resources) {
        if (resources.isEmpty()) {
            return new Vector<>(game.getPlayer(playerNo).getPotentialSettlements());
        }

        return game.getPlayer(playerNo).getPotentialSettlements().stream()
            .filter(node -> game.getBoard().getAdjacentHexesToNode(node).stream()
                .anyMatch(hex -> resources.contains(game.getBoard().getHexTypeFromCoord(hex)))
            ).collect(Collectors.toList());

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

        List<Integer> possibleNodes = findPotentialSettlementsFor(game, playerNo, resources);
        if(possibleNodes.isEmpty()) {
            possibleNodes = findPotentialSettlementsFor(game, playerNo, Collections.emptyList());
        }
        Optional<Integer> bestNode = possibleNodes.stream().max(Comparator.comparing(totalProbabilityAtNodeMapping(game.getBoard())));

        return bestNode.map(integer -> new SOCPossibleSettlement(player, integer, null)).orElse(null); //TODO add potential road list
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
     * Returns coords of best n possible road to place to maximize length
     *
     * @param game
     * @param player
     * @return Pair of best n roads
     */
    public static List<SOCPossiblePiece> bestPossibleLongRoad(SOCGame game, SOCPlayer player, int len) {
        // TODO refactor

        //check if the roads of our first settlement can connect to the roads of our second settlement
        final SOCBoard board = game.getBoard();
        Map<Boolean, Set<SOCPlayingPiece>> byOwner = new HashSet<>(board.getRoadsAndShips()).stream()
                .collect(Collectors.partitioningBy(road -> road.getPlayer().equals(player), Collectors.toSet()));
        Set<Integer> allRoads = board.getRoadsAndShips().stream()
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        Set<Integer> allSettlements = Stream.concat(board.getSettlements().stream(), board.getCities().stream())
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        Set<Integer> otherSettlements = Stream.concat(board.getSettlements().stream(), board.getCities().stream())
                .filter(settlement -> settlement.getPlayer() != player)
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        List<Set<Integer>> connectedNodes = getConnectedNodes(board, player);

        player.calcLongestRoad2();
        Vector<SOCLRPathData> pathData = player.getLRPaths();

        List<Integer> bestSoFar = new ArrayList<>();

        if (connectedNodes.size() == 2) {
            Optional<List<Integer>> connection = getBestConnection(
                    Arrays.asList(pathData.get(0).getBeginning(), pathData.get(0).getEnd()),
                    Arrays.asList(pathData.get(1).getBeginning(), pathData.get(1).getEnd()),
                    board,
                    allRoads,
                    otherSettlements,
                    StrategyConstants.MAX_DISTANCE_FOR_CONNECTION
            );

            if(connection.isPresent()) {
                D.ebugPrintln("Found a connection off of the longest road");
            } else {
                connection = getBestConnection(
                        connectedNodes.get(0),
                        connectedNodes.get(1),
                        board,
                        allRoads,
                        otherSettlements,
                        StrategyConstants.MAX_DISTANCE_FOR_CONNECTION
                );
                if(connection.isPresent()) {
                    D.ebugPrintln("Found a connection not off the longest road");
                }
            }

            if(connection.isPresent()) {
                //TODO compare nodes start to end vs end to start
                //TODO not enough in connections
                bestSoFar.addAll(connection.get());
            }
        }

        D.ebugPrintln("LRs was " + pathData.size());
        D.ebugPrintln("LR lengths were " + pathData.stream().map(SOCLRPathData::getLength).collect(Collectors.toList()));

        List<Integer> startNodes = Arrays.asList(pathData.get(0).getBeginning(), pathData.get(0).getEnd());

        if(bestSoFar.size() < len) {
            List<Integer> destinations = allSettlements.stream()
                    .flatMap(node -> board.getAdjacentNodesToNode(node).stream()
                            .map(board::getAdjacentNodesToNode)
                            .flatMap(Collection::stream)
                            .map(board::getAdjacentNodesToNode)
                            .flatMap(Collection::stream)
                    ).filter(node ->
                            !allSettlements.contains(node) &&
                                    board.getAdjacentNodesToNode(node).stream().noneMatch(allSettlements::contains))
                    .collect(Collectors.toList());


            //TODO how to get to nearest best - prioritize by 2 away not nearest frontier
            Optional<List<Integer>> pathToNearestPossibleSettlementOffLongest = getBestConnection(
                    startNodes,
                    destinations,
                    board,
                    allRoads,
                    otherSettlements,
                    StrategyConstants.MAX_DISTANCE_FOR_CONNECTION
            );
            if(pathToNearestPossibleSettlementOffLongest.isPresent() && pathToNearestPossibleSettlementOffLongest.get().size() > 0) {
                bestSoFar.addAll(pathToNearestPossibleSettlementOffLongest.get());
                D.ebugPrintln("Found extension to longest road towards nearest settlement");
            }
            //TODO add loop?
        }

        if(bestSoFar.size() < len) {
            //TODO actually try to branch longer
            List<Integer> extensions = Stream.of(pathData.get(0).getBeginning(), pathData.get(0).getEnd())
                    .flatMap(node -> board.getAdjacentEdgesToNode(node).stream())
                    .filter(edge -> !allRoads.contains(edge))
                    .filter(player::isLegalRoad)
                    .collect(Collectors.toList());
            if(extensions.size() > 0) {
                D.ebugPrintln("Found extension to longest road");
                bestSoFar.addAll(extensions);
            }
        }

        return bestSoFar.stream()
                .map(edge -> new SOCPossibleRoad(player, edge, null))
                .collect(Collectors.toList());
    }

    //TODO

    private static Optional<List<Integer>> getBestConnection(Collection<Integer> startNodes, Collection<Integer> endNodes, SOCBoard board, Set<Integer> forbiddenRoads, Set<Integer> forbiddenNodes, int maxDepth) {
        TreeSet<Integer> frontier = new TreeSet<>(startNodes);
        Map<Integer, Integer> parentNode = new HashMap<>();
        endNodes.removeAll(startNodes);
        int depth = 0;
        List<Integer> foundEndNodes = new ArrayList<>();
        while(frontier.size() > 0 && depth < maxDepth && foundEndNodes.isEmpty()) {
            depth++;
            TreeSet<Integer> newFrontier = new TreeSet<>(Comparator.comparing(node -> board.getAdjacentHexesToNode(node).stream()
                .mapToInt(board::getHexNumFromCoord)
                .sum()
            ));
            //TODO sort frontier better - monotonic?
            for(int node : frontier) {
                if(endNodes.contains(node)) {
                    foundEndNodes.add(node);
                    continue;
                }
                for(int childNode : board.getAdjacentNodesToNode(node)) {
                    if(
                            forbiddenNodes.contains(childNode)
                            || forbiddenRoads.contains(board.getEdgeBetweenAdjacentNodes(node, childNode))
                    ) {
                        continue;
                    }
                    if(parentNode.get(childNode) == null) {
                        parentNode.put(childNode, node);
                        newFrontier.add(childNode);
                    }
                }
            }
            frontier = newFrontier;
        }
        
        if(foundEndNodes.isEmpty()) {
            return Optional.empty();
        }
        int currentNode = foundEndNodes.stream()
                .max(Comparator.comparing(NDHelpers.totalProbabilityAtNodeMapping(board)))
                .orElseThrow(IllegalStateException::new);
        LinkedList<Integer> backtrack = new LinkedList<>();
        while(!startNodes.contains(currentNode)) {
            backtrack.addFirst(board.getEdgeBetweenAdjacentNodes(currentNode, parentNode.get(currentNode)));
            currentNode = parentNode.get(currentNode);
        }
        return Optional.of(backtrack);
    }

    private static List<Set<Integer>> getConnectedNodes(SOCBoard board, SOCPlayer player) {
        return Stream.concat(player.getSettlements().stream(), player.getCities().stream())
                .map(SOCPlayingPiece::getCoordinates)
                .map(coord -> getAllNodesConnected(coord, board, player))
                .distinct()
                .collect(Collectors.toList());
    }

    private static Set<Integer> getAllNodesConnected(int nodeCoord, SOCBoard board, SOCPlayer player) {
        Set<Integer> connected = new HashSet<>();
        TreeSet<Integer> frontier = board.getAdjacentEdgesToNode(nodeCoord).stream()
                .filter(edge -> player.getRoadsAndShips().stream().anyMatch(road -> road.getCoordinates() == edge))
                .collect(Collectors.toCollection(TreeSet::new));
        while(!frontier.isEmpty()) {
            Integer consideredEdge = frontier.first();
            frontier.remove(consideredEdge);
            connected.add(consideredEdge);
            frontier.addAll(
                    board.getAdjacentEdgesToEdge(consideredEdge).stream()
                            .filter(edge -> player.getRoadsAndShips().stream().anyMatch(road -> road.getCoordinates() == edge))
                            .filter(edge -> !connected.contains(edge) && !frontier.contains(edge))
                            .collect(Collectors.toSet())
            );
        }
        return connected.stream()
                .map(board::getAdjacentNodesToEdge)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
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
//    public static int distanceBetweenNodes(SOCGame game, SOCPlayer player, int start, int end) {
//    }

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

    public static int totalProbabilityOfHex(SOCBoard board, final int hexCoord) {
        int diceNumber = board.getNumberOnHexFromCoord(hexCoord);
        if(1 <= diceNumber && diceNumber <= 12) {
            return(diceNumber > 7) ? 13 - diceNumber : diceNumber - 1;
        }
        return 0;
    }

    /**
     * Returns the sum of the probabilities of the tiles surrounding a node
     * Memoizes since the value will not change
     *
     * @param board      the game board
     * @param board
     * @param nodeCoord the node to check
     * @return the total probability
     */
    public static int totalProbabilityAtNode(SOCBoard board, final int nodeCoord) {
        if (totalProbabilityAtNodeBoard != board) {
            totalProbabilityAtNodeCache = new HashMap<>();
            totalProbabilityAtNodeBoard = board;
        }
        if (!totalProbabilityAtNodeCache.containsKey(nodeCoord)) {
            int sum = board.getAdjacentHexesToNode(nodeCoord).stream()
                    .mapToInt(hexNode -> totalProbabilityOfHex(board, hexNode))
                    .sum();
            totalProbabilityAtNodeCache.put(nodeCoord, sum);
        }
        return totalProbabilityAtNodeCache.get(nodeCoord);
    }

    public static Function<Integer, Integer> totalProbabilityAtNodeMapping(SOCBoard board) {
        return node -> totalProbabilityAtNode(board, node);
    }
    
    public static ResourceSet getPlayerResources(NDRobotBrain brain) {
        return brain.getOurPlayerData().getResources();
    }

    public static boolean haveResourcesForRoadAndSettlement(NDRobotBrain brain) {
        if(brain.getOurPlayerData().getPieces().stream()
                .filter(piece -> piece instanceof SOCRoad)
                .count() == SOCPlayer.ROAD_COUNT
            || brain.getOurPlayerData().getPieces().stream()
                .filter(piece -> piece instanceof SOCSettlement)
                .count() == SOCPlayer.SETTLEMENT_COUNT
        ) {
            return false;
        }
        ResourceSet set = brain.getOurPlayerData().getResources();
        SOCResourceSet needed = getResourcesFor(SOCPossiblePiece.SETTLEMENT);
        needed.add(getResourcesFor(SOCPossiblePiece.ROAD));
        return set.contains(needed);
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
        return set.contains(getResourcesFor(type));
    }

    public static Optional<SOCPossibleSettlement> findQualitySettlementFor(List<Integer> resources, NDRobotBrain brain) {
        D.ebugPrintln("Finding quality settlement");
        return Optional.ofNullable(bestPossibleSettlement(brain.getGame(), brain.getOurPlayerData(), resources));
    }

    public static Optional<SOCPossibleCity> findQualityCityFor(List<Integer> resources, NDRobotBrain brain) {
        D.ebugPrintln("Finding quality city");
        SOCGame game = brain.getGame();
        Vector<SOCSettlement> ourSettlements = brain.getOurPlayerData().getSettlements();
        if (!resources.isEmpty()) {
            Optional<SOCPossibleCity> withResources = ourSettlements.stream()
                .filter(settlement -> settlement.getAdjacentHexes().stream()
                        .anyMatch(hex -> resources.contains(game.getBoard().getHexTypeFromCoord(hex)))
                )
                .map(SOCPlayingPiece::getCoordinates)
                .sorted(Comparator.comparing(totalProbabilityAtNodeMapping(game.getBoard())).reversed())
                .map(node -> new SOCPossibleCity(brain.getOurPlayerData(), node))
                .findFirst();
            if(withResources.isPresent()) {
                D.ebugPrintln("Found a city yielding a resource we want");
                return withResources;
            }
        }
        D.ebugPrintln("Looking for any city");
        return ourSettlements.stream()
                .map(SOCPlayingPiece::getCoordinates)
                .sorted(Comparator.comparing(totalProbabilityAtNodeMapping(game.getBoard())).reversed())
                .map(node -> new SOCPossibleCity(brain.getOurPlayerData(), node))
                .findFirst();
    }

    public static Optional<SOCPossibleSettlement> findQualitySettlement(NDRobotBrain brain) {
        return findQualitySettlementFor(Collections.emptyList(), brain);
    }

    public static Optional<SOCPossibleCity> findQualityCity(NDRobotBrain brain) {
        return findQualityCityFor(Collections.emptyList(), brain);
    }

    /**
     * Returns coord of best possible road to place to maximize length
     *
     * @return best road to build
     */
    public static Optional<SOCPossiblePiece> findQualityRoadForLongestRoad(NDRobotBrain brain) {
        D.ebugPrintln("Finding quality road for longest road");
        List<SOCPossiblePiece> roads = bestPossibleLongRoad(brain.getGame(), brain.getOurPlayerData(), 1);
        if(roads.size() > 0) {
            return Optional.of(roads.get(0));
        }
        return Optional.empty();
    }

    public static Optional<SOCPossiblePiece> findQualityRoadForExpansion(NDRobotBrain brain) {
        D.ebugPrintln("Finding quality road for expansion");
        SOCBoard board = brain.getGame().getBoard();
        SOCPlayer player = brain.getOurPlayerData();

        Set<Integer> ourNodes = getConnectedNodes(board, player).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Set<Integer> allRoads = board.getRoadsAndShips().stream()
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        Set<Integer> allSettlements = Stream.concat(board.getSettlements().stream(), board.getCities().stream())
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());
        Set<Integer> otherSettlements = Stream.concat(board.getSettlements().stream(), board.getCities().stream())
                .filter(settlement -> settlement.getPlayer() != player)
                .map(SOCPlayingPiece::getCoordinates)
                .collect(Collectors.toSet());

        List<Integer> destinations = allSettlements.stream()
                .flatMap(node -> board.getAdjacentNodesToNode(node).stream()
                        .map(board::getAdjacentNodesToNode)
                        .flatMap(Collection::stream)
                        .map(board::getAdjacentNodesToNode)
                        .flatMap(Collection::stream)
                ).filter(node ->
                        !allSettlements.contains(node) &&
                                board.getAdjacentNodesToNode(node).stream().noneMatch(allSettlements::contains))
                .collect(Collectors.toList());


        //TODO how to get to nearest best - prioritize by 2 away not nearest frontier
        Optional<List<Integer>> pathToNearestPossibleSettlementOffLongest = getBestConnection(
                ourNodes,
                destinations,
                board,
                allRoads,
                otherSettlements,
                StrategyConstants.MAX_DISTANCE_FOR_EXPANSION
        );
        if(pathToNearestPossibleSettlementOffLongest.isPresent()) {
            D.ebugPrintln("Found road towards near possible settlement");
            return Optional.of(pathToNearestPossibleSettlementOffLongest.get().get(0))
                    .map(edge -> new SOCPossibleRoad(player, edge, null));
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
        //get settlement nodes
        Stream<Integer> settlementNodes = brain.getOurPlayerData().getSettlements().stream()
                .map(SOCSettlement::getCoordinates);

        //get city nodes
        Stream<Integer> cityNodes = brain.getOurPlayerData().getCities().stream()
                .map(SOCCity::getCoordinates);
        //double city hexes since cities give double
        cityNodes = cityNodes.flatMap(coord -> Stream.generate(() -> coord).limit(2));

        //create a map of resource type to the total probability
        Map<Integer, Integer> result = Stream.concat(settlementNodes, cityNodes)
                .map(board::getAdjacentHexesToNode)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(board::getHexTypeFromCoord, Collectors.summingInt(hexNode -> totalProbabilityOfHex(board, hexNode))));
        IntStream.range(SOCResourceConstants.MIN, SOCResourceConstants.MAXPLUSONE - 1)
            .forEach(type -> result.putIfAbsent(type, 0));
        return result;
    }
    
    public static int getApparentScore(SOCPlayer p) {
    	return p.getPublicVP() + p.getInventory().getNumVPItems();
    }

}
