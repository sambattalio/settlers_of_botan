package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCResourceSet;
import soc.robot.DiscardStrategy;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCRobotBrain;

import java.util.Random;
import java.util.Stack;

public class NDDiscardStrategy extends DiscardStrategy {
    /**
     * Create a DiscardStrategy for a {@link NDRobotBrain}'s player.
     *
     * @param ga   Our game
     * @param pl   Our player data in {@code ga}
     * @param br   Robot brain for {@code pl}
     * @param rand Random number generator from {@code br}
     */
    public NDDiscardStrategy(SOCGame ga, SOCPlayer pl, SOCRobotBrain br, Random rand) {
        super(ga, pl, br, rand);
    }

    @Override
    public SOCResourceSet discard(int numDiscards, Stack<SOCPossiblePiece> buildingPlan) {
        return super.discard(numDiscards, buildingPlan);
    }
}