package common;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import reflection.Config;
import tw.util.S;

public class Copy {
	static class HtmlTrans implements Transferable {
		private String m_str;

		HtmlTrans( String str) {
			m_str = str;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {
			//		DataFlavor.stringFlavor,
//					DataFlavor.allHtmlFlavor,
					DataFlavor.fragmentHtmlFlavor,
//					DataFlavor.selectionHtmlFlavor
			};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			for (DataFlavor flav : getTransferDataFlavors() ) {
				if (flavor.equals(flav) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			return m_str;
		}
	}
	
	public static void main( String[] args) throws UnsupportedFlavorException, IOException {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		// margin is on the outside, horz only
		// padding is on the inside, vert and horz.		
		String myString = Config.template.replace( "%text", "<strong>abcd</strong>");
		
		Transferable stringSelection = new HtmlTrans(myString);
		clipboard.setContents(stringSelection, null);

		Transferable t = clipboard.getContents(null);
		//S.out( t.getTransferData(DataFlavor.stringFlavor));
//		S.out( t.getTransferData(DataFlavor.allHtmlFlavor));
		S.out( t.getTransferData(DataFlavor.fragmentHtmlFlavor));
//		S.out( t.getTransferData(DataFlavor.selectionHtmlFlavor));
	}
}
