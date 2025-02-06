/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package tw.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.JLabel;

public class HtmlButton extends JLabel {
	static Color light = new Color( 220, 220, 220);
	
	private String m_text;
	protected boolean m_selected;
	private ActionListener m_al;
	private Color m_bg = getBackground();

	public boolean isSelected() { return m_selected; }
	
	public void addActionListener(ActionListener v) { m_al = v; }
	public ActionListener al() { return m_al; }

	public HtmlButton( String text) {
		this( text, null);
	}
	
	public HtmlButton( String text, ActionListener v) {
		super( text);
		m_text = text;
		m_al = v;
		setOpaque( true);
		setForeground( Color.blue);
		
		MouseAdapter a = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				onPressed(e);
			}
			public void mouseReleased(MouseEvent e) {
				onReleased(e);
			}
			public void mouseEntered(MouseEvent e) {
				onEntered(e);
			}
			public void mouseExited(MouseEvent e) {
				onExited();
			}
			@Override public void mouseMoved(MouseEvent e) {
				onMouseMoved( e);
			}
		};
		addMouseListener( a);
		addMouseMotionListener(a);
		setFont( getFont().deriveFont( Font.PLAIN) );
		
//		setBorder( new TitledBorder("") ); 
//		setBorder( new LineBorder( new Color(184,207,229), 1, true) );
	}
	
	protected void onEntered(MouseEvent e) {
		if (!m_selected && isEnabled() ) {
			setText( underline( m_text) );
		}
	}

	protected void onExited() {
		if (!m_selected) {
			setText( m_text);
		}
	}

	protected void onPressed(MouseEvent e) {
		if (!m_selected && isEnabled() ) {
			setBackground( light);
		}
	}

	protected void onReleased(MouseEvent e) {
		if (isEnabled() ) {
			onClicked(e);
			setBackground( m_bg);
		}
	}

	protected void onClicked(MouseEvent e) {
		takeAction();
	}

	protected void takeAction() {
		if( m_al != null) {
			m_al.actionPerformed( null);
		}
	}

	public void setSelected( boolean v) {
		m_selected = v;
		if (v) {
			setText( underline( m_text) );
		}
		else {
			setText( m_text);
		}
	}

	protected void onMouseMoved(MouseEvent e) {
	}
	
	public static class HtmlCheckBox extends HtmlButton {
		public HtmlCheckBox( String text) {
			super( text);
		}
		
		@Override protected void takeAction() {
			setSelected( !m_selected);
			super.takeAction();
		}
	}

	public static class HtmlRadioButton extends HtmlButton {
		private HashSet<HtmlRadioButton> m_group;

		public HtmlRadioButton( String text, HashSet<HtmlRadioButton> group) {
			this( text, group, null);
		}
		
		public HtmlRadioButton( String text, HashSet<HtmlRadioButton> group, ActionListener listener) {
			super( text);
			m_group = group;
			group.add( this);
			
			if (listener != null) {
				addActionListener( listener);
			}
		}
		
		@Override final protected void takeAction() {
			for( HtmlRadioButton but : m_group) {
				but.setSelected( false);
			}
			setSelected( true);
			super.takeAction();
		}
		
		@Override public void setSelected(boolean v) {
			super.setSelected(v);
		}

		public boolean isAnySelected() {
			for (HtmlRadioButton button : m_group) {
				if (button.isSelected() ) {
					return true;
				}
			}
			return false;
		}
	}
	
	static String underline( String str) {
		return String.format( "<html><u>%s</html>", str);
	}
	
	static String bold( String str) {
		return String.format( "<html><b>%s</html>", str);
	}

	static String boldUnderline( String str) {
		return String.format( "<html><b><u>%s</html>", str);
	}
}
