package tw.google;

import java.util.HashMap;



public class Account {
	private static final String R = "Register";
	
	private String m_name;
	private String m_code;
	private String m_sheet;
	private String m_tab;
	private String m_fund;
	
	String name() { return m_name; }
	String code() { return m_code; }
	String sheet() { return m_sheet; }
	String tab() { return m_tab; }
	String fund() { return m_fund; }
	
	Account( String name, String code, String sheet, String tab, String fund) {
		m_name = name;
		m_code = code;
		m_sheet = sheet;
		m_tab = tab;
		m_fund = fund;
	}
	
	Account( String name, String code, String sheet, String tab) {
		this( name, code, sheet, tab, null);
	}
	
	@Override public String toString() {
		return m_name;
	}
	
	static Account[] accountss = { 
			new Account( "Maplewood", "Maplewood", "Maplewood", "Income and expenses"), 
			new Account( "Brisco Funding", "WB BF", "Brisco Funding", R, "BF"),
			new Account( "Brisco High Yield", "WB HY", "Brisco Funding", R, "HY"),
			new Account( "Trade Winds", "TWB/TW", "Trade Winds", R),
			new Account( "RJJ Wholesalers", "TWB/RJJW", "RJJ Wholesalers", R),
			
	};

	static HashMap<String,Account> accountMap = new HashMap<String,Account>();

	static {
		for (Account account : accountss) {
			accountMap.put( account.m_name, account);
		}
	}
	
	static class sheet {
		String m_name;
		String m_tab;
	}
	

}
