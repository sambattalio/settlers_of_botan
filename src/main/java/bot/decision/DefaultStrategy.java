package bot.decision;

import soc.game.*;

import soc.debug.D;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCRobotDM;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static soc.game.SOCResourceConstants.WOOD;
import static soc.game.SOCResourceConstants.CLAY;

import bot.NDHelpers;

import static soc.robot.SOCPossiblePiece.SETTLEMENT;
import static soc.robot.SOCPossiblePiece.ROAD;
import static soc.robot.SOCPossiblePiece.CITY;
import static soc.robot.SOCPossiblePiece.CARD;

import static soc.robot.SOCPossiblePiece.*;

public class DefaultStrategy {
    public static boolean shouldUse() {
        return true;
    }

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
    	D.ebugPrintln("----- Start Default Plan-----");
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;
        Optional<SOCPossiblePiece> possibleRoad;
        
        if (NDHelpers.haveResourcesForRoadAndSettlement(decisionTreeDM.getBrain())) {
            D.ebugPrintln("----- Road & Settlement -----");
            Optional<SOCPossiblePiece> result = NDHelpers.findQualityRoadForExpansion(decisionTreeDM.getBrain());
            //TODO add other checks like this
            if(result.isPresent()) {
            	return result.get();
			}
        }
        
        if (decisionTreeDM.getBrain().getAttempt(CITY) && (possibleCity = NDHelpers.findQualityCityFor(Arrays.asList(WOOD, CLAY), decisionTreeDM.getBrain())).isPresent()) {
        	D.ebugPrintln("----- City -----");
        	return possibleCity.get();
        }
        else {
        	D.ebugPrintln("No quality city");
        }
        

        if (decisionTreeDM.getBrain().getAttempt(SETTLEMENT) && NDHelpers.canBuildSettlement(decisionTreeDM.getPlayerNo(), decisionTreeDM.getBrain()) && (possibleSettlement = NDHelpers.findQualitySettlementFor(Arrays.asList(WOOD, CLAY), decisionTreeDM.getBrain())).isPresent()) {
        	D.ebugPrintln("----- Settlement -----");
		    return possibleSettlement.get();
        } else {
        	D.ebugPrintln("No settlement");
        }
        
        if(decisionTreeDM.getBrain().getAttempt(CITY) && (possibleRoad = NDHelpers.findQualityRoadForExpansion(decisionTreeDM.getBrain())).isPresent()) {
        	D.ebugPrintln("----- Road -----");
	        return possibleRoad.get();
        } else {
        	D.ebugPrintln("No Road");
        }
        
        if (decisionTreeDM.getBrain().getAttempt(CARD) && NDHelpers.getPlayerResources(decisionTreeDM.getBrain()).getTotal() > 5) {
        	D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } 
        
    	D.ebugPrintln("Reached Null");
    	
        return null;
        
    }
}
