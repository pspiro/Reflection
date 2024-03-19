package monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import positions.HookConfig;
import positions.Streams;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.UI;

class HookServerPanel extends JsonPanel {
	private JTextField m_wallet = new JTextField(30);
	private HookConfig local = new HookConfig();
	
	HookServerPanel() throws Exception {
		super( new BorderLayout(), String.format( 
				"wallet,native,approved,positions", Monitor.m_config.busd().name(), Monitor.m_config.nativeTokName() ) );
		
		JPanel top = new JPanel(new FlowLayout( FlowLayout.LEFT, 15, 8));
		top.add( m_wallet);
		top.add( new HtmlButton( "Reset wallet", e -> resetWallet() ) );
		top.add( new HtmlButton( "Reset all wallets", e -> resetAllWallets() ) );
		top.add( new HtmlButton( "Get wallet", e -> getWallet() ) );
		top.add( new HtmlButton( "JsonObject json = query 'My Wallet'", e -> myWallet() ) );
		top.add( new HtmlButton( "Debug on", e -> debugOn() ) );
		top.add( new HtmlButton( "Debug off", e -> debugOff() ) );
		top.add( new HtmlButton( "Delete hooks at Moralis", e -> deleteHooks() ) );
	
		add( top, BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	private void deleteHooks() {
		wrap( () -> {
			if (Util.confirm( this, "Are you sure you want to delete the WebHooks?") ) {
				String suffix = getSuffix();  
						
				S.out( "Deleting transfers stream");
				Streams.deleteStreamByName( String.format( 
						"Transfers-%s", suffix) );
				
				S.out( "Deleting approvals stream");
				Streams.deleteStreamByName( String.format( 
						"Approvals-%s", suffix) );
				
				UI.flash( "Done");
			}
		});
	}

	private void resetWallet() {
		wrap( () -> {
			JsonObject json = query( "/hook/reset/" + m_wallet.getText() );
			UI.flash( json.toString() );
		});
	}

	private void resetAllWallets() {
		wrap( () -> {
			JsonObject json = query( "/hook/resetall");
			UI.flash( json.toString() );
		});
	}

	private void getWallet() {
		wrap( () -> {
			JsonObject json = query( "/hook/get-wallet/" + m_wallet.getText() );
			Util.inform( this, json.toHtml() );
		});
	}

	private void myWallet() {
		wrap( () -> {
			JsonObject json = query( "/hook/mywallet/" + m_wallet.getText() );
			Util.inform( this, json.toHtml() );
		});
	}

	private void debugOn() {
		wrap( () -> {
			JsonObject json = query( "/hook/debug-on");
			UI.flash( json.toString() );
		});
	}

	private void debugOff() {
		wrap( () -> {
			JsonObject json = query( "/hook/debug-off");
			UI.flash( json.toString() );
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

	static String getSuffix() throws Exception {
		return query( "/hook/status").getString("suffix");
	}

	static JsonObject query( String uri) throws Exception {
		return MyClient.getJson( Monitor.m_config.hookBaseUrl() + uri);
	}
}
