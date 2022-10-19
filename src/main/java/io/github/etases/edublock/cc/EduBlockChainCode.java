package io.github.etases.edublock.cc;

import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;

@Contract(name = "EduBlockChainCode",
        info = @Info(title = "EduBlock contract",
                description = "A contract to store & modify student records",
                version = "0.0.1"
//                license =
//                @License(name = "SPDX-License-Identifier: Apache-2.0",
//                        url = ""),
//                contact =  @Contact(email = "MyAssetContract@example.com",
//                        name = "MyAssetContract",
//                        url = "http://MyAssetContract.me")
        ))
@Default
public class EduBlockChainCode implements ContractInterface {
    public EduBlockChainCode() {
    }
}
