package bot;

import bot.decision.DecisionTreeDM;
import bot.decision.DecisionTreeType;

import bot.decision.LongestRoadStrategy;
import bot.decision.LargestArmyStrategy;
import bot.decision.DefaultStrategy;

import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;
import soc.robot.SOCPossiblePiece;
import soc.game.SOCResourceSet;
import soc.robot.SOCPossiblePiece;
import java.util.Arrays;

import soc.debug.D;

public class NDRobotBrain extends SOCRobotBrain {

    public NDRobotBrain(SOCRobotClient rc, SOCRobotParameters params, SOCGame ga, CappedQueue<SOCMessage> mq) {
        super(rc, params, ga, mq);
    }

    @Override
    public void setOurPlayerData() {
        super.setOurPlayerData();

        decisionMaker = new DecisionTreeDM(this);
        negotiator = new NDRobotNegotiator(this);
        monopolyStrategy = new NDMonopolyStrategy(game, ourPlayerData);
        robberStrategy = new NDRobberStrategy(game, ourPlayerData, this, rand);
        discardStrategy = new NDDiscardStrategy(game, ourPlayerData, this, rand);
        openingBuildStrategy = new NDOpeningBuildStrategy(game, ourPlayerData);
    }
     
    //private static boolean[] attemptTrade = {true, true, true, true};
    
    public void setWaitingResponse(boolean b) {
    	waitingForTradeResponse = b;
    }
    
    public boolean getWaitingResponse() {
    	return waitingForTradeResponse;
    }
    
    public void setTradeResponseTime(int i) {
    	tradeResponseTimeoutSec = i;
    }
    
    /*public static boolean getAttempt(int t) {
    	switch(t) {
			case SOCPossiblePiece.ROAD: 		return attemptTrade[0];
			case SOCPossiblePiece.SETTLEMENT: 	return attemptTrade[1];
			case SOCPossiblePiece.CITY: 		return attemptTrade[2];
			case SOCPossiblePiece.CARD: 		return attemptTrade[3];
		}
    	
    	return false;
    }
    
    private static int getIdx(int t) {
    	switch(t) {
    		case SOCPossiblePiece.ROAD: 		return 0;
    		case SOCPossiblePiece.SETTLEMENT: 	return 1;
    		case SOCPossiblePiece.CITY: 		return 2;
    		case SOCPossiblePiece.CARD: 		return 3; 
    	}
    	
    	return -1;
    		
    }
    
    private boolean checkShouldContinue() {
    	for(int i = 0; i < 4; i++) {
    		if(attemptTrade[i] == true) {
    			return true;
    		}
    	}
    	
    	return false;
    }*/
    
    @Override
    protected void buildOrGetResourceByTradeOrCard() throws IllegalStateException {
    	D.ebugPrintln("!---- BuildOrGetResourcesByTradeOrCard -----!");
    	SOCPossiblePiece targetPiece = buildingPlan.peek();
    	SOCResourceSet targetResources = targetPiece.getResourcesToBuild();
    	
    	negotiator.setTargetPiece(ourPlayerNumber, targetPiece);
    	
    	if (! expectWAITING_FOR_MONOPOLY) {
    		
    		if ((!ourPlayerData.getResources().contains(targetResources))) {
    			 waitingForTradeResponse = false;
    			 
    			 makeOffer(targetPiece);
    			 pause(1000);
    		}
    		
    		if (! waitingForTradeResponse) {
    			if (tradeToTarget2(targetResources))
                {
                    counter = 0;
                    waitingForTradeMsg = true;
                    pause(1500);
                }
    		}
    		
	    	if ((! (waitingForTradeMsg || waitingForTradeResponse)) && ourPlayerData.getResources().contains(targetResources)) {
	    		D.ebugPrintln("Build Piece");
	    		buildRequestPlannedPiece();
	    	} 
	    	
	    	/*if (checkShouldContinue()) {
	    		D.ebugPrintln("Turn Off " + getIdx(targetPiece.getType()));
	    		attemptTrade[targetPiece.getType()] = false;
	    		switch(DecisionTreeType.whichUse(game, getOurPlayerData())) {
	                case LONGEST_ROAD:
	                	
	                    break;
	                case LARGEST_ARMY:
	                	LargestArmyStrategy.plan(decisionMaker);
	                    break;
	                case DEFAULT:
	                	DefaultStrategy.plan(decisionMaker);
	                    break;
	            }
	    	} else {
	    		D.ebugPrintln("End Turn");
	    		Arrays.fill(attemptTrade, true);
	    	}*/
    	}
    }
}
