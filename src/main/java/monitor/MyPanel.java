package monitor;

import java.awt.FlowLayout;
import java.awt.LayoutManager;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MyPanel extends JPanel {
	MyPanel() {
//		super( new FlowLayout( FlowLayout.LEFT));
	}
	
	MyPanel( LayoutManager mgr) {
		super( mgr);
	}
	
	void horz( int width) {
		add( Box.createHorizontalStrut(width));
	}
	
	void label( String str) {
		add( new JLabel( str) );
	}
}
