package bot;

import bot.decision.DecisionTreeDM;
import bot.decision.DecisionTreeType;

import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;
import soc.robot.SOCPossiblePiece;
import soc.game.SOCResourceSet;

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
     
    private boolean attemptTrade = false;
    
    public void setWaitingResponse(boolean b) {
    	waitingForTradeResponse = b;
    }
    
    public boolean getWaitingResponse() {
    	return waitingForTradeResponse;
    }
    
    public void setTradeResponseTime(int i) {
    	tradeResponseTimeoutSec = i;
    }
    
    public void setAttemptTrade(boolean b) {
    	attemptTrade = b;
    }
    
    @Override
    protected void buildOrGetResourceByTradeOrCard() throws IllegalStateException {
    	D.ebugPrintln("!---- BuildOrGetResourcesByTradeOrCard -----!");
    	SOCPossiblePiece targetPiece = buildingPlan.peek();
    	SOCResourceSet targetResources = targetPiece.getResourcesToBuild();
    	
    	negotiator.setTargetPiece(ourPlayerNumber, targetPiece);
    	
    	if (! expectWAITING_FOR_MONOPOLY) {
    		
    		if ((!ourPlayerData.getResources().contains(targetResources))) {
    			 waitingForTradeResponse = false;
    			 attemptTrade = false;
    			
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
	    		buildRequestPlannedPiece();
	    	}
    	}
    }
}
