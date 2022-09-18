/*
 * $Id: ColorUtilities.java,v 1.6 2013/10/25 21:39:19 ygorodnitsky Exp $
 * 
 * @author Dennis Stetsenko
 * @since Sep 27, 2005
 * 
 * $Log: ColorUtilities.java,v $
 * Revision 1.6  2013/10/25 21:39:19  ygorodnitsky
 * ext dp20018 Execution - news ticks for API
 *
 * Revision 1.5  2012/11/29 06:48:20  gszekely
 * ext bz42507 Cannot change color of Candlestick tail color
 *
 * Revision 1.4  2012/06/04 15:09:52  adoroszl
 * int bz38069 CCI study disappears when SAR study is added to the same chart
 *
 * Revision 1.3  2011/11/15 21:10:33  dstetsenko
 * int dp14100 Activity Monitor - framework and Orders/Trades/Trades Summary screens (on VI/RF/DSa behalf)
 *
 * Revision 1.2.40.1  2011/11/15 12:11:16  vaivanov
 * int dp14100 - Activity Monitor - framework and Orders/Trades/Trades Summary screens
 *
 * Revision 1.2  2011/05/31 16:27:01  dstetsenko
 * int dp12665 Fundamentals - drop-down menu, company name, comparable companies, black color (on VI behalf)
 *
 * Revision 1.1  2010/06/16 13:42:27  smangla
 * int dp9969 Move packages in launcher folder to subpackages under twslaunch
 *
 * Revision 1.1  2010/05/10 15:19:35  smangla
 * int dp9575 Extract authentication phase from TWS
 *
 * Revision 1.11  2010/03/22 18:59:11  dstetsenko
 * int bz0000 issues with ? data marker
 *
 * Revision 1.10  2009/06/01 22:45:18  ygorodnitsky
 * int dp6958 iServer implementation
 *
 * Revision 1.9.28.2  2009/05/27 16:11:17  dtarasso
 * int dp7084 iServer continued implementation (Account details, trades, live orders)
 *
 * Revision 1.9.28.1  2009/05/27 15:16:06  ygorodnitsky
 * int dp7234 merge main
 *
 * Revision 1.9.10.1  2009/05/05 17:25:47  dtarasso
 * int dp7084 iServer continued implementation (Account details, trades, live orders)
 *
 * Revision 1.9  2009/04/08 10:17:29  dtarasso
 * ext dp6504 Add Last Trade Time column (colored) to TWS
 *
 * Revision 1.8  2008/04/09 18:54:12  pspiro
 * int bz9999 make light colors darker for alternating rows
 *
 * Revision 1.7  2008/04/01 15:00:13  vaivanov
 * ext dp4013 Add graphic columns for market data
 *
 * Revision 1.6  2008/03/10 21:50:53  dstetsenko
 * int dp0000 lf changes - alternation color for  book trader when grid lines are not present
 *
 * Revision 1.5  2008/02/07 23:13:07  ptitov
 * ext dp3251 Improve TWS look and feel (Dragon) (on behalf of Pete P and Dennis S)
 *
 * Revision 1.4.50.2  2008/02/04 18:24:36  ppoulos
 * ext dp3032 Improve TWS look and feel (Dragon) - Merging LAF changes into Order Wizard
 *
 * Revision 1.4.50.1  2008/02/01 17:16:34  dstetsenko
 * ext dp3032 Improve TWS look and feel - creating 2nd branch
 *
 * Revision 1.4.38.9  2008/01/14 23:04:15  ygorodnitsky
 * int dp3042 colors
 *
 * Revision 1.4.38.8  2008/01/14 22:00:17  ygorodnitsky
 * int dp3042 colors
 *
 * Revision 1.4.38.7  2008/01/14 17:59:24  ppoulos
 * ext dp3032 Improve TWS look and feel (Dragon)
 *
 * Revision 1.4.38.6  2008/01/10 20:36:55  ppoulos
 * ext dp3032 Improve TWS look and feel (Dragon)
 *
 * Revision 1.4.38.5  2008/01/07 19:27:45  ygorodnitsky
 * int dp3042 icons
 *
 * Revision 1.4.38.4  2008/01/07 19:07:41  ppoulos
 * ext dp3032 Improve TWS look and feel (Dragon)
 *
 * Revision 1.4.38.3  2008/01/07 15:05:00  ppoulos
 * ext dp3032 Improve TWS look and feel (Dragon)
 *
 * Revision 1.4.38.2  2008/01/03 18:57:19  ptitov
 * int dp3251 Improve TWS look and feel (Dragon)
 *
 * Revision 1.4.38.1  2008/01/03 18:49:01  ptitov
 * int dp3251 Improve TWS look and feel (Dragon)
 *
 * Revision 1.4  2007/06/12 15:25:12  ptitov
 * ext dp909 Face-lift for Account Window
 *
 * Revision 1.3  2007/06/04 16:05:45  ptitov
 * ext dp1143 Update and simplify initial TWS layout (combination Barrons/iTWS).
 *
 * Revision 1.2  2007/01/17 19:54:56  ptitov
 * enhancement: refactor method to be located in utilities class
 *
 * Revision 1.1  2006/09/18 20:46:10  yzou
 * move to lib
 *
 * Revision 1.4  2006/01/24 21:15:33  ygorodnitsky
 * add halfGamma method
 *
 * Revision 1.3  2005/09/30 21:34:19  pspiro
 * add mix
 *
 * Revision 1.2  2005/09/27 21:23:29  dstetsenko
 * minor changes
 *
 * Revision 1.1  2005/09/27 21:13:07  dstetsenko
 * refactoring color related static calls, moving them from S class to ColorUtilities
 *
 */
package tw.util;

import java.awt.Color;
import java.util.HashMap;
/**
 * Color related manipulation should go into this utility class
 */
public class ColorUtilities {
    private static final transient HashMap<Color, Color> s_faintMap = new HashMap<Color, Color>();
    
    private ColorUtilities() { /* utility class */ }
    
    public static Color brighten( Color color, int a, int b) {
        return new Color(
            brighten( color.getRed(),   a, b),
            brighten( color.getGreen(), a, b),
            brighten( color.getBlue(),  a, b) );
    }
    
    public static Color darken(Color color) {
        return new Color(
            brighten( color.getRed(),   120, 255),
            brighten( color.getGreen(), 120, 255),
            brighten( color.getBlue(),  120, 255) );
    }

    
    public static Color darken( Color color, int a, int b) {
        return new Color(
            darken( color.getRed(),   a, b),
            darken( color.getGreen(), a, b),
            darken( color.getBlue(),  a, b) );
    }


    /** note: if you'll use GRAY for this function it would return GRAY */
    public static Color invert(Color color) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
    
        return new Color(255 - red, 255 - green, 255 - blue);
    }
    
    public static Color transparency(Color color, int trans){
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), trans);
    }

    private static int brighten( int col, int c1, int c2) {
        // col is the color to brighten
        // c1 is the amt to brighten black
        // c2 is the cutoff at the high end where we don't brighten anymore
        int inc = c1 - (col * c1 / c2);
        col += inc;
        return col > 255 ? 255 : col < 0 ? 0 : col;
    }

    private static int darken( int col, int c1, int c2) {
        // col is the color to darken
        // c1 is the amt to darken black
        // c2 is the cutoff at the high end where we don't darken anymore
        int inc = c1 - (col * c1 / c2);
        col -= inc;
        return col > 255 ? 255 : col < 0 ? 0 : col;
    }
    
    public static Color halfGamma(Color oldColor){
        return new Color((int)(oldColor.getRed()*0.5),(int)(oldColor.getGreen()*0.5),(int)(oldColor.getBlue()*0.5));
    }
    
    public static Color brightest( Color color) {
        return brighten( color, 120, 255);
    }
    
    public static Color mix(Color c1, Color c2) {
        return new Color( (c1.getRed()   + c2.getRed())   / 2,
                          (c1.getGreen() + c2.getGreen()) / 2,
                          (c1.getBlue()  + c2.getBlue())  / 2);
    }
    
    /** almost same as awt methods (darker, brighter), but with factor as an argument */
    public static Color mod(Color c, float factor) {
        if (factor > 1.0) {
            return new Color(Math.min((int)(c.getRed() * factor), 255), 
                                Math.min((int)(c.getGreen() * factor), 255),
                                Math.min((int)(c.getBlue() * factor), 255));
        } 
        return new Color(Math.max((int)(c.getRed() * factor), 0), 
                            Math.max((int)(c.getGreen() * factor), 0),
                            Math.max((int)(c.getBlue() * factor), 0)); 
    }
        
    /** calculate the color */
    public static Color faintColor(Color sourceColor) {
        // in theory we should sync here too, but we won't do it since chances of us hitting concurrent mod exception is minuscule, 
        // not worth the performance penalty, and most likely we will recover from that gracefully 
        Color faintColor = s_faintMap.get(sourceColor);
        
        if ( faintColor == null ) {
            synchronized (s_faintMap) {
                // use double lock sync to achieve the best possible performance
                if ( faintColor == null ) {
                    int r = sourceColor.getRed();
                    int g = sourceColor.getGreen();
                    int b = sourceColor.getBlue();
                    int shift = 96;
                    
                    r = r < 128 ? r + shift : r - shift;
                    g = g < 128 ? g + shift : g - shift;
                    b = b < 128 ? b + shift : b - shift;
                    
                    // dst: don't use alpha channel != 255 since it cause problems in Java Scroll Pane!
                    faintColor = new Color(r, g, b, 255);
                    
                    s_faintMap.put(sourceColor, faintColor);
                }
            }
        }
        
        return faintColor; 
    }

    public static Color composite(Color colorA, Color colorB, float trans){
        float arr0[] = colorA.getRGBComponents(null);
        float arr1[] = colorB.getRGBComponents(null);
        float ret[] = new float[4];
        for(int i=0; i < ret.length; i++){ 
            ret[i] = (1-trans)*arr0[i] + arr1[i]*trans;
        }
        return new Color(ret[0], ret[1], ret[2]);
    
    }

    public static Color composite(Color colorA, Color colorB){
        return composite(colorA, colorB, colorB.getAlpha()/255f);
    }
    
    public static Color colorize(Color grayScale, Color color){
        return composite(grayScale, color, (255f - (grayScale.getRed() + grayScale.getGreen() + grayScale.getBlue())/3f)/255f);
    }

    /** returns the value of the brightest color component of the color */
    public static int brightness(Color color) {
        return Math.max(color.getRed(), Math.max(color.getBlue(), color.getGreen()));
    }
    
    /** rescale the colors value range from (0 to 255) to (offset to offset+range) */
    public static Color rescaleColor(Color color, int offset, int range) {
        int red   = color.getRed()   * range / 255 + offset;
        int green = color.getGreen() * range / 255 + offset;
        int blue  = color.getBlue()  * range / 255 + offset;
        int alpha = color.getAlpha();
        
        return new Color(red, green, blue, alpha);
    }

    /** rescale the colors value range from one color (brighter) to another (darker) using a scale factor (0.0 - 1.0)*/
    public static Color rescaleColor( Color from, Color to, double scale ) {
        int red   = (int) ((from.getRed() - to.getRed()) * scale) + to.getRed();
        int green = (int) ((from.getGreen() - to.getGreen()) * scale) + to.getGreen();
        int blue  = (int) ((from.getBlue() - to.getBlue()) * scale) + to.getBlue();
        int alpha = (int) ((from.getAlpha() - to.getAlpha()) * scale) + to.getAlpha();
        
        return new Color(red, green, blue, alpha);
    }

    /** Computes the highlight color for shaded controls on a given background color. 
     * The selected color is chosen such that there is enough contrast between the two colors to be distinguishable on any colored background. */
    public static Color deriveControlHighlight(Color color) {
        if (brightness(color) < 65) {
            return rescaleColor(color, 65, 200);
        }
        return rescaleColor(color, 40, 100);
    }

    /** Computes the shadow color for shaded controls on a given background color. 
     * The selected color is chosen such that there is enough contrast between the two colors to be distinguishable on any colored background. */
    public static Color deriveControlShadow(Color color) {
        if (brightness(color) < 65) {
            return rescaleColor(color, 40, 200);
        }
        return rescaleColor(color, 0, 100);
    }
    
    /** generates the html hex string for this color (ignores alpha channel) in the format #RRGGBB */
    public static String htmlString(Color color) {
        return color != null ? "#" + Integer.toHexString(color.getRGB() << 8 >>> 8) : null;
    }
       
    public static boolean isVeryClose(Color c1, Color c2) {
        return Math.abs(c1.getRed() - c2.getRed()) < 64 &&
               Math.abs(c1.getGreen() - c2.getGreen()) < 64 &&
               Math.abs(c1.getBlue() - c2.getBlue()) < 64;
    }

    public static boolean isVeryCloseOnTwo(Color one, Color two) {
        int similar = 0;
        if(one == null || two == null){
            return false;
        }
        float a_one[] = one.getColorComponents(null);
        float a_two[] = two.getColorComponents(null);
        for(int i =0; i < a_one.length && i < a_two.length; i++){
            if(Math.abs(a_one[i]-a_two[i]) < 0.1f  ){
                similar++;
            }
        }
        return similar >= 2;
    }
    
    public static boolean similarColors(Color one, Color two){
        if(one == null || two == null){
            return false;
        }
        float a_one[] = one.getColorComponents(null);
        float a_two[] = two.getColorComponents(null);
        for(int i =0; i < a_one.length && i < a_two.length; i++){
            if(Math.abs(a_one[i]-a_two[i]) > 0.3f  ){
                return false;
            }
        }
        return true;
    }

    public static double getTone(Color color){
        return getTone(new int[]{color.getRed(), color.getGreen(), color.getBlue()});
    }

    public static double getTone(int[] color) {
        return 0.299 * color[0] + 0.587 * color[1] + 0.114 * color[2];
    }

    /** if the colors have a similar tonal value, then if the background is a dark tone, then brighten the foreground, otherwise darken the forground */
    public static Color adjustTone(Color bg, Color fg){
        double bg_tone = getTone(bg);
        double fg_tone = getTone(fg);
        if(Math.abs(bg_tone-fg_tone) < 90){
            return composite(fg, bg_tone < 128 ? Color.WHITE : Color.BLACK, 0.75f);
        }
        return fg;
    }
    
    public static Color whiter(Color c) {
        float[] comp = c.getRGBColorComponents(null);
        return new Color(whiter(comp[0]), whiter(comp[1]), whiter(comp[2]));
    }

    public static float whiter(float c) {
        return 1 - (1 - c) * 0.7f;
    }    
    
    public static Color redGradation(Color baseColor, double factor) {
        double f = factor > 0.0 ? factor * 2.0 : 0.0;

        return new Color(
            baseColor.getRed(),
            baseColor.getGreen() + (int)(f * ( 255 - baseColor.getGreen())),
            baseColor.getBlue() + (int)(f * ( 255 - baseColor.getBlue())));
    }

    public static Color greenGradation(Color baseColor, double factor) {
        double f = factor < 1.0 ? 2 - factor * 2.0 : 0.0;

      return new Color( 
          baseColor.getRed() + (int)(f * (255 - baseColor.getRed())), 
          baseColor.getGreen(), 
          baseColor.getBlue() + (int)(f * (255 - baseColor.getBlue())));
    }
    
    public static int average(Color color) {
        return (color.getRed() + color.getBlue() + color.getGreen() ) / 3;
    }

    public static String toHexString( Color color ) {
        String hex = Integer.toHexString(color.getRGB() << 8 >>> 8).toUpperCase();
        while( hex.length() < 6 ) { // adding leading zeros. the color is 3 byte value (without alpha)  
            hex = "0" + hex;
        }
        return "#" + hex;
    }
    
    public static int distance(Color c1, Color c2) {
        return Math.abs(c1.getRed() - c2.getRed()) +
            Math.abs(c1.getGreen() - c2.getGreen()) +
            Math.abs(c1.getBlue() - c2.getBlue());
    }
    
    public static Color invertForegroundColorIfNecessary(Color foregroundColor, Color backgroundColor) {
        if (isVeryClose(foregroundColor, backgroundColor)) {
            Color inverseFg = invert(foregroundColor);
            
            // if both foreground and its inverse are "very close" to the background,
            // then the foreground would be flashing (inverted on each repaint);
            // to prevent that, we use the farther one of the two colors
            if (isVeryClose(inverseFg, backgroundColor)) {
                int distance = distance(foregroundColor, backgroundColor);
                int inverseDistance = distance(inverseFg, backgroundColor);
                if (inverseDistance > distance) {
                    return inverseFg;
                }
            } else {
                return inverseFg;
            }
        }
        return foregroundColor;
    }
}
