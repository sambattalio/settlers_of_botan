package bot;

import soc.game.SOCGame;
import soc.robot.SOCRobotClient;
import soc.robot.SOCRobotBrain;
import soc.message.SOCMessage;
import soc.util.CappedQueue;
import soc.util.SOCFeatureSet;
import soc.util.SOCRobotParameters;

public class NDRobotClient extends SOCRobotClient {

	NDRobotBrain bot = null;

	public NDRobotClient(final String h, final int p, final String nn, final String pw, final String co) {
		super(h, p, nn, pw, co);
	}

	public void init() {
		super.init();
	}
	
	public static void main(String [] args) {
		String server = "localhost";
		int port = 8880;
		String name = "nd";
		String pw = "";
		String cookie = "";

		for(int i = 0; i < args.length; i++) {
			if(args.length == i + 1){
				break;
			}
			if(args[i].equals("-h") || args[i].equals("--host")) {
				server = args[++i];
			} else if(args[i].equals("-p") || args[i].equals("--port")) {
				port = Integer.parseInt(args[++i]);
			} else if(args[i].equals("-n") || args[i].equals("--name")) {
				name = args[++i];
			} else if(args[i].equals("--password")) {
				pw = args[++i];
			}else if(args[i].equals("-c") || args[i].equals("--cookie")) {
				cookie = args[++i];
			}
		}

		System.out.println(name + (pw.equals("") ? "" : ":" + pw) + "@" + server + ":" + port);

		NDRobotClient b = new NDRobotClient(server, port, name, pw, cookie);
		b.init();
	}

	protected SOCFeatureSet buildClientFeats(){
        SOCFeatureSet feats = new SOCFeatureSet(false, false);

        return feats;
    }

    public SOCRobotBrain createBrain(final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq) {
		bot = new NDRobotBrain(this, params, ga, mq);
        return null;
    }
}