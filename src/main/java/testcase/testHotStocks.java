package testcase;

import reflection.Config;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;

public class testHotStocks {
	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dev-config");
		for (ListEntry item : config.getSymbolsTab().fetchRows() ) {
			if (item.getBool("Active") ) {
				
			}
		}
		NewSheet.getTab(NewSheet.Reflection, "Dev-config");
	}
}
