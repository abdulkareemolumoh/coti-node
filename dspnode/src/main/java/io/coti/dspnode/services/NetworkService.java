package io.coti.dspnode.services;

import io.coti.basenode.crypto.NodeCryptoHelper;
import io.coti.basenode.data.Hash;
import io.coti.basenode.data.NetworkData;
import io.coti.basenode.data.NetworkNodeData;
import io.coti.basenode.data.NodeType;
import io.coti.basenode.services.BaseNodeNetworkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NetworkService extends BaseNodeNetworkService {

    @Override
    public void handleNetworkChanges(NetworkData newNetworkData) {
        log.info("New network structure received");

        Map<Hash, NetworkNodeData> newDspNodeMap = newNetworkData.getMultipleNodeMaps().get(NodeType.DspNode);
        List<NetworkNodeData> connectedDspNodes = getMapFromFactory(NodeType.DspNode).values().stream()
                .filter(dspNode -> !dspNode.getNodeHash().equals(NodeCryptoHelper.getNodeHash()))
                .collect(Collectors.toList());
        handleConnectedDspNodesChange(connectedDspNodes, newDspNodeMap, NodeType.DspNode);

        List<NetworkNodeData> dspNodesToConnect = new ArrayList<>(CollectionUtils.subtract(newNetworkData.getMultipleNodeMaps().get(NodeType.DspNode).values(),
                getMapFromFactory(NodeType.DspNode).values()));

        addListToSubscription(dspNodesToConnect);

        handleConnectedSingleNodeChange(newNetworkData, NodeType.ZeroSpendServer, NodeType.DspNode);
        handleConnectedSingleNodeChange(newNetworkData, NodeType.FinancialServer, NodeType.DspNode);

        setNetworkData(newNetworkData);
    }


}
