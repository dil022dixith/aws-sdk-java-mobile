/**
 * Copyright 2015-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.mobileconnectors.s3.transferutility;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService.NetworkInfoReceiver;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * TransferRecord is used to store all the information of a transfer and
 * start/stop the a thread for the transfer task.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
class TransferRecord {
    private static final Log LOGGER = LogFactory.getLog(TransferRecord.class);

    private Record record;

    private Future<?> submittedTask;

    /**
     * Constructs a TransferRecord and initializes the transfer id and S3
     * client.
     *
     * @param id The id of a transfer.
     */
    public TransferRecord() {
    }

    /**
     * Updates all the fields from database using the given Cursor.
     *
     * @param record A Record pointing to a transfer record.
     */
    public void updateFromDB(Record record) {
        this.record = record;
    }

    /**
     * Checks the state of the transfer and starts a thread to run the transfer
     * task if possible.
     *
     * @param s3 s3 instance
     * @param dbUtil database util
     * @param updater status updater
     * @param networkInfo network info
     * @return Whether the task is running.
     */
    public boolean start(AmazonS3 s3, TransferDBUtil dbUtil, TransferStatusUpdater updater,
            NetworkInfoReceiver networkInfo) {
        if (!isRunning() && checkIsReadyToRun()) {
            if (TransferType.DOWNLOAD.equals(record.getType())) {
                LOGGER.info("Task Download ");
                submittedTask = TransferThreadPool
                        .submitTask(new DownloadTask(this, s3, updater, networkInfo));
            } else {
                LOGGER.info("Task Upload ");
                submittedTask = TransferThreadPool
                        .submitTask(new UploadTask(this, s3, dbUtil, updater, networkInfo));
            }
            return true;
        }
        return false;
    }

    /**
     * Pauses a running transfer.
     *
     * @param s3 s3 instance
     * @param updater status updater
     * @return true if the transfer is running and is paused successfully, false
     *         otherwise
     */
    public boolean pause(AmazonS3 s3, TransferStatusUpdater updater) {
        if (!isFinalState(record.getState()) && !TransferState.PAUSED.equals(record.getState())) {
            updater.updateState(record.getId(), TransferState.PAUSED);
            if (isRunning()) {
                submittedTask.cancel(true);
            }
            return true;
        }
        return false;
    }

    /**
     * Cancels a running transfer.
     * 
     * @param s3 s3 instance
     * @param updater status updater
     * @return true if the transfer is running and is canceled successfully,
     *         false otherwise
     */
    public boolean cancel(final AmazonS3 s3, final TransferStatusUpdater updater) {
        if (!isFinalState(record.getState())) {
            updater.updateState(record.getId(), TransferState.CANCELED);
            if (isRunning()) {
                submittedTask.cancel(true);
            }
            // additional cleanups
            if (record.getIsMultipart() == 1) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            s3.abortMultipartUpload(new AbortMultipartUploadRequest(record.getBucketName(),
                                    record.getKey(), record.getMultipartId()));
                            LOGGER.debug("Successfully clean up multipart upload: " + record.getId());
                        } catch (final AmazonClientException e) {
                            LOGGER.debug("Failed to abort multiplart upload: " + record.getId(), e);
                        }
                    }
                }).start();
            } else if (TransferType.DOWNLOAD.equals(record.getType())) {
                // remove partially download file
                new File(record.getFile()).delete();
            }
            return true;
        }
        return false;
    }

    /**
     * Checks whether the transfer is actively running
     *
     * @return true if the transfer is running
     */
    boolean isRunning() {
        return submittedTask != null && !submittedTask.isDone();
    }

    /**
     * Wait till transfer finishes.
     *
     * @param timeout the maximum time to wait in milliseconds
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    void waitTillFinish(long timeout) throws InterruptedException, ExecutionException,
            TimeoutException {
        if (isRunning()) {
            submittedTask.get(timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Determines whether a transfer state is a final state.
     */
    @SuppressWarnings("checkstyle:hiddenfield")
    private boolean isFinalState(TransferState state) {
        return TransferState.COMPLETED.equals(state)
                || TransferState.FAILED.equals(state)
                || TransferState.CANCELED.equals(state);
    }

    private boolean checkIsReadyToRun() {
        return record.getPartNumber() == 0 && !TransferState.COMPLETED.equals(record.getState());
    }

    public Record getRecord() {
        return record;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[")
                .append("id:").append(record.getId()).append(",")
                .append("bucketName:").append(record.getBucketName()).append(",")
                .append("key:").append(record.getKey()).append(",")
                .append("file:").append(record.getFile()).append(",")
                .append("type:").append(record.getType()).append(",")
                .append("bytesTotal:").append(record.getBytesTotal()).append(",")
                .append("bytesCurrent:").append(record.getBytesCurrent()).append(",")
                .append("fileOffset:").append(record.getFileOffset()).append(",")
                .append("state:").append(record.getState()).append(",")
                .append("cannedAcl:").append(record.getCannedAcl()).append(",")
                .append("mainUploadId:").append(record.getMainUploadId()).append(",")
                .append("isMultipart:").append(record.getIsMultipart()).append(",")
                .append("isLastPart:").append(record.getIsLastPart()).append(",")
                .append("partNumber:").append(record.getPartNumber()).append(",")
                .append("multipartId:").append(record.getMultipartId()).append(",")
                .append("eTag:").append(record.geteTag())
                .append("]");
        return sb.toString();
    }
}
