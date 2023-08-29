package monitor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.StockToken;
import http.MyHttpClient;
import monitor.Monitor.RefPanel;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.MyTable;
import tw.util.MyTableModel;
import tw.util.S;

public class TokensPanel extends JPanel implements RefPanel {
	Mod m_model = new Mod();
	Records m_records = new Records();
	RecMap m_recMap = new RecMap();

	TokensPanel() {
		super( new BorderLayout() );
		add( m_model.createTable() );
	}
	
	public void refresh() throws Exception {
		S.out( "Refreshing Tokens panel");
		
		// or this whole thing can run in a browser and be fed only html?
		S.out( "Querying stock positions");
		JsonArray ar = new MyHttpClient("localhost", 8383)
			.get("?msg=getpositions")
			.readJsonArray();
		
		for (JsonObject obj : ar) {
			Record rec = getOrCreate( obj.getInt("conid") );
			rec.m_position = obj.getDouble("position");
			Util.require( rec.m_conid != 0 && rec.m_position != 0.0, "Invalid json for position query");
		}
		SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
		
		for (Record rec : m_records) {
			if (S.isNotNull(rec.m_address) ) {
				S.out( "Querying totalSupply for %s", rec.m_symbol);
				rec.m_tokens = new StockToken( rec.m_address).queryTotalSupply();
			}
		}
		SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
		
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
	
	void initialize() throws Exception {
		
		Monitor.stocks.stockSet().forEach( stock -> {
			Record record = getOrCreate( stock.getConid() );
			record.m_symbol = stock.getSymbol();
			record.m_address = stock.getSmartContractId();  
			record.m_active = "Y";
			record.m_tokens = -1;
			record.m_position = -1;
		});
		
		m_model.fireTableDataChanged();
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

	@Override public void activated() {
	}

	@Override public void closed() {
	}

}
