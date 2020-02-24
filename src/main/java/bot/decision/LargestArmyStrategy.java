package bot.decision;

import soc.game.SOCGame;
import soc.game.SOCPlayer;

import bot.NDHelpers;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;

import java.util.Arrays;
import java.util.Optional;

import static soc.game.SOCResourceConstants.*;
import static soc.robot.SOCPossiblePiece.*;
import static soc.robot.SOCPossiblePiece.CARD;

public class LargestArmyStrategy {
    public static boolean shouldUse(SOCGame game, SOCPlayer player) {
    	return NDHelpers.isCompetitiveForLargestArmy(game, player.getPlayerNumber());
    }

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;

        if (decisionTreeDM.getHelpers().haveResourcesFor(CITY) &&
                (possibleCity = decisionTreeDM.getHelpers().findQualityCityFor(Arrays.asList(WHEAT, ORE, SHEEP))).isPresent()) {
            return possibleCity.get();
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0);
        } else if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
            if (decisionTreeDM.getHelpers().canBuildSettlement() &&
                    (possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlementFor(Arrays.asList(WHEAT, ORE, SHEEP))).isPresent()) {
                return possibleSettlement.get();
            } else {
                return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
            }
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        }

        return null;
    }
}
