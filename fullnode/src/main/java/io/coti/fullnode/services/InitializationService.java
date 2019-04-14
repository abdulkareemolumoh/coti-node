package io.coti.fullnode.services;

import io.coti.basenode.crypto.NetworkNodeCrypto;
import io.coti.basenode.crypto.NodeCryptoHelper;
import io.coti.basenode.data.*;
import io.coti.basenode.data.interfaces.IPropagatable;
import io.coti.basenode.services.BaseNodeInitializationService;
import io.coti.basenode.services.interfaces.ICommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

@Service
@Slf4j
public class InitializationService extends BaseNodeInitializationService {
    @Autowired
    private ICommunicationService communicationService;
    @Value("${server.port}")
    private String serverPort;
    @Autowired
    private NetworkNodeCrypto networkNodeCrypto;
    @Value("${minimumFee}")
    private BigDecimal minimumFee;
    @Value("${maximumFee}")
    private BigDecimal maximumFee;
    @Value("${fee.percentage}")
    private BigDecimal nodeFee;
    private EnumMap<NodeType, List<Class<? extends IPropagatable>>> publisherNodeTypeToMessageTypesMap = new EnumMap<>(NodeType.class);

    @PostConstruct
    public void init() {
        super.initDB();
        super.getNetwork();

        publisherNodeTypeToMessageTypesMap.put(NodeType.DspNode, Arrays.asList(TransactionData.class, AddressData.class, DspConsensusResult.class));

        communicationService.initSubscriber(NodeType.FullNode, publisherNodeTypeToMessageTypesMap);

        List<NetworkNodeData> dspNetworkNodeData = networkService.getShuffledNetworkNodeDataListFromMapValues(NodeType.DspNode);
        if (!dspNetworkNodeData.isEmpty()) {
            networkService.setRecoveryServerAddress(dspNetworkNodeData.get(0).getHttpFullAddress());
        }
        for (int i = 0; i < dspNetworkNodeData.size() && i < 2; i++) {
            communicationService.addSubscription(dspNetworkNodeData.get(i).getPropagationFullAddress(), NodeType.DspNode);
            communicationService.addSender(dspNetworkNodeData.get(i).getReceivingFullAddress());
            ((NetworkService) networkService).addToConnectedDspNodes(dspNetworkNodeData.get(i));
        }

        super.init();
    }

    protected NetworkNodeData createNodeProperties() {
        FeeData feeData = new FeeData(nodeFee, minimumFee, maximumFee);
        if (networkService.validateFeeData(feeData)) {
            NetworkNodeData networkNodeData = new NetworkNodeData(NodeType.FullNode, nodeIp, serverPort, NodeCryptoHelper.getNodeHash(), networkType, feeData);
            return networkNodeData;
        }
        log.error("Fee Data is invalid, please fix fee properties by following coti instructions. Shutting down the server!");
        System.exit(-1);
        return null;
    }
}