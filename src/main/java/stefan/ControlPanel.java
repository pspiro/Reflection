package stefan;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

import common.Util;
import stefan.Stefan.UserStatus;
import tw.util.Circle;
import tw.util.HtmlButton;
import tw.util.HtmlButton.HtmlRadioButton;
import tw.util.MyTableModel;
import tw.util.MyTableModel.SimpleTableModel;
import tw.util.S;
import tw.util.UpperField;
import tw.util.VerticalPanel;
import tw.util.VerticalPanel.BorderPanel;

public class ControlPanel extends BorderPanel {
	private static final int size = 20;
	private Stefan m_stefan;
	private final HashSet<HtmlRadioButton> group = new HashSet<>();
	private final ArrayList<Stock> m_stocks;
	private final Model m_model = new Model();
	private final ActionListener listener = ev -> m_stefan.onUserStatusUpdated( getUserStatus() );
	private final HtmlRadioButton start = new HtmlRadioButton("Start", group, listener);
	private final HtmlRadioButton stop = new HtmlRadioButton("Stop", group, listener);
	private final HtmlRadioButton closingOnly = new HtmlRadioButton("Closing orders only", group, listener);
	private final UpperField remainingPct = new UpperField().readOnly().right();
	private final UpperField remaining = new UpperField().readOnly().right();
//	private final UpperField twsStatus = new UpperField(11).readOnly();
//	private final UpperField ibStatus = new UpperField(11).readOnly(); 
	private final Circle twsStatus = new Circle(Color.red, size);
	private final Circle ibStatus = new Circle(Color.red, size); 
	private final UpperField programStatus = new UpperField(25).readOnly();
	private final Circle programColor = new Circle( Color.red, size);
	
	// for testing market data
	UpperField symbol = new UpperField();
	UpperField price = new UpperField();
	UpperField fillPrice = new UpperField();

	public ControlPanel(Stefan stefan, ArrayList<Stock> stocks) {
		m_stefan = stefan;
		m_stocks = stocks;
		
		stop.setSelected( true);
		
		VerticalPanel topPanel = new VerticalPanel();
		topPanel.addHeader( "Controls");
		topPanel.addChoices( "Action", start, stop, closingOnly);
		topPanel.add( "Program status", programColor, programStatus);
		
		topPanel.addHeader( "Connection Status");
		topPanel.add( "TWS Connection", twsStatus);
		topPanel.add( "IB Connection", ibStatus);
		
		topPanel.addHeader( "Available for Trading");
		topPanel.add( "Remaining", remaining, remainingPct);
		
		topPanel.addHeader( "Testing");
		topPanel.add( "Symbol", symbol);
		topPanel.addChoices( "Price", price, 
				new HtmlButton( "Set", ev -> tick( symbol.getString(), price, 1) ),  
				new HtmlButton( "Tick Down", ev -> tick( symbol.getString(), price, 2) ),  
				new HtmlButton( "Tick Up", ev -> tick( symbol.getString(), price, 3) )
				);
		topPanel.addChoices( "Fill at price", fillPrice, new HtmlButton( "Fill opening order", ev -> simFill(true) ), new HtmlButton( "Fill closing order", ev -> simFill(false) ) );
		symbol.setText( "IBM");
		
		topPanel.addHeader( "Stock Table");
		
		add( topPanel, BorderLayout.NORTH);
		add( m_model.createTable() );
	}

	/** simulate filling the order */
	private void simFill(boolean open) {
		// must be active
		if (m_stefan.userStatus() == UserStatus.Inactive) {
			Util.inform( this, "Algo is inactive");
			return;
		}
		
		m_stefan.simFill( symbol.getString(), open, fillPrice.getDouble() );
	}

	/** manually tick a stock price */
	private void tick(String symbol, UpperField field, int action) {
		// must be active
		if (m_stefan.userStatus() == UserStatus.Inactive) {
			Util.inform( this, "Algo is inactive");
			return;
		}
		
		double price = field.getDouble();
		if (action == 2) {
			price -= .01;
		}
		else if (action == 3) {
			price += .01;
		}
		field.set2d( price);
		
		m_stefan.tick( symbol, Util.round( price) );
	}


	private UserStatus getUserStatus() {
		return	start.isSelected() ? UserStatus.Active :
				stop.isSelected() ? UserStatus.Inactive : UserStatus.ClosingOnly;
	}

	// put a timer or pacing on this. pas
	public void refreshStockTable() {
		m_model.fireTableDataChanged();
	}
	
	public void refreshAmounts( double total, double util) {
		remaining.set2c( total - util);
		
		double pct = util / total * 100;
		remainingPct.setText( S.fmt2d( 100 - pct) + "%");
	}
	
	public void refreshStatus( boolean tws, boolean ib, UserStatus userStatus, String program) {
		twsStatus.setColor( tws ? Color.green : Color.red);
		ibStatus.setColor( ib ? Color.green : Color.red);
//		twsStatus.setText( tws ? "Connected" : "Disconnected");
//		ibStatus.setText( ib ? "Connected" : "Disconnected");
		programColor.setColor( userStatus.getColor() );
		programStatus.setText( program);
	}
	
	class Model extends SimpleTableModel {
		Model() {
			columnNames = "Symbol,Position,Bid,Ask,Last,S Loss Trigger,S Loss Limit,S Trigger,S Line,B Line,B Trigger,B Loss Limit,B Loss Trigger,Status".split( ",");
			justification = "lrrrrrrrrrrrrl";
		}

		@Override public int getRowCount() {
			return m_stocks.size();
		}

		@Override public Object getValueAt_(int row, int col) {
			return m_stocks.get( row).getTableDisplay( col);
		}
	}

	/** this is strictly so the stocks can refresh the table */
	public MyTableModel model() {
		return m_model;
	}

}
