package bot;

import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotDM;
import soc.game.SOCPlayingPiece;
import soc.game.SOCSettlement;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCPossiblePiece;
import soc.debug.D;
import soc.game.SOCResourceSet;
import soc.game.SOCPlayer;
import soc.game.SOCRoutePiece;
import soc.game.SOCRoad;
import java.util.Iterator;
import java.util.Vector;
import java.util.Queue;
import java.util.LinkedList;
import javafx.util.Pair;
import java.util.Stack;

public class NDRobotDM extends SOCRobotDM {

    public NDRobotDM(SOCRobotBrain br) {
        super(br);
    }

    @Override
    public void planStuff(int strategy) {
        D.ebug_enable();
        D.ebugPrintln("!!! Starting our overriden planStuff method !!!");

        // reset any building plan already in place
        this.buildingPlan.clear(); 

        // Grab all of bot's current board pieces
        Vector vec = this.ourPlayerData.getPieces();
        Iterator<SOCPlayingPiece> i = vec.iterator();
        Vector<Integer> best_path = new Vector<Integer>();
        while (i.hasNext()) {
            SOCPlayingPiece piece = i.next();
            // look for ways to build off of any placed roads using BFS and resource odds
            if (piece.getType() == SOCPlayingPiece.ROAD) {
                Vector<Integer> result = BFS(piece.getCoordinates());
                D.ebugPrintln("Best on road " + piece + "was" + result.toString());
                // has an initial "real" road inside of it so get rid of it
                if (result.size() > 0) result.removeElementAt(0);
                // Check if better than the best path
                if (result.size() < best_path.size() || best_path.size() == 0) {
                    best_path = result;
                    D.ebugPrintln("Best path set:" + best_path.toString());
                } else if (result.size() == best_path.size() && difference_from_seven_at_node(result.lastElement()) <  difference_from_seven_at_node(best_path.lastElement())){
                    best_path = result;
                }
            } 
        }

        D.ebugPrintln("Best at end" + best_path.toString());
        // change last coord to settlement as it will always be one from our BFS
        this.buildingPlan.push(new SOCPossibleSettlement(this.ourPlayerData, best_path.lastElement(), null));
        for (int j = best_path.size() - 2; j >= 0; j--)
            this.buildingPlan.push(new SOCPossibleRoad(this.ourPlayerData, best_path.get(j), null));
    }

    // Returns if legally possible to hypothetically place a settlement at coord
    public boolean can_build_settlement(final int coord) {
        if (this.game.getBoard().settlementAtNode(coord) != null) return false;

        for (int i = 0; i <= 2; i++) {
            D.ebugPrintln("Trying to find adjacent nodes from node: " + coord + " in direction: " + i);
            if (this.game.getBoard().settlementAtNode(this.game.getBoard().getAdjacentNodeToNode(coord, i)) != null)
                return false;
        }
        return true;
    }

    // Returns if legally possible to hypothetically place a road at coord
    public boolean can_build_road(final int edgeCoord) {
        for (SOCRoutePiece r : this.game.getBoard().getRoadsAndShips())
        {
            if (edgeCoord == r.getCoordinates())
            {
                return false;
            }
        }

        return true;
    }

    // returns abs value difference from 21 dice number at node
    public float difference_from_seven_at_node(final int nodeCoord) {
        int sum   = 0;
        int count = 0;
        for (Integer hexCoord : this.game.getBoard().getAdjacentHexesToNode(nodeCoord)) {
            count ++; 
            int dice_number = this.game.getBoard().getNumberOnHexFromCoord(hexCoord);
            D.ebugPrintln("Value of Hex piece " + String.valueOf(hexCoord) + ": " + String.valueOf(dice_number));
            // set water == 0
            if (dice_number == -1) dice_number = 0;
            sum += (dice_number > 7) ? dice_number - 7 : 7 - dice_number;
        }
        D.ebugPrintln("Sum after difference: " + String.valueOf(sum));
        
        // return average dist from 7
        return (float)sum / (float)count;
    }

    // Returns "best" path to a new settlement from starting coord
    // Returns a vector of coordinates where 0 is initial existing road
    // and n - 1 is the optimal settlement placement found
    public Vector<Integer> BFS(int coord) {
        Queue<Vector<Integer>> stack = new LinkedList<>();
        
        // push initial coord
        Vector<Integer> vec = new Vector<Integer>();
        vec.add(coord);
        stack.add(vec);
        
        Vector<Integer> path = new Vector<Integer>();

        while (!stack.isEmpty()) {
            Vector<Integer> current = stack.poll();

            if (current.size() > path.size() && path.size() != 0) continue;

            // Check for possible settlements
            for (int x : this.game.getBoard().getAdjacentNodesToEdge(current.lastElement())) {
                D.ebugPrintln("Considering node " + x + " onto " + current);
                if (this.can_build_settlement(x)) {
                    D.ebugPrintln("Can build settlement at " + x);
                    D.ebugPrintln("Average diff from seven: " + String.valueOf(difference_from_seven_at_node(x)));
                    if (path.size() == 0 || current.size() < path.size() - 1) {
                        path = (Vector<Integer>) new Vector<>(current);
                        // add in the settlement to end of path
                        D.ebugPrintln("Adding node " + String.valueOf(x) + " to " + path);
                        path.add(x);
                        continue;
                    } else if (path.size() == current.size() && difference_from_seven_at_node(x) < difference_from_seven_at_node(path.lastElement())) {
                        path = (Vector<Integer>) new Vector<>(current);
                        // add in the settlement to end of path
                        D.ebugPrintln("Adding node " + String.valueOf(x) + " to " + path);
                        path.add(x);
                        continue;
                    }
                }
            }

            // Check for new roads to build & add to stack
            for (int x : this.game.getBoard().getAdjacentEdgesToEdge(current.lastElement())) {
                D.ebugPrintln("Adjacent edge to " + current.lastElement() + ": " + x);
                if (this.can_build_road(x) && !current.contains(x)) {
                    D.ebugPrintln("Can build road on " + x);
                    Vector<Integer> new_vector = new Vector<>(current);
                    new_vector.add(x);
                    stack.add(new_vector);
                }
            }

        }

        return path;
    }

}
