package monitor;

import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;

import org.json.simple.JsonObject;

import monitor.Monitor.RefPanel;

/** Joins the order and the trade */  // you should join the commission here as well 
public class TransPanel extends QueryPanel implements RefPanel {
	static String names = "timestamp,wallet_public_key,action,quantity,conid,price,status,tds,rounded_quantity,perm_id,fireblocks_id,commission,currency,cumfill,side,avgprice,exchange,time,order_id,tradekey";
	static String sql = """
			select *
			from crypto_transactions ct
			left join trades tr on ct.order_id = tr.order_id
			limit 100
			;""";

	TransPanel() {
		super( names, sql);
	}
	
	@Override protected JsonModel createModel(String allNames, String sql) {
		return new TransModel(allNames, sql);
	}
	
	@Override public void onRightClick(MouseEvent e, int row, int col) {
	}
	
	@Override public void closed() {
	}
	
	static SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");

	class TransModel extends QueryModel {
		public TransModel(String allNames, String sql) {
			super(allNames, sql);
		}

		@Override public void adjust(JsonObject obj) {
			obj.update( "timestamp", val -> fmt.format( (Long)val * 1000) ); 
		}
	}
}
