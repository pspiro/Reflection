package monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.HtmlButton;
import tw.util.UI;

class HookServerPanel extends JsonPanel {
	private JTextField m_wallet = new JTextField(30);
	
	HookServerPanel() throws Exception {
		super( new BorderLayout(), String.format( 
				"wallet,native,approved,positions", Monitor.m_config.busd().name(), Monitor.m_config.nativeTok() ) );
		
		JPanel top = new JPanel(new FlowLayout( FlowLayout.LEFT, 15, 8));
		top.add( m_wallet);
		top.add( new HtmlButton( "Reset wallet", e -> resetWallet() ) );
		top.add( new HtmlButton( "Reset all wallets", e -> resetAllWallets() ) );
		top.add( new HtmlButton( "Get wallet", e -> getWallet() ) );
		top.add( new HtmlButton( "Query 'My Wallet'", e -> myWallet() ) );
		top.add( new HtmlButton( "Debug on", e -> debugOn() ) );
		top.add( new HtmlButton( "Debug off", e -> debugOff() ) );
	
		add( top, BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	private void resetWallet() {
		Util.wrap( () -> {
			String str = MyClient.getString( Monitor.m_config.hookBaseUrl() + "/hook/reset/" + m_wallet.getText() );
			UI.flash( str);
		});
	}

	private void resetAllWallets() {
		Util.wrap( () -> {
			String str = MyClient.getString( Monitor.m_config.hookBaseUrl() + "/hook/resetall");
			UI.flash( str);
		});
	}

	private void getWallet() {
		Util.wrap( () -> {
			JsonObject json = MyClient.getJson( Monitor.m_config.hookBaseUrl() + "/hook/get-wallet/" + m_wallet.getText() );
			Util.inform( this, json.toHtml() );
		});
	}

	private void myWallet() {
		Util.wrap( () -> {
			JsonObject json = MyClient.getJson( Monitor.m_config.hookBaseUrl() + "/hook/mywallet/" + m_wallet.getText() );
			Util.inform( this, json.toHtml() );
		});
	}

	private void debugOn() {
		Util.wrap( () -> {
			String str = MyClient.getString( Monitor.m_config.hookBaseUrl() + "/hook/debug-on");
			UI.flash( str);
		});
	}

	private void debugOff() {
		Util.wrap( () -> {
			String str = MyClient.getString( Monitor.m_config.hookBaseUrl() + "/hook/debug-off");
			UI.flash( str);
		});
	}

	@Override protected Object format(String tag, Object value) {
		if (tag.equals("positions") ) {
			return ((JsonArray)value).size();
		}
		return value;
	}
	
	@Override protected void refresh() throws Exception {
		JsonArray ar = MyClient.getArray(Monitor.m_config.hookBaseUrl() + "/hook/get-all-wallets");
		setRows( ar);
		m_model.fireTableDataChanged();
	}
}