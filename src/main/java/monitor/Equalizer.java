package monitor;

import javax.swing.SwingUtilities;

import fireblocks.Fireblocks;
import fireblocks.StockToken;
import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import monitor.Monitor.Record;
import positions.Wallet;
import reflection.Config;
import reflection.Util;
import tw.util.S;

public class Equalizer {
	Config m_config;
	
	public static void main(String[] args) {
		//new Equalizer().run();
	}

	private void run() throws Exception {
		m_config = Config.readFrom("Dt-config");
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
//		// or this whole thing can run in a browser and be fed only html?
//		S.out( "Querying stock positions");
//		MyJsonArray ar = new MyHttpClient("localhost", 8383)
//			.get("?msg=getpositions")
//			.readMyJsonArray();
//		
//		for (MyJsonObject obj : ar) {
//			Record rec = getOrCreate( obj.getInt("conid") );
//			rec.m_position = obj.getDouble("position");
//			Util.require( rec.m_conid != 0 && rec.m_position != 0.0, "Invalid json for position query");
//		}
//		SwingUtilities.invokeLater( () -> m_mod.fireTableDataChanged() );
//		
//		for (Record rec : m_records) {
//			if (S.isNotNull(rec.m_address) ) {
//				S.out( "Querying totalSupply for %s", rec.m_symbol);
//				rec.m_tokens = new StockToken( rec.m_address).queryTotalSupply();
//			}
//		}
//		SwingUtilities.invokeLater( () -> m_mod.fireTableDataChanged() );
//		
//		Wallet refWallet = Fireblocks.getWallet("RefWallet");
//
//		double usdc = refWallet.getBalance(m_config.busdAddr());
//		SwingUtilities.invokeLater( () -> m_usdc.setText( S.fmt2(usdc) ) );
//		SwingUtilities.invokeLater( () -> m_usdc2.setText( S.fmt2(usdc) ) );
//
//		double nativeBal = refWallet.getNativeTokenBalance();
//		SwingUtilities.invokeLater( () -> m_nativeToken.setText( S.fmt2(nativeBal) ) );
//
//		double admin1Bal = Fireblocks.getWallet("Admin1").getNativeTokenBalance();
//		SwingUtilities.invokeLater( () -> m_admin1.setText( S.fmt2(admin1Bal) ) );
//
//		double admin2Bal = Fireblocks.getWallet("Admin2").getNativeTokenBalance();
//		SwingUtilities.invokeLater( () -> m_admin2.setText( S.fmt2(admin2Bal) ) );
//
//		double rusd = m_config.rusd().queryTotalSupply();
//		SwingUtilities.invokeLater( () -> m_rusd.setText( S.fmt2(rusd) ) );
//		
//		double val = new MyHttpClient("localhost", 8383)
//				.get( "/?msg=getCashBal")
//				.readMyJsonObject()
//				.getDouble("TotalCashValue");
//		SwingUtilities.invokeLater( () -> m_cash.setText( S.fmt2(val) ) );
	}	
}
