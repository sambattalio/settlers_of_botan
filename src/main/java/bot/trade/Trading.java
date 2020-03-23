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
    private SOCPlayer player;
    

    public Trading(NDRobotBrain br) {
        super(br);
        
        brain = br;
        game = brain.getGame();
        brain.setTradeResponseTime(100);
        playerNo = brain.getOurPlayerData().getPlayerNumber();
        player = brain.getOurPlayerData();

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
    
    private int toArrayIdx(int type) {
    	switch(type) {
    	case SOCResourceConstants.CLAY:
    		return 0;
    	case SOCResourceConstants.ORE:
    		return 1;
    	case SOCResourceConstants.SHEEP:
    		return 2;
    	case SOCResourceConstants.WHEAT:
    		return 3;
    	case SOCResourceConstants.WOOD:
    		return 4;
    	}
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
    	
    	SOCResourceSet getResourceSet = new SOCResourceSet();
        SOCResourceSet giveResourceSet = new SOCResourceSet();
        
        // Get Probabilities for Each Hex Type
        
        int[] freqs = new int[5];
        Arrays.fill(freqs, 0);
        
        Vector<SOCSettlement> settlements = player.getSettlements();
        Iterator i = settlements.iterator();
        while (i.hasNext()) {
        	SOCSettlement s = i.next();
        	for (int hexCoord : game.getBoard().getAdjacentHexesToNode(s)) {
                int diceNumber = game.getBoard().getNumberOnHexFromCoord(hexCoord);
                // skip water
                if (diceNumber > 12 || diceNumber <= 0) continue;
                int probability = (diceNumber > 7) ? 13 - diceNumber : diceNumber - 1;
                freqs[toArrayIdx(getHexTypeFromCoord(s))] += probability;
            }
        }
        
        // Get Number Of Each Resource That I Currently Have
        SortedMap<int, int> resourcesSorted = new TreeMap<int, int>(); 
        for (int r : resourceArray) {
        	resourcesSorted.put(resources.getAmount(r), r);
        }
        
        Set set = treemap.entrySet(); 
        Iterator i = set.iterator();
        
        // Figure out what to give
        switch(type) {
        	case SOCPossiblePiece.ROAD:
        		// Trade sheep, wheat, or ore with high frequency - if don't have with high freq, trade one with most - last case, trade surplus wood or brick
        		boolean first = true;
        		int firstResource = SOCResourceConstants.UNKNOWN;
        		
                while (i.hasNext()) { 
                    Map.Entry me = (Map.Entry)i.next(); 
                    if (me.getValue() != SOCResourceConstants.WOOD && me.getValue() != SOCResourceConstants.BRICK) {
                    	if (first) {
                    		first = false;
                    		firstResource = me.getValue();
                    	}
                    	
                    	if (freq[toArrayIdx(me.getValue())] > 15){ //Not Sure What Value Here
                        	giveResourceSet.add(1, me.getValue());
                        	break;
                        }
                    }
                }
                
                if (giveResourceSet.getTotal() == 0) {
                	giveResourceSet.add(1, firstResource);
                }
                
                if (giveResourceSet.getTotal() == 0) {
                	if (resources.getAmount(SOCResourceConstants.WOOD) > resources.getAmount(SOCResourceConstants.BRICK)) {
                		giveResourceSet.add(1, SOCResourceConstants.WOOD);
                	} else if (resources.getAmount(SOCResourceConstants.BRICK) > resources.getAmount(SOCResourceConstants.WOOD)) {
                		giveResourceSet.add(1, SOCResourceConstants.BRICK);
                	}
                }
                
        		break;
        	case SOCPossiblePiece.SETTLEMENT:
        		// trade ore first but if no ore, trade highest freq surplus of others
        		int bestFreq = -100;
        		int bestFreqResource = SOCResourceConstants.UNKNOWN;
        		
        		if(resources.getAmount(SOCResourceConstants.ORE) > 0) {
        			giveResourceSet.add(1, SOCResourceConstants.ORE);
        			break;
        		}
        		
        		while (i.hasNext()) { 
                    if (resources.getAmount(me.getVale()) > 1 && freq[toArrayIdx(me.getValue())] > bestFreq){
                    	bestFreq = freq[toArrayIdx(me.getValue())];
                    	bestFreqResource = me.getValue();
                    }
                } 
        		
        		giveResourceSet.add(1, bestFreqResource);
        		
        		break;
        	case SOCPossiblePiece.CITY:
        		// skip ore and wheat and get rid of most cards - none of those then get rid of surlpus ore or wheat
        		while (i.hasNext()) { 
                    Map.Entry me = (Map.Entry)i.next(); 
                    if (me.getValue() == SOCResourceConstants.ORE || me.getValue() == SOCResourceConstants.WHEAT) {
                    	continue;
                    } else if (freq[toArrayIdx(me.getValue())] > 15){ //Not Sure What Value Here
                    	giveResourceSet.add(1, me.getValue());
                    	break;
                    }
                } 
                
                if ( needed.getTotal() == 0 && resources.getAmount(SOCResourceConstants.ORE) > 3) {
            		giveResourceSet.add(1, SOCResourceConstants.ORE);
            	} else if ( needed.getTotal() == 0 && resources.getAmount(SOCResourceConstants.WHEAT) > 2) {
            		giveResourceSet.add(1, SOCResourceConstants.WHEAT);
            	}
                
        		break;
        	case SOCPossiblePiece.CARD:
        		// pick between wood or brick - if neither give surplus sheep, wheat, or ore
        		if (resources.getAmount(SOCResourceConstants.BRICK) > 0 && freq[toArrayIdx(OCResourceConstants.BRICK)] > freq[toArrayIdx(OCResourceConstants.WOOD)]) {
        			giveResourceSet.add(1, SOCResourceConstants.BRICK);
        			break;
        		} else if (resources.getAmount(SOCResourceConstants.WOOD) > 0 && freq[toArrayIdx(OCResourceConstants.WOOD)] > freq[toArrayIdx(OCResourceConstants.BRICK)]) {
        			giveResourceSet.add(1, SOCResourceConstants.WOOD);
        			break;
        		} else if (resources.getAmount(SOCResourceConstants.BRICK) > resources.getAmount(SOCResourceConstants.WOOD)) {
        			giveResourceSet.add(1, SOCResourceConstants.BRICK);
        			break;
        		} else if (resources.getAmount(SOCResourceConstants.WOOD) > resources.getAmount(SOCResourceConstants.BRICK)) {
        			giveResourceSet.add(1, SOCResourceConstants.WOOD);
        			break;
        		}
        		
        		while (i.hasNext()) {
        			Map.Entry me = (Map.Entry)i.next();
        			if (me.getValue() != SOCResourceConstants.WOOD && me.getValue() != SOCResourceConstants.BRICK) {
        				giveResourceSet.add(1, me.getValue());
        				break;
        			}
        		}
        		
        		break;
        }
        
        if(giveResourceSet.getTotal() == 0) {
        	checkPortsAndFours();
        }
        
    	//Figure Out What To Get 
        for (int r : resourceArray) {
    		if(needed.getAmount(r) > 0) {
   				getResourceSet.add(1, r);
   				break;
   			}
   		}
        
        makeTradeOffer(giveResourceSet, getResourceSet);
        
        return false;
    }
    
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
        
        for(SOCPlayer p : game.getPlayers()) {
        	if (p.getPublicVP() > player.getTotalVP()) {
        		players_to_offer[p.getPlayerNumber()] = false;
        	}
        }

        if (game.getCurrentPlayerNumber() == playerNo) {
        	
        	SOCTradeOffer offer = new SOCTradeOffer(game.getName(), playerNo, players_to_offer, give, get);
        	
            // see if we've made this offer before
            
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
        	
            // Make offer if not made previously this turn
            
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
