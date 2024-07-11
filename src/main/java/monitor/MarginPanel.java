package monitor;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JEditorPane;
import javax.swing.JTextField;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.HorzDualPanel;
import tw.util.VerticalPanel;

public class MarginPanel extends MonPanel {
	// warning, names must be trimmed
	static final String someNames = """
			placed,
			orderId,
			status,
			symbol,
			bidPrice, 
			askPrice,
			desiredQty,
			loanAmt, 
			sharesHeld,
			sharesToBuy,
			completedHow,
			wallet_public_key
			""";

	static final String endpoint = "/api/margin-get-all";

	class LeftPanel extends JsonPanel {
		public LeftPanel() {
			super( new BorderLayout(), someNames);
			add( m_model.createTable() );
		}
		
		@Override protected Object format(String key, Object value) {
			if (key.equals( "placed") && value instanceof Long) {
				return Util.yToS.format( value);
			}
			return super.format(key, value);
		}
		
		@Override protected void refresh() throws Exception {
			MyClient.getArray(Monitor.refApiBaseUrl() + endpoint, ar -> {
				setRows( ar);
				m_model.fireTableDataChanged();
			});
		}
		
		@Override public void onSelChanged(JsonObject json) {
			Util.wrap( () -> right.update( json) ); 
		}
	}

	/** Display a table from JsonArray */
	static class HtmlPanel extends JEditorPane {
		HtmlPanel() {
			setContentType("text/html");
	        setEditable(false);
		}
		
		HtmlPanel(JsonArray json) {
			this();
			setText( json);
		}
		
		void setText( JsonArray obj) {
	        setText( obj.toHtml() );
		}
	}
	
	static class RightPanel extends VerticalPanel {
		private JsonObject m_json;

		RightPanel() {
		}
		
		/** json would be null when row is deselected 
		 * @throws Exception */
		void update( JsonObject json) throws Exception {
			removeAll();
			
			if (json != null) {
				m_json = json;
				
				addOne( "wallet_public_key", "wallet_public_key");
				addOne( "mkt prices", "bidPrice", "askPrice");
				addHeader( "Entered by user");
				addOne( "stock", "symbol", "conid");
				addOne( "order prices", "stopLossPrice", "entryPrice", "profitTakerPrice");
				addOne( "amount/leverage", "amountToSpend", "leverage");
				addOne( "quantity (desired/rounded)", "desiredQty", "roundedQty");
				addOne( "misc", "goodUntil", "currency");
				
				addHeader( "Status");
				addOne( "orderId", "orderId");
				addOne( "status", "status", "completedHow");
				addOne( "trans hash/gotReceipt", "transHash", "gotReceipt");
				addOne( "shares (held/toBuy)", "sharesHeld", "sharesToBuy");
				addOne( "loan/value", "loanAmt", "value");
				
				add( "map", new HtmlPanel( json.getObject( "orderMap").toArray() ) );
			}
			
			revalidate();
			repaint();
		}

		/** pass label and all the json tags you want to display on the same line */
		private void addOne(String label, String... tags) {
			var vals = new Component[tags.length];
			for (int i = 0; i < tags.length; i++) {
				var val = m_json.get( tags[i]);
				vals[i] = new JTextField( val != null ? val.toString() : "   ");
			}
			add( label, vals); 
		}
	}
	
	final RightPanel right = new RightPanel();
	final LeftPanel left = new LeftPanel();

	MarginPanel() {
		super( new BorderLayout() );
		
		HorzDualPanel pan = new HorzDualPanel();
		pan.add( "1", left);
		pan.add( "2", right);
		
		add( pan);
	}

	@Override protected void refresh() throws Exception {
		left.refresh();
	}

//	@Override protected Object format(String key, Object value) {
//		return switch (key) {
//		case "ceatedAt" -> value instanceof Long ? Util.yToS.format( (long)value) : value; 
//		default -> value;
//		};
//	}

}
