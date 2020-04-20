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
import soc.game.SOCResourceConstants;
import soc.game.SOCDevCardConstants;
import soc.robot.SOCPossibleRoad;
import soc.game.SOCRoad;
import soc.game.SOCResourceSet;
import bot.decision.DecisionTreeType;
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
     
    public static boolean[] attemptTrade = {true, true, true, false};
    
    public void setWaitingResponse(boolean b) {
    	waitingForTradeResponse = b;
    }
    
    public boolean getWaitingResponse() {
    	return waitingForTradeResponse;
    }
    
    public void setTradeResponseTime(int i) {
    	tradeResponseTimeoutSec = i;
    }
    
    public static boolean getAttempt(int t) {
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

    /*private boolean playRoadCard() {
        // i guess if top two are roads lets get it
        if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.ROADS)) {
            SOCPossiblePiece first = buildingPlan.pop();

            // weird stuff but i guess this is how to get it to work with the run() method
            if (first != null && first instanceof SOCPossibleRoad) {
                SOCPossiblePiece sec = (buildingPlan.isEmpty()) ? null : buildingPlan.peek();
                if (sec != null && sec instanceof SOCPossibleRoad) {
                    whatWeWantToBuild = new SOCRoad(ourPlayerData, first.getCoordinates(), null);
                    client.playDevCard(game, SOCDevCardConstants.ROADS);
                    return true;
                } else {
                    buildingPlan.push(first);
                }
            } else {
                buildingPlan.push(first);
            }
            
        }
        return false;
    }

    private boolean playKnightCard() {
        if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.ROADS)) {
            client.playDevCard(game, SOCDevCardConstants.KNIGHT);
            pause(1500); // honestly just wait b/c it does in jsettlers /shrug
            return true;
        }
        return false;
    }

    private boolean playYearOfPlentyCard() {
        // add 2 resources to the resourceChoices SOCResourceSet, inherited from SOCRobotBrain        
        if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.DISC)) { // year of plenty
            SOCResourceSet ourResources = ourPlayerData.getResources();
            
            // check for city
            int oreNeededForCity = 3 - ourResources.getAmount(SOCResourceConstants.ORE);
            int wheatNeededForCity = 2 - ourResources.getAmount(SOCResourceConstants.WHEAT);

            // for settlement
            int oreForSettlement   = ourResources.getAmount(SOCResourceConstants.ORE) > 0 ? 0 : 1;
            int wheatForSettlement = ourResources.getAmount(SOCResourceConstants.WHEAT) > 0 ? 0 : 1;
            int clayForSettlement  = ourResources.getAmount(SOCResourceConstants.CLAY) > 0 ? 0 : 1;
            int sheepForSettlement = ourResources.getAmount(SOCResourceConstants.SHEEP) > 0 ? 0 : 1;

            if (oreNeededForCity + wheatNeededForCity == 2) {
                // build up resources for city only if we optimially will use this card
                resourceChoices.clear();
                // this should only add to 2
                resourceChoices.add(oreNeededForCity, SOCResourceConstants.ORE);
                resourceChoices.add(wheatNeededForCity, SOCResourceConstants.WHEAT);
                client.playDevCard(game, SOCDevCardConstants.DISC);
                pause(1500); // honestly just wait b/c it does in jsettlers /shrug
                return true;
            } else if (oreForSettlement + wheatForSettlement + clayForSettlement + sheepForSettlement == 2) {
                // build up resources for settlement only if we optimally will use this card
                resourceChoices.clear(); 
                // this should only add to 2
                resourceChoices.add(oreForSettlement, SOCResourceConstants.ORE);
                resourceChoices.add(wheatForSettlement, SOCResourceConstants.WHEAT);
                resourceChoices.add(clayForSettlement, SOCResourceConstants.CLAY);
                resourceChoices.add(sheepForSettlement, SOCResourceConstants.SHEEP);
                client.playDevCard(game, SOCDevCardConstants.DISC);
                pause(1500);
                return true;
            }
        }
        return false;
    }

    private boolean playMonopolyCard() {
        // TODO TRACK ENEMY RESOURCES -- otherwise can't fairly ever play this
        if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.MONO)) {
            client.playDevCard(game, SOCDevCardConstants.MONO);
            pause(1500);
            return true;
        }
        return false;
    }

    /**
     * Attempts to play a development card to increase resources / build what we need
     *
     
    public boolean tryToPlayDevCard() {
        if (game.getGameState() == SOCGame.PLAY1 && !ourPlayerData.hasPlayedDevCard()) {
            // first priority.. if there is a robber blocking one of our hexes -> move it
            if (!ourPlayerData.getNumbers().hasNoResourcesForHex(game.getBoard().getRobberHex())) {
                D.ebugPrintln("Trying to move knight because one is blocking our resources");
                if (playKnightCard()) return true;
            }
            // second thing we should do is check if strategy based decision can be made
            switch(DecisionTreeType.whichUse(game, ourPlayerData)) {
                case LONGEST_ROAD:
                    D.ebugPrintln("Trying to play road card b/c strategy");
                    if (playRoadCard()) return true;
                    break;
                case LARGEST_ARMY:
                    D.ebugPrintln("Trying to play knight card b/c strategy");
                    if (playKnightCard()) return true;
                    break;
                case DEFAULT:
                    break;
            }

            // Look to see if any other are valuable to play right now...  road       
            D.ebugPrintln("trying year of plenty");
            if (playYearOfPlentyCard()) return true;
            D.ebugPrintln("trying monopoly");
            if (playMonopolyCard()) return true;
            if (playKnightCard()) return true;
        }
        return false;
    }*/

    /* UNCOMMENT TO PLAY DEV CARDS OUR WAY BUT ALSO lose some functionality
    protected void buildOrGetResourceByTradeOrCard() throws IllegalStateException {
        if (tryToPlayDevCard()) {
            // update plan b/c ur boy just played a dev card
        }
        buildRequestPlannedPiece();
   	}*/
    
    
    private boolean checkShouldContinue() {
    	for(int i = 0; i < 4; i++) {
    		if(attemptTrade[i] == true) {
    			return true;
    		}
    	}
    	
    	return false;
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
    		
    		D.ebugPrintln("Target Piece: " + getIdx(targetPiece.getType()));
    		D.ebugPrintln("Whole Statement: " + ((! (waitingForTradeMsg || waitingForTradeResponse)) && ourPlayerData.getResources().contains(targetResources)) );
	    	D.ebugPrintln("Not waiting for Trade Msg or response: " + (! (waitingForTradeMsg || waitingForTradeResponse)));
	    	D.ebugPrintln("Our Resources: " + ourPlayerData.getResources());
	    	D.ebugPrintln("Target Resources: " + targetResources);
    		
	    	boolean built = false;
    		if ((! (waitingForTradeMsg || waitingForTradeResponse)) && ourPlayerData.getResources().contains(targetResources)) {
	    		D.ebugPrintln("Build Piece");
	    		built = true;
	    		buildRequestPlannedPiece();
	    	} 
	    	
	    	if (checkShouldContinue()) {
	    		if (!built && (! (waitingForTradeMsg || waitingForTradeResponse))) {
	    			D.ebugPrintln("Turn Off " + getIdx(targetPiece.getType()));
	    			attemptTrade[getIdx(targetPiece.getType())] = false;
	    		}
	    		
	    		built = false;
	    		switch(DecisionTreeType.whichUse(game, getOurPlayerData())) {
	                case LONGEST_ROAD:
	                	D.ebugPrintln("Rerun Longest Road Plan");
	                	decisionMaker.planStuff(-1);
	                    return;
	                case LARGEST_ARMY:
	                	D.ebugPrintln("Rerun Largest Army Plan");
	                	decisionMaker.planStuff(-1);
	                    return;
	                case DEFAULT:
	                	D.ebugPrintln("Rerun DefaultPlan");
	                	decisionMaker.planStuff(-1);
	                    return;
	            }
	    	} 

    	}
    	D.ebugPrintln("End of Function");
    }
}
