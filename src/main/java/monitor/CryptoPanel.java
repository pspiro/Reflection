package monitor;

import java.awt.BorderLayout;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import fireblocks.Fireblocks;
import http.MyClient;
import monitor.Monitor.MonPanel;
import positions.Wallet;
import tw.util.S;
import tw.util.VerticalPanel;

public class CryptoPanel extends MonPanel {
	private JTextField m_rusd = new JTextField(10);
	private JTextField m_busd = new JTextField(10);
	private JTextField m_nativeToken = new JTextField(10);
	private JTextField m_admin1 = new JTextField(10);
	private JTextField m_admin2 = new JTextField(10);
	private JTextField m_cash = new JTextField(10);

	CryptoPanel() {
		super( new BorderLayout() );

		VerticalPanel rusdPanel = new VerticalPanel();
		rusdPanel.setBorder( new TitledBorder("RUSD Analysis"));
		rusdPanel.add( "RUSD Outstanding", m_rusd);
		rusdPanel.add( "Non-RUSD in RefWallet", m_busd);
		rusdPanel.add( "Cash in brokerage", m_cash);
		rusdPanel.add( "RefWallet MATIC", m_nativeToken);
		rusdPanel.add( "Admin1 MATIC", m_admin1);
		rusdPanel.add( "Admin2 MATIC", m_admin2);
		
		add(rusdPanel);
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
