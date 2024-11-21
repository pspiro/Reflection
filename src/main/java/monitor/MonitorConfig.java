package monitor;

import javax.swing.JOptionPane;

import chain.Chain;
import reflection.Config.MultiChainConfig;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;
import web3.Busd;
import web3.NodeInstance;
import web3.Rusd;

public class MonitorConfig extends MultiChainConfig {
	private String mdBaseUrl;	// does not end with /
	private String fbBaseUrl;
	private String hookBaseUrl;

	public String hookBaseUrl() {
		return hookBaseUrl;
	}

	public String mdBaseUrl() {
		return mdBaseUrl;
	}

	public String fbBaseUrl() {
		return fbBaseUrl;
	}

	protected void readFromSpreadsheet(Tab tab) throws Exception {
		super.readFromSpreadsheet(tab);
		
		this.mdBaseUrl = m_tab.get("mdBaseUrl");  // used only by Monitor program
		this.fbBaseUrl = m_tab.get("fbBaseUrl");  // used only by Monitor program
		this.hookBaseUrl = m_tab.get("hookBaseUrl");  // used only by Monitor program
	}
	
	public static MonitorConfig askk() throws Exception {
		java.awt.Toolkit.getDefaultToolkit().beep();
		String tab = JOptionPane.showInputDialog("Enter config tab name prefix");
		if (S.isNull(tab)) {
			System.exit(0);
		}
		
		MonitorConfig config = new MonitorConfig();
		config.readFromSpreadsheet(tab + "-config");
		return config;
	}
	
	public static Chain chain() {
		return Monitor.chain();
	}

	public Busd busd() {
		return chain().busd();
	}

	public static NodeInstance node() {
		return chain().node();
	}

	public static String nativeTokName() {
		return chain().params().platformBase();
	}

	public static Rusd rusd() {
		return chain().rusd();
	}

	public String refWalletAddr() {
		return chain().params().refWalletAddr();
	}

	public String ownerKey() throws Exception {
		return chain().params().ownerKey();
	}
}
