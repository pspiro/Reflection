package tw.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JPanel;

public class DualPanel extends JPanel implements LayoutManager {
	private final Dimension pref = new Dimension(1, 1);
	private Component c1;
	private Component c2;

	public DualPanel() {
		setLayout( this);
	}

	@Override public void addLayoutComponent(String name, Component comp) {
		if (name.equals("1")) {
			c1 = comp;
		}
		else {
			c2 = comp;
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
		int height = getSize().height / 2;
		int width = getSize().width;
		c1.setLocation( 0, 0);
		c1.setSize( width, height);
		c2.setLocation( 0, height);
		c2.setSize( width, height);
	}

}
