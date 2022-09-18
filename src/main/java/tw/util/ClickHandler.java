package tw.util;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/** This is a global click handler which simulates MOUSE_CLICKED events. 
 *  For tables, it simulates the event if the mouse was released over the 
 *  same cell that was clicked on. For all other components, it simulates 
 *  the event if x/y coordinates have changed by less than TOLERANCE. */
public class ClickHandler {
    private static final int TOLERANCE = 3;
    private static Point m_point;

    public static void install() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override public void eventDispatched(AWTEvent event) {
                onEvent( event);
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private static void onEvent(final AWTEvent eventIn) {
        if (!(eventIn instanceof MouseEvent) ) {
            return;
        }
    
        final MouseEvent event = (MouseEvent)eventIn;
    
        if (event.getID() == MouseEvent.MOUSE_PRESSED) {
            if (event.getSource() instanceof JTable) {
                JTable table = (JTable)event.getSource();
                int row = table.rowAtPoint(event.getPoint());
                int col = table.columnAtPoint(event.getPoint());
                m_point = new Point( row, col);
            }
            else {
                m_point = new Point( event.getX(), event.getY() );
            }
        }
        else if (event.getID() == MouseEvent.MOUSE_RELEASED && m_point != null && event.getSource() instanceof Component) {
            SwingUtilities.invokeLater( new Runnable() {
                @Override public void run() {
                    if (m_point != null && accept( event) ) {
                        MouseEvent newEvent = new M(
                            (Component)event.getSource(), MouseEvent.MOUSE_CLICKED, event.getWhen(), event.getModifiers(), 
                            event.getX(), event.getY(), event.getXOnScreen(), event.getYOnScreen(), 
                            event.getClickCount(), event.isPopupTrigger(), event.getButton() );
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent( newEvent); 
                    }
                    m_point = null;
                }
            });
        }
        else if (event.getID() == MouseEvent.MOUSE_CLICKED) {
            m_point = null;
        }
    }

    private static boolean accept(MouseEvent event) {
        if (event.getSource() instanceof JTable) {
            JTable table = (JTable)event.getSource();
            return table.rowAtPoint(event.getPoint()) == m_point.x && 
                   table.columnAtPoint(event.getPoint()) == m_point.y;
        }
        // alternatively, we could decide to call it a click if the 
        // MOUSE_RELEASED happens over the same component
//        if (event.getSource() instanceof Component) {
//            Component comp = (Component)event.getSource();
//            Dimension size = comp.getSize();
//            return event.getX() >= 0 && event.getX() < size.width &&
//                   event.getY() >= 0 && event.getY() < size.height &&
//                   Math.abs( event.getX() - m_point.x) <= TOLERANCE &&
//                   Math.abs( event.getY() - m_point.y) <= TOLERANCE;
//        }
        return Math.abs( event.getX() - m_point.x) <= TOLERANCE &&
               Math.abs( event.getY() - m_point.y) <= TOLERANCE;
    }

    public static void main(String[] args) {
        JLabel lab = new JLabel( "label 1");
        lab.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                S.out( "LABEL1 CLICKED " + e.getClass() + " " + e.getClickCount() );
            }
        });
        
        JLabel lab2 = new JLabel( "label 2");
 
        AbstractTableModel m = new AbstractTableModel() {
            @Override public int getRowCount() { return 4; }
            @Override public int getColumnCount() { return 4; }
            @Override public Object getValueAt(int rowIndex, int columnIndex) { return "xyz"; }
        };
        
        JTable t = new JTable( m);
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                S.out( "TABLE CLICKED " + e.getClass() + " " + e.getClickCount() );
            }
        });
        
        t.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                S.out( "HEADER CLICKED " + e.getClass() + " " + e.getClickCount() );
            }
        });
     

        JScrollPane s = new JScrollPane( t);
        
        ClickHandler.install();
        JFrame f = new JFrame();
        f.add( lab, BorderLayout.NORTH);
        f.add( s);
        f.add( lab2, BorderLayout.SOUTH);
        f.setSize( 200, 200);
        f.show();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                S.out( "FRAME CLICKED " + e.getClass() + " " + e.getClickCount() );
            }
        });
    }
    
    static class M extends MouseEvent {

        public M(Component source, int mouseClicked, long when, int modifiers, int x, int y, int xOnScreen, int yOnScreen, int clickCount, boolean popupTrigger, int button) {
            super( source, mouseClicked, when, modifiers, x, y, xOnScreen, yOnScreen, clickCount, popupTrigger, button);
        }
    }
}
