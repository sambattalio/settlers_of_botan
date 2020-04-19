package bot.decision;

import soc.debug.D;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.SOCRobotBrain;

public enum DecisionTreeType {
    LONGEST_ROAD(),
    LARGEST_ARMY(),
    DEFAULT();

    static DecisionTreeType whichUse(SOCGame game, SOCPlayer playerData) {
        try {
            if (LongestRoadStrategy.shouldUse(game, playerData)) {
                return LONGEST_ROAD;
            } else if (LargestArmyStrategy.shouldUse(game, playerData)) {
                return LARGEST_ARMY;
            }
        } catch(Exception e) {
            D.ebugPrintStackTrace(e, e.toString());
        }
        return DEFAULT;
    }
}
