package redis;

import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;

public class ConfigBase {
	public enum Web3Type { Fireblocks, Refblocks };

	protected String symbolsTab;  // tab name where symbols are stored
	protected String redisHost; // not used
	protected int redisPort;
	protected Web3Type web3Type;
	
	public Web3Type web3Type() {
		return web3Type;
	}
	
	public String redisHost() { 
		return redisHost; 
	}
	
	public int redisPort() { 
		return redisPort; 
	}

	public String symbolsTab() { 
		return symbolsTab; 
	}
	
	public Tab getSymbolsTab() throws Exception {
		return NewSheet.getTab(NewSheet.Reflection, symbolsTab);
	}
	
	/** WARNING: the connection has to be closed */
	public MyRedis newRedis() throws Exception {
		return new MyRedis(redisHost, redisPort);
	}
	
}
