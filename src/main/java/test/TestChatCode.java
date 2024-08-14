package test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;

public class TestChatCode {
    private static final String INFURA_URL = "https://polygon-rpc.com/";
    private static final String WALLET_ADDRESS = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce";
    private static final List<String> ERC20_CONTRACT_ADDRESSES = Arrays.asList(
            "0xad7244b5be15e038f592f3748b4eeaa67966a1fb"
            // Add more token contract addresses here
    );

    public static void main(String[] args) throws Exception {
        Web3j web3j = Web3j.build(new HttpService(INFURA_URL));
        TransactionManager transactionManager = new ReadonlyTransactionManager(web3j, WALLET_ADDRESS);

        for (String contractAddress : ERC20_CONTRACT_ADDRESSES) {
            BigInteger balance = getERC20Balance(web3j, transactionManager, WALLET_ADDRESS, contractAddress);
            String symbol = getERC20Symbol(web3j, transactionManager, contractAddress);
            System.out.println("Balance of " + symbol + ": " + balance);
        }
    }

    private static BigInteger getERC20Balance(Web3j web3j, TransactionManager transactionManager, String walletAddress, String contractAddress) throws Exception {
        Function function = new Function(
                "balanceOf",
                Collections.singletonList(new org.web3j.abi.datatypes.Address(walletAddress)),
                Collections.singletonList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(walletAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return (BigInteger) results.get(0).getValue();
    }

    private static String getERC20Symbol(Web3j web3j, TransactionManager transactionManager, String contractAddress) throws Exception {
        Function function = new Function(
                "symbol",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return results.isEmpty() ? "UNKNOWN" : (String) results.get(0).getValue();
    }
}
