package monitor;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import positions.Wallet;
import reflection.Stock;
import tw.util.S;
import tw.util.VerticalPanel;

public class WalletPanel extends JsonPanel {
	private static final double minBalance = .0001;
	private final JTextField m_wallet = new JTextField(32); 
	private final JLabel m_rusd = new JLabel(); 
	private final JLabel m_usdc = new JLabel(); 
	private final JLabel m_approved = new JLabel(); 
	private final JLabel m_matic = new JLabel(); 
	
	WalletPanel() throws Exception {
		super( new BorderLayout(), "Symbol,Balance");
		
		m_wallet.addActionListener( e -> { 
			try {
				refresh();
			} catch (Exception e1) {
				e1.printStackTrace();
			} 
		});

		VerticalPanel v = new VerticalPanel();
		v.setBorder( new TitledBorder( "Balances") );
		v.add( "Wallet", m_wallet);
		v.add( "RUSD", m_rusd);
		v.add( "USDC", m_usdc);
		v.add( "Approved", m_approved);
		v.add( "MATIC", m_matic);
		
//		QPanel v2 = new QPanel( "select * from users where wallet_public_key = " +
		
		add( v, BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	@Override JsonModel createModel(String allNames) {
		return new Model(allNames);
	}
	
	public void refresh() throws Exception {
		S.out( "Refreshing Wallet panel");
		
		MyClient.getJson(Monitor.base + "/api/mywallet/" + m_wallet.getText(), obj -> {
			JsonArray ar = obj.getArray("tokens");
			Util.require( ar.size() == 3, "Invalid mywallet query results for wallet %s", m_wallet.getText() ); 

			m_rusd.setText("" + S.formatPrice( ar.get(0).getDouble("balance")));
			m_usdc.setText("" + S.formatPrice( ar.get(1).getDouble("balance")));
			m_approved.setText("" + S.formatPrice( ar.get(1).getDouble("approvedBalance")));
			m_matic.setText("" + ar.get(2).getDouble("balance"));			
		});
		
		m_model.refresh();		
	}
	
	class Model extends JsonModel {
		public Model(String allNames) {
			super(allNames);
		}

		void refresh() throws Exception {
			super.refresh();
			m_ar.clear();

			Wallet wallet = new Wallet( m_wallet.getText() );
			for (Stock stock : Monitor.stocks) {
				JsonObject obj = new JsonObject();
				double bal = wallet.getBalance( stock.getSmartContractId() );
				if (bal > minBalance) {
					obj.put( "Symbol", stock.getSymbol() );
					obj.put( "Balance", bal);
					m_ar.add(obj);
				}
			}
			fireTableDataChanged();
		}
	}

	@Override public void activated() {
	}

	@Override public void closed() {
	}
}
