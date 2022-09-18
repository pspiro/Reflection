/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package tw.util;

import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class ApiUtil {
	private static final int BUF = 14;
	private static int MAX = 300;
	
	public static void setMaxColumnWidth( int width) { MAX = width; }
	
	/** Resize all columns in the table to fit widest row including header. */ 
	public static void resizeColumns( JTable table) {
		if (table.getGraphics() == null) {
			return;
		}
		
		FontMetrics fm = table.getFontMetrics( table.getFont() );

		TableColumnModel mod = table.getColumnModel();
		for (int iCol = 0; iCol < mod.getColumnCount(); iCol++) {
			TableColumn col = mod.getColumn( iCol);
			
			if (col.getWidth() < MAX) {
				int max = 0; //col.getWidth() - BUF;
				
				String header = table.getModel().getColumnName( iCol);
				if (header != null) {
					max = Math.max( max, fm.stringWidth( header) );
				}
				
				for (int iRow = 0; iRow < table.getModel().getRowCount(); iRow++) {
					Object obj = table.getModel().getValueAt(iRow, iCol);
					String str = obj == null ? "" : obj.toString();
					max = Math.max( max, fm.stringWidth( str) );
				}
	
				col.setPreferredWidth( Math.min( max + BUF, MAX) );
				//col.setMaxWidth( MAX);
			}
		}
		table.revalidate();
		table.repaint();
	}
	
	/** Configure dialog to close when Esc is pressed. */
	public static void closeOnEsc( final JDialog dlg) {
        dlg.getRootPane().getActionMap().put( "Cancel", new AbstractAction() {
        	public void actionPerformed(ActionEvent e) {
        		dlg.dispose();
        	}
        });

        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put( KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel");
	}

	public static void sleep( int ms) {
		try {
			Thread.sleep( ms);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
