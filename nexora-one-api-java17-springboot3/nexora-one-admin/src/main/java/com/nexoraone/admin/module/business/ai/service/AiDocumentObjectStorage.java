package com.nexoraone.admin.module.business.ai.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import java.net.URI;

/**
 * 解析任务与知识库文档共用的 MinIO 对象存储，配置沿用 knowledge.minio。
 * 源文件只保存在对象存储中，本地不再产生任何业务目录或工作副本。
 */
@Service
public class AiDocumentObjectStorage {
    @Value("${knowledge.minio.endpoint:${KNOWLEDGE_MINIO_ENDPOINT:}}") private String endpoint;
    @Value("${knowledge.minio.bucket:${KNOWLEDGE_MINIO_BUCKET:nexora-knowledge}}") private String bucket;
    @Value("${knowledge.minio.access-key:${KNOWLEDGE_MINIO_ACCESS_KEY:}}") private String accessKey;
    @Value("${knowledge.minio.secret-key:${KNOWLEDGE_MINIO_SECRET_KEY:}}") private String secretKey;
    @Value("${knowledge.minio.region:${KNOWLEDGE_MINIO_REGION:us-east-1}}") private String region;
    @Value("${knowledge.minio.path-style-access:${KNOWLEDGE_MINIO_PATH_STYLE_ACCESS:true}}")
    private boolean pathStyleAccess;

    /** 写入对象，首次使用时检测存储桶是否已创建。 */
    public void put(String key, byte[] bytes, String contentType) {
        try (S3Client client = client()) {
            try {
                client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (S3Exception exception) {
                if (exception.statusCode() != 404) throw exception;
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            }
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(StrUtil.blankToDefault(contentType, "application/octet-stream")).build(),
                    RequestBody.fromBytes(bytes));
        }
    }

    /** 读取对象字节，供解析任务、下载接口使用。 */
    public byte[] get(String key) {
        try (S3Client client = client()) {
            return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        }
    }

    /** 删除对象。 */
    public void delete(String key) {
        try (S3Client client = client()) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }
    }

    /** 使用专用凭据与路径式寻址创建 MinIO 客户端。 */
    private S3Client client() {
        if (StrUtil.hasBlank(endpoint, accessKey, secretKey))
            throw new IllegalStateException("请配置 knowledge.minio.endpoint、access-key 和 secret-key");
        return S3Client.builder().region(Region.of(region)).endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccess).build()).build();
    }
}
