package monitor;

import javax.swing.JOptionPane;

import reflection.Config;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;

public class MonitorConfig extends Config {
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
	
	public static MonitorConfig ask() throws Exception {
		java.awt.Toolkit.getDefaultToolkit().beep();
		String tab = JOptionPane.showInputDialog("Enter config tab name prefix");
		if (S.isNull(tab)) {
			System.exit(0);
		}
		
		MonitorConfig config = new MonitorConfig();
		config.readFromSpreadsheet(tab + "-config");
		return config;
	}
}
