package io.coti.financialserver.http.data;

import io.coti.basenode.data.Hash;
import io.coti.financialserver.data.DisputeData;
import io.coti.financialserver.data.DisputeItemData;

import java.util.List;

public class ArbitratorDisputeResponseData extends GetDisputeResponseData {

    public ArbitratorDisputeResponseData(DisputeData disputeData, Hash arbitratorHash) {
        super(disputeData);
        setDisputeItems(disputeData.getDisputeItems(), arbitratorHash);
    }

    private void setDisputeItems(List<DisputeItemData> disputeItems, Hash arbitratorHash) {
        disputeItems.forEach(disputeItemData -> this.disputeItems.add(new ArbitratorDisputeItemResponseData(disputeItemData, arbitratorHash)));
    }
}
