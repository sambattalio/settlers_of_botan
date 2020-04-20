package bot;

import soc.baseclient.ServerConnectInfo;
import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCFeatureSet;
import soc.util.SOCRobotParameters;

public class NDRobotClient extends SOCRobotClient {

    public NDRobotClient(ServerConnectInfo sci, String nn, String pw) throws IllegalArgumentException {
        super(sci, nn, pw);
    }

    public void init() {
        super.init();
    }

    public static void main(String[] args) {
        String server = "localhost";
        int port = 8880;
        String name = "nd";
        String pw = "";
        String cookie = "";

        for (int i = 0; i < args.length; i++) {
            if (args.length == i + 1) {
                break;
            }
            if (args[i].equals("-h") || args[i].equals("--host")) {
                server = args[++i];
            } else if (args[i].equals("-p") || args[i].equals("--port")) {
                port = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-n") || args[i].equals("--name")) {
                name = args[++i];
            } else if (args[i].equals("--password")) {
                pw = args[++i];
            } else if (args[i].equals("-c") || args[i].equals("--cookie")) {
                cookie = args[++i];
            }
        }

        System.out.println(name + (pw.equals("") ? "" : ":" + pw) + "@" + server + ":" + port);

        ServerConnectInfo serverConnectInfo = new ServerConnectInfo(server, port, cookie);
        NDRobotClient b = new NDRobotClient(serverConnectInfo, name, pw);
        b.init();
    }

    protected SOCFeatureSet buildClientFeats() {
        SOCFeatureSet feats = new SOCFeatureSet(false, false);

        return feats;
    }

	@Override
    public SOCRobotBrain createBrain(final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq) {
        return new NDRobotBrain(this, params, ga, mq);
    }
}