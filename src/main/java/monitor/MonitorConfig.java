package monitor;

import javax.swing.JOptionPane;

import reflection.Config;
import tw.google.NewSheet.Book.Tab;

public class MonitorConfig extends Config {
	private String baseUrl;
	private String mdBaseUrl;
	private String fbBaseUrl;

	public String baseUrl() {
		return baseUrl;
	}

	public String mdBaseUrl() {
		return mdBaseUrl;
	}

	public String fbBaseUrl() {
		return fbBaseUrl;
	}

	protected void readFromSpreadsheet(Tab tab) throws Exception {
		super.readFromSpreadsheet(tab);
		
		this.baseUrl = m_tab.get("baseUrl");  // used only by Monitor program
		this.mdBaseUrl = m_tab.get("mdBaseUrl");  // used only by Monitor program
		this.fbBaseUrl = m_tab.get("fbBaseUrl");  // used only by Monitor program
	}
	
	public static MonitorConfig ask() throws Exception {
		java.awt.Toolkit.getDefaultToolkit().beep();
		String tab = JOptionPane.showInputDialog("Enter config tab name prefix") + "-config";
		MonitorConfig config = new MonitorConfig();
		config.readFromSpreadsheet(tab);
		return config;
	}
}
