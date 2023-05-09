package reflection;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import tw.util.NewLookAndFeel;

// use this to query wallet balances, it is super-quick and returns all the positions for the wallet
// https://deep-index.moralis.io/api/v2/:address/erc20	
// you could use this to easily replace the Backend method that combines it with with the market data 

public class test {
	JFrame m_frame = new JFrame();
	private JTextField stablecoin = new JTextField(20);
	private JTextField nativeToken = new JTextField(20);
	private JTextField nativeToken2 = new JTextField(20);
	
	public static void main(String[] args) throws Exception {
		NewLookAndFeel.register();
		new test();
	}

	test() throws Exception {
		
		
		JPanel refPanel = new JPanel();
		refPanel.setBorder( new TitledBorder("RefWallet Balances") );
		refPanel.add( new JLabel("USDC"));
		refPanel.add( stablecoin);
		refPanel.add( new JLabel("Native"));
		refPanel.add( nativeToken);
		refPanel.add( new JLabel("Native2"));
		refPanel.add( nativeToken2);

		m_frame.add(refPanel, BorderLayout.SOUTH);
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 800, 1000);
		m_frame.setVisible(true);
	}
}
