package test;

import common.Util;
import fireblocks.FbServer;
import monitor.Monitor;
import redis.MdServer;
import reflection.Main;

public class StartAll {
	public static void main(String[] args) throws Exception {
		Util.execute( () -> FbServer.main( new String[] { "Dt-config" }) );
		Util.execute( () -> MdServer.main(new String[] { "Dt-mds"}) );
		Util.execute( () -> Main.main(new String[] { "Dt-config"}) );
		Monitor.main(null);
	}
}
