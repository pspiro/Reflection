/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package tw.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** A stack of FlowPanels where each row has two columns and the first column has all the same width, i.e.
 *  One     _________
 *  Seven   ______           */
public class VerticalPanel extends JPanel {
	private class RowPanel extends JPanel {
		RowPanel( Component[] comps) {
			setLayout( new FlowLayout( FlowLayout.LEFT, 5, 2) );
			for( Component comp : comps) {
				add( comp);
			}
			setAlignmentX(0);
		}
		
		@Override public Dimension getMaximumSize() {
			return super.getPreferredSize();
		}

		public int wid() {
			return getComponent( 0).getPreferredSize().width;
		}

		public void wid( int i) {
			Dimension d = getComponent( 0).getPreferredSize();
			d.width = i;
			getComponent( 0).setPreferredSize( d);
		}
	}

	public VerticalPanel() {
		setLayout( new BoxLayout( this, BoxLayout.Y_AXIS));
	}

	@Override public Component add(Component comp) {
		add( new Component[] { comp } );
		return comp;
	}
	
	@Override public Component add(String str, Component comp) {
		add( new Component[] { new JLabel( str), comp } );
		return null;
	}
	
	public void add( String str, Component... cs) {
		Component[] ar = new Component[cs.length + 1];
		ar[0] = new JLabel( str);
		System.arraycopy(cs, 0, ar, 1, cs.length);
		add( ar);
	}

	public void add( Component... comps) {
		add( -1, comps);
	}
	
	@Override public Component add(Component comp, int index) {
		add( index, comp);
		return null;
	}
	
	public void add( int index, Component... comps) {
		super.add( new RowPanel( comps), index);

		int max = 0;
		for (int i = 0; i < getComponentCount(); i++) {
			RowPanel comp = (RowPanel)getComponent( i);
			max = Math.max( comp.wid(), max);
		}

		for (int i = 0; i < getComponentCount(); i++) {
			RowPanel comp = (RowPanel)getComponent( i);
			comp.wid( max);
		}
	}

	public void add(String str, Component comp, int index) {
		add( index, new JLabel( str), comp);				
	}
	
	@Override public void add(Component comp, Object constraints) {
		throw new RuntimeException(); // not valid
	}
	
	@Override public void add(Component comp, Object constraints, int index) {
		throw new RuntimeException(); // not valid
	}
	

	public static class HorzPanel extends JPanel {
		public HorzPanel() {
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS));
		}
		
		public void add(JComponent comp) {
			comp.setAlignmentY(0);
			super.add( comp);
		}
	}

	public static class FlowPanel extends JPanel {
		public FlowPanel(int h, int v, Component... cs) {
			setLayout( new FlowLayout( FlowLayout.LEFT, h, v) );
			addAll(cs);
		}
		
		/** Doesn't work w/ just add(...) */
		public void addAll(Component... cs) {
			for (Component c : cs) {
				add( c);
			}
		}
	}

	public static class BorderPanel extends JPanel {
		public BorderPanel() {
			setLayout( new BorderLayout() );
		}
	}
	
	public static class StackPanel extends JPanel {
		public StackPanel() {
			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS));
		}

		public void add(JComponent comp) {
			comp.setAlignmentX( 0);
			super.add( comp);
		}
		
		@Override public Dimension getMaximumSize() {
			return super.getPreferredSize();
		}
	}
	
	public static void main(String[] args) {
		VerticalPanel p = new VerticalPanel();
		p.add( "one", new JTextField( 4));
		p.add( "seventeen", new JTextField( 9));
		
		JFrame f = new JFrame();
		f.add( p);
		f.setSize( 200, 200);
		f.setDefaultCloseOperation(( JFrame.EXIT_ON_CLOSE));
		f.show();
	}
	
	public static class Header extends JLabel {
		public Header(String text) {
			super( HtmlButton.underline(text) );
			setBorder( new EmptyBorder(10, 0, 0, 0) );
		}
	}

	public void addHeader(String string) {
		add( new Header(string) );
	}
}
