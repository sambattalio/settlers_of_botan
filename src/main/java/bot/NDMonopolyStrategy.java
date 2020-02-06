package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.MonopolyStrategy;

public class NDMonopolyStrategy extends MonopolyStrategy {

    /**
     * Create a MonopolyStrategy for a {@link NDRobotBrain}'s player.
     *
     * @param ga Our game
     * @param pl Our player data in {@code ga}
     */
    public NDMonopolyStrategy(SOCGame ga, SOCPlayer pl) {
        super(ga, pl);
    }
}