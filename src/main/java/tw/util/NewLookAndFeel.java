/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package tw.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.metal.MetalCheckBoxUI;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.plaf.metal.MetalLabelUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class NewLookAndFeel extends MetalLookAndFeel {
	@Override protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        
        Object[] uiDefaults = new Object[] { 
            	"CheckBoxUI", NewCheckUI.class.getName(),
            	"LabelUI", NewLabelUI.class.getName(),
            	"ComboBoxUI", NewComboUI.class.getName(),
    		    "TableUI", NewTableUI.class.getName(),
    		    "TextFieldUI", NewTextFieldUI.class.getName(),
        };
        
        table.putDefaults(uiDefaults);
    }

	public static void register() {
		try {
			UIManager.setLookAndFeel( new NewLookAndFeel() );
			
			// Set the tooltip to show up after 500 milliseconds
			//ToolTipManager.sharedInstance().setInitialDelay(500);

			// Set the tooltip to stay visible for 10000 milliseconds (10 seconds)
			ToolTipManager.sharedInstance().setDismissDelay(60000);
			
			
			// don't show selected cell in table
			Border border = new EmptyBorder(1, 1, 1, 1);
			UIManager.put("Table.focusCellHighlightBorder", border);
			UIManager.put("Table.focusSelectedCellHighlightBorder", border);
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}
	
	/* the flow is like this:
	 *  JComponent.paintComponent() -> 
	 *  	UI.update -> 
	 *  		paints background, then calls UI.paintSafely() 
	 */
	
    /**
     * A custom UI delegate for JTextField that paints a rounded rectangle background
     * and clips the text rendering to the same shape.
     */
    static class NewTextFieldUI extends BasicTextFieldUI {
        private final int arcWidth = 10;
        private final int arcHeight = 10;

		@Override public Dimension getPreferredSize(JComponent c) {
			Dimension d = super.getPreferredSize(c);
			d.height = 24;
			return d;
		}
		
//		// this didn't work
//        @Override
//        public void update(Graphics g, JComponent c) {
//            // Create a Graphics2D copy and enable antialiasing for smooth edges.
//            Graphics2D g2 = (Graphics2D) g.create();
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                    RenderingHints.VALUE_ANTIALIAS_ON);
//
//            // Paint the background as a rounded rectangle.
//            g2.setColor(c.getBackground());
//            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arcWidth, arcHeight);
//            g2.dispose();
//
//            // Now let the UI delegate paint the rest (the text, caret, etc.)
//            paint(g, c);
//        }
//
//        /**
//         * Override paintSafely to set a clipping region so that the text
//         * doesn’t paint outside the rounded bounds.
//         */
//        @Override
//        protected void paintSafely(Graphics g) {
//            JTextComponent comp = getComponent();
//            if (comp == null) {
//                return;
//            }
//            Graphics2D g2 = (Graphics2D) g.create();
//            // Save the old clipping region.
//            Shape oldClip = g2.getClip();
//            // Set the clip to our rounded rectangle.
//            RoundRectangle2D roundedClip = new RoundRectangle2D.Float(
//                    0, 0, comp.getWidth(), comp.getHeight(), arcWidth, arcHeight);
//            g2.clip(roundedClip);
//
//            // Let the superclass do its text painting.
//            super.paintSafely(g2);
//            // Restore the old clip.
//            g2.setClip(oldClip);
//            g2.dispose();
//        }
    }
	
    /** labels are non-bold */
	public static class NewLabelUI extends MetalLabelUI {
	    private static final NewLabelUI UI = new NewLabelUI();

	    public static ComponentUI createUI(JComponent c) {
	        return UI;
	    }
	    
	    @Override public void installUI(JComponent c) {
	    	super.installUI(c);
	    	c.setFont( c.getFont().deriveFont(0) );
	    }
	}
	
	public static class NewCheckUI extends MetalCheckBoxUI {
	    private static final NewCheckUI UI = new NewCheckUI();

	    public static ComponentUI createUI(JComponent c) {
	        return UI;
	    }
	    
		@Override public void installUI(JComponent c) {
	    	super.installUI(c);
	    	((JCheckBox)c).setBorder( new EmptyBorder( 3, 0, 3, 0) );
	    }
	}

	public static class NewComboUI extends MetalComboBoxUI {
	    public static ComponentUI createUI(JComponent c) {
	        return new NewComboUI();
	    }
	    
	    @Override public void installUI(JComponent c) {
	    	super.installUI(c);
	    	c.setFont( c.getFont().deriveFont(0) );
	    	c.setPreferredSize( new Dimension( c.getPreferredSize().width, 19));
	    }
	}
	
	public static class NewTableUI extends BasicTableUI {
	    public static ComponentUI createUI(JComponent c) {
	        return new NewTableUI();
	    }
		
	    @Override public void installUI(JComponent c) {
	    	super.installUI(c);
	    	
	    	final JTable table = (JTable)c;
	    	table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF);
	    	
			table.setFont( table.getFont().deriveFont( (float)16));
			table.setRowHeight( 20);
			table.getColumnModel().setColumnMargin(10); // this is spread out over left and right sides of cell
	    	
			TableColumnModel mod = table.getColumnModel();
			for (int iCol = 0; iCol < mod.getColumnCount(); iCol++) {
				TableColumn col = mod.getColumn( iCol);
				col.setPreferredWidth( 40);
			}

	    	final Timer timer = new Timer( 300, new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					ApiUtil.resizeColumns( table);
				}
	    	});
	    	timer.setRepeats( false);
	    	timer.start();
	    	
	    	table.getModel().addTableModelListener( new TableModelListener() {
				@Override public void tableChanged(TableModelEvent e) {
					timer.restart();
				}
	    	});
	    }
	    
	    @Override public void paint(Graphics g, JComponent c) {
	    	int margin = table.getColumnModel().getColumnMargin();
	    	if (margin > 0) {
	    		colorSelectedRows(g, c, margin);
	    	}
	    	super.paint( g, c);
	    }
	    
	    /** Color the margin between selected cells. */
	    private void colorSelectedRows(Graphics g, JComponent c, int margin) {
	        // get first visible row
	    	Rectangle clip = g.getClipBounds();
	        Point upperLeft = clip.getLocation();
	        int rMin = table.rowAtPoint(upperLeft);
	        if (rMin == -1) {
	            rMin = 0;
	        }

	        // get last visible row
	        Point lowerRight = new Point(clip.x + clip.width - 1, clip.y + clip.height - 1);
	        int rMax = table.rowAtPoint(lowerRight);
	        if (rMax == -1) {
	            rMax = table.getRowCount()-1;
	        }

            g.setColor(table.getSelectionBackground());

            // loop through visible selected rows and set background color
            for (int row = rMin; row <= rMax; row++) {
	    		if (table.isRowSelected(row) ) {
    				Rectangle r1 = table.getCellRect( row, 0, true);
    				Rectangle r2 = table.getCellRect( row, table.getColumnCount() - 1, true);
    				int x1 = r1.x + margin;
    	            g.fillRect(x1, r1.y, r2.x - x1 + 1 + margin, r1.height);
	    		}
	    	}
	    }
	}
	
	public static void main(String[] args) {
		NewLookAndFeel.register();

		JPanel p = new JPanel();
		p.add( new JTextField( 20));
		p.add( new JLabel("lkjsdf") );
		
		JFrame f = new JFrame();
		f.add( p, BorderLayout.NORTH);
		f.setSize( 600, 600);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
