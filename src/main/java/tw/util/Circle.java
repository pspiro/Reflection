package tw.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

public class Circle extends JComponent {
    private Color color;

    // Constructor to set the circle's color
    public Circle(Color color, int size) {
        this.color = color;
        setPreferredSize(new Dimension(size, size));
    }
    
    public void setColor( Color c) {
    	color = c;
    	repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Cast to Graphics2D for better control over rendering
        Graphics2D g2d = (Graphics2D) g;
        // Enable antialiasing for smoother edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Determine the diameter of the circle using the smaller dimension of the component
        int diameter = Math.min(getWidth(), getHeight());
        // Center the circle within the component
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        // Set the color and fill the circle (no border is drawn)
        g2d.setColor(color);
        g2d.fillOval(x, y, diameter, diameter);
    }
}
