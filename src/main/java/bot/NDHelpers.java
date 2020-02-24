package bot;

import soc.game.*;
import soc.robot.*;
import soc.debug.D;

import java.util.*;

import static java.lang.Math.abs;

public class NDHelpers {
    
    static final int MAX_ROAD_DIFF = 3;
    static final int MAX_ARMY_DIFF = 2;

    /* Returns true if input player has longest or is within 3 roads */
    public static boolean isCompetitiveForLongestRoad(SOCGame game, int playerNo) {
        SOCPlayer bestPlayer = game.getPlayerWithLongestRoad();

        /* Of course i know him... he's me */
        if (bestPlayer.getPlayerNumber() == playerNo) return true;

        /* Check diff in length */
        SOCPlayer ndBot = game.getPlayer(playerNo);
        return isLongestRoadPossible(game, playerNo) && abs(ndBot.getLongestRoadLength() - bestPlayer.getLongestRoadLength()) <= MAX_ROAD_DIFF;
    }
  
    /**
     * Returns if playerNo is deemed competitive for largest army.
     *
     * @param game
     * @param playerNo
     * @return true if army count - best army count < MAX_ARMY_DIFF
     */
    public static boolean isCompetitiveForLargestArmy(SOCGame game, int playerNo) {
        SOCPlayer bestPlayer = game.getPlayerWithLargestArmy();
        if (bestPlayer.getPlayerNumber() == playerNo) return true;

        /* Check diff in length */
        SOCPlayer ndBot = game.getPlayer(playerNo);
        return game.getNumDevCards() != 0 && abs(ndBot.getNumKnights() - bestPlayer.getNumKnights()) <= MAX_ARMY_DIFF;
    }

    /**
     * Returns if a player can afford to build settlement, given a resourceset
     * 
     * @param resources
     * @return true if can afford else false
     */
    public static boolean canAffordSettlement(SOCResourceSet resources) {
        return (resources.contains(SOCResourceConstants.CLAY) && resources.contains(SOCResourceConstants.WOOD)
             && resources.contains(SOCResourceConstants.WHEAT) && resources.contains(SOCResourceConstants.SHEEP));
    }

    /**
     * Returns if a player can build a settlement
     *
     * @param game
     * @param playerNo
     * @return true if possible to build a settlement
     */
    public static boolean canBuildSettlement(SOCGame game, int playerNo) {
         return game.getPlayer(playerNo).getPotentialSettlements().size() != 0;
    }


    /**
     * Returns if there exists a settlement that will yield the resources in the set
     *
     * TODO maybe ports too?
     * Right now only handles arrays up to size 3
     *
     * @param game
     * @param playerNo
     * @param resources: int array where integers are SOCResourceConstant resource types
     * @return if there is a settlement that can yield these resources
     */
    public static boolean existsQualitySettlementFor(SOCGame game, int playerNo, int[] resources) {
        return !findPotentialSettlementsFor(game, playerNo, resources).isEmpty();       
    }

     /**
     * Returns coords of settlements that will yield the resources in the set
     *
     * TODO maybe ports too?
     * Right now only handles arrays up to size 3
     *
     * @param game
     * @param playerNo
     * @param resources: int array where integers are SOCResourceConstant resource types
     * @return coords vector 
     */
    public static Vector<Integer> findPotentialSettlementsFor(SOCGame game, int playerNo, int[] r) {
        
        Vector<Integer> nodes = new Vector<Integer>();

        for (int node : game.getPlayer(playerNo).getPotentialSettlements_arr()) {
            Set<Integer> resources = new HashSet<>();//(Arrays.asList(r));
            for (int i : r) {
                resources.add(i);
            }
            for (int hex : game.getBoard().getAdjacentHexesToNode(node)) {
               resources.remove(game.getBoard().getHexTypeFromCoord(hex));
            }
            if (resources.isEmpty()) nodes.add(node);
        }

        return nodes;
    }


     /**
     * Returns *coord* of the best possible settlement for given resources
     *
     * @param board
     * @param playerNo
     * @param resources
     *
     * @return coord of best settlement
     *
     */
    public static int bestPossibleSettlement(SOCGame game, int playerNo, int[] resources) {
        Vector<Integer> possible_nodes = findPotentialSettlementsFor(game, playerNo, resources);
        
        int best_node = possible_nodes.indexOf(0);

        for (int i = 1; i < possible_nodes.size(); i++) {
            if (compareSettlementOptions(best_node, possible_nodes.indexOf(i), game) < 0) {
                best_node = possible_nodes.indexOf(i); 
            }
        }
        
        return best_node;
    }


    /**
     * Finds possible roads that can be built from given coord
     *
     * @param game
     * @param edgeCoord edge to build off of
     * @return Vector of coords
     */
    public static Vector<Integer> findPossibleRoads(SOCGame game, final int edgeCoord) {
        
        Vector<Integer> possibleRoads = new Vector<Integer>();
        
        for (int edge : game.getBoard().getAdjacentEdgesToEdge(edgeCoord)) {
            if (NDRobotDM.canBuildRoad(game, edge, edgeCoord)) {
                possibleRoads.add(edge);
            } 
        }

        return possibleRoads;
    }

    /**
     * Returns coord of best possible road to place to maximize length
     *
     * @param game
     * @param playerNo
     * @return coord of best road to build
     */
    public static int bestPossibleLongRoad(SOCGame game, int playerNo) {
        // for now the strat is to try to build off of the longest road(s)
        // of the player
        // TODO maybe import ?
        Vector<SOCLRPathData> pathData = game.getPlayer(playerNo).getLRPaths();

        /*for (SOCLRPathData path : pathData) {
            // check if can build off beginning
            Vector<Integer> possibleFront = findPossibleRoads(path.getBeginning());
            // for now just return the first possible... later we need to prolly
            // search this shizz our
            if (possibleFront.size() != 0) return possibleFront.indexOf(0);

            // same but end...
            Vector<Integer> possibleEnd = findPossibleRoads(path.getEnd());
            // for now just return the first possible... later we need to prolly
            // search this shizz our
            if (possibleEnd.size() != 0) return possibleEnd.indexOf(0);
        }*/
        
        return -1;
    }

    /**
     * Returns if longest road is possible to build
     *
     * @param game
     * @param playerNo
     * @return true if can build to longest road
     */
    public static boolean isLongestRoadPossible(SOCGame game, int playerNo) {
        if (!isCompetitiveForLongestRoad(game, playerNo)) return false;
     
        // here we know we are competitive... now lets see if we can reach
        //TODO try to build off getLRPaths()????
        
        return true;
    }
    
    /**
     * Compares the potential location of two settlements based on the values of the hexes around them
     * Does not check the validity of the settlements
     * @param one
     * @param two
     * @return
     */
    public static int compareSettlementOptions(int one, int two, SOCGame game) {
        if (NDRobotDM.totalProbabilityAtNode(game, one) > NDRobotDM.totalProbabilityAtNode(game, two)) {
            return 1;
        } else if (NDRobotDM.totalProbabilityAtNode(game, one) < NDRobotDM.totalProbabilityAtNode(game, two)) {
            return -1;
        }

        return 0;
    }



}
