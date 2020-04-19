package bot.decision;

import soc.debug.D;
import soc.robot.*;

import java.util.Optional;

import static soc.robot.SOCPossiblePiece.*;

public class DefaultStrategy {
    public static boolean shouldUse() {
        return true;
    }

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;
        if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
        	D.ebugPrintln("----- Settlement & Road -----");
            return decisionTreeDM.getHelpers().findQualityRoadForExpansion().orElse(null);
        }

        // check what port we got cookin' if any
        //boolean shouldPort = decisionTreeDM.getBrain();

	if (decisionTreeDM.getHelpers().canBuildSettlement()) {
        	D.ebugPrintln("Maybe Settlement");
		possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlement();
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
            return decisionTreeDM.getHelpers().findQualityRoadForExpansion().orElse(null);
        } else {
        	while(decisionTreeDM.getBrain().trade(new SOCPossibleRoad(null, -1, null))) {
        		continue;
        	}

        	D.ebugPrintln("done trading");

        	if(decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
        		D.ebugPrintln("----- Road -----");
                return decisionTreeDM.getHelpers().findQualityRoadForExpansion().orElse(null);
        	}
        }

	if((possibleCity = decisionTreeDM.getHelpers().findQualityCity()).isPresent()){
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
