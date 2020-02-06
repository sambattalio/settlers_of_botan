package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.RobberStrategy;
import soc.robot.SOCRobotBrain;

import java.util.Random;

public class NDRobberStrategy extends RobberStrategy {
    /**
     * Create a RobberStrategy for a {@link NDRobotBrain}'s player.
     *
     * @param ga   Our game
     * @param pl   Our player data in {@code ga}
     * @param br   Robot brain for {@code pl}
     * @param rand Random number generator from {@code br}
     */
    public NDRobberStrategy(SOCGame ga, SOCPlayer pl, SOCRobotBrain br, Random rand) {
        super(ga, pl, br, rand);
    }
}