package bot.decision;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;

import java.util.Optional;

import static soc.robot.SOCPossiblePiece.*;
import static soc.robot.SOCPossiblePiece.CARD;

import soc.debug.D;
import bot.trade.Trading;

public class DefaultStrategy {
    public static boolean shouldUse() {
        return true;
    }

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;
        
        if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
        	D.ebugPrintln("----- Settlement & Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } 

	if (decisionTreeDM.getHelpers().canBuildSettlement()) {
        	D.ebugPrintln("Maybe Settlement");
        	if(decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
        		D.ebugPrintln("----- Settlement -----");
		    return possibleSettlement.get();
        	} else {
        		
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
        } else {
        	while(decisionTreeDM.getBrain().trade(new SOCPossibleRoad(null, -1, null))) {
        		continue;
        	}
        	
        	D.ebugPrintln("done trading");
        	
        	if(decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
        		D.ebugPrintln("----- Road -----");
                return decisionTreeDM.getHelpers().findQualityRoad(true).orElse(null);
        	}
        }
	
        if(decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
    	    D.ebugPrintln("----- City -----");
	    return possibleCity.get();
	} else if (decisionTreeDM.getHelpers().getPlayerResources().getTotal() > 5) {
	    while(decisionTreeDM.getBrain().trade(new SOCPossibleCity(null, -1))) {
		continue;
	    }
		    
	    D.ebugPrintln("done trading");
		    
	    if(decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
		D.ebugPrintln("----- City -----");
		return possibleCity.get();
	    }
	  }
        		
        if (decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
        	D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } else if (decisionTreeDM.getHelpers().getPlayerResources().getTotal() > 5) {
        	while(decisionTreeDM.getBrain().trade(new SOCPossibleCard(null, -1))) {
    			continue;
    		}
        	
        	D.ebugPrintln("done trading");
        	
        	if(decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
        	    D.ebugPrintln("----- Card -----");
		    return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
    		}
        }
        return null;
    }
}
