package redis;

import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;

public class ConfigBase {
	String symbolsTab;  // tab name where symbols are stored
	
	public String symbolsTab() { 
		return symbolsTab; 
	}
	
	public Tab getSymbolsTab() throws Exception {
		return NewSheet.getTab(NewSheet.Reflection, symbolsTab);
	}
	
}