package testcase;

import java.util.Map;

import org.json.simple.JsonObject;

import common.Util;
import refblocks.Refblocks;
import web3.NodeServer;

public class TestReqPositionsMap extends MyTestCase {
	
	public void test() throws Exception {
		Map<String, Double> map = Refblocks.reqPositionsMap( 
				NodeServer.prod, 
				Util.toArray(
					"0xae42be9e63a3cbfecd22cde0f22796a5e2c55b27", // correct here, monitor displays wrong value 
					"0x7dc9dd2ef97120a9706996ae991262a2de18cddc",
					"0x204060c7952b1983c136b192906394ff3aae14f0",
					"0xc8810dd59f9efc1273611a440c015edb8f04c69c",
					"0x76968e7e223eba6f3c84d8dc9f3620b7cee75bc3",
					"0x6891d4d1e4e548812ff3bd095698818fc8700ff8",
					"0x514f644d3ef906c45c53168253177d3e2fea197d",
					"0x52cdf43c5eacd1515c01a0a0195865b1fd51cbd5",
					"0xba889cb68adcb6a63bf26a258a45157339d42575",
					"0x9b499219798a051dfb50ffa3fa31a6b4259d96c5",
					"0xf236f7679c683c30be7d4f877c0d5bb320892d99",
					"0x9a70e2053559011644fcc047881cc40bf004f43a",
					"0x972eb737a9ba0e263cfd70d47a0e907f835675ec"),
				0);
		new JsonObject( map).display();
	}
}