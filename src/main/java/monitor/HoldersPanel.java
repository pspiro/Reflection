package monitor;

import java.awt.BorderLayout;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Erc20;
import reflection.ModifiableDecimal;

public class HoldersPanel extends JsonPanel {
	HoldersPanel() {
		super( new BorderLayout(), "wallet,balance");
		add( m_model.createTable() );
	}

	public void refresh(String contractAddr) {
		Util.wrap( () -> {
			HashMap<String, ModifiableDecimal> map = new Erc20( contractAddr, 18, "").getAllBalances();

			JsonArray ar = new JsonArray();
			map.forEach( (wallet, balance) -> 
					ar.add( Util.toJson( "wallet", Util.left(wallet, 8), "balance", balance ) ) );
			
			m_model.m_ar = ar;
			m_model.fireTableDataChanged();
		});
	}

}
