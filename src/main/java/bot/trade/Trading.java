package bot.trade;

import soc.game.*;
import soc.robot.*;
import java.util.*;

import soc.debug.D;

import bot.*;


public class Trading extends SOCRobotNegotiator {
    private int[] resourceArray = new int[6];
    private NDRobotBrain brain;
    private SOCGame game;
    private int playerNo; 
    

    public Trading(NDRobotBrain br) {
        super(br);
        
        brain = br;
        game = brain.getGame();
        brain.setTradeResponseTime(100);
        playerNo = brain.getOurPlayerData().getPlayerNumber();

        resourceArray[0] = SOCResourceConstants.CLAY;
        resourceArray[1] = SOCResourceConstants.ORE;
        resourceArray[2] = SOCResourceConstants.SHEEP;
        resourceArray[3] = SOCResourceConstants.WHEAT;
        resourceArray[4] = SOCResourceConstants.WOOD;
        resourceArray[5] = SOCResourceConstants.UNKNOWN;
    }
    
    public static final int IGNORE_OFFER = -1;

    /** Response: Reject an offer. */
    public static final int REJECT_OFFER = 0;

    /** Response: Accept an offer. */
    public static final int ACCEPT_OFFER = 1;

    private SOCResourceSet getPlayerResources() {
    	return brain.getOurPlayerData().getResources();
    }
    
    public int getNumberOfResources() {
    	return getPlayerResources().getKnownTotal();
    }

    private boolean checkPortsAndFours() {
    	return false;
    }

    private SOCResourceSet determineWhatIsNeeded(int type) {
		SOCResourceSet resources = getPlayerResources();
		SOCResourceSet needed = new SOCResourceSet();
		SOCResourceSet requirements = null;
		
		switch(type) {
		    case SOCPossiblePiece.ROAD:
			requirements = new SOCResourceSet(1, 0, 0, 0, 1, 0);
			break;
		    
		    case SOCPossiblePiece.SETTLEMENT:
			requirements = new SOCResourceSet(1, 0, 1, 1, 1, 0);
			break;
	
		    case SOCPossiblePiece.CITY:
			requirements = new SOCResourceSet(0, 3, 0, 2, 0, 0);
			break;
	
		    case SOCPossiblePiece.CARD:
			requirements = new SOCResourceSet(0, 1, 1, 1, 0, 0);
			break;
		}
	
		for (int r : resourceArray) {
		    if(resources.getAmount(r) < requirements.getAmount(r)) {
		    	needed.add(requirements.getAmount(r) - resources.getAmount(r), r);
	        }
	    }
	        
		D.ebugPrintln("What Bot Has: " + resources.toShortString());
	    D.ebugPrintln("What's needed: " + needed.toShortString());

	    return needed;
    }

    public boolean tradingThinking(int type) {
    	D.ebugPrintln("----- Trading Thinking -----");
    	SOCResourceSet needed = determineWhatIsNeeded(type);
    	SOCResourceSet resources = getPlayerResources();
    	
    	if(needed.getTotal() > 2) {
    		return false;
    	}
    	
    	SOCResourceSet giveResourceSet = new SOCResourceSet(0, resources.getAmount(SOCResourceConstants.ORE), resources.getAmount(SOCResourceConstants.SHEEP), resources.getAmount(SOCResourceConstants.WHEAT), 0, resources.getAmount(SOCResourceConstants.UNKNOWN));
        SOCResourceSet getResourceSet = needed;
    	
        makeTradeOffer(giveResourceSet, getResourceSet);
        
        return false;
    }
    
    /*protected boolean makeOffer(SOCTradeOffer offer)
    {
        boolean result = false;
        game.getPlayer(playerNo).setCurrentOffer(offer);
        //resetWantsAnotherOffer();

        if (offer != null)
        {
            ///
            ///  reset the offerRejections flag and check for human players
            ///
            boolean anyHumans = false;
            for (int pn = 0; pn < game.maxPlayers; ++pn)
            {
                offerRejections[pn] = false;
                if (! (game.isSeatVacant(pn) || game.getPlayer(pn).isRobot()))
                    anyHumans = true;
            }

            waitingForTradeResponse = true;
            tradeResponseTimeoutSec = (anyHumans)
                ? TRADE_RESPONSE_TIMEOUT_SEC_HUMANS
                : TRADE_RESPONSE_TIMEOUT_SEC_BOTS_ONLY;
            counter = 0;
            client.offerTrade(game, offer);
            result = true;
        }
        else
        {
            doneTrading = true;
            waitingForTradeResponse = false;
        }

        return result;
    }*/
    
    private String toString(int type) {
    	switch(type) {
		    case SOCPossiblePiece.ROAD:
				return "Road";
			    
			case SOCPossiblePiece.SETTLEMENT:
				return "Settlement";
		
			case SOCPossiblePiece.CITY:
				return "City";
		
			case SOCPossiblePiece.CARD:
				return "Card";
    	}
    	
    	return "";
    }

    private boolean makeTradeOffer(SOCResourceSet give, SOCResourceSet get) {
    	D.ebugPrintln("!!! ----- MAKE OFFER ----- !!!");
    	
    	boolean[] players_to_offer = new boolean[game.maxPlayers];
        Arrays.fill(players_to_offer, true);
        players_to_offer[playerNo] = false; // don't offer self

        if (game.getCurrentPlayerNumber() == playerNo) {
        	
        	SOCTradeOffer offer = new SOCTradeOffer(game.getName(), playerNo, players_to_offer, give, get);
        	
            /// see if we've made this offer before
            
            boolean match = false;
            Iterator<SOCTradeOffer> offersMadeIter = offersMade.iterator();

            while ((offersMadeIter.hasNext() && !match))
            {
                SOCTradeOffer pastOffer = offersMadeIter.next();

                if ((pastOffer != null) && (pastOffer.getGiveSet().equals(give)) && (pastOffer.getGetSet().equals(get)))
                {
                    match = true;
                }
            }
        	
        	if(!match) {
        		brain.setWaitingResponse(true);
	        	addToOffersMade(offer);
	        	brain.setCounter(0);
	        	game.getPlayer(playerNo).setCurrentOffer(offer);
	        	brain.getClient().offerTrade(game, offer);
        	}
        }
        
        return false;
    }
    
    public boolean attemptTradeOffer(int type) {
    	D.ebugPrintln("!!! ----- Trading " + toString(type) + " ----- !!!");
    	return tradingThinking(type);
    }
    
    
}
