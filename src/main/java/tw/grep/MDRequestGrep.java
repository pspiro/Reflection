/*
 * $Id: MDRequestGrep.java,v 1.1 2011/02/22 21:09:43 ptitov Exp $
 * 
 * @author Pavel Titov
 * @since Feb 22, 2011
 * 
 * $Log: MDRequestGrep.java,v $
 * Revision 1.1  2011/02/22 21:09:43  ptitov
 * int bz0000 status subscriptions
 *
 */
package tw.grep;

import java.io.BufferedReader;
import java.io.FileReader;

/** loads and parses svcIn.t */
public class MDRequestGrep {

	public static final char FIELDSEPARATOR_CHAR = 0x01;
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: svcOut.t");
			return;
		}
		try {
			int topCount = 0;
			int mdStatusCount = 0;
			
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			String line = null;
			while ((line = br.readLine()) != null) {
				String nline = line.replace(FIELDSEPARATOR_CHAR, '|');
				if (nline.contains("|263=1|")) {
					continue; // something else, not subscription
				}
				topCount += countTokens(nline, "|264=1|");
				mdStatusCount += countTokens(nline, "|264=1|");
			}			
			br.close();
			
			System.out.println("RESULT:");
			System.out.println("\tTop = " + topCount);
			System.out.println("\tMdStatus = " + mdStatusCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static int countTokens(String line, String token) {
		int count = 0;
		int pos = -1;
		while ((pos = line.indexOf(token, pos + 1)) >= 0) {
			count++;
		}
		return count;
	}
}

