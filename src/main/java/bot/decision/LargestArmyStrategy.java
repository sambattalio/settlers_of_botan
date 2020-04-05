package bot.decision;

import soc.game.SOCGame;
import soc.game.SOCPlayer;

import bot.NDHelpers;
import bot.trade.Trading;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;

import java.util.Arrays;
import java.util.Optional;

import static soc.game.SOCResourceConstants.*;
import static soc.robot.SOCPossiblePiece.*;
import static soc.robot.SOCPossiblePiece.CARD;

import soc.debug.D;

public class LargestArmyStrategy {
    public static boolean shouldUse(SOCGame game, SOCPlayer player) {
    	return NDHelpers.isCompetitiveForLargestArmy(game, player.getPlayerNumber());
    }

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;
        
        if ((possibleCity = decisionTreeDM.getHelpers().findQualityCityFor(Arrays.asList(WHEAT, ORE, SHEEP))).isPresent()) {
        	if(decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
		    D.ebugPrintln("----- City -----");
		    return possibleCity.get();
		} else {
        		
	        	while(decisionTreeDM.getBrain().trade(new SOCPossibleCity(null, -1))){
	        		continue;
	        	}
	        	
	        	D.ebugPrintln("done trading");
	        	
	        	if(decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
	        	    D.ebugPrintln("----- City -----");
			    return possibleCity.get();
	        	}
        	}
		
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
            D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } else {
	    while(decisionTreeDM.getBrain().trade(new SOCPossibleCard(null, -1))){
		continue;
            }

	    D.ebugPrintln("done trading");

	    if(decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
		D.ebugPrintln("----- Card -----");
		return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
	    }
	} 

	if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
            D.ebugPrintln("----- Settlement & Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } if (decisionTreeDM.getHelpers().canBuildSettlement() && (possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlementFor(Arrays.asList(WHEAT, ORE, SHEEP))).isPresent()) {
        	D.ebugPrintln("Maybe Settlement");
        	if(decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
        		D.ebugPrintln("----- Settlement -----");
                return possibleSettlement.get();
        	} else if(decisionTreeDM.getHelpers().getPlayerResources().getTotal() > 5){
        		
	        	while(decisionTreeDM.getBrain().trade(new SOCPossibleSettlement(null, -1, null))){
	        		continue;
	        	}
	        	
	        	D.ebugPrintln("done trading");
	        	
	        	if(decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
	        		D.ebugPrintln("----- Settlement -----");
	            	return possibleSettlement.get();
	        	}
        	}
        } 
        
        if (decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
            D.ebugPrintln("----- Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(true).orElse(null);
        } else if (decisionTreeDM.getHelpers().getPlayerResources().getTotal() > 5){
        	while(decisionTreeDM.getBrain().trade(new SOCPossibleRoad(null, -1, null))) {
        		continue;
        	}
        	
        	D.ebugPrintln("done trading");
        	
        	if(decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
        		D.ebugPrintln("----- Road -----");
                return decisionTreeDM.getHelpers().findQualityRoad(true).orElse(null);
        	}
        }
        return null;
    }
}
