package positions;

import java.util.HashSet;

import reflection.Util;
import tw.util.S;

public class PosUtil {
	static String nullWallet = "0x0000000000000000000000000000000000000000";

	/** Return a string of length 42 starting with 0x */
	static String formatWallet(String str) {
		return "0x" + Util.right( str, 40).toLowerCase();
	}

	/** Create null database entries for each block. */
	static void createNullEntriesIfNeeded(String token, HashSet<Integer> blocks) {
		try {
			for (Integer block : blocks) {
				S.out( "Inserting null entry for block %s", block);
				//MyHttpServer.insert( m_database, block, "0x0", 0); // don't enter zeros because those get filtered
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
