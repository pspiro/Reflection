package monitor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellRenderer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Fireblocks;
import fireblocks.StockToken;
import http.MyHttpClient;
import positions.Wallet;
import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.MyTable;
import tw.util.MyTableModel;
import tw.util.NewLookAndFeel;
import tw.util.NewTabbedPanel;
import tw.util.S;
import tw.util.VerticalPanel;

// use this to query wallet balances, it is super-quick and returns all the positions for the wallet
// https://deep-index.moralis.io/api/v2/:address/erc20	
// you could use this to easily replace the Backend method that combines it with with the market data 

public class Monitor {
	static final String chain = "goerli";  // or eth
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	

	Records m_records = new Records();
	RecMap m_recMap = new RecMap();
	JFrame m_frame = new JFrame();
	Mod m_mod = new Mod();
	private JTextField m_usdc = new JTextField(10);
	private JTextField m_rusd = new JTextField(10);
	private JTextField m_usdc2 = new JTextField(10);
	private JTextField m_nativeToken = new JTextField(10);
	private JTextField m_admin1 = new JTextField(10);
	private JTextField m_admin2 = new JTextField(10);
	private JTextField m_cash = new JTextField(10);
	
	final static Config m_config = new Config();
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new Exception( "You must specify a config tab name");
		}

		NewLookAndFeel.register();
		new Monitor( args[0] );
	}

	Monitor(String tabName) throws Exception {
		// read config settings from google sheet
		S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		S.out( "  done");
		
		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");
		
		JTable tab = new MyTable( m_mod);
		
		JScrollPane scroll = new JScrollPane(tab);
		
//		JPanel p = new JPanel(new BorderLayout() );
//		p.add( scroll);
		
		JButton but = new JButton("Refresh");
		but.addActionListener( e -> refresh() );
		
		JPanel butPanel = new JPanel();
		butPanel.add(but);
		
		JPanel refPanel = new JPanel();
		refPanel.setBorder( new TitledBorder("Native Token Balances") );
		refPanel.add( new JLabel("RefWallet"));
		refPanel.add( m_nativeToken);
		refPanel.add( new JLabel("Admin1"));
		refPanel.add( m_admin1);
		refPanel.add( new JLabel("Admin2"));
		refPanel.add( m_admin2);
		
		VerticalPanel rusdPanel = new VerticalPanel();
		rusdPanel.setBorder( new TitledBorder("RUSD"));
		rusdPanel.add( "RUSD Outstanding", m_rusd);
		rusdPanel.add( "USDC in RefWallet", m_usdc2);
		rusdPanel.add( "Cash in brokerage", m_cash);
		
		JPanel mainPanel = new JPanel( new BorderLayout() );
		mainPanel.add( scroll);
		mainPanel.add(rusdPanel, BorderLayout.EAST);
		mainPanel.add(butPanel, BorderLayout.NORTH);
		
		
		NewTabbedPanel tabs = new NewTabbedPanel();
		tabs.addTab( "Main", mainPanel);
		
		m_frame.add( tabs);
		m_frame.setTitle( "Reflection Monitor");
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 800, 1000);
		m_frame.setVisible(true);
		m_frame.add(refPanel, BorderLayout.SOUTH);
		
		refresh();
	}
	
	private void refresh() {
		try {
			refresh_();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void refresh_() throws Exception {
		// or this whole thing can run in a browser and be fed only html?
		S.out( "Querying stock positions");
		JsonArray ar = new MyHttpClient("localhost", 8383)
			.get("?msg=getpositions")
			.readMyJsonArray();
		
		for (JsonObject obj : ar) {
			Record rec = getOrCreate( obj.getInt("conid") );
			rec.m_position = obj.getDouble("position");
			Util.require( rec.m_conid != 0 && rec.m_position != 0.0, "Invalid json for position query");
		}
		SwingUtilities.invokeLater( () -> m_mod.fireTableDataChanged() );
		
		for (Record rec : m_records) {
			if (S.isNotNull(rec.m_address) ) {
				S.out( "Querying totalSupply for %s", rec.m_symbol);
				rec.m_tokens = new StockToken( rec.m_address).queryTotalSupply();
			}
		}
		SwingUtilities.invokeLater( () -> m_mod.fireTableDataChanged() );
		
		Wallet refWallet = Fireblocks.getWallet("RefWallet");

		double usdc = refWallet.getBalance(m_config.busdAddr());
		SwingUtilities.invokeLater( () -> m_usdc.setText( S.fmt2(usdc) ) );
		SwingUtilities.invokeLater( () -> m_usdc2.setText( S.fmt2(usdc) ) );

		double nativeBal = refWallet.getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_nativeToken.setText( S.fmt2(nativeBal) ) );

		double admin1Bal = Fireblocks.getWallet("Admin1").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin1.setText( S.fmt2(admin1Bal) ) );

		double admin2Bal = Fireblocks.getWallet("Admin2").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin2.setText( S.fmt2(admin2Bal) ) );

		double rusd = m_config.rusd().queryTotalSupply();
		SwingUtilities.invokeLater( () -> m_rusd.setText( S.fmt2(rusd) ) );
		
		double val = new MyHttpClient("localhost", 8383)
				.get( "/?msg=getCashBal")
				.readMyJsonObject()
				.getDouble("TotalCashValue");
		SwingUtilities.invokeLater( () -> m_cash.setText( S.fmt2(val) ) );
	}


	private Record getOrCreate(int conid) {
		Record rec = m_recMap.get(conid);
		if (rec == null) {
			rec = new Record(conid);
			m_records.add(rec);
			m_recMap.put( conid, rec);
		}
		return rec;
	}


	class Mod extends MyTableModel {
		public TableCellRenderer getRenderer(int row, int col) {
			return col == 0 ? DEFAULT : RIGHT_RENDERER;
		}
		@Override public int getRowCount() {
			return m_records.size();
		}

		@Override public int getColumnCount() {
			return 6;
		}
		
		@Override public String getColumnName(int col) {
			switch( col) {
				case 0: return "Symbol";
				case 1: return "Conid";
				case 2: return "Tokens";
				case 3: return "Position";
				case 4: return "Difference";
				case 5: return "Active";
				default: return null;
			}
		}

		@Override public Object getValueAt(int row, int col) {
			Record record = m_records.get(row);
			
			if (record == null) {
				S.out( "Error: no row at index %s", row);
				return null;
			}
			
			switch( col) {
				case 0: return record.m_symbol;
				case 1: return record.m_conid;
				case 2: return S.fmt2(record.m_tokens);
				case 3: return S.fmt2(record.m_position);
				case 4: return S.fmt2(record.difference());
				case 5: return record.m_active;
				default: return null;
			}
		}
	}
	
	class Record {
		String m_symbol;
		int m_conid;
		String m_address;
		double m_tokens;
		double m_position;
		String m_active;

		Record(int conid) {
			m_conid = conid;
		}
		
		double difference() {
			return Math.abs(m_tokens - m_position);
		}
	}
	
	class Records extends ArrayList<Record> {
	}
	
	class RecMap extends HashMap<Integer,Record> {
	}
	
	private void readStockListFromSheet() throws Exception {
		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), m_config);
		
		stocks.stockSet().forEach( stock -> {
			Record record = getOrCreate( stock.getConidInt() );
			record.m_symbol = stock.getSymbol();
			record.m_address = stock.getSmartContractId();  
			record.m_active = "Y";
			record.m_tokens = -1;
			record.m_position = -1;
		});
	}
}
