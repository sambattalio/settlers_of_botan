package bot.decision;

import soc.game.*;

import soc.debug.D;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;

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

public class LongestRoadStrategy {

    public static boolean shouldUse(SOCGame game, SOCPlayer player) {
        return NDHelpers.isLongestRoadPossible(game, player.getPlayerNumber());
    };

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
    	D.ebugPrintln("----- Start Longest Road Plan-----");
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;

        //TODO Check if road is threatened before performing other actions
        if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
            D.ebugPrintln("----- Road & Settlement -----");
            return decisionTreeDM.getHelpers().findQualityRoadForLongestRoad().orElse(null);
        }

        if (decisionTreeDM.getHelpers().canBuildSettlement() && (possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlementFor(Arrays.asList(WOOD, CLAY))).isPresent()) {
        	D.ebugPrintln("Maybe Settlement");
        	if(decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
        	    D.ebugPrintln("----- Settlement -----");
		    return possibleSettlement.get();
        	} else {

	        	while(decisionTreeDM.getBrain().trade(new SOCPossibleSettlement(null, -1, null))){
	        		continue;
	        	}

			if(decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
                            D.ebugPrintln("----- Settlement -----");
                            return possibleSettlement.get();
                        }

			decisionTreeDM.getBrain().setFour(true);

			D.ebugPrintln("----- Fours Loop -----");
			while(decisionTreeDM.getBrain().trade(new SOCPossibleSettlement(null, -1, null))){
			    D.ebugPrintln("----- Attempting Fours -----");
                        }

			decisionTreeDM.getBrain().setFour(false);

	        	D.ebugPrintln("done trading");

	        	if(decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
	        	    D.ebugPrintln("----- Settlement -----");
			    return possibleSettlement.get();
	        	}
        	}
        } else {
	    D.ebugPrintln("No settlement");
	}

        if (decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
            D.ebugPrintln("----- Road -----");
            return decisionTreeDM.getHelpers().findQualityRoadForLongestRoad().orElse(null);
        } else {

        	while(decisionTreeDM.getBrain().trade(new SOCPossibleRoad(null, -1, null))) {
			D.ebugPrintln("Trading for Road");
        		continue;
        	}

		if(decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
                    D.ebugPrintln("----- Road -----");
                    SOCPossiblePiece road = decisionTreeDM.getHelpers().findQualityRoadForLongestRoad().orElse(null);
                    if(road == null) {
                         D.ebugPrintln("Null road");
                    }
                    return decisionTreeDM.getHelpers().findQualityRoadForLongestRoad().orElse(null);
                }

		decisionTreeDM.getBrain().setFour(true);

		D.ebugPrintln("----- Fours Loop -----");
                while(decisionTreeDM.getBrain().trade(new SOCPossibleRoad(null, -1, null))){
		    D.ebugPrintln("----- Attempting Fours -----");
                }

                decisionTreeDM.getBrain().setFour(false);

        	D.ebugPrintln("done trading");

        	if(decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
        	    D.ebugPrintln("----- Road -----");
		    SOCPossiblePiece road = decisionTreeDM.getHelpers().findQualityRoadForLongestRoad().orElse(null);
		    if(road == null) {
			 D.ebugPrintln("Null road");
		    }
		    return decisionTreeDM.getHelpers().findQualityRoadForLongestRoad().orElse(null);
        	}
        }


        if ((possibleCity = decisionTreeDM.getHelpers().findQualityCityFor(Arrays.asList(WOOD, CLAY))).isPresent()) {
        	D.ebugPrintln("Maybe City");
        	if(decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
        	    D.ebugPrintln("----- City -----");
		    return possibleCity.get();
        	} else if (decisionTreeDM.getHelpers().getPlayerResources().getTotal() > 5) {
		    while(decisionTreeDM.getBrain().trade(new SOCPossibleCity(null, -1))) {
			continue;
		    }

		    if(decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
                        D.ebugPrintln("----- City -----");
                        return possibleCity.get();
                    }

		    decisionTreeDM.getBrain().setFour(true);

		    D.ebugPrintln("----- Fours Loop -----");
                    while(decisionTreeDM.getBrain().trade(new SOCPossibleCity(null, -1))){
			D.ebugPrintln("----- Attempting Fours -----");
                    }

                    decisionTreeDM.getBrain().setFour(false);

		    D.ebugPrintln("done trading");

		    if(decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
			D.ebugPrintln("----- City -----");
			return possibleCity.get();
		    }
        	}
        } else {
	    D.ebugPrintln("No quality city");
	}

        if (decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
        	D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } else if (decisionTreeDM.getHelpers().getPlayerResources().getTotal() > 5) {
        	while(decisionTreeDM.getBrain().trade(new SOCPossibleCard(null, -1))) {
    			continue;
    		}

		if(decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
                    D.ebugPrintln("----- Card -----");
                    return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
                }

		decisionTreeDM.getBrain().setFour(true);

		D.ebugPrintln("----- Fours Loop -----");
                while(decisionTreeDM.getBrain().trade(new SOCPossibleCard(null, -1))){
                    D.ebugPrintln("----- Attempting Fours -----");
                }

                decisionTreeDM.getBrain().setFour(false);

        	D.ebugPrintln("done trading");

        	if(decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
        	    D.ebugPrintln("----- Card -----");
		    return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
    		}
        }

        D.ebugPrintln("Reached Null");

        return null;
    }
}
