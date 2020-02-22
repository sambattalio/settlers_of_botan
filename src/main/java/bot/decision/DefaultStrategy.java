package bot.decision;

import soc.robot.SOCPossibleCard;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleSettlement;

import java.util.Optional;

import static soc.robot.SOCPossiblePiece.*;
import static soc.robot.SOCPossiblePiece.CARD;

public class DefaultStrategy {
    public static boolean shouldUse() {
        return true;
    }

    public static SOCPossiblePiece plan(DecisionTreeDM decisionTreeDM) {
        Optional<SOCPossibleSettlement> possibleSettlement;
        Optional<SOCPossibleCity> possibleCity;
        if (decisionTreeDM.getHelpers().haveResourcesForRoadAndSettlement()) {
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(SETTLEMENT)) {
            if (decisionTreeDM.getHelpers().canBuildSettlement()) {
                possibleSettlement = decisionTreeDM.getHelpers().findQualitySettlement();
                if (possibleSettlement.isPresent()) {
                    return possibleSettlement.get();
                }
            } else {
                return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
            }
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(ROAD)) {
            return decisionTreeDM.getHelpers().findQualityRoad(false).orElse(null);
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CITY)) {
            possibleCity = decisionTreeDM.getHelpers().findQualityCity();
            if (possibleCity.isPresent()) {
                return possibleCity.get();
            }
        } else if (decisionTreeDM.getHelpers().haveResourcesFor(CARD)) {
            return new SOCPossibleCard(decisionTreeDM.getPlayer(), 0); // try to get road cards
        }

        return null;
    }
}
