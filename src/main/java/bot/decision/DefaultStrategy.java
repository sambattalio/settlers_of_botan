package bot.decision;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;

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
        
        Trading trades = decisionTreeDM.getTrades();
        
        if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
        	D.ebugPrintln("----- Settlement & Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
            if (decisionTreeDM.getHelpers().canBuildSettlement()) {
                possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlement();
                if (possibleSettlement.isPresent()) {
                	D.ebugPrintln("----- Settlement 1 -----");
                    return possibleSettlement.get();
                }
            } else {
                return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
            }
        } else if ((possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlement()).isPresent() && trades.attemptTradeOffer(SETTLEMENT)) {
        	D.ebugPrintln("----- Settlement 2 -----");
        	return possibleSettlement.get();
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(ROAD) || trades.attemptTradeOffer(ROAD)) {
        	D.ebugPrintln("----- Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
            possibleCity = decisionTreeDM.getHelpers().findQualityCity();
            if (possibleCity.isPresent()) {
            	D.ebugPrintln("----- City -----");
                return possibleCity.get();
            }
        } else if ((possibleCity = decisionTreeDM.getHelpers().findQualityCity()).isPresent() && trades.getNumberOfResources() > 5 && trades.attemptTradeOffer(SETTLEMENT)) {
        	D.ebugPrintln("----- Settlement 2 -----");
        	return possibleCity.get();
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CARD) || (trades.getNumberOfResources() > 5 && trades.attemptTradeOffer(CARD))) {
        	D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0); // try to get road cards
        }

        return null;
    }
}
