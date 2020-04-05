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
    
    public NDRobotBrain getBrain() {
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
            Vector<SOCSettlement> ourSettlements = brain.getOurPlayerData().getSettlements();
            if(resources.isEmpty()) {
                return ourSettlements.stream()
                        .map(SOCPlayingPiece::getCoordinates)
                        .sorted(Comparator.comparing(node -> NDHelpers.totalProbabilityAtNode(game, node)))
                        .map(node -> new SOCPossibleCity(brain.getOurPlayerData(), node))
                        .findFirst();
            }
            return ourSettlements.stream()
                    .filter(settlement -> settlement.getAdjacentHexes().stream()
                            .anyMatch(hex -> resources.contains(brain.getGame().getBoard().getHexTypeFromCoord(hex)))
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

        public Optional<SOCPossiblePiece> findQualityRoad(boolean considerLongestRoad) {
            if (considerLongestRoad) return Optional.ofNullable(NDHelpers.bestPossibleLongRoad(brain.getGame(), brain.getOurPlayerData()));
            else return Optional.empty(); //TODO add quality road search based on resources like with settlements & cities
        }
    }
}
