package fireblocks;

import redis.MyRedis;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;

public class WebhookConfig {
	// program parameters
	private String fbHost;  // TWS is listening on this host
	private int fbPort;  // TWS is listening on this port
	private String redisHost;
	private int redisPort;
	private GTable m_tab;

	public String redisHost() { return redisHost; }
	public int redisPort() { return redisPort; }
	public String fbHost() { return fbHost; }
	public int fbPort() { return fbPort; }
	
	public static WebhookConfig readFrom(String tab) throws Exception {
		return new WebhookConfig().readFromSpreadsheet(tab);
	}

	public WebhookConfig readFromSpreadsheet(String tabName) throws Exception {
		readFromSpreadsheet( NewSheet.getTab(NewSheet.Reflection, tabName) );
		return this;
	}
	
	private void readFromSpreadsheet(Tab tab) throws Exception {
		m_tab = new GTable( tab, "Tag", "Value", true);

		this.redisHost = m_tab.getRequiredString( "redisHost");
		this.redisPort = m_tab.getRequiredInt( "redisPort");
		this.fbHost = m_tab.getRequiredString( "redisHost");
		this.fbPort = m_tab.getRequiredInt( "redisPort");
	}
	
	/** WARNING: the connection has to be closed */
	public MyRedis newRedis() throws Exception {
		return new MyRedis(redisHost, redisPort);
	}
}
