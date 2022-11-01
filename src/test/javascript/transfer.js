import fs from "fs";
import path from "path";

import { FireblocksSDK, PeerType, TransactionStatus} from "fireblocks-sdk";
const apiSecret = fs.readFileSync(path.resolve(".", "./signer.key"), "utf8");
const fireblocks = new FireblocksSDK(apiSecret, "e3f1c5b4-48f3-f495-7b14-23e729bc3628");

 const args = {
    operation: "TRANSFER",
    assetId: "ETH",

    source: {
        type: PeerType.VAULT_ACCOUNT,
        id: "2",
    },
    destination: {        
        type: "ONE_TIME_ADDRESS",
        //id: nn for OTA
        oneTimeAddress: {
            address: "0xb016711702D3302ceF6cEb62419abBeF5c44450e"
            // tag: for Ripple only
        }
    },
    amount: "1",
    note: "Created by Peter"
};

const ret = await fireblocks.createTransaction( args);
console.log( JSON.stringify(ret) );
