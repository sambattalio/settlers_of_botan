package bot.decision;

import bot.NDHelpers;
import bot.NDRobotNegotiator;
import soc.debug.D;
import soc.game.*;
import soc.robot.*;
import bot.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecisionTreeDM extends SOCRobotDM {
	private NDRobotBrain brain;

	private NDRobotNegotiator trades;

    int callCount = 0;

    public DecisionTreeDM(NDRobotBrain br) {
        super(br);

        trades = new NDRobotNegotiator(br);
        brain = br;
    }

    @Override
    public void planStuff(int strategy) {
        D.ebugPrintln("----- Plan Stuff " + callCount++ + " -----");
        try {
        	if(strategy == 10) {
        		addToPlan(LongestRoadStrategy.plan(this));
        	}
            switch(DecisionTreeType.whichUse(game, brain.getOurPlayerData())) {
                case LONGEST_ROAD:
                    addToPlan(LongestRoadStrategy.plan(this));
                    break;
                case LARGEST_ARMY:
                    addToPlan(LargestArmyStrategy.plan(this));
                    break;
                case DEFAULT:
                    addToPlan(DefaultStrategy.plan(this));
                    break;
            }
        } catch(Exception e) {
            D.ebugPrintStackTrace(e, e.toString());
        }
    }

    public NDRobotBrain getBrain() {
        return brain;
    }
    
    protected void addToPlan(SOCPossiblePiece piece) {
        if (piece == null) return;
        this.buildingPlan.add(piece);
    }

    public SOCPlayer getPlayer() {
        return brain.getOurPlayerData();
    }
    
    public int getPlayerNo() {
        return brain.getOurPlayerData().getPlayerNumber();
    }


}
