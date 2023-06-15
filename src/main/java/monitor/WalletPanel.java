package monitor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import positions.Wallet;
import reflection.Config;
import reflection.Stock;
import reflection.Stocks;
import tw.util.S;

public class WalletPanel extends JPanel {
	private static final double minBalance = .0001;
	private final JTextField m_wallet = new JTextField(32); 
	private final Stocks stocks = new Stocks();
	private final Config m_config;
	
	public static void main(String[] args) throws Exception {
		JFrame m_frame = new JFrame();
		
		m_frame.add( new WalletPanel() );
		m_frame.setTitle( "WalletPanel");
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 800, 1000);
		m_frame.setVisible(true);
		
	}
	
	WalletPanel() throws Exception {
		m_config = Config.readFrom("Dt-config");
		stocks.readFromSheet(m_config);
		m_wallet.addActionListener( e -> refresh() );
		add( m_wallet);
	}

	private void refresh() {
		try {
			refresh_();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void refresh_() throws Exception {
		Wallet wallet = new Wallet( m_wallet.getText() );

		for (Stock stock : stocks) {
			double bal = wallet.getBalance( stock.getSmartContractId() );
			if (bal > minBalance) {
				S.out( "%s = %s", stock.getSymbol(), bal);
			}
		}
		
		
	}
}
