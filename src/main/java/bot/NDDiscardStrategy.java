package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.DiscardStrategy;
import soc.robot.SOCRobotBrain;

import java.util.Random;

public class NDDiscardStrategy extends DiscardStrategy {
    /**
     * Create a DiscardStrategy for a {@link SOCRobotBrain}'s player.
     *
     * @param ga   Our game
     * @param pl   Our player data in {@code ga}
     * @param br   Robot brain for {@code pl}
     * @param rand Random number generator from {@code br}
     */
    public NDDiscardStrategy(SOCGame ga, SOCPlayer pl, SOCRobotBrain br, Random rand) {
        super(ga, pl, br, rand);
    }
}