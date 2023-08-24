package monitor;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.NewLookAndFeel;
import tw.util.NewTabbedPanel;
import tw.util.S;
import tw.util.NewTabbedPanel.INewTab;

// use this to query wallet balances, it is super-quick and returns all the positions for the wallet
// https://deep-index.moralis.io/api/v2/:address/erc20	
// you could use this to easily replace the Backend method that combines it with with the market data 

public class Monitor {
	static final String chain = "goerli";  // or eth
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final Stocks stocks = new Stocks();
	static final Config m_config = new Config();
	JFrame m_frame = new JFrame();
	NewTabbedPanel m_tabs = new NewTabbedPanel(true);
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new Exception( "You must specify a config tab name");
		}

		NewLookAndFeel.register();
		new Monitor( args[0] );
	}
	
	Monitor(String tabName) throws Exception {
		// read config
		S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		S.out( "  done");
		
		// read stocks
		S.out( "Reading stock list from google sheet");
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), Monitor.m_config);
		S.out( "  done");

		MiscPanel m_miscPanel = new MiscPanel();
		WalletPanel m_wallet = new WalletPanel();
		QueryPanel m_transPanel = createTransPanel();
		QueryPanel m_usersPanel = createUsersPanel();
		TokensPanel m_tokensPanel = new TokensPanel();
		PricesPanel m_pricesPanel = new PricesPanel();

		
		JButton but = new JButton("Refresh");
		but.addActionListener( e -> refresh() );
		
		JPanel butPanel = new JPanel();
		butPanel.add(but);
		
		m_tabs.addTab( "Misc", m_miscPanel);
		m_tabs.addTab( "Wallet", m_wallet);
		m_tabs.addTab( "Transactions", m_transPanel);
		m_tabs.addTab( "Users", m_usersPanel);
		m_tabs.addTab( "Tokens", m_tokensPanel);
		m_tabs.addTab( "Prices", m_pricesPanel);
		
		m_frame.add( butPanel, BorderLayout.NORTH);
		m_frame.add( m_tabs);
		m_frame.setTitle( "Reflection Monitor");
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 1000, 600);
		m_frame.setVisible(true);
		
		m_tokensPanel.initialize();
		m_pricesPanel.initialize();
	}
	
	/** called when Refresh button is clicked */
	private void refresh() {
		try {
			((RefPanel)m_tabs.current()).refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	interface RefPanel extends INewTab {
		void refresh() throws Exception; // called when Refresh button is clicked
	}

	static QueryPanel createUsersPanel() {
		String names = "aadhaar,active,address,city,country,created_at,email,id,is_black_listed,kyc_status,name,pan_number,persona_response,phone,updated_at,wallet_public_key";
		String sql = "select * from users";
		return new QueryPanel( names, sql);
	}

	public static QueryPanel createTransPanel() {
		String names = "tds,rounded_quantity,perm_id,fireblocks_id,price,action,commission,currency,timestamp,cumfill,side,quantity,avgprice,wallet_public_key,conid,exchange,time,order_id,tradekey,status,";
		String sql = """
				select *
				from crypto_transactions ct
				left join trades tr on ct.order_id = tr.order_id
				;""";
//				join commissions co on tr.tradekey = co.tradekey
		return new QueryPanel( names, sql);
	}
}
