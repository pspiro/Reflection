package onramp;

import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;
import tw.util.S;

public class OnrampServer {
	private Config m_config;
	static int poll = 5000;
	
	static String hash = "transactionHash";
	

	public static void main(String[] args) throws Exception {
		S.out( "Starting onramp server with %s ms polling interval", poll);
		Onramp.useProd();
		Onramp.debugOff();
		new OnrampServer( null); //Config.ask() );
	}

	public OnrampServer(Config ask) {
		m_config = ask;
		Util.executeEvery(0, poll, this::query);
	}
	
	HashMap<String, JsonObject> map = new HashMap<>();
	
	void query() {
		Util.wrap( () -> { 
			for (var trans : Onramp.getAllTransactions() ) {
				String id = trans.getString( "transactionId");
				int status = trans.getInt( "status");
				
				var old = map.get( id);
				
				if (old == null) {
					S.out( "New onramp transaction detected - " + trans);
					map.put( id, trans);
				}
				else if (old.getInt( "status") != status) {
					S.out( "Status change detected from %s to %s - %s",
							old.getInt( "status"), status, trans);
				}
				else if (!old.has( hash) && trans.has( hash) ) {
					S.out( "Hash was set to %s - %s", trans.getString( "hash"), trans);
				}
				
				map.put( id, trans);
			}
			
		});
	}
}