package io.coti.trustscore.services;

import io.coti.common.crypto.NodeCryptoHelper;
import io.coti.common.data.Hash;
import io.coti.common.data.TrustScoreData;
import io.coti.common.http.*;
import io.coti.common.http.data.TrustScoreResponseData;
import io.coti.common.model.TrustScores;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;

import static io.coti.common.http.HttpStringConstants.NON_EXISTING_USER_MESSAGE;
import static io.coti.common.http.HttpStringConstants.STATUS_ERROR;

@Slf4j
@Service
public class TrustScoreService {

    @Autowired
    private TrustScores trustScores;
    @Value("${kycserver.public.key}")
    private String kycServerPublicKey;

    public ResponseEntity<BaseResponse> getUserTrustScore(Hash userHash) {
        TrustScoreData trustScoreData = trustScores.getByHash(userHash);
        if (trustScoreData == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response(NON_EXISTING_USER_MESSAGE, STATUS_ERROR));
        }

        GetUserTrustScoreResponse getUserTrustScoreResponse = new GetUserTrustScoreResponse(userHash.toHexString(), trustScoreData.getTrustScore());
        return ResponseEntity.status(HttpStatus.OK).body(getUserTrustScoreResponse);
    }

    public ResponseEntity<BaseResponse> getTransactionTrustScore(Hash userHash, Hash transactionHash) {
        TrustScoreData trustScoreData = trustScores.getByHash(userHash);
        if (trustScoreData == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response(NON_EXISTING_USER_MESSAGE, STATUS_ERROR));
        }
        GetTransactionTrustScoreResponse getTransactionTrustScoreResponse =
                new GetTransactionTrustScoreResponse(
                        new TrustScoreResponseData(
                                userHash,
                                transactionHash,
                                trustScoreData.getTrustScore(),
                                NodeCryptoHelper.getNodeHash(),
                                NodeCryptoHelper.signMessage((transactionHash.toHexString() + trustScoreData.getTrustScore().toString()).getBytes())
                        ));
        return ResponseEntity.status(HttpStatus.OK).body(getTransactionTrustScoreResponse);
    }

    public ResponseEntity<BaseResponse> setKycTrustScore(SetKycTrustScoreRequest request) {
        try {
            TrustScoreData trustScoreData = new TrustScoreData(request.userHash, request.kycTrustScore, request.signature);
      /*  if (!CryptoHelper.verifyKycTrustScoreSignature(trustScoreData, kycServerPublicKey)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response(NON_EXISTING_USER_MESSAGE, STATUS_ERROR));
        } */
            TrustScoreData dbTrustScoreData = trustScores.getByHash(trustScoreData.getUserHash());
            Date date = new Date();
            if (dbTrustScoreData != null) {
                double updatedTrustScore = trustScoreData.getKycTrustScore() + (dbTrustScoreData.getTrustScore() - dbTrustScoreData.getKycTrustScore());
                trustScoreData.setTrustScore(updatedTrustScore);
                trustScoreData.setCreateTime(dbTrustScoreData.getCreateTime());
                trustScoreData.setLastUpdateTime(date);
            } else {
                trustScoreData.setTrustScore(trustScoreData.getKycTrustScore());
                trustScoreData.setCreateTime(date);
                trustScoreData.setLastUpdateTime(date);
            }
            trustScores.put(trustScoreData);
            SetKycTrustScoreResponse kycTrustScoreResponse = new SetKycTrustScoreResponse(trustScoreData);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(kycTrustScoreResponse);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response(NON_EXISTING_USER_MESSAGE, STATUS_ERROR));
        }
    }
}