/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import com.amazonaws.AmazonClientException;
import com.amazonaws.RequestClientOptions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.io.BaseEncoding;

/**
 * An {@link ArtifactRepository} implementation for the AWS S3 service. All
 * binaries are stored in single bucket using the configured name
 * {@link S3RepositoryProperties#getBucketName()}.
 *
 * From the AWS S3 documentation:
 * <p>
 * There is no limit to the number of objects that can be stored in a bucket and
 * no difference in performance whether you use many buckets or just a few. You
 * can store all of your objects in a single bucket, or you can organize them
 * across several buckets.
 * </p>
 */
@Validated
public class S3Repository extends AbstractArtifactRepository {
    private static final Logger LOG = LoggerFactory.getLogger(S3Repository.class);

    private final AmazonS3 amazonS3;
    private final S3RepositoryProperties s3Properties;

    /**
     * Constructor.
     *
     * @param amazonS3
     *            the amazonS3 client to use
     * @param s3Properties
     *            the properties which e.g. holds the name of the bucket to
     *            store in
     */
    public S3Repository(final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties) {
        this.amazonS3 = amazonS3;
        this.s3Properties = s3Properties;
    }

    @Override
    protected AbstractDbArtifact store(final String tenant, final DbArtifactHash base16Hashes, final String contentType,
            final String tempFile) throws IOException {
        final File file = new File(tempFile);

        final S3Artifact s3Artifact = createS3Artifact(tenant, base16Hashes, contentType, file);
        final String key = objectKey(tenant, base16Hashes.getSha1());

        LOG.info("Storing file {} with length {} to AWS S3 bucket {} with key {}", file.getName(), file.length(),
                s3Properties.getBucketName(), key);

        if (existsByTenantAndSha1(tenant, base16Hashes.getSha1())) {
            LOG.debug("Artifact {} already exists on S3 bucket {}, don't need to upload twice", key,
                    s3Properties.getBucketName());
            return s3Artifact;
        }

        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(file),
                RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE)) {
            final ObjectMetadata objectMetadata = createObjectMetadata(base16Hashes.getMd5(), contentType, file);
            final PutObjectResult result = amazonS3.putObject(s3Properties.getBucketName(), key, inputStream,
                    objectMetadata);

            LOG.debug("Artifact {} stored on S3 bucket {} with server side Etag {} and MD5 hash {}", key,
                    s3Properties.getBucketName(), result.getETag(), result.getContentMd5());

            return s3Artifact;
        } catch (final AmazonClientException e) {
            throw new ArtifactStoreException("Failed to store artifact into S3 ", e);
        }
    }

    private S3Artifact createS3Artifact(final String tenant, final DbArtifactHash hashes, final String contentType,
            final File file) {
        return new S3Artifact(amazonS3, s3Properties, objectKey(tenant, hashes.getSha1()), hashes.getSha1(), hashes,
                file.length(), contentType);
    }

    private ObjectMetadata createObjectMetadata(final String mdMD5Hash16, final String contentType, final File file) {
        final ObjectMetadata objectMetadata = new ObjectMetadata();
        final String mdMD5Hash64 = Base64.getEncoder()
                .encodeToString(BaseEncoding.base16().lowerCase().decode(mdMD5Hash16));
        objectMetadata.setContentMD5(mdMD5Hash64);
        objectMetadata.setContentType(contentType);
        objectMetadata.setContentLength(file.length());
        if (s3Properties.isServerSideEncryption()) {
            objectMetadata.setHeader(Headers.SERVER_SIDE_ENCRYPTION, s3Properties.getServerSideEncryptionAlgorithm());
        }
        return objectMetadata;
    }

    @Override
    public void deleteBySha1(final String tenant, final String sha1Hash) {
        final String key = objectKey(tenant, sha1Hash);

        LOG.info("Deleting S3 object from bucket {} and key {}", s3Properties.getBucketName(), key);
        amazonS3.deleteObject(new DeleteObjectRequest(s3Properties.getBucketName(), key));
    }

    private static String objectKey(final String tenant, final String sha1Hash) {
        return sanitizeTenant(tenant) + "/" + sha1Hash;
    }

    @Override
    public AbstractDbArtifact getArtifactBySha1(final String tenant, final String sha1Hash) {
        final String key = objectKey(tenant, sha1Hash);

        LOG.info("Retrieving S3 object from bucket {} and key {}", s3Properties.getBucketName(), key);

        var req = new GetObjectMetadataRequest(s3Properties.getBucketName(), key);
        var objMeta = amazonS3.getObjectMetadata(req);
        if (objMeta == null) {
            return null;
        }

        return new S3Artifact(amazonS3, s3Properties, key, sha1Hash, new DbArtifactHash(sha1Hash, null, null),
                objMeta.getContentLength(), objMeta.getContentType());
    }

    @Override
    public boolean existsByTenantAndSha1(final String tenant, final String sha1Hash) {
        return amazonS3.doesObjectExist(s3Properties.getBucketName(), objectKey(tenant, sha1Hash));
    }

    @Override
    public void deleteByTenant(final String tenant) {
        final String folder = sanitizeTenant(tenant);

        LOG.info("Deleting S3 object folder (tenant) from bucket {} and key {}", s3Properties.getBucketName(), folder);

        // Delete artifacts
        ObjectListing objects = amazonS3.listObjects(s3Properties.getBucketName(), folder + "/");
        do {
            for (final S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                amazonS3.deleteObject(s3Properties.getBucketName(), objectSummary.getKey());
            }
            objects = amazonS3.listNextBatchOfObjects(objects);
        } while (objects.isTruncated());

    }

}
