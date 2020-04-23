package bot.decision;

import soc.game.SOCGame;
import soc.game.SOCPlayer;

import bot.NDHelpers;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCRobotDM;

import java.util.Arrays;
import java.util.Optional;

import static soc.game.SOCResourceConstants.*;
import static soc.robot.SOCPossiblePiece.*;

import soc.debug.D;

public class LargestArmyStrategy {
    public static boolean shouldUse(SOCGame game, SOCPlayer player) {
    	//TODO add check for number of cards left
    	return (NDHelpers.isCompetitiveForLargestArmy(game, player.getPlayerNumber())) && !(NDHelpers.canSwitchFromLargestArmy(game, player.getPlayerNumber()));
    }

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
    	D.ebugPrintln("----- Start Largest Army Plan-----");
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;
        Optional<SOCPossiblePiece> possibleRoad;

        if (decisionTreeDM.getBrain().getAttempt(CITY) && (possibleCity = NDHelpers.findQualityCityFor(Arrays.asList(WOOD, CLAY), decisionTreeDM.getBrain())).isPresent()) {
        	D.ebugPrintln("----- City -----");
        	return possibleCity.get();
        } else {
        	//TODO this originally fired if there was no city that had the resources - was that right?
        	D.ebugPrintln("No quality city");
        }
        
        if ((NDHelpers.haveResourcesFor(CARD, decisionTreeDM.getBrain())) && NDHelpers.getPlayerResources(decisionTreeDM.getBrain()).getTotal() > 5) {
        	D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } else {
        	D.ebugPrintln("No card");
        }
        
        if (NDHelpers.haveResourcesForRoadAndSettlement(decisionTreeDM.getBrain())) {
            D.ebugPrintln("----- Road & Settlement -----");
            Optional<SOCPossiblePiece> result = NDHelpers.findQualityRoadForExpansion(decisionTreeDM.getBrain());
            //TODO add other checks like this
            if(result.isPresent()) {
            	return result.get();
			}
        }
	
        if (NDHelpers.canBuildSettlement(decisionTreeDM.getPlayerNo(), decisionTreeDM.getBrain()) && (possibleSettlement = NDHelpers.findQualitySettlementFor(Arrays.asList(WOOD, CLAY), decisionTreeDM.getBrain())).isPresent()) {
        	if(NDHelpers.haveResourcesFor(SETTLEMENT, decisionTreeDM.getBrain())) {
        	    D.ebugPrintln("----- Settlement -----");
		    	return possibleSettlement.get();
        	} else if( decisionTreeDM.getBrain().getAttempt(SETTLEMENT) &&  NDHelpers.getPlayerResources(decisionTreeDM.getBrain()).getTotal() > 5){
        		D.ebugPrintln("Trade for Settlement");
        		return possibleSettlement.get();
        	}
        } else {
        	D.ebugPrintln("No settlement");
        }
        
        if((possibleRoad = NDHelpers.findQualityRoadForExpansion(decisionTreeDM.getBrain())).isPresent()) {
	        if (NDHelpers.haveResourcesFor(ROAD, decisionTreeDM.getBrain())) {
	            D.ebugPrintln("----- Road -----");
	            return possibleRoad.get();
	        } else if(decisionTreeDM.getBrain().getAttempt(ROAD) &&  NDHelpers.getPlayerResources(decisionTreeDM.getBrain()).getTotal() > 5){
	        	D.ebugPrintln("Trade for Road");
        		return possibleRoad.get();
	        }
        } else {
        	D.ebugPrintln("No Road");
        }
        
        if(decisionTreeDM.getBrain().getAttempt(CARD)) {
        	D.ebugPrintln("Reached end - trade for card");
        	return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } else {
        	D.ebugPrintln("Return Null");
        	return null;
        }
    }
}
