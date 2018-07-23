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

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.util.json.JsonUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides methods to conveniently perform database operations.
 */
class TransferDBUtil {

    private static final Log LOGGER = LogFactory.getLog(TransferDBUtil.class);

    private static final String QUERY_PLACE_HOLDER_STRING = ",?";

    /**
     * Constructs a TransferDBUtil with the given Context.
     *
     * @param context An instance of Context.
     */
    public TransferDBUtil() {
    }

    /**
     * Inserts a part upload record into database with the given values.
     *
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     *            object.
     * @param file The file to upload.
     * @param fileOffset The byte offset for the file to upload.
     * @param partNumber The part number of this part.
     * @param uploadId The multipart upload id of the upload.
     * @param bytesTotal The Total bytes of the file.
     * @param isLastPart Whether this part is the last part of the upload.
     */
    public int insertMultipartUploadRecord(String bucket, String key, File file,
            long fileOffset, int partNumber, String uploadId, long bytesTotal, int isLastPart) {
        final Record record = generateRecordForMultiPartUpload(bucket, key, file,
                fileOffset, partNumber, uploadId, bytesTotal, isLastPart, new ObjectMetadata(),
                null);
        Service.getInstance().addRecord(record);
        return record.getId();
    }

    /**
     * Inserts a transfer record into database with the given values.
     *
     * @param type The type of the transfer, can be "upload" or "download".
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     *            object.
     * @param file The file to upload.
     * @param metadata The S3 Object metadata associated with this object
     */
    public int insertSingleTransferRecord(TransferType type, String bucket, String key, File file,
            ObjectMetadata metadata) {
        return insertSingleTransferRecord(type, bucket, key, file, metadata, null);
    }

    /**
     * Inserts a transfer record into database with the given values.
     *
     * @param type The type of the transfer, can be "upload" or "download".
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     *            object.
     * @param file The file to upload.
     * @param metadata The S3 Object metadata associated with this object
     * @param cannedAcl The canned Acl of this S3 object
     * @return An Uri of the record inserted.
     */
    public int insertSingleTransferRecord(TransferType type, String bucket, String key, File file,
            ObjectMetadata metadata, CannedAccessControlList cannedAcl) {
        final Record record = generateRecordForSinglePartTransfer(type, bucket, key, file,
                metadata, cannedAcl);
        Service.getInstance().addRecord(record);
        return record.getId();
    }

    /**
     * Inserts a transfer record into database with the given values.
     *
     * @param type The type of the transfer, can be "upload" or "download".
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     *            object.
     * @param file The file to upload.
     */
    public int insertSingleTransferRecord(TransferType type, String bucket, String key, File file) {
        return insertSingleTransferRecord(type, bucket, key, file, new ObjectMetadata());
    }

    /**
     * Inserts multiple records at a time.
     *
     * @param recordsArray An array of values to insert.
     */
    public int bulkInsertTransferRecords(Record[] recordsArray) {
        for (Record record : recordsArray) {
            Service.getInstance().addRecord(record);
    }
        return recordsArray.length;
    }

    /**
     * Writes transfer status including transfer state, current transferred
     * bytes and total bytes into database.
     *
     * @param transfer a TransferRecord object
     */
    public int updateTransferRecord(TransferRecord transfer) {
        final Record record = Service.getInstance().getRecord(transfer.getRecord().getId());
        record.setId(transfer.getRecord().getId());
        record.setState(transfer.getRecord().getState());
        record.setBytesTotal(transfer.getRecord().getBytesTotal());
        record.setBytesCurrent(transfer.getRecord().getBytesCurrent());
        Service.getInstance().updateRecord(transfer.getRecord(), record);
        return record.getId();
    }

    /**
     * Updates the current bytes of a transfer record.
     *
     * @param id The id of the transfer
     * @param bytes The bytes currently transferred
     */
    public int updateBytesTransferred(int id, long bytes) {
        final Record record = getRecord(id);
        record.setBytesCurrent(bytes);
        Service.getInstance().updateRecord(getRecord(id), record);
        return record.getId();
    }

    /**
     * Updates the total bytes of a download record.
     *
     * @param id The id of the transfer
     * @param bytes The total bytes of the download.
     */
    public int updateBytesTotalForDownload(int id, long bytes) {
        final Record record = getRecord(id);
        record.setBytesTotal(bytes);
        Service.getInstance().updateRecord(getRecord(id), record);
        return record.getId();
    }

    /**
     * Updates the state but do not notify TransferService to refresh its
     * transfer record list. Therefore, only TransferObserver knows the state
     * change of the transfer record. If the new state is STATE_FAILED, we need
     * to check the original state, because "pause", "cancel" and
     * "disconnect network" actions may also cause failure message of the
     * threads, but these are not actual failure of transfers.
     *
     * @param id The id of the transfer.
     * @param state The new state of the transfer.
     */
    public int updateState(int id, TransferState state) {
        final Record record = getRecord(id);
        record.setState(state);
        Service.getInstance().updateRecord(getRecord(id), record);
        return record.getId();
    }

    /**
     * Updates the state and also notify TransferService to refresh its transfer
     * record list. The method is called by TransferUtility, more typically, by
     * applications to perform "pause" or "resume" actions, so it needs to
     * explicitly notify the Service after updating the database.
     *
     * @param id The id of the transfer.
     * @param state The new state of the transfer.
     */
    public int updateStateAndNotifyUpdate(int id, TransferState state) {
        final Record record = getRecord(id);
        record.setState(state);
        Service.getInstance().updateRecord(getRecord(id), record);
        return record.getId();
    }

    /**
     * Updates the multipart id of the transfer record.
     *
     * @param id The id of the transfer.
     * @param multipartId The multipart id of the transfer.
     */
    public int updateMultipartId(int id, String multipartId) {
        final Record record = getRecord(id);
        record.setMultipartId(multipartId);
        Service.getInstance().updateRecord(getRecord(id), record);
        return record.getId();
    }

    /**
     * Updates the Etag of the transfer record.
     *
     * @param id The id of the transfer.
     * @param etag The Etag of the transfer.
     */
    public int updateETag(int id, String etag) {
        final Record record = getRecord(id);
        record.seteTag(etag);
        Service.getInstance().updateRecord(getRecord(id), record);
        return record.getId();
    }

    /**
     * Updates states of all transfer records which are "running" and "waiting"
     * to "network disconnect"
     *
     */
    public void updateNetworkDisconnected() {
        for (Record record : Service.getInstance().getRecords()) {
            if (record.getState().equals(TransferState.IN_PROGRESS) || 
                    record.getState().equals(TransferState.RESUMED_WAITING) || 
                    record.getState().equals(TransferState.WAITING)) {
                Record newRecord = record;
                newRecord.setState(TransferState.PENDING_NETWORK_DISCONNECT);
                Service.getInstance().updateRecord(record, newRecord);
            }
        }
    }

    /**
     * Updates states of all transfer records which are "waiting for network" to
     * "waiting to resume"
     *
     */
    public void updateNetworkConnected() {
        for (Record record : Service.getInstance().getRecords()) {
            if (record.getState().equals(TransferState.PENDING_NETWORK_DISCONNECT) || 
                    record.getState().equals(TransferState.WAITING_FOR_NETWORK)) {
                Record newRecord = record;
                newRecord.setState(TransferState.RESUMED_WAITING);
                Service.getInstance().updateRecord(record, newRecord);
            }
        }
    }

    /**
     * Updates states of all transfer records which are "running" and "waiting"
     * to "paused"
     *
     * @return Number of rows updated.
     */
    public void setAllRunningRecordsToPausedBeforeShutdownService() {
        for (Record record : Service.getInstance().getRecords()) {
            if (record.getState().equals(TransferState.IN_PROGRESS) || 
                    record.getState().equals(TransferState.PENDING_PAUSE) || 
                    record.getState().equals(TransferState.RESUMED_WAITING) || 
                    record.getState().equals(TransferState.WAITING)) {
                Record newRecord = record;
                newRecord.setState(TransferState.PAUSED);
                Service.getInstance().updateRecord(record, newRecord);
            }
        }
    }

    /**

    /**
     * Queries the transfer record specified by main upload id.
     *
     * @param mainUploadId The mainUploadId of a multipart upload task
     * @return The bytes already uploaded for this multipart upload task
     */
    public long queryBytesTransferredByMainUploadId(int mainUploadId) {
        long bytesTotal = 0;
        for (Record record : getPartRecord(mainUploadId)) {
            final TransferState state = record.getState();
            if (TransferState.PART_COMPLETED.equals(TransferState.getState(state.name()))) {
                bytesTotal += record.getBytesTotal();
            }
        }
        return bytesTotal;
    }

    /**
     * Deletes the record with the given id.
     *
     * @param id The id of the transfer to be deleted.
     */
    public int deleteTransferRecords(int id) {
        Service.getInstance().removeRecord(getRecord(id));
        return id;
    }

    /**
     * Queries all the PartETags of completed parts from the multipart upload
     * specified by the mainUploadId. The list of PartETags is used to complete
     * a multipart upload, so it's usually called after all partUpload tasks are
     * finished.
     *
     * @param mainUploadId The mainUploadId of a multipart upload task
     * @return A list of PartEtag of completed parts
     */
    public List<PartETag> queryPartETagsOfUpload(int mainUploadId) {
        final List<PartETag> partETags = new ArrayList<PartETag>();
        for (Record record : getPartRecord(mainUploadId)) {
            int partNum = record.getPartNumber();
            String eTag = record.geteTag();
                partETags.add(new PartETag(partNum, eTag));
            }
        return partETags;
    }

    /**
     * Queries uncompleted partUpload tasks of a multipart upload and constructs
     * a UploadPartRequest for each task. It's used when resuming a multipart
     * upload
     *
     * @param mainUploadId The mainUploadId of a multipart upload task
     * @param multipartId The multipartId of a multipart upload task
     * @return A list of UploadPartRequest
     */
    public List<UploadPartRequest> getNonCompletedPartRequestsFromDB(int mainUploadId,
            String multipartId) {
        final ArrayList<UploadPartRequest> list = new ArrayList<UploadPartRequest>();
        for (Record record : getPartRecord(mainUploadId)) {
            final TransferState state = record.getState();
            if (! TransferState.PART_COMPLETED.equals(TransferState.getState(state.name()))) {
                final UploadPartRequest putPartRequest = new UploadPartRequest()
                        .withId(record.getId())
                        .withMainUploadId(record.getMainUploadId())
                        .withBucketName(record.getBucketName())
                        .withKey(record.getKey())
                        .withUploadId(multipartId)
                        .withFile(new File(record.getFile()))
                        .withFileOffset(record.getFileOffset())
                        .withPartNumber(record.getPartNumber())
                        .withPartSize(record.getBytesTotal())
                        .withLastPart(1 == record.getIsLastPart());
                list.add(putPartRequest);
            }
        }
        return list;
    }

    /**
     * Queries waiting for network partUpload tasks of a multipart upload and returns
     * true if one such partUpload tasks
     *
     * @param mainUploadId The mainUploadId of a multipart upload task
     * @return If a partUpload task waiting for network exist
     */
    public boolean checkWaitingForNetworkPartRequestsFromDB(int mainUploadId) {
        boolean isNetworkInterrupted = false;
        for (Record record : getPartRecord(mainUploadId)) {
            if (record.getState().equals(TransferState.WAITING_FOR_NETWORK)) {
                isNetworkInterrupted = true;
                break;
            }
            }
        return isNetworkInterrupted;
    }

    /**
     * Create a string with the required number of placeholders
     *
     * @param length Number of placeholders needed
     * @return String with the required placeholders
     */
    private String createPlaceholders(int numPlaceHolders) {
        if (numPlaceHolders <= 0) {
            LOGGER.error("Cannot create a string of 0 or less placeholders.");
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder(
                numPlaceHolders * QUERY_PLACE_HOLDER_STRING.length() - 1);
        stringBuilder.append("?");

        for (int index = 1; index < numPlaceHolders; index++) {
            stringBuilder.append(QUERY_PLACE_HOLDER_STRING);
        }
        return stringBuilder.toString();
    }

    /**
     * Generates a ContentValues object to insert into the database with the
     * given values for a multipart upload record.
     *
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     *            object.
     * @param file The file to upload.
     * @param fileOffset The byte offset for the file to upload.
     * @param partNumber The part number of this part.
     * @param uploadId The multipart upload id of the upload.
     * @param bytesTotal The Total bytes of the file.
     * @param isLastPart Whether this part is the last part of the upload.
     * @param metadata The S3 ObjectMetadata to send along with the object
     * @param cannedAcl The canned ACL associated with the object
     * @return The Record object generated.
     */
    public Record generateRecordForMultiPartUpload(String bucket,
            String key, File file, long fileOffset, int partNumber, String uploadId,
            long bytesTotal, int isLastPart, ObjectMetadata metadata,
            CannedAccessControlList cannedAcl) {
        final Record record = new Record();
        record.setType(TransferType.UPLOAD);
        record.setState(TransferState.WAITING);
        record.setBucketName(bucket);
        record.setKey(key);
        record.setFile(file.getAbsolutePath());
        record.setBytesCurrent(0L);
        record.setBytesTotal(bytesTotal);
        record.setIsMultipart(1);
        record.setPartNumber(partNumber);
        record.setFileOffset(fileOffset);
        record.setMultipartId(uploadId);
        record.setIsLastPart(isLastPart);
        record.setIsEncrypted(0);
        setRecordForObjectMetadata(record, metadata);
        if (cannedAcl != null) {
            record.setCannedAcl(cannedAcl.toString());
        }
        return record;
    }

    /**
     * Adds mappings to a ContentValues object for the data in the passed in
     * ObjectMetadata
     *
     * @param metadata The ObjectMetadata the content values should be filled
     *            with
     */
    private void setRecordForObjectMetadata(Record values, ObjectMetadata metadata) {
        values.setUserMetadata(metadata.getUserMetadata());
        values.setHeaderContentType(metadata.getContentType());
        values.setHeaderContentEncoding(metadata.getContentEncoding());
        values.setHeaderCacheControl(metadata.getCacheControl());
        values.setMd5(metadata.getContentMD5());
        values.setHeaderContentDisposition(metadata.getContentDisposition());
        values.setSseAlgorithm(metadata.getSSEAlgorithm());
        values.setSseKMSKey(metadata.getSSEAwsKmsKeyId());
        values.setExpirationTimeRuleId(metadata.getExpirationTimeRuleId());
        if (metadata.getHttpExpiresDate() != null) {
            values.setHttpExpires(String.valueOf(metadata.getHttpExpiresDate().getTime()));
        }
    }

    /**
     * Generates a ContentValues object to insert into the database with the
     * given values for a single chunk upload or download.
     *
     * @param type The type of the transfer, can be "upload" or "download".
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     *            object.
     * @param file The file to upload.
     * @param metadata The S3 ObjectMetadata to send along with the object
     * @param cannedAcl The canned ACL associated with the object
     * @return The Record object generated.
     */
    private Record generateRecordForSinglePartTransfer(TransferType type,
            String bucket, String key, File file, ObjectMetadata metadata,
            CannedAccessControlList cannedAcl) {
        final Record record = new Record();
        record.setType(type);
        record.setState(TransferState.WAITING);
        record.setBucketName(bucket);
        record.setKey(key);
        record.setFile(file.getAbsolutePath());
        record.setBytesCurrent(0L);
        if (type.equals(TransferType.UPLOAD)) {
            record.setBytesTotal(file == null ? 0L : file.length());
        }
        record.setIsMultipart(0);
        record.setPartNumber(0);
        record.setIsEncrypted(0);
        setRecordForObjectMetadata(record, metadata);
        if (cannedAcl != null) {
            record.setCannedAcl(cannedAcl.toString());
        }
        return record;
    }

    /**
     * Gets the record.
     *
     * @param id The id of the transfer.
     * @return The record specified by the id.
     */
    public Record getRecord(int id) {
        return Service.getInstance().getRecord(id);
    }

    /**
     * Gets the Uri of part records of a multipart upload.
     *
     * @param mainUploadId The main upload id of the transfer.
     * @return The Uri of the part upload records that have the given
     *         mainUploadId value.
     */
    public List<Record> getPartRecord(int mainUploadId) {
        List<Record> records = new ArrayList<Record>();
        for (Record r : Service.getInstance().getRecords()) {
            if (r.getMultipartId().equals(mainUploadId)) {
                records.add(r);
            }
        }
        return records;
    }

    /**
     * Gets the TransferRecord by id.
     *
     * @param id transfer id
     * @return a TransferRecord if exists, null otherwise
     */
    TransferRecord getTransferById(int id) {
        TransferRecord transfer = new TransferRecord();
        transfer.updateFromDB(getRecord(id));
        return transfer;
    }

}

