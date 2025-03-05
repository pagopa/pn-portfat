package it.pagopa.pn.portfat.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class PortFatDownload {

    public static final String DOWNLOAD_ID = "downloadId";
    public static final String DOWNLOAD_URL = "downloadUrl";
    public static final String FILE_VERSION = "fileVersion";
    public static final String SHA256 = "sha256";
    public static final String STATUS = "status";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";
    public static final String ERROR_MESSAGE = "errorMessage";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(DOWNLOAD_ID)}))
    private String downloadId;

    @Getter(onMethod=@__({@DynamoDbAttribute(DOWNLOAD_URL)}))
    private String downloadUrl;

    @Getter(onMethod=@__({@DynamoDbAttribute(FILE_VERSION)}))
    private String fileVersion;

    @Getter(onMethod=@__({@DynamoDbAttribute(SHA256)}))
    private String sha256;

    @Getter(onMethod=@__({@DynamoDbAttribute(STATUS)}))
    private DownloadStatus status;

    @Getter(onMethod=@__({@DynamoDbAttribute(CREATED_AT)}))
    private String createdAt;

    @Getter(onMethod=@__({@DynamoDbAttribute(UPDATED_AT)}))
    private String updatedAt;

    @Getter(onMethod=@__({@DynamoDbAttribute(ERROR_MESSAGE)}))
    private String errorMessage;

}
