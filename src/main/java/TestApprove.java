import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import common.Util;
import refblocks.Refblocks;
import testcase.MyTestCase;
import tw.util.S;
import web3.Erc20;

public class TestApprove extends MyTestCase {
	
	public void testApprove() throws Exception {
		int n = Util.rnd.nextInt( 10000) + 10;

		// let Owner approve RUSD to spend BUSD
		S.out( "testing approve");
		
		m_config.busd().approve( m_config.ownerKey(), m_config.rusdAddr(), n)
				.displayHash();

		// wait for it to be reflected in wallet
		waitFor( 30, () -> {
			double appr = m_config.busd().getAllowance( m_config.ownerAddr(), m_config.rusdAddr() );
			S.out( "approved %s", appr);
			return appr == n;  
		});
	}
	
	public void testGet() throws Exception {
		S.out( m_config.busd().getAllowance( m_config.ownerAddr(), m_config.rusdAddr() ) );
		
		S.out( getAllowance( 
				m_config.ownerAddr(),
				m_config.rusdAddr(),
				m_config.busdAddr() ) );
	}
	
	public double getAllowance(String ownerAddress, String spenderAddress, String contractAddress) throws Exception {
        Function function = new Function(
                "allowance",
                Arrays.asList(new Address(ownerAddress), new Address(spenderAddress)),
                Collections.singletonList(new org.web3j.abi.TypeReference<Uint256>() {
                })
        );

        EthCall response = Refblocks.web3j.ethCall(
                Transaction.createEthCallTransaction(ownerAddress, contractAddress, FunctionEncoder.encode(function)),
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send();

        return Erc20.fromBlockchain( response.getValue(), 18 );
    }
}
