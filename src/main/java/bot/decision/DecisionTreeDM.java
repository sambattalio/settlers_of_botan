package bot.decision;

import bot.NDRobotDM;
import bot.NDHelpers;
import bot.trade.Trading;
import soc.game.*;
import soc.robot.*;
import bot.*;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecisionTreeDM extends SOCRobotDM {
	private NDRobotBrain brain;

	private Trading trades;
	
    public DecisionTreeDM(NDRobotBrain br) {
        super(br);
        
        trades = new Trading(br);
        brain = br;
    }

    @Override
    public void planStuff(int strategy) { 
        if( LongestRoadStrategy.shouldUse(brain.getGame(), brain.getOurPlayerData()) ) {
            addToPlan(LongestRoadStrategy.plan(this));
        } else if(LargestArmyStrategy.shouldUse(brain.getGame(), brain.getOurPlayerData())) {
            addToPlan(LargestArmyStrategy.plan(this));
        } else {
            addToPlan(DefaultStrategy.plan(this));
        }
    }
    
    public SOCRobotBrain getBrain() {
    	return brain;
    }
    
    public Trading getTrades() {
    	return trades;
    }

    protected void addToPlan(SOCPossiblePiece piece) {
        if(piece == null) return;
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
            ResourceSet set = brain.getOurPlayerData().getResources();
            return set.getAmount(SOCResourceConstants.CLAY) >= 2 &&
                    set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                    set.getAmount(SOCResourceConstants.WHEAT) >= 1 &&
                    set.getAmount(SOCResourceConstants.WOOD) >= 2;
        }

        public boolean haveResourcesFor(int type) {
            ResourceSet set = brain.getOurPlayerData().getResources();
            switch(type) {
                case SOCPossiblePiece.ROAD:
                    return set.getAmount(SOCResourceConstants.CLAY) >= 1 &&
                            set.getAmount(SOCResourceConstants.WOOD) >= 1;
                case SOCPossiblePiece.SETTLEMENT:
                    return set.getAmount(SOCResourceConstants.CLAY) >= 1 &&
                            set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                            set.getAmount(SOCResourceConstants.WHEAT) >= 1 &&
                            set.getAmount(SOCResourceConstants.WOOD) >= 1;
                case SOCPossiblePiece.CITY:
                    return set.getAmount(SOCResourceConstants.ORE) >= 3 &&
                            set.getAmount(SOCResourceConstants.WHEAT) >= 2;
                case SOCPossiblePiece.CARD:
                    return set.getAmount(SOCResourceConstants.ORE) >= 1 &&
                            set.getAmount(SOCResourceConstants.SHEEP) >= 1 &&
                            set.getAmount(SOCResourceConstants.WHEAT) >= 1;
            }
            return false;
        }

        public boolean canBuildSettlement() {
            return NDHelpers.canBuildSettlement(brain.getGame(), brain.getOurPlayerData().getPlayerNumber());
        }

        public Optional<SOCPossibleSettlement> findQualitySettlementFor(List<Integer> resources) {
            return Optional.ofNullable(NDHelpers.bestPossibleSettlement(brain.getGame(), brain.getOurPlayerData(), resources));
        }

        public Optional<SOCPossibleCity> findQualityCityFor(List<Integer> resources) {
        	//TODO Upgrade settlements to cities
            return Optional.empty();
        }

        public Optional<SOCPossibleSettlement> findQualitySettlement() {
            return findQualitySettlementFor(Collections.emptyList());
        }

        public Optional<SOCPossibleCity> findQualityCity() {
            return findQualityCityFor(Collections.emptyList());
        }

        public Optional<SOCPossiblePiece> findQualityRoad(boolean considerLongestRoad) {
            if (considerLongestRoad) return Optional.ofNullable(NDHelpers.bestPossibleLongRoad(brain.getGame(), brain.getOurPlayerData()));
            else return Optional.empty(); //TODO add quality road search based on resources like with settlements & cities
        }
    }
}

class Utility {

//    protected SOCPossibleSettlement findBestSettlement(DecisionBlackboard blackboard) {
//        Set<Integer> blockedNodes = Stream.concat(
//                    blackboard.getBoard().getSettlements().stream()
//                        .filter(settlement -> settlement.getPlayerNumber() != blackboard.getPlayerData().getPlayerNumber())
//                        .map(SOCPlayingPiece::getCoordinates),
//                    blackboard.getBoard().getSettlements().stream()
//                        .filter(settlement -> settlement.getPlayerNumber() == blackboard.getPlayerData().getPlayerNumber())
//                        .map(SOCPlayingPiece::getCoordinates)
//                )
//                .flatMap(coord -> Stream.concat(Stream.of(coord), blackboard.getBoard().getAdjacentNodesToNode(coord).stream()))
//                .collect(Collectors.toSet());
//
//        return blackboard.getPlayerData().getPieces().stream()
//                .filter(piece -> piece.getType() == SOCPlayingPiece.ROAD)
//                .map(SOCPlayingPiece::getCoordinates)
//                .flatMap(coordinate -> blackboard.getBoard().getAdjacentNodesToEdge(coordinate).stream())
//                .filter(blockedNodes::contains)
//                .max(Comparator.comparing(settlement -> NDRobotDM.totalProbabilityAtNode(blackboard.getGame(), settlement)))
//                .map(settlement -> new SOCPossibleSettlement(blackboard.getPlayerData(), settlement, null))
//                .orElse(null);
//    }
//
//    private List<SOCPlayingPiece> findBest(int startingNode, Set<SOCPlayingPiece> unusedRoads, Set<Integer> occupiedNodes, DecisionBlackboard blackboard) {
//        return blackboard.getBoard().getAdjacentEdgesToNode(startingNode).stream()
//                .filter(edge -> !occupiedNodes.contains(edge))
//                .map(edge -> {
//                    List<SOCPlayingPiece> best = new ArrayList<>();
//                    int nextNode = blackboard.getBoard().getAdjacentNodesToEdge(edge).stream()
//                            .filter(node -> node != startingNode)
//                            .findFirst()
//                            .orElseThrow(NullPointerException::new);
//                    if (unusedRoads.contains(edge)) {
//                        Set<SOCPlayingPiece> newUnusedRoads = new HashSet<>(unusedRoads);
//                        SOCPlayingPiece edgePiece = newUnusedRoads.stream()
//                                .filter(road -> road.getCoordinates() == edge)
//                                .findFirst()
//                                .orElse(null);
//                        newUnusedRoads.remove(edgePiece);
//                        best.add(edgePiece);
//                        if (!occupiedNodes.contains(nextNode)) {
//                            best.addAll(findBest(nextNode, newUnusedRoads, occupiedNodes, blackboard));
//                        }
//                    }
//
//                    return best;
//                }).max(Comparator.comparing(List::size))
//                .orElse(new ArrayList<>());
//    }
//
//
//    protected List<SOCPlayingPiece> findLongestRoad(DecisionBlackboard blackboard) {
//        Set<SOCPlayingPiece> currentRoads = blackboard.getPlayerData().getPieces().stream()
//                .filter(piece -> piece.getType() == SOCPlayingPiece.ROAD)
//                .collect(Collectors.toSet());
//
//        Set<Integer> blockedNodes = blackboard.getBoard().getSettlements().stream()
//                .filter(settlement -> settlement.getPlayerNumber() != blackboard.getPlayerData().getPlayerNumber())
//                .map(SOCPlayingPiece::getCoordinates)
//                .collect(Collectors.toSet());
//
//        Set<Integer> startingNodes = currentRoads.stream()
//                .map(SOCPlayingPiece::getCoordinates)
//                .flatMap(coordinate -> blackboard.getBoard().getAdjacentNodesToEdge(coordinate).stream())
//                .filter(blockedNodes::contains)
//                .collect(Collectors.toSet());
//
//        return startingNodes.stream()
//                .map(startingNode -> findBest(startingNode, currentRoads, blockedNodes, blackboard))
//                .max(Comparator.comparing(Collection::size))
//                .orElse(null);
//    }
//
//    protected SOCPossiblePiece findBestCity(DecisionBlackboard blackboard) {
//        Function<SOCPlayingPiece, Integer> f = settlement -> NDRobotDM.totalProbabilityAtNode(blackboard.getGame(), settlement.getCoordinates());
//        return blackboard.getPlayerData().getPieces().stream()
//                .filter(p -> p.getType() == SOCPlayingPiece.SETTLEMENT)
//                .max(Comparator.comparing(f))
//                .map(SOCPlayingPiece::getCoordinates)
//                .map(settlement -> new SOCPossibleCity(blackboard.getPlayerData(), settlement))
//                .orElse(null);
//    }
}