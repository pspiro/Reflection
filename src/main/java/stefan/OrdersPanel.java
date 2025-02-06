package stefan;

import java.util.ArrayList;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;

import common.Util;
import tw.util.DualPanel;
import tw.util.MyTableModel.SimpleTableModel;
import tw.util.S;

public class OrdersPanel extends DualPanel {
	static record Ord( Contract contract, Order order) {
		public Object getAnyDisplay(int col) {
			return switch( col) {
				case 0 -> Util.hhmmss.format( order.placedAt() );
				case 1 -> order.orderId();
				case 2 -> order.status();
				case 3 -> order.action();
				case 4 -> order.roundedQty();
				case 5 -> contract.symbol();
				case 6 -> order.orderType();
				default -> "";
			};
		}
	
		public Object getOpenDisplay(int col) {
			return switch( col) {
				case 7 -> order.orderType() == OrderType.LMT ? S.fmt2d( order.lmtPrice() ) : null;
				default -> getAnyDisplay( col);
			};
		}
	
		public Object getCloseDisplay(int col) {
			return switch( col) {
				case 7 -> S.fmt2d( order.trailStopPrice() ); // this has to get updated somehow. pas
				default -> getAnyDisplay( col);
			};
		}
	}

	private Stefan m_stefan;
	private OpenModel m_openModel = new OpenModel();
	private CloseModel m_closeModel = new CloseModel();

	public OrdersPanel(Stefan stefan) {
		m_stefan = stefan;
		
		add( m_openModel.createTable("Opening Orders"), "1");
		add( m_closeModel.createTable("Closing Orders"), "2");
	}

	public void addOpenOrder(Contract contract, Order order) {
		m_openModel.add( contract, order);
		m_openModel.fireTableDataChanged();
	}

	public void addCloseOrder(Contract contract, Order order) {
		m_closeModel.add( contract, order);
		m_closeModel.fireTableDataChanged();
	}
	
	abstract class Model extends SimpleTableModel {		
		ArrayList<Ord> m_orders = new ArrayList<>();

		@Override public int getRowCount() {
			return m_orders.size();
		}
		void add( Contract contract, Order order) {
			m_orders.add( new Ord( contract, order) );
		}
	}
	
	class OpenModel extends Model {
		OpenModel() {
			columnNames = "Submitted At,ID,Status,Action,Quantity,Symbol,Type,Limit Price".split( ",");
			justification = "llllrllr";
		}
		
		@Override public Object getValueAt_(int row, int col) {
			return m_orders.get( row).getOpenDisplay(col);
		}
	}

	class CloseModel extends Model {
		CloseModel() {
			columnNames = "Submitted At,ID,Status,Action,Quantity,Symbol,Type,Stop Price".split( ",");
			justification = "llllrlrr";
		}
		
		@Override public Object getValueAt_(int row, int col) {
			return m_orders.get( row).getCloseDisplay(col);
		}
	}

	public void refreshOpenOrders() {
		m_openModel.fireTableDataChanged();
	}

	public void refreshClosingOrders() {
		m_closeModel.fireTableDataChanged();
	}

}
