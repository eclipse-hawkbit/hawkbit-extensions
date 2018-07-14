/**
 * Copyright (c) 2018 Microsoft and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import com.google.common.io.BaseEncoding;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.ResultContinuation;
import com.microsoft.azure.storage.ResultSegment;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;


/**
 * An {@link ArtifactRepository} implementation for Azure Storage.
 */
@Validated
public class AzureStorageRepository extends AbstractArtifactRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AzureStorageRepository.class);

    private final CloudBlobClient blobClient;
    private final AzureStorageRepositoryProperties properties;

    public AzureStorageRepository(final CloudStorageAccount storageAccount,
            final AzureStorageRepositoryProperties properties) {
        this.blobClient = storageAccount.createCloudBlobClient();
        this.properties = properties;
    }

    private CloudBlobContainer getContainer() throws URISyntaxException, StorageException {
        final CloudBlobContainer container = blobClient.getContainerReference(properties.getContainerName());
        container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(),
                new OperationContext());

        return container;
    }

    @Override
    protected AbstractDbArtifact store(final String tenant, final String sha1Hash16, final String mdMD5Hash16,
            final String contentType, final String tempFile) throws IOException {

        final File file = new File(tempFile);

        try {
            final CloudBlockBlob blob = getBlob(tenant, sha1Hash16);

            final AzureStorageArtifact artifact = new AzureStorageArtifact(blob, sha1Hash16,
                    new DbArtifactHash(sha1Hash16, mdMD5Hash16), file.length(), contentType);

            LOG.info("Storing file {} with length {} to Azure Storage container {} in directory {}", file.getName(),
                    file.length(), properties.getContainerName(), blob.getParent());

            if (blob.exists()) {
                LOG.debug(
                        "Artifact {} for tenant {} already exists on Azure Storage container {}, don't need to upload twice",
                        sha1Hash16, tenant, properties.getContainerName());
                return artifact;
            }

            // Creating blob and uploading file to it
            blob.getProperties().setContentType(contentType);
            blob.uploadFromFile(tempFile);

            final String md5Base16 = convertToBase16(blob.getProperties().getContentMD5());

            LOG.debug("Artifact {} stored on Azure Storage container {} with  server side Etag {} and MD5 hash {}",
                    sha1Hash16, blob.getContainer().getName(), blob.getProperties().getEtag(), md5Base16);

            return artifact;
        } catch (final URISyntaxException | StorageException e) {
            throw new ArtifactStoreException("Failed to store artifact into Azure storage", e);
        }
    }

    private static String convertToBase16(final String md5Base64) {
        if (md5Base64 == null) {
            return null;
        }

        return BaseEncoding.base16().lowerCase().encode(BaseEncoding.base64().decode(md5Base64));
    }

    private CloudBlockBlob getBlob(final String tenant, final String sha1Hash16)
            throws URISyntaxException, StorageException {
        final CloudBlobContainer container = getContainer();
        final CloudBlobDirectory tenantDirectory = container.getDirectoryReference(sanitizeTenant(tenant));
        return tenantDirectory.getBlockBlobReference(sha1Hash16);
    }

    @Override
    public void deleteBySha1(final String tenant, final String sha1Hash16) {
        try {
            final CloudBlockBlob blob = getBlob(tenant, sha1Hash16);

            LOG.info("Deleting Azure Storage blob from container {} and hash {} for tenant {}",
                    blob.getContainer().getName(), sha1Hash16, tenant);
            blob.delete();

        } catch (final URISyntaxException | StorageException e) {
            throw new ArtifactStoreException("Failed to delete artifact drom Azure storage", e);
        }
    }

    @Override
    public AbstractDbArtifact getArtifactBySha1(final String tenant, final String sha1Hash16) {
        try {
            final CloudBlockBlob blob = getBlob(tenant, sha1Hash16);

            if (blob == null || !blob.exists()) {
                return null;
            }

            LOG.info("Loading Azure Storage blob from container {} and hash {} for tenant {}",
                    blob.getContainer().getName(), sha1Hash16, tenant);

            return new AzureStorageArtifact(blob, sha1Hash16,
                    new DbArtifactHash(sha1Hash16, convertToBase16(blob.getProperties().getContentMD5())),
                    blob.getProperties().getLength(), blob.getProperties().getContentType());
        } catch (final URISyntaxException | StorageException e) {
            throw new ArtifactStoreException("Failed to load artifact into Azure storage", e);
        }
    }

    @Override
    public void deleteByTenant(final String tenant) {

        try {
            final CloudBlobContainer container = getContainer();
            final CloudBlobDirectory tenantDirectory = container.getDirectoryReference(sanitizeTenant(tenant));

            LOG.info("Deleting Azure Storage blob folder (tenant) from container {} for tenant {}", container.getName(),
                    tenant);

            final ResultSegment<ListBlobItem> blobs = tenantDirectory.listBlobsSegmented();
            ResultContinuation token = null;
            do {
                token = blobs.getContinuationToken();
                blobs.getResults().stream().filter(blob -> blob instanceof CloudBlob).forEach(blob -> {
                    try {
                        ((CloudBlob) blob).delete();
                    } catch (final StorageException e) {
                        throw new ArtifactStoreException("Failed to delete tenant directory from Azure storage", e);
                    }
                });
            } while (token != null);

        } catch (final URISyntaxException | StorageException e) {
            throw new ArtifactStoreException("Failed to delete tenant directory from Azure storage", e);
        }

    }

}
