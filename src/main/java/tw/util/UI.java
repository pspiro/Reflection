package tw.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import common.Util;
import common.Util.ExRunnable;

public class UI {

	private static final Object COMMAND_CANCEL = "cancel";

	public static Component h(int i) { return Box.createHorizontalStrut( i); }
	public static Component v(int i) { return Box.createVerticalStrut( i); }

	public static void addActionOnEscape(final RootPaneContainer rootPaneContainer, final ActionListener action) {
		rootPaneContainer.getRootPane().getActionMap().put( COMMAND_CANCEL, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				action.actionPerformed(e);
				S.out( "esc pressed");
			}
		});
		
		rootPaneContainer.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),  COMMAND_CANCEL);
	}

    static public void centerOnScreen( Window window) {
        // this functions centers the window in the middle
        // if the screen

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        int x = (screenSize.width - window.getWidth() ) / 2;
        int y = (screenSize.height - window.getHeight() ) / 2;
        window.setLocation( x, y);
    }
    
    static public void centerOnOwner( Window window) {
        Window owner = window.getOwner();
        if( owner != null) {
        	centerOnWindow(window, owner);
        }
        else {
        	centerOnScreen( window);
        }
    }
    
    static public void centerOnWindow( Window windowToCenter, Window anchorWindow) {
        windowToCenter.setLocationRelativeTo(anchorWindow);
    }

	public static void disposeOnEsc(final JFrame frame) {
		addActionOnEscape(frame, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
			}
		});
	}

	/** Display a quick message popup */
	public static void quick( Window parent, String str) {
		JPanel p = new JPanel( new FlowLayout( FlowLayout.CENTER, 10, 10));
		p.setBorder( new LineBorder( Color.black) );
		p.add( new JLabel( str) );
		p.setBackground( Color.yellow);

		final JDialog d = new JDialog(parent, ModalityType.MODELESS);
		d.setUndecorated(true);
		d.add( p);
		d.pack();
		UI.centerOnOwner( d);
		d.setVisible( true);

		new Thread() {
			public void run() {
				S.sleep( 750);
				d.dispose();
			}
		}.start();
	}
	
	public static double getDub(JTextField field) {
		return S.parseDouble2( field.getText() );
	}
	
	public static void main(String[] args) throws Exception {
		JPanel p = new JPanel( new FlowLayout( FlowLayout.CENTER, 10, 10));
		p.setBorder( new LineBorder( Color.black) );
		p.add( new JLabel( "lkj") );
		p.setBackground( Color.yellow);

		final JFrame d = new JFrame();
		d.add( p);
		d.setSize( 400, 400);
		d.setVisible( true);
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Hourglass g = new Hourglass(d);
		S.sleep(2000);
		g.restore();
		
//		final JDialog d = new JDialog(parent, ModalityType.MODELESS);
//		d.setUndecorated(true);
//		d.add( p);
//		d.pack();
//		UI.centerOnOwner( d);
//		d.setVisible( true);
		
	}
	
	public static class Hourglass {
		private Cursor m_current;
		private JFrame m_frame;

		public Hourglass(JFrame f) {
			m_frame = f;
			m_current = f.getCursor();
			f.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
		
		public void restore() {
			m_frame.setCursor(m_current);
		}
	}
	
	/** Display hourglass and catch exceptions */
	public static void watch( JFrame frame, ExRunnable runnable) {
		Hourglass glass = new Hourglass( frame);
		Util.wrap( () -> runnable.run() );
		glass.restore();
	}

	/** Flash a msg on the screen for 2500 ms and make a beep */
	public static void flash(String text) {
		JFrame d = new JFrame();
		d.setUndecorated(true);
		d.setSize( 300, 80);
		d.setAlwaysOnTop(true);
		((JComponent)d.getContentPane()).setBorder( new TitledBorder( "") );
		UI.centerOnOwner(d);
		d.add( Util.tweak( new JLabel(text), lab -> lab.setHorizontalAlignment( SwingConstants.CENTER) ) );
		d.setVisible(true);
		Util.executeIn(2500, () -> d.setVisible(false) );
		
		java.awt.Toolkit.getDefaultToolkit().beep();
	}
}
