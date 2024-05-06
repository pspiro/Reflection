//package web3;
//
//import fireblocks.FbBusdCore;
//import fireblocks.FbRusdCore;
//import fireblocks.FbStockTokenCore;
//import refblocks.MyBusdCore;
//import refblocks.MyRusdCore;
//import refblocks.MyStockTokenCore;
//import reflection.Config.Web3Type;
//import web3.Busd.IBusd;
//import web3.Rusd.IRusd;
//import web3.StockToken.IStockToken;
//
//public class CoreMaker {
//	interface ICoreMaker {
//		public IRusd getRusdCore(String address, int decimals) throws Exception;
//		public IBusd getBusdCore(String address, int decimals, String name) throws Exception;
//		public IStockToken getStockTokenCore(String address) throws Exception;
//	}
//	
//	static ICoreMaker coreMaker;
//	
//	public static void set( Web3Type type) {
//		coreMaker = type == Web3Type.Fireblocks
//				? new FbCoreMaker()
//				: new RbCoreMaker();
//	}		
//			
//	
//	static class FbCoreMaker implements ICoreMaker {
//		public IRusd getRusdCore(String address, int decimals) {
//			return new FbRusdCore(address, decimals);
//		}
//		
//		public IBusd getBusdCore(String address, int decimals, String name) throws Exception {
//			return new FbBusdCore(address, decimals, name);
//		}
//		
//		public IStockToken getStockTokenCore(String address) throws Exception {
//			return new FbStockTokenCore(address);
//		}
//	}
//	
//	static class RbCoreMaker implements ICoreMaker {
//		public IRusd getRusdCore(String address, int decimals) {
//			return new MyRusdCore( address, decimals);
//		}
//		
//		public IBusd getBusdCore( String address, int decimals, String name) {
//			return new MyBusdCore( address, decimals, name);
//		}
//		
//		public IStockToken getStockTokenCore(String address) throws Exception {
//			return new MyStockTokenCore(address);
//		}
//	}
//	
//}
