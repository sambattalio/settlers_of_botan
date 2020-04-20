package bot;

import bot.decision.DecisionTreeDM;

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

    public void setFour(boolean should){
	NDRobotNegotiator.shouldFour = should;
    }

    private boolean playRoadCard() {
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
     */
    public void tryToPlayDevCard() {
        if (game.getGameState() == SOCGame.PLAY1 && !ourPlayerData.hasPlayedDevCard()) {
            // first priority.. if there is a robber blocking one of our hexes -> move it
            if (!ourPlayerData.getNumbers().hasNoResourcesForHex(game.getBoard().getRobberHex())) {
                if (playKnightCard()) return;
            }
            // second thing we should do is check if strategy based decision can be made
            switch(DecisionTreeType.whichUse(game, ourPlayerData)) {
                case LONGEST_ROAD:
                    if (playRoadCard()) return;
                    break;
                case LARGEST_ARMY:
                    if (playKnightCard()) return;
                    break;
                case DEFAULT:
                    break;
            }

            // Look to see if any other are valuable to play right now...        
            if (playYearOfPlentyCard()) return;
            if (playMonopolyCard()) return;
        }
    }
    
    public boolean trade(SOCPossiblePiece p) {
    	
    	boolean result = makeOffer(p);

    	if(!result) {
    	    D.ebugPrintln("No offer made");
    	}

	pause(5000);
	D.ebugPrintln("Done Pause");
    	
    	return result;
    }

}
