package monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.HtmlButton;
import tw.util.HtmlPane;
import tw.util.S;
import tw.util.UI;

class HookServerPanel extends JsonPanel {
	private JTextField m_wallet = new JTextField(30);
	private JEditorPane m_htmlPane = new HtmlPane();
	
	HookServerPanel() throws Exception {
		super( new BorderLayout(), String.format( 
				"wallet,native,approved,positions", Monitor.m_config.busd().name(), Monitor.m_config.nativeTokName() ) );
		
		JPanel top = new JPanel(new FlowLayout( FlowLayout.LEFT, 15, 8));
		top.add( m_wallet);
		top.add( new HtmlButton( "Get wallet", e -> getWallet() ) );
		top.add( new HtmlButton( "'MyWallet' query", e -> myWallet() ) );
		top.add( new HtmlButton( "Reset wallet", e -> resetWallet() ) );
		top.add( new HtmlButton( "Reset all wallets", e -> resetAllWallets() ) );
		top.add( new HtmlButton( "Debug on", e -> debugOn() ) );
		top.add( new HtmlButton( "Debug off", e -> debugOff() ) );
		top.add( new HtmlButton( "Delete hooks at Moralis", e -> deleteHooks() ) );
	
		add( top, BorderLayout.NORTH);
		add( m_htmlPane, BorderLayout.EAST);
		add( m_model.createTable() );
	}
	
	private void deleteHooks() {
		wrap( () -> {
			throw new Exception( "disabled");
//			if (Util.confirm( this, "Are you sure you want to delete the WebHooks?") ) {
//				String suffix = Monitor.m_config.getHookNameSuffix();  
//						
//				S.out( "Deleting transfers stream");
//				MoralisStreams.deleteStreamByName( String.format( 
//						"Transfers-%s", suffix) );
//				
//				S.out( "Deleting approvals stream");
//				MoralisStreams.deleteStreamByName( String.format( 
//						"Approvals-%s", suffix) );
//				
//				UI.flash( "Done");
//			}
		});
	}
	
	@Override protected void onDouble(String tag, Object val) {
		if (tag.equals( "wallet") ) {
			m_wallet.setText( val.toString() );
			getWallet();
		}
	}
	
	private void resetWallet() {
		wrap( () -> {
			JsonObject json = getJson( "/reset/" + m_wallet.getText() );
			UI.flash( json.toString() );
		});
	}

	private void resetAllWallets() {
		wrap( () -> {
			JsonObject json = getJson( "/resetall");
			UI.flash( json.toString() );
		});
	}

	private void getWallet() {
		wrap( () -> {
			var positions = getJson( "/get-wallet/" + m_wallet.getText() )
					.getArray( "positions");
			for (var pos : positions) { 
				pos.put( "description", Monitor.getDescription( pos.getString( "address") ) );
			}
			m_htmlPane.setText( positions.toHtml(true) );
		});
	}

	private void myWallet() {
		wrap( () -> {
			JsonObject json = getJson( "/mywallet/" + m_wallet.getText() );
			Util.inform( this, json.toHtml() );
		});
	}

	private void debugOn() {
		wrap( () -> {
			JsonObject json = getJson( "/debug-on");
			UI.flash( json.toString() );
		});
	}

	private void debugOff() {
		wrap( () -> {
			JsonObject json = getJson( "/debug-off");
			UI.flash( json.toString() );
		});
	}

	@Override protected Object format(String tag, Object value) {
		return switch (tag) {
			case "positions" -> ((JsonArray)value).size();
			case "native" -> S.fmt( "" + value);
			case "approved" -> S.fmt( "" + value);
			default -> value;
		};
	}
	
	@Override protected void refresh() throws Exception {
		JsonArray ar = MyClient.getArray(
				Monitor.m_config.hookBaseUrl() + 
				chain().params().hookServerSuffix() + "/get-all-wallets");
		setRows( ar);
		m_model.fireTableDataChanged();
	}

	static JsonObject getJson( String uri) throws Exception {
		return MyClient.getJson( 
				Monitor.m_config.hookBaseUrl() +  // monitor uses localhost
				chain().params().hookServerSuffix() +  // e.g. /hook/polygon 
				uri);
	}

}
