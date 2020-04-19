package bot;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.RobberStrategy;
import soc.robot.SOCRobotBrain;
import soc.game.SOCPlayerNumbers;

import java.util.Random;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

public class NDRobberStrategy extends RobberStrategy {

    private List<Map.Entry<Integer, Integer>> sortedNodeProbabilities = null;

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

    /**
     * Finds player number of player we think is doing "Best"
     * @return player number of player in lead not us
     */
    SOCPlayer findTargetEnemy() {
        int highestVP = -1;
        SOCPlayer bestEnemy = null;
        for (SOCPlayer p : game.getPlayers()) {
            if (p.getPlayerNumber() == ourPlayerData.getPlayerNumber()) continue;
            if (p.getPublicVP() > highestVP) {
                highestVP = p.getPublicVP();
                bestEnemy = p;
            } 
        }
        return bestEnemy;
    }

    /**
     * Determine best hex coord to place robber
     * @return Best coordinate for robber move -- based on probability and who is affected
     */
    public int getBestRobberHex() { 
        int[] possibleHexes = game.getBoard().getLandHexCoords();

        int currentRobberLocation = game.getBoard().getRobberHex();

        if (sortedNodeProbabilities == null) setSortedNodes();

        SOCPlayer targetEnemy = findTargetEnemy();

        // loop through sorted descending node pairs finding "best"
        for (Map.Entry<Integer, Integer> nodePair : sortedNodeProbabilities) {
            int hex = nodePair.getKey();
            // check if same location
            if (hex == currentRobberLocation) continue; 
            // make sure our player wont be playing themselves
            if (!ourPlayerData.getNumbers().hasNoResourcesForHex(hex)) continue;

            if (!targetEnemy.getNumbers().hasNoResourcesForHex(hex))
                return hex;
        }
        // something went wrong if we are here...
        return -1;
    }

    public void setSortedNodes() {
        List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(NDOpeningBuildStrategy.probMap.entrySet());

        // Sort
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            public int compare(Map.Entry<Integer,Integer> e1, Map.Entry<Integer, Integer> e2) {
                return (e2.getValue()).compareTo(e1.getValue()); // descending
            }
        });
    
        sortedNodeProbabilities = list;
    }

    /**
     * Choose a person to steal from after placing robber
     * tries to pick player around hex that is highest VP
     * @param isVictim is a boolean array of players who can be stolen from
     * @param canChooseNone is some thing that needs to be in place to override this method
     * @return player number to steal from
     */
    public int chooseRobberVictim (final boolean[] isVictim, final boolean canChooseNone) {
        if (canChooseNone) return -1;

        int victim = -1;
        int highestVP = -1;
        for (int i = 0; i < game.maxPlayers; i++) {
            if (game.isSeatVacant(i) || !isVictim[i]) continue;
             if (victim < 0) {
                victim = i;
                highestVP = game.getPlayer(i).getPublicVP();
             } else {
                int vp = game.getPlayer(i).getPublicVP();
                if (vp > highestVP) {
                    victim = i;
                    highestVP = vp;
                }
             }
        }
        return victim;
    }
}
