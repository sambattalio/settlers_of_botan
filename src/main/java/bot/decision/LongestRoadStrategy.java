package bot.decision;

import soc.game.*;

import soc.debug.D;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;

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

import bot.trade.Trading;

public class LongestRoadStrategy {

    public static boolean shouldUse(SOCGame game, SOCPlayer player) {
        return NDHelpers.isLongestRoadPossible(game, player.getPlayerNumber());
    };
    
    private boolean isThreatened;
    
    private boolean isAggressive;

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;
	
        Trading trades = decisionTreeDM.getTrades();
    
        //TODO Check if road is threatened before performing other actions
        if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
        	D.ebugPrintln("----- Road & Settlement -----");
            return decisionTreeDM.getHelpers().findQualityRoad(true).orElse(null);
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
            if (decisionTreeDM.getHelpers().canBuildSettlement() &&
                    (possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlementFor(Arrays.asList(WOOD, CLAY))).isPresent()) {
            	D.ebugPrintln("----- Settlement -----");
                return possibleSettlement.get();
            } else {
            	D.ebugPrintln("----- Bad Settlement -> Road -----");
                return decisionTreeDM.getHelpers().findQualityRoad(true).orElse(null);
            }
        } else if ((possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlementFor(Arrays.asList(WOOD, CLAY))).isPresent() && trades.attemptTradeOffer(SETTLEMENT)) {
        	D.ebugPrintln("----- Settlement -----");
        	return possibleSettlement.get();
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(ROAD) || trades.attemptTradeOffer(ROAD)) {
        	D.ebugPrintln("----- Road -----");
            return decisionTreeDM.getHelpers().findQualityRoad(true).orElse(null);
        } else if ((possibleCity = decisionTreeDM.getHelpers().findQualityCityFor(Arrays.asList(WOOD, CLAY))).isPresent() && 
        		(decisionTreeDM.getHelpers().haveResourcesFor(CITY) || ( trades.getNumberOfResources()  > 5 && trades.attemptTradeOffer(CITY)))) {
        	D.ebugPrintln("----- City -----");
            return possibleCity.get();
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CARD) || (trades.getNumberOfResources() > 5 && trades.attemptTradeOffer(CARD))) {
        	D.ebugPrintln("----- Card -----");
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        }
        return null;
    }
}
