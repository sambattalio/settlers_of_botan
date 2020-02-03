package bot;

import soc.robot.SOCRobotClient;

public class NDBot extends SOCRobotClient {

	public NDBot(final String h, final int p, final String nn, final String pw, final String co) {
		super(h, p, nn, pw, co);
	}

	public void init() {
		System.out.println("Hello");
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
		NDBot b = new NDBot(server, port, name, pw, cookie);
		b.init();
	}
}