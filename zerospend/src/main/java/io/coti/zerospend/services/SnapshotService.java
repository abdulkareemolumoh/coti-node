package io.coti.zerospend.services;

import io.coti.basenode.data.PrepareForSnapshot;
import io.coti.basenode.services.BaseNodeSnapshotService;
import io.coti.basenode.services.interfaces.ISnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SnapshotService extends BaseNodeSnapshotService {

    @Override
    public void handlePrepareForSnapshot(PrepareForSnapshot prepareForSnapshot) {
        boolean bp = true;
    }
}