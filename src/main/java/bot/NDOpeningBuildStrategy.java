package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.OpeningBuildStrategy;
import soc.robot.SOCRobotBrain;

public class NDOpeningBuildStrategy extends OpeningBuildStrategy {

    /**
     * Create an OpeningBuildStrategy for a {@link NDRobotBrain}'s player.
     *
     * @param ga Our game
     * @param pl Our player data in <tt>ga</tt>
     */
    public NDOpeningBuildStrategy(SOCGame ga, SOCPlayer pl) {
        super(ga, pl);
    }
}