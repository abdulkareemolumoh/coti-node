package io.coti.common.services;

import io.coti.common.crypto.CryptoHelper;
import io.coti.common.data.Hash;
import io.coti.common.data.TransactionData;
import io.coti.common.data.TransactionIndexData;
import io.coti.common.model.TransactionIndexes;
import io.coti.common.model.Transactions;
import io.coti.common.services.interfaces.ITransactionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class TransactionIndexService {

    @Autowired
    private ITransactionHelper transactionHelper;
    @Autowired
    private TransactionIndexes transactionIndexes;
    @Autowired
    private Transactions transactions;
    private TransactionIndexData lastTransactionIndex;

    public void init(AtomicLong maxTransactionIndex) {
        byte[] accumulatedHash = "GENESIS".getBytes();
        TransactionIndexData transactionIndexData = null;
        for (long i = 0; i <= maxTransactionIndex.get(); i++) {
            transactionIndexData = transactionIndexes.getByHash(new Hash(i));
            if (transactionIndexData == null) {
                log.error("Null transaction index data found for index: {}", i);
                System.exit(-1);
            }

            TransactionData transactionData = transactions.getByHash(transactionIndexData.getTransactionHash());
            if (transactionIndexData == null || transactionData == null) {
                log.error("Null transaction data found for index: {}", i);
                System.exit(-1);
            }
            accumulatedHash = getAccumulatedHash(accumulatedHash, transactionData);
            if (!Arrays.equals(accumulatedHash, transactionIndexData.getAccumulatedHash())) {
                log.error("Incorrect accumulated hash");
                System.exit(-1);
            }
        }
        lastTransactionIndex = transactionIndexData;
    }

    public boolean insertNewTransaction(TransactionData transactionData) {
        if (transactionData.getDspConsensusResult() == null) {
            log.error("Invalid transaction index");
            return false;
        }
        if (transactionData.getDspConsensusResult().getIndex() == lastTransactionIndex.getIndex() + 1) {
            log.debug("Inserting new transaction with index: {}", lastTransactionIndex.getIndex() + 1);
            lastTransactionIndex = new TransactionIndexData(
                    transactionData.getHash(),
                    transactionData.getDspConsensusResult().getIndex(),
                    getAccumulatedHash(lastTransactionIndex.getAccumulatedHash(), transactionData));
            transactionIndexes.put(lastTransactionIndex);
            transactionHelper.removeNoneIndexedTransaction(transactionData);
        } else {
            log.error("Index is not of the last transaction: Index={}, currentLast={}", transactionData.getDspConsensusResult().getIndex(), lastTransactionIndex.getIndex());
            return false;
        }
        return true;
    }

    public TransactionIndexData getLastTransactionIndex() {
        return lastTransactionIndex;
    }

    public static byte[] getAccumulatedHash(byte[] previousAccumulatedHash, TransactionData newTransactionData) {
        byte[] newTransactionHash = newTransactionData.getHash().getBytes();
        log.debug("{}",previousAccumulatedHash);
        log.debug("{}",newTransactionHash);
        log.debug("{}",newTransactionData.getDspConsensusResult().getIndex());
        ByteBuffer combinedHash = ByteBuffer.allocate(previousAccumulatedHash.length + newTransactionHash.length + Long.BYTES);
        combinedHash.put(previousAccumulatedHash).put(newTransactionHash).putLong(newTransactionData.getDspConsensusResult().getIndex());
        return CryptoHelper.cryptoHash(combinedHash.array()).getBytes();
    }

    public Boolean isSynchronized(TransactionIndexData transactionIndexData) {
        TransactionIndexData actualTransactionIndexData = transactionIndexes.getByHash(new Hash(transactionIndexData.getIndex()));
        if (actualTransactionIndexData == null ||
                transactionIndexData.getAccumulatedHash() == null ||
                transactionIndexData.getTransactionHash() == null) {
            return false;
        }

        return transactionIndexData.getIndex() > lastTransactionIndex.getIndex() - 10 &&
                Arrays.equals(actualTransactionIndexData.getAccumulatedHash(), transactionIndexData.getAccumulatedHash()) &&
                transactionIndexData.getTransactionHash().equals(actualTransactionIndexData.getTransactionHash());
    }
}