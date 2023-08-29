package monitor;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import fireblocks.Fireblocks;
import http.MyHttpClient;
import monitor.Monitor.RefPanel;
import positions.Wallet;
import tw.util.NewTabbedPanel.INewTab;
import tw.util.S;
import tw.util.VerticalPanel;

public class MiscPanel extends JPanel implements RefPanel, INewTab {
	private JTextField m_usdc = new JTextField(10);
	private JTextField m_rusd = new JTextField(10);
	private JTextField m_usdc2 = new JTextField(10);
	private JTextField m_nativeToken = new JTextField(10);
	private JTextField m_admin1 = new JTextField(10);
	private JTextField m_admin2 = new JTextField(10);
	private JTextField m_cash = new JTextField(10);

	MiscPanel() {
		super( new BorderLayout() );

		VerticalPanel rusdPanel = new VerticalPanel();
		rusdPanel.setBorder( new TitledBorder("RUSD Analysis"));
		rusdPanel.add( "RUSD Outstanding", m_rusd);
		rusdPanel.add( "USDC in RefWallet", m_usdc2);
		rusdPanel.add( "Cash in brokerage", m_cash);
		rusdPanel.add( "RefWallet MATIC", m_nativeToken);
		rusdPanel.add( "Admin1 MATIC", m_admin1);
		rusdPanel.add( "Admin2 MATIC", m_admin2);
		
		add(rusdPanel);
	}
	
	public void refresh() throws Exception {
		S.out( "Refreshing Misc panel");
		Wallet refWallet = Fireblocks.getWallet("RefWallet");

		double usdc = refWallet.getBalance(Monitor.m_config.busdAddr());
		SwingUtilities.invokeLater( () -> m_usdc.setText( S.fmt2(usdc) ) );
		SwingUtilities.invokeLater( () -> m_usdc2.setText( S.fmt2(usdc) ) );

		double nativeBal = refWallet.getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_nativeToken.setText( S.fmt2(nativeBal) ) );

		double admin1Bal = Fireblocks.getWallet("Admin1").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin1.setText( S.fmt2(admin1Bal) ) );

		double admin2Bal = Fireblocks.getWallet("Admin2").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin2.setText( S.fmt2(admin2Bal) ) );

		double rusd = Monitor.m_config.rusd().queryTotalSupply();
		SwingUtilities.invokeLater( () -> m_rusd.setText( S.fmt2(rusd) ) );
		
		Monitor.queryObj( "/api/?msg=getCashBal", obj -> {
			double val = obj.getDouble("TotalCashValue");
			SwingUtilities.invokeLater( () -> m_cash.setText( S.fmt2(val) ) );
		});
	}

	@Override public void activated() {
		try {
			refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override public void closed() {
	}

}
