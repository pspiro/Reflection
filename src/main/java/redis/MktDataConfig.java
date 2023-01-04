package redis;

import reflection.Mode;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

public class MktDataConfig {

	// program parameters
	private Mode mode = Mode.paper;  // paper or production
	private String twsMdHost;  // TWS is listening on this host
	private int twsMdPort;  // TWS is listening on this port
	private long reconnectInterval = 5000;  // when we lost connection with TWS
	private String postgresUrl;
	private String postgresUser;
	private String postgresPassword;
	private String symbolsTab;  // tab name where symbols are stored
	private String redisHost;
	private int redisPort;
	private int batchTime; // in ms

	public Mode mode() { return mode; }
	public String twsMdHost() { return twsMdHost; }
	public int twsMdPort() { return twsMdPort; }
	public String symbolsTab() { return symbolsTab; }
	public String redisHost() { return redisHost; }
	public int redisPort() { return redisPort; }
	public long reconnectInterval() { return reconnectInterval; }

	public MktDataConfig() { 
	}
	
	public void readFromSpreadsheet(String tabName) throws Exception {
		GTable tab = new GTable( NewSheet.Reflection, tabName, "Tag", "Value");

		this.redisHost = tab.get( "redisHost");
		require( S.isNotNull( this.redisHost), "redisHost is missing" );
		
		this.redisPort = tab.getInt( "redisPort");
		this.mode = S.getEnum( tab.get( "paperMode"), Mode.values() );
		this.twsMdHost = tab.get( "twsMdHost");
		this.twsMdPort = tab.getInt( "twsMdPort");
		this.postgresUrl = tab.get( "postgresUrl");
		this.postgresUser = tab.get( "postgresUser");
		this.postgresPassword = tab.get( "postgresPassword");

		this.batchTime = tab.getInt( "redisBatchTime");
		require( batchTime >= 0 && batchTime <= 5000, "redisBatchTime");
		
		this.reconnectInterval = tab.getInt( "reconnectInterval");
		require( reconnectInterval >= 1000 && reconnectInterval <= 60000, "reconnectInterval");

		this.symbolsTab = tab.get( "symbolsTab");
		require( S.isNotNull( symbolsTab), "symbolsTab is missing" );
	}
	
	private void require( boolean v, String parameter) throws Exception {
		if (!v) {
			throw new Exception( String.format( "Config parameter %s is invalid", parameter) );
		}
	}
	
	private void require(GTable t, String param, double lower, double upper) throws Exception {
		double value = t.getDouble( param);
		require( value >= lower && value <= upper, param);
	}

	/** Batch time in ms. */
	public int batchTime() {
		return batchTime;
	}

}
