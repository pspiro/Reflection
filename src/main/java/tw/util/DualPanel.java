package tw.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/** Split into two or three panels stacked vertically;
 *  see also HorzDualPanel */
public class DualPanel extends JPanel implements LayoutManager {
	private final Dimension pref = new Dimension(1, 1);
	private Component c1;
	private Component c2;
	private Component c3;

	public DualPanel() {
		setLayout( this);
	}

	@Override public void addLayoutComponent(String name, Component comp) {
		if (name.equals("1")) {
			c1 = comp;
		}
		else if (name.equals("2")){
			c2 = comp;
		}
		else {
			c3 = comp;
		}
	}

	@Override public void removeLayoutComponent(Component comp) {
	}

	@Override public Dimension preferredLayoutSize(Container parent) {
		return pref;
	}

	@Override public Dimension minimumLayoutSize(Container parent) {
		return pref;
	}

	@Override public void layoutContainer(Container parent) {
		int width = getSize().width;
		int height = getSize().height / (c3 != null ? 3 : 2);

		c1.setLocation( 0, 0);
		c1.setSize( width, height);
		
		c2.setLocation( 0, height);
		c2.setSize( width, height);
		
		if (c3 != null) {
			c3.setLocation( 0, height * 2);
			c3.setSize( width, height);
		}
	}

}
