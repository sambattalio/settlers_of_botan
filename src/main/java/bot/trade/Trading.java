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

	public static boolean shouldFour = false;
	public static boolean shouldPort = false;
	public static boolean shouldTwo = false;

	public Trading(NDRobotBrain br) {
		super(br);

		brain = br;
		game = brain.getGame();
		playerNo = brain.getOurPlayerData().getPlayerNumber();
		player = brain.getOurPlayerData();

		resourceArray[0] = SOCResourceConstants.CLAY;
		resourceArray[1] = SOCResourceConstants.ORE;
		resourceArray[2] = SOCResourceConstants.SHEEP;
		resourceArray[3] = SOCResourceConstants.WHEAT;
		resourceArray[4] = SOCResourceConstants.WOOD;
		resourceArray[5] = SOCResourceConstants.UNKNOWN;
	}

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
			case SOCResourceConstants.UNKNOWN:
				return 5;
		}

		return -1;
	}

	private String toStringResources(int type) {

		switch(type) {
			case SOCResourceConstants.CLAY:
				return "Clay";
			case SOCResourceConstants.ORE:
				return "Ore";
			case SOCResourceConstants.SHEEP:
				return "Sheep";
			case SOCResourceConstants.WHEAT:
				return "Wheat";
			case SOCResourceConstants.WOOD:
				return "Wood";
			case SOCResourceConstants.UNKNOWN:
				return "Unknown";
		}

		return "";
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

    /*public boolean tradingThinking(int type) {
    	D.ebugPrintln("----- Trading Thinking -----");
    	SOCResourceSet needed = determineWhatIsNeeded(type);
    	SOCResourceSet resources = getPlayerResources();
    	D.ebugPrintln("Resources: " + resources);
    	
    	if(needed.getTotal() > 2) {
    		return false;
    	}
    	
    	SOCResourceSet getResourceSet = new SOCResourceSet();
        SOCResourceSet giveResourceSet = new SOCResourceSet();
        
        // Get Probabilities for Each Hex Type
        
        int[] freqs = new int[6];
        Arrays.fill(freqs, 0);
        
        Vector<SOCSettlement> settlements = player.getSettlements();
        Iterator<SOCSettlement> settlementIdx = settlements.iterator();
        while (settlementIdx.hasNext()) {
        	SOCSettlement s = settlementIdx.next();
        	
        	for (int hexCoord : game.getBoard().getAdjacentHexesToNode(s.getCoordinates())) {
                int diceNumber = game.getBoard().getNumberOnHexFromCoord(hexCoord);
                // skip water
                if (diceNumber > 12 || diceNumber <= 0) continue;
                int probability = (diceNumber > 7) ? 13 - diceNumber : diceNumber - 1;
                freqs[toArrayIdx(game.getBoard().getHexTypeFromCoord(hexCoord))] += probability;
            }
        }
        
        D.ebugPrintln("freqs: " + Arrays.toString(freqs));
        
        // Get Number Of Each Resource That I Currently Have
        Map<Integer, Integer> initialResourceMap = new HashMap<>();
        LinkedHashMap<Integer, Integer> resourcesSorted = new LinkedHashMap<>();
        
        for (int r : resourceArray) {
        	initialResourceMap.put(r, resources.getAmount(r));
        }
        
        initialResourceMap.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) 
        .forEachOrdered(x -> resourcesSorted.put(x.getKey(), x.getValue()));
        
        D.ebugPrintln("Resources Sorted: " + resourcesSorted);
      
        // Figure out what to give
        switch(type) {
        	case SOCPossiblePiece.ROAD:
        		// Trade sheep, wheat, or ore with high freqsuency - if don't have with high freqs, trade one with most - last case, trade surplus wood or CLAY
        		boolean first = true;
        		int firstResource = SOCResourceConstants.UNKNOWN;
        		
        		for(Integer key: resourcesSorted.keySet()) { 
                    if (key != SOCResourceConstants.WOOD && key != SOCResourceConstants.CLAY && resources.getAmount(key) > 0) {
                    	if (first) {
                    		first = false;
                    		firstResource = key;
                    	}
                    	
                    	if (freqs[toArrayIdx(key)] > 6 && resources.getAmount(key) > 0){ //Not Sure What key Here
                    		D.ebugPrintln("Trade " + toStringResources(key) + "Because High Frequency and not wood or brick - for road");
                        	giveResourceSet.add(1, key);
                        	break;
                        }
                    }
                }
                
                if (giveResourceSet.getTotal() == 0 && firstResource != SOCResourceConstants.UNKNOWN) {
                	giveResourceSet.add(1, firstResource);
                	D.ebugPrintln("Trade " + toStringResources(firstResource) + " bc most quantity since no high freq resources - for road");
                }
                
                if (giveResourceSet.getTotal() == 0) {
                	if (resources.getAmount(SOCResourceConstants.WOOD) > resources.getAmount(SOCResourceConstants.CLAY) && resources.getAmount(SOCResourceConstants.WOOD) > 1) {
                		giveResourceSet.add(1, SOCResourceConstants.WOOD);
                		D.ebugPrintln("Trade wood bc surplus - for road");
                	} else if (resources.getAmount(SOCResourceConstants.CLAY) > resources.getAmount(SOCResourceConstants.WOOD) && resources.getAmount(SOCResourceConstants.CLAY) > 1) {
                		giveResourceSet.add(1, SOCResourceConstants.CLAY);
                		D.ebugPrintln("Trade clay bc surplus - for road");
                	}
                }
                
        		break;
        	case SOCPossiblePiece.SETTLEMENT:
        		// trade ore first but if no ore, trade highest freqs surplus of others
        		int bestfreqs = -100;
        		int bestfreqsResource = SOCResourceConstants.UNKNOWN;
        		
        		if(resources.getAmount(SOCResourceConstants.ORE) > 0) {
        			D.ebugPrintln("Trade ore bc not need - for settlement");
        			giveResourceSet.add(1, SOCResourceConstants.ORE);
        			break;
        		}
        		
        		for(Integer key: resourcesSorted.keySet()) { 
                    if (resources.getAmount(key) > 1 && freqs[toArrayIdx(key)] > bestfreqs){
                    	bestfreqs = freqs[toArrayIdx(key)];
                    	bestfreqsResource = key;
                    }
                } 
        		
        		D.ebugPrintln("Trade " + toStringResources(bestfreqsResource) + " bc most likely to get again - for settlement");
        		if(bestfreqsResource != SOCResourceConstants.UNKNOWN) {
        			giveResourceSet.add(1, bestfreqsResource);
        		}
        		
        		break;
        	case SOCPossiblePiece.CITY:
        		// skip ore and wheat and get rid of most cards - none of those then get rid of surlpus ore or wheat
        		for(Integer key: resourcesSorted.keySet()) { 
                    if (key == SOCResourceConstants.ORE || key == SOCResourceConstants.WHEAT) {
                    	continue;
                    } else if (freqs[toArrayIdx(key)] > 6 && resources.getAmount(key) > 0){ //Not Sure What key Here
                    	D.ebugPrintln("Trade " + toStringResources(key) + "Because High Frequency and not ore or wheat - for city");
                    	giveResourceSet.add(1, key);
                    	break;
                    }
                } 
                
                if ( needed.getTotal() == 0 && resources.getAmount(SOCResourceConstants.ORE) > 3) {
                	D.ebugPrintln("Trade surplus ore - for settlement");
            		giveResourceSet.add(1, SOCResourceConstants.ORE);
            	} 
                
                if ( needed.getTotal() == 0 && resources.getAmount(SOCResourceConstants.WHEAT) > 2) {
            		giveResourceSet.add(1, SOCResourceConstants.WHEAT);
            		D.ebugPrintln("Trade surplus wheat - for settlement");
            	}
                
        		break;
        	case SOCPossiblePiece.CARD:
        		// pick between wood or CLAY - if neither give surplus sheep, wheat, or ore
        		if (resources.getAmount(SOCResourceConstants.CLAY) > 0 && freqs[toArrayIdx(SOCResourceConstants.CLAY)] > freqs[toArrayIdx(SOCResourceConstants.WOOD)]) {
        			D.ebugPrintln("Trade clay bc better freq than wood and we have it- for card");
        			giveResourceSet.add(1, SOCResourceConstants.CLAY);
        			break;
        		} else if (resources.getAmount(SOCResourceConstants.WOOD) > 0 && freqs[toArrayIdx(SOCResourceConstants.WOOD)] > freqs[toArrayIdx(SOCResourceConstants.CLAY)]) {
        			D.ebugPrintln("Trade wood bc better freq than clay and we have it- for card");
        			giveResourceSet.add(1, SOCResourceConstants.WOOD);
        			break;
        		} else if (resources.getAmount(SOCResourceConstants.CLAY) > resources.getAmount(SOCResourceConstants.WOOD)) {
        			D.ebugPrintln("Trade clay bc have more of it than wood- for card");
        			giveResourceSet.add(1, SOCResourceConstants.CLAY);
        			break;
        		} else if (resources.getAmount(SOCResourceConstants.WOOD) > resources.getAmount(SOCResourceConstants.CLAY)) {
        			D.ebugPrintln("Trade wood bc have more of it than clay- for card");
        			giveResourceSet.add(1, SOCResourceConstants.WOOD);
        			break;
        		}
        		
        		for(Integer key: resourcesSorted.keySet()) { 
        			if (key != SOCResourceConstants.WOOD && key != SOCResourceConstants.CLAY && resources.getAmount(key) > 1) {
        				D.ebugPrintln("Trade " + toStringResources(key) + " bc most surplus of that- for card");
        				giveResourceSet.add(1, key);
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
        
        D.ebugPrintln("Official giveResourceSet: " + giveResourceSet);
        D.ebugPrintln("Official getResourceSet: " + getResourceSet);
        
        if(giveResourceSet.getTotal() != 0) {
        	return makeTradeOffer(giveResourceSet, getResourceSet);
        }
        
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
        	SOCResourceSet resourcesBefore = getPlayerResources();
        	
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
	        	brain.setTradeResponseTime(100);
	        	brain.getClient().offerTrade(game, offer);
	        	
	        	while(brain.getWaitingResponse()) {
	        		D.ebugPrintln("-");
	        	}
	        	
	        	SOCResourceSet resourcesAfter = getPlayerResources();
	        	
	        	D.ebugPrintln("Resources Before: " + resourcesBefore);
	        	D.ebugPrintln("Resources After: " + resourcesAfter);
	        	
	        	return true;
        	}
        }
        
        return false;
    }
    
    public boolean attemptTradeOffer(int type) {
    	D.ebugPrintln("!!! ----- Trading " + toString(type) + " ----- !!!");
    	return tradingThinking(type);
    }*/

	public int getFirstResourceNeeded(SOCResourceSet resources) {
		for (int resource = SOCResourceConstants.CLAY; resource <= SOCResourceConstants.WOOD; resource++) {
			if (resources.contains(resource)) return resource;
		}
		return -1;
	}

	public boolean attemptPortTrade(SOCResourceSet whatIsNeeded, SOCPossiblePiece targetPiece) {
		SOCResourceSet resourcesToBuild = whatIsNeeded;
		SOCResourceSet actualToBuild = targetPiece.getResourcesToBuild();
		SOCResourceSet playerResources  = this.player.getResources();

		boolean[] portFlags = this.player.getPortFlags();
		// port type == resource type
		for (int portType = SOCBoard.CLAY_PORT; portType <= SOCBoard.WOOD_PORT; portType++) {
			// skip "no ports"
			if (!portFlags[portType]) continue;

			// break if hit all resources to build
			if (resourcesToBuild.getTotal() == 0) break;

			// get count of current resource in player's hand
			int count = playerResources.getAmount(portType);

			// subtract amount to make current target
			// eg. we don't want to trade away wood if we need wood to build
			count -= actualToBuild.getAmount(portType);

			// make 2:1 trades until no more to trade
			while (count >= 2) {
				count -= 2;
				SOCResourceSet giveResourceSet = new SOCResourceSet();
				giveResourceSet.add(2, portType);
				SOCResourceSet getResourceSet = new SOCResourceSet();
				int needed = this.getFirstResourceNeeded(resourcesToBuild);
				getResourceSet.add(1, needed);
				// remove from needed
				resourcesToBuild.subtract(1, needed);
				// make trade
				brain.getClient().bankTrade(game, giveResourceSet, getResourceSet);
			}
		}

		// misc type is special case... any 3-1
		// do it AFTER doing 2:1 trades
		if (portFlags[SOCBoard.MISC_PORT]) {
			// handle later
			for (int resource = SOCResourceConstants.CLAY; resource <= SOCResourceConstants.WOOD; resource++) {
				// break if hit all resources to build
				if (resourcesToBuild.getTotal() == 0) break;

				// get count of current resource in player's hand
				int count = playerResources.getAmount(resource);

				// subtract amount to make current target
				count -= actualToBuild.getAmount(resource);

				// make 3:1 trades until no more to trade
				while (count >= 3) {
					count -= 3;
					SOCResourceSet giveResourceSet = new SOCResourceSet();
					giveResourceSet.add(3, resource);
					SOCResourceSet getResourceSet = new SOCResourceSet();
					int needed = this.getFirstResourceNeeded(resourcesToBuild);
					getResourceSet.add(1, needed);
					// remove from needed
					resourcesToBuild.subtract(1, needed);
					// make trade
					brain.getClient().bankTrade(game, giveResourceSet, getResourceSet);
				}
			}
		}

		// return if port trading successfully fuffilled needed pieces
		return resourcesToBuild.getTotal() == 0;
	}

	@Override
	public SOCTradeOffer makeOffer(SOCPossiblePiece targetPiece) {
		int type = targetPiece.getType();
		D.ebugPrintln("----- Make Offer Thinking -----");
		SOCResourceSet needed = determineWhatIsNeeded(type);
		SOCResourceSet resources = getPlayerResources();
		D.ebugPrintln("Resources: " + resources);
		brain.setTradeResponseTime(1000);
		brain.setWaitingResponse(true);

		if(needed.getTotal() > 2) {
			return null;
		}

		SOCResourceSet getResourceSet = new SOCResourceSet();
		SOCResourceSet giveResourceSet = new SOCResourceSet();

		// Get Probabilities for Each Hex Type

		int[] freqs = new int[6];
		Arrays.fill(freqs, 0);

		Vector<SOCSettlement> settlements = player.getSettlements();
		Iterator<SOCSettlement> settlementIdx = settlements.iterator();
		while (settlementIdx.hasNext()) {
			SOCSettlement s = settlementIdx.next();

			for (int hexCoord : game.getBoard().getAdjacentHexesToNode(s.getCoordinates())) {
				int diceNumber = game.getBoard().getNumberOnHexFromCoord(hexCoord);
				// skip water
				if (diceNumber > 12 || diceNumber <= 0) continue;
				int probability = (diceNumber > 7) ? 13 - diceNumber : diceNumber - 1;
				freqs[toArrayIdx(game.getBoard().getHexTypeFromCoord(hexCoord))] += probability;
			}
		}

		D.ebugPrintln("freqs: " + Arrays.toString(freqs));

		// Get Number Of Each Resource That I Currently Have
		Map<Integer, Integer> initialResourceMap = new HashMap<>();
		LinkedHashMap<Integer, Integer> resourcesSorted = new LinkedHashMap<>();

		for (int r : resourceArray) {
			initialResourceMap.put(r, resources.getAmount(r));
		}

		initialResourceMap.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEachOrdered(x -> resourcesSorted.put(x.getKey(), x.getValue()));

		D.ebugPrintln("Resources Sorted: " + resourcesSorted);

		if(!shouldFour) {
			// Figure out what to give
			switch(type) {
				case SOCPossiblePiece.ROAD:
					// Trade sheep, wheat, or ore with high freqsuency - if don't have with high freqs, trade one with most - last case, trade surplus wood or CLAY
					boolean first = true;
					int firstResource = SOCResourceConstants.UNKNOWN;

					for(Integer key: resourcesSorted.keySet()) {
						if (key != SOCResourceConstants.WOOD && key != SOCResourceConstants.CLAY && resources.getAmount(key) > 0) {
							if (first) {
								first = false;
								firstResource = key;
							}

							if (freqs[toArrayIdx(key)] > 6 && resources.getAmount(key) > 0){ //Not Sure What key Here
								D.ebugPrintln("Trade " + toStringResources(key) + "Because High Frequency and not wood or brick - for road");
								giveResourceSet.add(1, key);
								break;
							}
						}
					}

					if (giveResourceSet.getTotal() == 0 && firstResource != SOCResourceConstants.UNKNOWN) {
						giveResourceSet.add(1, firstResource);
						D.ebugPrintln("Trade " + toStringResources(firstResource) + " bc most quantity since no high freq resources - for road");
					}

					if (giveResourceSet.getTotal() == 0) {
						if (resources.getAmount(SOCResourceConstants.WOOD) > resources.getAmount(SOCResourceConstants.CLAY) && resources.getAmount(SOCResourceConstants.WOOD) > 1) {
							giveResourceSet.add(1, SOCResourceConstants.WOOD);
							D.ebugPrintln("Trade wood bc surplus - for road");
						} else if (resources.getAmount(SOCResourceConstants.CLAY) > resources.getAmount(SOCResourceConstants.WOOD) && resources.getAmount(SOCResourceConstants.CLAY) > 1) {
							giveResourceSet.add(1, SOCResourceConstants.CLAY);
							D.ebugPrintln("Trade clay bc surplus - for road");
						}
					}

					break;
				case SOCPossiblePiece.SETTLEMENT:
					// trade ore first but if no ore, trade highest freqs surplus of others
					int bestfreqs = -100;
					int bestfreqsResource = SOCResourceConstants.UNKNOWN;

					if(resources.getAmount(SOCResourceConstants.ORE) > 0) {
						D.ebugPrintln("Trade ore bc not need - for settlement");
						giveResourceSet.add(1, SOCResourceConstants.ORE);
						break;
					}

					for(Integer key: resourcesSorted.keySet()) {
						if (resources.getAmount(key) > 1 && freqs[toArrayIdx(key)] > bestfreqs){
							bestfreqs = freqs[toArrayIdx(key)];
							bestfreqsResource = key;
						}
					}

					D.ebugPrintln("Trade " + toStringResources(bestfreqsResource) + " bc most likely to get again - for settlement");
					if(bestfreqsResource != SOCResourceConstants.UNKNOWN) {
						giveResourceSet.add(1, bestfreqsResource);
					}

					break;
				case SOCPossiblePiece.CITY:
					// skip ore and wheat and get rid of most cards - none of those then get rid of surlpus ore or wheat
					for(Integer key: resourcesSorted.keySet()) {
						if (key == SOCResourceConstants.ORE || key == SOCResourceConstants.WHEAT) {
							continue;
						} else if (freqs[toArrayIdx(key)] > 6 && resources.getAmount(key) > 0){ //Not Sure What key Here
							D.ebugPrintln("Trade " + toStringResources(key) + "Because High Frequency and not ore or wheat - for city");
							giveResourceSet.add(1, key);
							break;
						}
					}

					if ( needed.getTotal() == 0 && resources.getAmount(SOCResourceConstants.ORE) > 3) {
						D.ebugPrintln("Trade surplus ore - for settlement");
						giveResourceSet.add(1, SOCResourceConstants.ORE);
					}

					if ( needed.getTotal() == 0 && resources.getAmount(SOCResourceConstants.WHEAT) > 2) {
						giveResourceSet.add(1, SOCResourceConstants.WHEAT);
						D.ebugPrintln("Trade surplus wheat - for settlement");
					}

					break;
				case SOCPossiblePiece.CARD:
					// pick between wood or CLAY - if neither give surplus sheep, wheat, or ore
					if (resources.getAmount(SOCResourceConstants.CLAY) > 0 && freqs[toArrayIdx(SOCResourceConstants.CLAY)] > freqs[toArrayIdx(SOCResourceConstants.WOOD)]) {
						D.ebugPrintln("Trade clay bc better freq than wood and we have it- for card");
						giveResourceSet.add(1, SOCResourceConstants.CLAY);
						break;
					} else if (resources.getAmount(SOCResourceConstants.WOOD) > 0 && freqs[toArrayIdx(SOCResourceConstants.WOOD)] > freqs[toArrayIdx(SOCResourceConstants.CLAY)]) {
						D.ebugPrintln("Trade wood bc better freq than clay and we have it- for card");
						giveResourceSet.add(1, SOCResourceConstants.WOOD);
						break;
					} else if (resources.getAmount(SOCResourceConstants.CLAY) > resources.getAmount(SOCResourceConstants.WOOD)) {
						D.ebugPrintln("Trade clay bc have more of it than wood- for card");
						giveResourceSet.add(1, SOCResourceConstants.CLAY);
						break;
					} else if (resources.getAmount(SOCResourceConstants.WOOD) > resources.getAmount(SOCResourceConstants.CLAY)) {
						D.ebugPrintln("Trade wood bc have more of it than clay- for card");
						giveResourceSet.add(1, SOCResourceConstants.WOOD);
						break;
					}

					for(Integer key: resourcesSorted.keySet()) {
						if (key != SOCResourceConstants.WOOD && key != SOCResourceConstants.CLAY && resources.getAmount(key) > 1) {
							D.ebugPrintln("Trade " + toStringResources(key) + " bc most surplus of that- for card");
							giveResourceSet.add(1, key);
							break;
						}
					}

					break;
			}}

		//Figure Out What To Get
		for (int r : resourceArray) {
			if(needed.getAmount(r) > 0) {
				getResourceSet.add(1, r);
				break;
			}
		}

		D.ebugPrintln("Official giveResourceSet: " + giveResourceSet);
		D.ebugPrintln("Official getResourceSet: " + getResourceSet);

		boolean[] players_to_offer = new boolean[game.maxPlayers];
		Arrays.fill(players_to_offer, true);
		players_to_offer[playerNo] = false; // don't offer self

		for(SOCPlayer p : game.getPlayers()) {
			if (p.getPublicVP() > player.getTotalVP()) {
				players_to_offer[p.getPlayerNumber()] = false;
			}
		}

		D.ebugPrintln("Shouldfour: " + shouldFour);
		if(giveResourceSet.getTotal() != 0 && getResourceSet.getTotal() != 0) {
			SOCTradeOffer offer = new SOCTradeOffer(game.getName(), playerNo, players_to_offer, giveResourceSet, getResourceSet);

			boolean match = false;
			Iterator<SOCTradeOffer> offersMadeIter = offersMade.iterator();

			while ((offersMadeIter.hasNext() && !match))
			{
				SOCTradeOffer pastOffer = offersMadeIter.next();
				D.ebugPrintln("Past Offer: " + pastOffer);

				if ((pastOffer != null) && (pastOffer.getGiveSet().equals(giveResourceSet)) && (pastOffer.getGetSet().equals(getResourceSet)))
				{
					match = true;
				}
			}
			if(!match) {
				addToOffersMade(offer);
				return offer;
			}
			D.ebugPrintln("Claim Match");
		} else if (shouldPort) {
			attemptPortTrade(needed, targetPiece);

		} else if (shouldFour && resources.getTotal() > 5) {
			D.ebugPrintln("Attempt Four");
			for (int r : resourceArray) {
				if(resources.getAmount(r) > 3){
					switch(type) {
						case SOCPossiblePiece.ROAD:
							if( r == SOCResourceConstants.WOOD && resources.getAmount(r) - 5 >= 0){
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.CLAY && resources.getAmount(r) - 5 >= 0) {
								giveResourceSet.add(4, r);
							}
							else if (r != SOCResourceConstants.WOOD && r != SOCResourceConstants.CLAY){
								giveResourceSet.add(4, r);
							}
							break;
						case SOCPossiblePiece.SETTLEMENT:
							if( r == SOCResourceConstants.WOOD && resources.getAmount(r) - 5 >= 0){
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.CLAY && resources.getAmount(r) - 5 >= 0) {
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.WHEAT && resources.getAmount(r) - 5 >= 0) {
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.SHEEP && resources.getAmount(r) - 5 >= 0) {
								giveResourceSet.add(4, r);
							}
							else if (r != SOCResourceConstants.ORE){
								giveResourceSet.add(4, r);
							}
							break;
						case SOCPossiblePiece.CITY:
							if( r == SOCResourceConstants.ORE && resources.getAmount(r) - 7 >= 0){
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.WHEAT && resources.getAmount(r) - 6 >= 0) {
								giveResourceSet.add(4, r);
							}
							else if (r != SOCResourceConstants.ORE && r != SOCResourceConstants.WHEAT){
								giveResourceSet.add(4, r);
							}
							break;
						case SOCPossiblePiece.CARD:
							if( r == SOCResourceConstants.ORE && resources.getAmount(r) - 5 >= 0){
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.SHEEP && resources.getAmount(r) - 5 >= 0) {
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.WHEAT && resources.getAmount(r) - 5 >= 0) {
								giveResourceSet.add(4, r);
							}
							else if (r == SOCResourceConstants.WOOD || r == SOCResourceConstants.CLAY){
								giveResourceSet.add(4, r);
							}
							break;

					}

					for(SOCPlayer p : game.getPlayers()) {
						players_to_offer[p.getPlayerNumber()] = false;
					}

					//return new SOCTradeOffer(game.getName(), ourPlayerNumber, players_to_offer, giveResourceSet, getResourceSet);

					brain.getClient().bankTrade(game, giveResourceSet, getResourceSet);
					return null;
				}
			}
		}

		D.ebugPrintln("Trading returned null");
		return null;
	}

}
