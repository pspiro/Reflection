package positions;

import reflection.Config;
import tw.google.NewSheet.Book.Tab;

public class HookConfig extends Config {
	private int hookServerPort; // move into HookConfig 
	private String hookServerUrl; // this is the webhook url  move into HookConfig
	private String hookServerChain;  // move into HookConfig
	
	@Override protected void readFromSpreadsheet(Tab tab) throws Exception {
		super.readFromSpreadsheet(tab);

		this.hookServerPort = m_tab.getInt("hookServerPort");
		this.hookServerUrl = m_tab.getRequiredString("hookServerUrl");
		this.hookServerChain = m_tab.getRequiredString("hookServerChain");
	}
	
	public int hookServerPort() {
		return hookServerPort;
	}
	
	public String hookServerUrl() {
		return hookServerUrl;
	}
	
	public String hookServerChain() {
		return hookServerChain;
	}

	/** suffix used when creating Moralis WebHook */ 
	public String getHookNameSuffix() {
		return String.format( "%s-%s", hookServerChain, hookServerPort);
	}
}
