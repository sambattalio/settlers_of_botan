package bot;

import soc.baseclient.ServerConnectInfo;
import soc.game.SOCGame;
import soc.game.SOCLRPathData;
import soc.game.SOCPlayer;
import soc.message.*;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCFeatureSet;
import soc.util.SOCRobotParameters;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NDRobotClient extends SOCRobotClient {
    public static String SHOULD_CALC_STAT = "bot.stats";

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
    public void treat(SOCMessage mes) {
        if(mes.getType() == SOCMessage.GAMESTATS) {
            super.treat(mes, true);
        }
        if(mes.getType() == SOCMessage.TURN) {
            handleTURN((SOCTurn) mes);
        }
        super.treat(mes);
    }

    private String culmulativePoints;
    private String culmulativeResources;
    private String[] othersPoints;

    @Override
    protected void handleJOINGAME(SOCJoinGame mes) {
        super.handleJOINGAME(mes);
        culmulativePoints = "0";
        culmulativeResources = "0";
        othersPoints = new String[]{"0", "0", "0", "0"};
    }

    @Override
    protected void handleTURN(SOCTurn mes) {
        if(System.getProperty(SHOULD_CALC_STAT) != null) {
            SOCGame game = this.games.get(mes.getGame());
            SOCPlayer player = game.getPlayer(this.getNickname());
            culmulativePoints += "," + player.getTotalVP();
            IntStream.range(0, game.getPlayers().length - 1)
                    .filter(i -> game.getPlayer(i) != player)
                    .forEach(i -> othersPoints[i] += "," + game.getPlayers()[i].getTotalVP());
            culmulativeResources += "," + player.getResources().getTotal();
        }
        super.handleTURN(mes);
    }

    @Override
    protected void handleGAMESTATS(SOCGameStats mes) {
        if(System.getProperty(SHOULD_CALC_STAT) != null) {
            SOCGame game = this.games.get(mes.getGame());
            SOCPlayer player = game.getPlayer(this.getNickname());
            if (player.getTotalVP() == 0) {
                return;
            }
            String stats = (player.getSettlements().size() + player.getCities().size()) + "," +
                    player.getSettlements().size() + "," +
                    player.getCities().size() + "," +
                    player.getLRPaths().stream().mapToInt(SOCLRPathData::getLength).max().orElse(0) + "," +
                    player.getNumKnights() + "," +
                    player.getTotalVP() + "," +
                    (player.getTotalVP() == 10 ? 1 : 0);
            System.err.println("STATS:" + stats);
            System.err.println("POINTS:" + culmulativePoints);
            System.err.println("RESOURCES:" + culmulativeResources);
            try {
                new PrintStream(new FileOutputStream("./stat.log", true)).println(stats);
                new PrintStream(new FileOutputStream("./points.log", true)).println(culmulativePoints);
                new PrintStream(new FileOutputStream("./other-points.log", true)).println(String.join("\n", othersPoints));
                new PrintStream(new FileOutputStream("./resources.log", true)).println(culmulativeResources);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        super.handleGAMESTATS(mes);
    }

    @Override
    public SOCRobotBrain createBrain(final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq) {
        return new NDRobotBrain(this, params, ga, mq);
    }
}