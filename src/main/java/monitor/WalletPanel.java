package monitor;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import monitor.Monitor.TransPanel;
import positions.Wallet;
import reflection.Stock;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.VerticalPanel;

public class WalletPanel extends JsonPanel {
	private static final double minBalance = .0001;
	private final JTextField m_wallet = new JTextField(32); 
	private final JLabel m_rusd = new JLabel(); 
	private final JLabel m_usdc = new JLabel(); 
	private final JLabel m_approved = new JLabel(); 
	private final JLabel m_matic = new JLabel(); 
	private final JTextField m_username = new JTextField(8); 
	private final JTextField m_mintAmt = new JTextField(8); 
	private final JTextField m_burnAmt = new JTextField(8); 

	private final TransPanel transPanel = new TransPanel();

	WalletPanel() throws Exception {
		super( new BorderLayout(), "Symbol,Balance");

		m_wallet.addActionListener( e -> { 
			try {
				refresh();
			} catch (Exception e1) {
				e1.printStackTrace();
			} 
		});

		VerticalPanel vp = new VerticalPanel();
		vp.setBorder( new TitledBorder( "Balances") );
		vp.add( "Wallet", m_wallet);
		vp.add( "RUSD", m_rusd);
		vp.add( "USDT", m_usdc);
		vp.add( "Approved", m_approved);
		vp.add( "MATIC", m_matic);
		vp.add( "Mint RUSD", m_mintAmt, new HtmlButton("Mint", e -> mint() ) ); 
		vp.add( "Burn RUSD", m_burnAmt, new HtmlButton("Burn", e -> burn() ) ); 
		vp.add( "Create user", m_username, new HtmlButton("Create", e -> createUser() ) );

		JPanel leftPanel = new JPanel(new BorderLayout() );
		leftPanel.add( vp, BorderLayout.NORTH);
		leftPanel.add( m_model.createTable() );

		add( leftPanel, BorderLayout.WEST);
		add( transPanel);
	}

	private void createUser() {
		Util.wrap( () -> {
			Monitor.m_config.sqlCommand( sql -> sql.insertJson( "users",
					Util.toJson( 
							"wallet_public_key", m_wallet.getText().toLowerCase(),
							"first_name", m_username.getText() ) ) );
			Util.inform( this, "Done");
		});
	}

	private void mint() {
		try {
			Util.require( Util.isValidAddress(m_wallet.getText()), "Invalid wallet address");
			
			double amt = Double.parseDouble( m_mintAmt.getText() );
			if ( amt > 0 && Util.confirm(this, "Minting %s RUSD for %s", amt, m_wallet.getText() ) ) {
			
				String hash = Monitor.m_config.rusd().mintRusd( 
						m_wallet.getText(), amt, Monitor.stocks.getAnyStockToken() ).waitForHash();
				
				Util.inform(this, hash);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void burn() {
		try {
			Util.require( Util.isValidAddress(m_wallet.getText()), "Invalid wallet address");

			double amt = Double.parseDouble( m_burnAmt.getText() );
			if ( amt > 0 && Util.confirm(this, "Burning %s RUSD from %s", amt, m_wallet.getText() ) ) {
			
				String hash = Monitor.m_config.rusd().burnRusd( 
						m_wallet.getText(), amt, Monitor.stocks.getAnyStockToken() ).waitForHash();
				
				Util.inform(this, hash);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setWallet(String addr) {
		m_wallet.setText(addr);
		Util.wrap( () -> refresh() );
	}

	public void refresh() throws Exception {
		S.out( "Refreshing Wallet panel");

		rows().clear();

		String walletAddr = m_wallet.getText();

		if (Util.isValidAddress(walletAddr)) {
			MyClient.getJson(Monitor.refApiBaseUrl() + "/api/mywallet/" + walletAddr, obj -> {
				JsonArray ar = obj.getArray("tokens");
				Util.require( ar.size() == 3, "Invalid mywallet query results for wallet %s", walletAddr ); 

				m_rusd.setText("" + S.formatPrice( ar.get(0).getDouble("balance")));
				m_usdc.setText("" + S.formatPrice( ar.get(1).getDouble("balance")));
				m_approved.setText("" + S.formatPrice( ar.get(1).getDouble("approvedBalance")));
				m_matic.setText("" + ar.get(2).getDouble("balance"));			
			});

			Wallet wallet = new Wallet( walletAddr);
			for (Stock stock : Monitor.stocks) {
				JsonObject obj = new JsonObject();
				double bal = wallet.getBalance( stock.getSmartContractId() );
				if (bal > minBalance) {
					obj.put( "Symbol", stock.symbol() );
					obj.put( "Balance", bal);
					rows().add(obj);
				}
			}

			transPanel.where.setText( String.format("where wallet_public_key = '%s'", walletAddr) );
			transPanel.refresh();
		}
		else {
			m_rusd.setText(null);
			m_usdc.setText(null);
			m_approved.setText(null);
			m_matic.setText(null);

			transPanel.clear();
		}

		m_model.fireTableDataChanged();
	}

	public void filter(String wallet) {
		m_wallet.setText(wallet);
		Util.wrap( () -> refresh() );
	}
}
