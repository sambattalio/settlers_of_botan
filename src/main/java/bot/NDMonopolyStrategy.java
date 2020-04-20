package bot;

import soc.debug.D;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.MonopolyStrategy;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;

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

    /**
     * Grab most frequent resource
     * @return resource constant denoting which to steal from everybody
     */
    public int getMonopolyChoice() {
        SOCResourceSet resCounts = new SOCResourceSet(0,0,0,0,0,0);
        // add counts to set
        for (int p = 0; p < game.maxPlayers; p++) {
            if (p == ourPlayerData.getPlayerNumber()) continue;
            resCounts.add(game.getPlayer(p).getResources());
        }
        
        // loop over resource types and find best
        int bestRes = -1;
        int mostRes = -1;
        for (int i = SOCResourceConstants.CLAY; i < SOCResourceConstants.WOOD; i++) {
            if (resCounts.getAmount(i) > mostRes) {
                bestRes = i;
                mostRes = i;
            }
        }
        D.ebugPrintln("Picking resource enum value " + String.valueOf(bestRes) + " for monopoly");
        return bestRes;
    }
}
