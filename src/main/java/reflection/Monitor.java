package reflection;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import fireblocks.Erc20;
import fireblocks.StockToken;
import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import positions.MoralisServer;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.MyTable;
import tw.util.MyTableModel;
import tw.util.NewLookAndFeel;
import tw.util.S;

// use this to query wallet balances, it is super-quick and returns all the positions for the wallet
// https://deep-index.moralis.io/api/v2/:address/erc20	
// you could use this to easily replace the Backend method that combines it with with the market data 

public class Monitor {
	static final String chain = "goerli";  // or eth
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final String abi = Util.toJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");
	

	Records m_records = new Records();
	RecMap m_recMap = new RecMap();
	JFrame m_frame = new JFrame();
	Mod m_mod = new Mod();
	final static Config m_config = new Config();
	//MyHttpClient client = new MyHttpClient("34.125.38.193", 8383);  // will have to move to localhost or go through nginx
	
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
		
		m_frame.add( scroll);
		m_frame.add(butPanel, BorderLayout.NORTH);
		m_frame.setTitle( "Reflection Monitor");
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 800, 1000);
		m_frame.setVisible(true);
		
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
		MyJsonArray ar = new MyHttpClient("localhost", 8383)
			.get("?msg=getpositions")
			.readMyJsonArray();
		
		for (MyJsonObject obj : ar) {
			Record rec = getOrCreate( obj.getInt("conid") );
			rec.m_position = obj.getDouble("position");
			Util.require( rec.m_conid != 0 && rec.m_position != 0.0, "Invalid json for position query");
		}
		SwingUtilities.invokeLater( () -> m_mod.fireTableDataChanged() );
		
		for (Record rec : m_records) {
			if (S.isNotNull(rec.m_address) ) {
				S.out( "Querying totalSupply for %s", rec.m_symbol);
				String supply = MoralisServer.contractCall( rec.m_address, "totalSupply", abi);
				Util.require( supply != null, "Moralis contract call returned null");
				rec.m_tokens = Erc20.fromBlockchain(
						supply.replaceAll("\"", ""), // strip quotes
						StockToken.stockTokenDecimals
				);
			}
		}
		SwingUtilities.invokeLater( () -> m_mod.fireTableDataChanged() );
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
		// read master list of symbols and map conid to entry
		HashMap<Integer,ListEntry> map = new HashMap<>();
		for (ListEntry entry : NewSheet.getTab( NewSheet.Reflection, "Master-symbols").fetchRows(false) ) {
			map.put( entry.getInt("Conid"), entry);
		}
		
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, m_config.symbolsTab() ).fetchRows(false) ) {
			Stock stock = new Stock();
			if ("Y".equals( row.getString( "Active") ) ) {
				Record record = getOrCreate( row.getInt("Conid") );
				record.m_symbol = row.getString("ContractSymbol");
				record.m_address = row.getString("Token Address");  
				record.m_active = "Y";
				record.m_tokens = -1;
				record.m_position = -1;
			}
		}
	}
	
}
