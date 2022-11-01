import fs from "fs";
import path from "path";

import { FireblocksSDK, PeerType, TransactionStatus} from "fireblocks-sdk";
const apiSecret = fs.readFileSync(path.resolve(".", "./signer.key"), "utf8");
const fireblocks = new FireblocksSDK(apiSecret, "e3f1c5b4-48f3-f495-7b14-23e729bc3628");

const ret = await fireblocks.getVaultAccountsWithPageInfo();

console.log( JSON.stringify(ret) );