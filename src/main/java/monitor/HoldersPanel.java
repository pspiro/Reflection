package monitor;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.JLabel;

import org.json.simple.JsonArray;

import common.Util;
import fireblocks.Erc20;
import reflection.ModifiableDecimal;

/** Shows the holders for a given token (wallet and balance */
public class HoldersPanel extends JsonPanel {
	private JLabel m_title = new JLabel();
	
	HoldersPanel() {
		super( new BorderLayout(), "wallet,balance");
		add( m_title, BorderLayout.NORTH);
		add( m_model.createTable() );
	}

	@Override protected void refresh() throws Exception {
	}

	public void refresh(Erc20 token) {  // the decimal is wrong here, that's why rusd doesn't work
		Util.wrap( () -> {
			m_title.setText( token.getName() );
			
			HashMap<String, ModifiableDecimal> map = token.getAllBalances();

			JsonArray ar = new JsonArray();
			map.forEach( (wallet, balance) -> { 
				if (balance.value() >= .009) {
					ar.add( Util.toJson( "wallet", Util.left(wallet, 8), "balance", balance ) );
				}
			});
			
			setRows( ar);
			m_model.fireTableDataChanged();
		});
	}


}
