package bot.decision;

import bot.NDHelpers;
import bot.trade.Trading;
import soc.debug.D;
import soc.game.*;
import soc.robot.*;
import bot.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecisionTreeDM extends SOCRobotDM {
	private NDRobotBrain brain;

	private Trading trades;

    int callCount = 0;

    public DecisionTreeDM(NDRobotBrain br) {
        super(br);

        trades = new Trading(br);
        brain = br;
    }

    @Override
    public void planStuff(int strategy) {
        D.ebugPrintln("----- Plan Stuff " + callCount++ + " -----");
        try {
            if (LongestRoadStrategy.shouldUse(game, brain.getOurPlayerData())) {
                addToPlan(LongestRoadStrategy.plan(this));
            } else if (LargestArmyStrategy.shouldUse(game, brain.getOurPlayerData())) {
                addToPlan(LargestArmyStrategy.plan(this));
            } else {
                addToPlan(DefaultStrategy.plan(this));
            }
        } catch(Exception e) {
            D.ebugPrintStackTrace(e, e.toString());
        }
    }

    public NDRobotBrain getBrain() {
        return brain;
    }

    public Trading getTrades() {
    	return trades;
    }

    protected void addToPlan(SOCPossiblePiece piece) {
        if (piece == null) return;
        this.buildingPlan.add(piece);
    }

    DecisionTreeHelpers getHelpers() {
        return new DecisionTreeHelpers();
    }

    public SOCPlayer getPlayer() {
        return brain.getOurPlayerData();
    }

    class DecisionTreeHelpers {

        private DecisionTreeHelpers() {

        }

        public ResourceSet getPlayerResources() {
            return brain.getOurPlayerData().getResources();
        }

        public boolean haveResourcesForRoadAndSettlement() {
            if(getPlayer().getPieces().stream().filter(piece -> piece instanceof SOCRoad).count() == SOCPlayer.ROAD_COUNT ||
                    getPlayer().getPieces().stream().filter(piece -> piece instanceof SOCSettlement).count() == SOCPlayer.SETTLEMENT_COUNT
            ) {
                return false;
            }
            ResourceSet set = brain.getOurPlayerData().getResources();
            return set.getAmount(SOCResourceConstants.CLAY) >= 2 &&
                    set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                    set.getAmount(SOCResourceConstants.WHEAT) >= 1 &&
                    set.getAmount(SOCResourceConstants.WOOD) >= 2;
        }

        public boolean haveResourcesFor(int type) {
            ResourceSet set = brain.getOurPlayerData().getResources();
            switch (type) {
                case SOCPossiblePiece.ROAD: {
                    if(getPlayer().getPieces().stream().filter(piece -> piece instanceof SOCRoad).count() == SOCPlayer.ROAD_COUNT) {
                        return false;
                    }
                    return set.getAmount(SOCResourceConstants.CLAY) >= 1 &&
                            set.getAmount(SOCResourceConstants.WOOD) >= 1;
                }
                case SOCPossiblePiece.SETTLEMENT: {
                    if(getPlayer().getPieces().stream().filter(piece -> piece instanceof SOCSettlement).count() == SOCPlayer.SETTLEMENT_COUNT) {
                        return false;
                    }
                    return set.getAmount(SOCResourceConstants.CLAY) >= 1 &&
                            set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                            set.getAmount(SOCResourceConstants.WHEAT) >= 1 &&
                            set.getAmount(SOCResourceConstants.WOOD) >= 1;
                }
                case SOCPossiblePiece.CITY: {
                    if(getPlayer().getPieces().stream().filter(piece -> piece instanceof SOCCity).count() == SOCPlayer.CITY_COUNT) {
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

        public boolean canBuildSettlement() {
            return NDHelpers.canBuildSettlement(game, brain.getOurPlayerData().getPlayerNumber());
        }

        public Optional<SOCPossibleSettlement> findQualitySettlementFor(List<Integer> resources) {
            D.ebugPrintln("Finding quality settlement");
            return Optional.ofNullable(NDHelpers.bestPossibleSettlement(game, brain.getOurPlayerData(), resources));
        }

        public Optional<SOCPossibleCity> findQualityCityFor(List<Integer> resources) {
            D.ebugPrintln("Finding quality city");
            Vector<SOCSettlement> ourSettlements = brain.getOurPlayerData().getSettlements();
            if (resources.isEmpty()) {
                return ourSettlements.stream()
                        .map(SOCPlayingPiece::getCoordinates)
                        .sorted(Comparator.comparing(node -> NDHelpers.totalProbabilityAtNode(game, node)))
                        .map(node -> new SOCPossibleCity(brain.getOurPlayerData(), node))
                        .findFirst();
            }
            return ourSettlements.stream()
                    .filter(settlement -> settlement.getAdjacentHexes().stream()
                            .anyMatch(hex -> resources.contains(game.getBoard().getHexTypeFromCoord(hex)))
                    )
                    .map(SOCPlayingPiece::getCoordinates)
                    .sorted(Comparator.comparing(node -> NDHelpers.totalProbabilityAtNode(game, node)))
                    .map(node -> new SOCPossibleCity(brain.getOurPlayerData(), node))
                    .findFirst();
        }

        public Optional<SOCPossibleSettlement> findQualitySettlement() {
            return findQualitySettlementFor(Collections.emptyList());
        }

        public Optional<SOCPossibleCity> findQualityCity() {
            return findQualityCityFor(Collections.emptyList());
        }

        public Optional<SOCPossiblePiece> findQualityRoadForLongestRoad() {
            D.ebugPrintln("Finding quality road for longest road");
            return Optional.ofNullable(NDHelpers.bestPossibleLongRoad(game, brain.getOurPlayerData()));
        }

        public Optional<SOCPossiblePiece> findQualityRoadForExpansion() {
            D.ebugPrintln("Finding quality road for expansion");
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
    }
}
