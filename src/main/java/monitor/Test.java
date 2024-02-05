package monitor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import tw.util.DualPanel;

public class Test {
	
	public static void main(String[] args) {
		JPanel p1 = new JPanel();
		p1.setBorder( new TitledBorder( "p1") );
		
		JPanel p2 = new JPanel();
		p2.setBorder( new TitledBorder( "p2") );
		
		DualPanel p = new DualPanel();
		p.add( "1", p1);
		p.add( "2", p2);
//		p.add( p1, BorderLayout.NORTH);
//		p.add( p2, BorderLayout.SOUTH);
		
		JFrame f = new JFrame();
		f.add( p);
		f.setSize( 200, 200);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
