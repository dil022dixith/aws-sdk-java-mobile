package com.amazonaws.mobileconnectors.s3.transferutility;

import java.util.Map;

public class Record {
    
    private int id;
    private int mainUploadId;
    private int isRequesterPays;
    private int isMultipart;
    private int isLastPart;
    private int isEncrypted;
    private int partNumber;
    private long bytesTotal;
    private long bytesCurrent;
    private long speed;
    private long rangeStart;
    private long rangeLast;
    private long fileOffset;
    private TransferType type;
    private TransferState state;
    private String bucketName;
    private String key;
    private String versionId;
    private String file;
    private String multipartId;
    private String eTag;
    private String headerContentType;
    private String headerContentLanguage;
    private String headerContentDisposition;
    private String headerContentEncoding;
    private String headerCacheControl;
    private String headerExpire;

    private Map<String, String> userMetadata;
    private String expirationTimeRuleId;
    // This is a long representing a date, however it may be null
    private String httpExpires;
    private String sseAlgorithm;
    private String sseKMSKey;
    private String md5;
    private String cannedAcl;
    
    public Record() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMainUploadId() {
        return mainUploadId;
    }

    public void setMainUploadId(int mainUploadId) {
        this.mainUploadId = mainUploadId;
    }

    public int getIsRequesterPays() {
        return isRequesterPays;
    }

    public void setIsRequesterPays(int isRequesterPays) {
        this.isRequesterPays = isRequesterPays;
    }

    public int getIsMultipart() {
        return isMultipart;
    }

    public void setIsMultipart(int isMultipart) {
        this.isMultipart = isMultipart;
    }

    public int getIsLastPart() {
        return isLastPart;
    }

    public void setIsLastPart(int isLastPart) {
        this.isLastPart = isLastPart;
    }

    public int getIsEncrypted() {
        return isEncrypted;
    }

    public void setIsEncrypted(int isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public long getBytesTotal() {
        return bytesTotal;
    }

    public void setBytesTotal(long bytesTotal) {
        this.bytesTotal = bytesTotal;
    }

    public long getBytesCurrent() {
        return bytesCurrent;
    }

    public void setBytesCurrent(long bytesCurrent) {
        this.bytesCurrent = bytesCurrent;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(long rangeStart) {
        this.rangeStart = rangeStart;
    }

    public long getRangeLast() {
        return rangeLast;
    }

    public void setRangeLast(long rangeLast) {
        this.rangeLast = rangeLast;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public void setFileOffset(long fileOffset) {
        this.fileOffset = fileOffset;
    }

    public TransferType getType() {
        return type;
    }

    public void setType(TransferType type) {
        this.type = type;
    }

    public TransferState getState() {
        return state;
    }

    public void setState(TransferState state) {
        this.state = state;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getMultipartId() {
        return multipartId;
    }

    public void setMultipartId(String multipartId) {
        this.multipartId = multipartId;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String getHeaderContentType() {
        return headerContentType;
    }

    public void setHeaderContentType(String headerContentType) {
        this.headerContentType = headerContentType;
    }

    public String getHeaderContentLanguage() {
        return headerContentLanguage;
    }

    public void setHeaderContentLanguage(String headerContentLanguage) {
        this.headerContentLanguage = headerContentLanguage;
    }

    public String getHeaderContentDisposition() {
        return headerContentDisposition;
    }

    public void setHeaderContentDisposition(String headerContentDisposition) {
        this.headerContentDisposition = headerContentDisposition;
    }

    public String getHeaderContentEncoding() {
        return headerContentEncoding;
    }

    public void setHeaderContentEncoding(String headerContentEncoding) {
        this.headerContentEncoding = headerContentEncoding;
    }

    public String getHeaderCacheControl() {
        return headerCacheControl;
    }

    public void setHeaderCacheControl(String headerCacheControl) {
        this.headerCacheControl = headerCacheControl;
    }

    public String getHeaderExpire() {
        return headerExpire;
    }

    public void setHeaderExpire(String headerExpire) {
        this.headerExpire = headerExpire;
    }

    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }

    public void setUserMetadata(Map<String, String> userMetadata) {
        this.userMetadata = userMetadata;
    }

    public String getExpirationTimeRuleId() {
        return expirationTimeRuleId;
    }

    public void setExpirationTimeRuleId(String expirationTimeRuleId) {
        this.expirationTimeRuleId = expirationTimeRuleId;
    }

    public String getHttpExpires() {
        return httpExpires;
    }

    public void setHttpExpires(String httpExpires) {
        this.httpExpires = httpExpires;
    }

    public String getSseAlgorithm() {
        return sseAlgorithm;
    }

    public void setSseAlgorithm(String sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }

    public String getSseKMSKey() {
        return sseKMSKey;
    }

    public void setSseKMSKey(String sseKMSKey) {
        this.sseKMSKey = sseKMSKey;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(String cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    @Override
    public String toString() {
        return "Record{" + "id=" + id + ", mainUploadId=" + mainUploadId + ", isRequesterPays=" + isRequesterPays + ", isMultipart=" + isMultipart + ", isLastPart=" + isLastPart + ", isEncrypted=" + isEncrypted + ", partNumber=" + partNumber + ", bytesTotal=" + bytesTotal + ", bytesCurrent=" + bytesCurrent + ", speed=" + speed + ", rangeStart=" + rangeStart + ", rangeLast=" + rangeLast + ", fileOffset=" + fileOffset + ", type=" + type + ", state=" + state + ", bucketName=" + bucketName + ", key=" + key + ", versionId=" + versionId + ", file=" + file + ", multipartId=" + multipartId + ", eTag=" + eTag + ", headerContentType=" + headerContentType + ", headerContentLanguage=" + headerContentLanguage + ", headerContentDisposition=" + headerContentDisposition + ", headerContentEncoding=" + headerContentEncoding + ", headerCacheControl=" + headerCacheControl + ", headerExpire=" + headerExpire + ", userMetadata=" + userMetadata + ", expirationTimeRuleId=" + expirationTimeRuleId + ", httpExpires=" + httpExpires + ", sseAlgorithm=" + sseAlgorithm + ", sseKMSKey=" + sseKMSKey + ", md5=" + md5 + ", cannedAcl=" + cannedAcl + '}';
    }

}
