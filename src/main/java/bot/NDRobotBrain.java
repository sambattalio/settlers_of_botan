package bot;

import bot.decision.DecisionTreeDM;
import bot.trade.Trading;

import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;
import soc.robot.SOCPossiblePiece;
import soc.game.SOCTradeOffer;
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
        negotiator = new Trading(this);
        monopolyStrategy = new NDMonopolyStrategy(game, ourPlayerData);
        robberStrategy = new NDRobberStrategy(game, ourPlayerData, this, rand);
        discardStrategy = new NDDiscardStrategy(game, ourPlayerData, this, rand);
        openingBuildStrategy = new NDOpeningBuildStrategy(game, ourPlayerData);
    }
     
    public void setWaitingResponse(boolean b) {
    	waitingForTradeResponse = b;
    }
    
    public boolean getWaitingResponse() {
    	return waitingForTradeResponse;
    }
    
    public void setTradeResponseTime(int i) {
    	tradeResponseTimeoutSec = i;
    }
    
    public void setCounter(int i) {
    	counter = i;
    }
    
    public boolean trade(SOCPossiblePiece p) {
    	SOCResourceSet resourcesBefore = ourPlayerData.getResources();
    	
    	boolean result = makeOffer(p);
    	
    	SOCResourceSet resourcesAfter = ourPlayerData.getResources();
    	
    	D.ebugPrintln("Before: " + resourcesBefore);
    	D.ebugPrintln("After: " + resourcesAfter);
    	
    	if(!result) {
    		D.ebugPrintln("No offer made");
    	}
    	
    	return result && !resourcesBefore.equals(resourcesAfter);
    }
    
    /*public void check() {
    	final SOCMessage mes = gameEventQ.get();  // Sleeps until message received
    	final int mesType;
    	
    	if(mes != null) {
    		 mesType = mes.getType();
             if (mesType != SOCMessage.TIMINGPING)
                 turnEventsCurrent.addElement(mes);
             if (D.ebugOn)
                 D.ebugPrintln("mes - " + mes);
    	} else {
            mesType = -1;
        }
    	
    	if (waitingForTradeResponse && (counter > tradeResponseTimeoutSec))
        {
            tradeStopWaitingClearOffer();
        }
    }*/
}