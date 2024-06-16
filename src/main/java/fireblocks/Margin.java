//package fireblocks;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//public class Margin {
//	static class User {
//		ArrayList<String> tokens;
//		HashMap<String,Loan> loans;
//	}
//	
//	HashMap<String,User> map;
//	
//	static class Loan {
//		double loanAmt;
//		double tokenAmt;
//
//		public double loanAmt() {
//			return loanAmt;
//		}
//		
//		public double tokenAmt() {
//			return tokenAmt;
//		}
//
//		public void increment(double loanAmt_, double tokenAmt_) {
//			loanAmt += loanAmt_;
//			tokenAmt += tokenAmt_;
//		}
//
//		public void decrement(double loanAmt_, double tokenAmt_) {
//			loanAmt -= loanAmt_;
//			tokenAmt -= tokenAmt_;
//		}
//	}
//	
//	void buy( String walletAddr, String tokenAddr, String stablecoin, double cashAmt, double borrowedAmt, double tokenAmt) {
//		rusd.buyStock( walletAddr, stablecoin, tokenAmt, cashAmt, 0);
//		
//		// mint tokenAmt stockToken into this contract
//		
//		String key = walletAddr + tokenAddr;
//		Loan loan = map.get( key);
//		if (loan == null) {
//			loan = new Loan();
//			map.put( key, loan);
//		}
//
//		loan.increment( borrowedAmt, tokenAmt); 
//	}
//	
//	void sell( String walletAddr, String tokenAddr, double saleAmt, double tokenAmt) {
//		String key = walletAddr + tokenAddr;
//		Loan loan = map.get( key);
//		require( loan != null);
//		require( tokenAmt <= loan.tokenAmt() );
//		
//		// mint excess into user's wallet
//		double excess = saleAmt - loan.loanAmt();
//		if (excess > 0) {
//			rusd.sellStock( walletAddr, rusd.address(), tokenAddr, excess, 0);
//		}
//			
//		loan.decrement( saleAmt, tokenAmt);
//		// q: what to do when selling partial shares pays off the entire loan
//		// a. keep the shares here with a loan size of zero
//		// b. transfer the shares to the user's wallet***
//		
//		// i think you need to design the interface to answer this question
//		
//		if (loan.loanAmt() == 0) {
//			delete this loan
//		
//		
//	}
//	
//	private void require(boolean b) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	void sellAll( String walletAddr, String tokenAddr, double saleAmt) {
//		String key = walletAddr + tokenAddr;
//		Loan loan = map.get( key);
//		require( loan != null);
//		
//		sell( walletAddr, tokenAddr, loan.tokenAmt(), saleAmt);
//		
//		map.delete(key);  // or loan.zeroOut()
//	}
//	
//		
//		
//		
//}
//// question: what happens if they sell and they have the stock in their wallet  