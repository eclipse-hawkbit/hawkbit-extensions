/**
 * Copyright (c) 2019 Rico Pahlisch and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.io.BaseEncoding;

/**
 * An {@link ArtifactRepository} implementation for the Gcloud GCS service. All
 * binaries are stored in single bucket using the configured name
 * {@link GcsRepositoryProperties#getBucketName()}.
 * </p>
 */
@Validated
public class GcsRepository extends AbstractArtifactRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GcsRepository.class);

    private final Storage gcsStorage;
    private final GcsRepositoryProperties gcsProperties;

    /**
     * Constructor.
     *
     * @param gcsStorage
     *            the gcsStorage client to use
     * @param gcsProperties
     *            the properties which e.g. holds the name of the bucket to store in
     */
    public GcsRepository(final Storage gcsStorage, final GcsRepositoryProperties gcsProperties) {
        this.gcsStorage = gcsStorage;
        this.gcsProperties = gcsProperties;
    }

    private static String objectKey(final String tenant, final String sha1Hash) {
        return sanitizeTenant(tenant) + "/" + sha1Hash;
    }

    @Override
    protected AbstractDbArtifact store(final String tenant, final String sha1Hash16, final String mdMD5Hash16,
            final String contentType, final String tempFile) throws IOException {
        final File file = new File(tempFile);

        final GcsArtifact gcsArtifact = createGcsArtifact(tenant, sha1Hash16, mdMD5Hash16, contentType, file);
        final String key = objectKey(tenant, sha1Hash16);

        LOG.info("Storing file {} with length {} to GCS bucket {} with key {}", file.getName(), file.length(),
                gcsProperties.getBucketName(), key);

        if (exists(key)) {
            LOG.debug("Artifact {} already exists on GCS bucket {}, don't need to upload twice", key,
                    gcsProperties.getBucketName());
            return gcsArtifact;
        }

        try (InputStream fileStream = new FileInputStream(file)) {
            byte[] data = IOUtils.toByteArray(fileStream);
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(gcsProperties.getBucketName(), key)).setMd5(mdMD5Hash16)
                    .setContentType(contentType).build();
            Blob blob = gcsStorage.create(blobInfo, data);
            LOG.debug("Artifact {} stored on GCS bucket {} with server side Etag {} and MD5 hash {}", key,
                    gcsProperties.getBucketName(), blob.getEtag(), blob.getMd5());
            return gcsArtifact;
        }
    }

    private GcsArtifact createGcsArtifact(final String tenant, final String sha1Hash, final String mdMD5Hash16,
            final String contentType, final File file) {
        return new GcsArtifact(gcsStorage, gcsProperties, objectKey(tenant, sha1Hash), sha1Hash,
                new DbArtifactHash(sha1Hash, mdMD5Hash16), file.length(), contentType);
    }

    @Override
    public void deleteBySha1(final String tenant, final String sha1Hash) {
        final String key = objectKey(tenant, sha1Hash);
        LOG.info("Deleting GCS object from bucket {} and key {}", gcsProperties.getBucketName(), key);
        gcsStorage.delete(BlobId.of(gcsProperties.getBucketName(), key));
    }

    @Override
    public AbstractDbArtifact getArtifactBySha1(final String tenant, final String sha1Hash) {
        final String key = objectKey(tenant, sha1Hash);

        LOG.info("Retrieving GCS object from bucket {} and key {}", gcsProperties.getBucketName(), key);
        Blob blob = gcsStorage.get(gcsProperties.getBucketName(), key);
        if (blob == null || !blob.exists()) {
            return null;
        }
        // the MD5Content is stored in the ETag
        return new GcsArtifact(gcsStorage, gcsProperties, key, sha1Hash,
                new DbArtifactHash(sha1Hash,
                        BaseEncoding.base16().lowerCase().encode(BaseEncoding.base64().decode(blob.getMd5()))),
                blob.getSize(), blob.getContentType());

    }

    private boolean exists(final String sha1) {
        Blob blob = gcsStorage.get(gcsProperties.getBucketName(), sha1);
        if (blob == null) {
            return false;
        }
        return blob.exists();
    }

    @Override
    public void deleteByTenant(final String tenant) {
        final String folder = sanitizeTenant(tenant);

        LOG.info("Deleting GCS object folder (tenant) from bucket {} and key {}", gcsProperties.getBucketName(),
                folder);
        Page<Blob> blobs = gcsStorage.list(gcsProperties.getBucketName(), Storage.BlobListOption.currentDirectory(),
                Storage.BlobListOption.prefix(tenant));
        for (Blob blob : blobs.iterateAll()) {
            gcsStorage.delete(blob.getBlobId());
        }
    }

    @Override
    public boolean existsByTenantAndSha1(final String tenant, final String sha1Hash) {
        return exists(objectKey(tenant, sha1Hash));
    }
}
