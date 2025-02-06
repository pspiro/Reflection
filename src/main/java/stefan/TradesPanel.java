package stefan;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import com.ib.client.Contract;
import com.ib.client.Execution;

import tw.util.MyTableModel.SimpleTableModel;
import tw.util.S;

public class TradesPanel extends JPanel {
	static record Trade( Contract contract, Execution execution) {
		public String getTableDisplay(int col) {
			return switch( col) {
				case 0 -> execution.time(); 
				case 1 -> execution.side();
				case 2 -> execution.shares().toDouble().toString();
				case 3 -> contract.symbol();
				case 4 -> S.fmt2d( execution.avgPrice() );
				case 5 -> S.fmt2c( execution.avgPrice() * execution.shares().toDouble() );
				default -> "";
			};
		}
	}
	
	private Stefan m_stefan;
	private ArrayList<Trade> m_trades = new ArrayList<>();
	private final Model model = new Model();

	public TradesPanel(Stefan stefan) {
		m_stefan = stefan;
		
		setLayout( new BorderLayout() );
		add( model.createTable() );
	}
	
	void addTrade( Contract contract, Execution execution) {
		m_trades.add( new Trade( contract, execution) );
		model.fireTableDataChanged();
	}
	
	class Model extends SimpleTableModel {
		Model() {
			columnNames = "Date/time,Action,Quantity,Symbol,Price,Amount".split( ",");
			justification = "llrlrr";
		}

		@Override public int getRowCount() {
			return m_trades.size();
		}

		@Override public Object getValueAt_(int row, int col) {
			return m_trades.get( row).getTableDisplay( col);
		}
	}

}
