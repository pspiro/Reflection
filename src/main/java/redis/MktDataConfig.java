package redis;

import reflection.Util;
import tw.google.GTable;
import tw.google.NewSheet;

public class MktDataConfig extends ConfigBase {
	// program parameters
	private Mode mode = Mode.paper;  // paper or production
	private String twsMdHost;  // TWS is listening on this host
	private int twsMdPort;  // TWS is listening on this port
	private int twsMdClientId;
	private long reconnectInterval = 5000;  // when we lost connection with TWS
	private String redisHost;
	private int redisPort;
	private int redisBatchTime; // in ms

	public Mode mode() { return mode; }
	public String twsMdHost() { return twsMdHost; }
	public int twsMdPort() { return twsMdPort; }
	public String redisHost() { return redisHost; }
	public int redisPort() { return redisPort; }
	public long reconnectInterval() { return reconnectInterval; }
	public int twsMdClientId() { return twsMdClientId; }

	public MktDataConfig() { 
	}
	
	public void readFromSpreadsheet(String tabName) throws Exception {
		GTable tab = new GTable( NewSheet.Reflection, tabName, "Tag", "Value");

		this.redisHost = tab.getRequiredString( "redisHost");
		this.redisPort = tab.getRequiredInt( "redisPort");
		this.mode = Util.getEnum( tab.get( "paperMode"), Mode.values() );
		this.twsMdHost = tab.getRequiredString( "twsMdHost");
		this.twsMdPort = tab.getRequiredInt( "twsMdPort");
		this.twsMdClientId = tab.getRequiredInt( "twsMdClientId");

		this.redisBatchTime = tab.getRequiredInt( "redisBatchTime");
		require( redisBatchTime >= 0 && redisBatchTime <= 5000, "redisBatchTime");
		
		this.reconnectInterval = tab.getRequiredInt( "reconnectInterval");
		require( reconnectInterval >= 1000 && reconnectInterval <= 60000, "reconnectInterval");

		this.symbolsTab = tab.getRequiredString( "symbolsTab");
	}
	
	private void require( boolean v, String parameter) throws Exception {
		if (!v) {
			throw new Exception( String.format( "Config parameter %s is invalid", parameter) );
		}
	}
	
	/** Batch time in ms. */
	public int redisBatchTime() {
		return redisBatchTime;
	}

}
