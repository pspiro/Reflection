package positions;

import reflection.Config;
import tw.google.NewSheet.Book.Tab;

class HookServerConfig extends Config { // you could change this to ConfigBase except for the fireblocks, so move it
	private String hookServerUrl;
	private String hookServerChain;

	protected void readFromSpreadsheet(Tab tab) throws Exception {
		super.readFromSpreadsheet(tab);
		
		hookServerUrl = m_tab.getRequiredString("hookServerUrl");
		hookServerChain = m_tab.getRequiredString("hookServerChain");
	}

	
	String hookServerUrl() {
		return hookServerUrl;
	}
	
	String hookServerChain() {
		return hookServerChain;
	}
}