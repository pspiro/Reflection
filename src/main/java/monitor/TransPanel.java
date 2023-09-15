package monitor;

import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import monitor.Monitor.RefPanel;

public class TransPanel extends QueryPanel implements RefPanel {
	static String names = "wallet_public_key,action,quantity,conid,price,status,tds,rounded_quantity,perm_id,fireblocks_id,commission,currency,timestamp,cumfill,side,avgprice,exchange,time,order_id,tradekey";
	static String sql = """
			select *
			from crypto_transactions ct
			left join trades tr on ct.order_id = tr.order_id
			;""";

	TransPanel() {
		super( names, sql);
	}
	
	@Override public void onRightClick(MouseEvent e, int row, int col) {
	}
	
	@Override public void closed() {
	}
}
