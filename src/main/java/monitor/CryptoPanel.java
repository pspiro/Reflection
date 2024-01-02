package monitor;

import java.awt.BorderLayout;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import common.Util;
import fireblocks.Fireblocks;
import http.MyClient;
import monitor.Monitor.MonPanel;
import positions.Wallet;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.VerticalPanel;

/** Not a json panel */
public class CryptoPanel extends MonPanel {
	private JTextField m_rusd = new JTextField(10);
	private JTextField m_busd = new JTextField(10);
	private JTextField m_nativeToken = new JTextField(10);
	private JTextField m_admin1 = new JTextField(10);
	private JTextField m_admin2 = new JTextField(10);
	private JTextField m_cash = new JTextField(10);
	HoldersPanel holdersPanel = new HoldersPanel();

	CryptoPanel() {
		super( new BorderLayout() );
		
		HtmlButton button = new HtmlButton( "Show Wallets", ev -> {
			Util.wrap( () -> holdersPanel.refresh( Monitor.m_config.rusd() ) );
		});

		VerticalPanel rusdPanel = new VerticalPanel();
		rusdPanel.setBorder( new TitledBorder("RUSD Analysis"));
		rusdPanel.add( "RUSD Outstanding", m_rusd, button); 
		rusdPanel.add( "Non-RUSD in RefWallet", m_busd);
		rusdPanel.add( "Cash in brokerage", m_cash);
		rusdPanel.add( "RefWallet MATIC", m_nativeToken);
		rusdPanel.add( "Admin1 MATIC", m_admin1);
		rusdPanel.add( "Admin2 MATIC", m_admin2);
		
		add(rusdPanel);
		add(holdersPanel, BorderLayout.EAST);
	}
	
	@Override public void activated() {
		Util.wrap( () -> refresh() );
	}
	
	@Override public void refresh() throws Exception {
		S.out( "Refreshing Crypto panel");
		Wallet refWallet = Fireblocks.getWallet("RefWallet");

		double busd = refWallet.getBalance(Monitor.m_config.busdAddr());
		SwingUtilities.invokeLater( () -> m_busd.setText( S.fmt2(busd) ) );

		double nativeBal = refWallet.getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_nativeToken.setText( S.fmt2(nativeBal) ) );

		double admin1Bal = Fireblocks.getWallet("Admin1").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin1.setText( S.fmt2(admin1Bal) ) );

		double admin2Bal = Fireblocks.getWallet("Admin2").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin2.setText( S.fmt2(admin2Bal) ) );

		double rusd = Monitor.m_config.rusd().queryTotalSupply();
		SwingUtilities.invokeLater( () -> m_rusd.setText( S.fmt2(rusd) ) );
		
		MyClient.getJson( Monitor.refApiBaseUrl() + "/api/?msg=getCashBal", obj -> {
			double val = obj.getDouble("TotalCashValue");
			SwingUtilities.invokeLater( () -> m_cash.setText( S.fmt2(val) ) );
		});
	}
}
