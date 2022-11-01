import fs from "fs";
import path from "path";

import { FireblocksSDK, PeerType, TransactionStatus} from "fireblocks-sdk";
const apiSecret = fs.readFileSync(path.resolve(".", "./signer.key"), "utf8");
const fireblocks = new FireblocksSDK(apiSecret, "e3f1c5b4-48f3-f495-7b14-23e729bc3628");

 const args = {
    operation: "CONTRACT_CALL",
    assetId: "ETH",
    amount: "0",  // without this, I got "internal error"
    note: "Created by Peter",

    source: {
        type: PeerType.VAULT_ACCOUNT,
        id: "2",
    },
    destination: {        
        type: "ONE_TIME_ADDRESS",
        //id: nn for OTA
        oneTimeAddress: {
            address: "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"  // this contract on which you are calling the smart method
            // tag: for Ripple only
        }
    },
    extraParameters: {  // the abi; see here for details: https://docs.soliditylang.org/en/v0.8.17/abi-spec.html; basically it is [0x][4 bytes for method sig][32 bytes for each static param]
        contractCallData: "0x095ea7b3000000000000000000000000b016711702D3302ceF6cEb62419abBeF5c44450e0000000000000000000000000000000000000000000000000000000000000001"
        //contractCallData: "0x38ed17390000000000000000000000000000000000000000000000000000000bb6c179cc";
    }

};

const ret = await fireblocks.createTransaction( args);
console.log( JSON.stringify(ret) );
