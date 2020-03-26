package bot.decision;

import soc.game.SOCGame;
import soc.game.SOCPlayer;

import bot.NDHelpers;
import bot.trade.Trading;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;

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
        
        Trading trades = decisionTreeDM.getTrades();

        if ((possibleCity = decisionTreeDM.getHelpers().findQualityCityFor(Arrays.asList(WHEAT, ORE, SHEEP))).isPresent() && (decisionTreeDM.getHelpers().haveResourcesFor(CITY) || trades.attemptTradeOffer(CITY))) {
        	D.ebugPrintln("----- City -----");
        	return possibleCity.get();
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CARD) || trades.attemptTradeOffer(CARD)) {
        	D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } else if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
        	D.ebugPrintln("----- Settlement & Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
            if (decisionTreeDM.getHelpers().canBuildSettlement() &&
                    (possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlementFor(Arrays.asList(WHEAT, ORE, SHEEP))).isPresent()) {
            	D.ebugPrintln("----- Settlement -----");
                return possibleSettlement.get();
            } else {
            	D.ebugPrintln("----- Road -----");
                return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
            }
        } else if ((possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlementFor(Arrays.asList(WHEAT, ORE, SHEEP))).isPresent() && trades.getNumberOfResources() > 5 && trades.attemptTradeOffer(SETTLEMENT)) {
        	D.ebugPrintln("----- Settlement -----");
        	return possibleSettlement.get();
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(ROAD) || (trades.getNumberOfResources() > 5 && trades.attemptTradeOffer(ROAD))) {
        	D.ebugPrintln("----- Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        }

        return null;
    }
}
