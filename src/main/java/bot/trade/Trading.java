package bot.trade;

import soc.game.*;
import soc.robot.*;
import soc.debug.D;

import bot.NDRobotNegotiator;

public class Trading extends SOCRobotNegotiator {
    private int[] resourceArray = new int[6];

    public Trading(SOCRobotBrain br) {
        super(br);
	D.ebugPrintln("!!! ----- Trading ----- !!!");

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

    public void tradingThinking(int type) {
	SOCResourceSet needed = determineWhatIsNeeded(type);

	
    }

    public boolean makeTradeOffer() {
	return false;
    }
}
