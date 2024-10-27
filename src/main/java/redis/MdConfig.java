package redis;

import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

public class MdConfig {
	// program parameters
	private String twsMdHost;  // TWS is listening on this host
	private int twsMdPort;  // TWS is listening on this port
	private int twsMdClientId;
	private long reconnectInterval = 5000;  // when we lost connection with TWS
	private int redisBatchTime; // in ms
	private int mdsPort;
	private boolean twsDelayed;
	private boolean simulateBidAsk;
	private String symbolsTab;

	public String twsMdHost() { return twsMdHost; }
	public int twsMdPort() { return twsMdPort; }
	public long reconnectInterval() { return reconnectInterval; }
	public int twsMdClientId() { return twsMdClientId; }
	public int mdsPort() { return mdsPort; }
	public boolean twsDelayed() { return twsDelayed; }

	public MdConfig() { 
	}
	
	public void readFromSpreadsheet(String tabName) throws Exception {
		S.out( "Using config tab %s", tabName);
		GTable tab = new GTable( NewSheet.Reflection, tabName, "Tag", "Value");

		this.twsMdHost = tab.getRequiredString( "twsMdHost");
		this.twsMdPort = tab.getRequiredInt( "twsMdPort");
		this.twsMdClientId = tab.getRequiredInt( "twsMdClientId");
		this.mdsPort = tab.getRequiredInt("mdsPort");
		this.twsDelayed = tab.getBoolean("twsDelayed");
		this.simulateBidAsk = tab.getBoolean("simulateBidAsk");
		this.symbolsTab = tab.getRequiredString( "symbolsTab");
		
		this.redisBatchTime = tab.getRequiredInt( "redisBatchTime");
		require( redisBatchTime >= 0 && redisBatchTime <= 5000, "redisBatchTime");
		
		this.reconnectInterval = tab.getRequiredInt( "reconnectInterval");
		require( reconnectInterval >= 1000 && reconnectInterval <= 60000, "reconnectInterval");
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
	public boolean simulateBidAsk() {
		return simulateBidAsk;
	}
	
	String symbolsTab() {
		return symbolsTab;
	}

}
