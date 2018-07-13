/**
 * Copyright (c) 2018 Microsoft Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.InputStream;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.springframework.util.Assert;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

/**
 * An {@link AbstractDbArtifact} implementation which retrieves the
 * {@link InputStream} from the {@link AmazonS3} client.
 */
public class AzureStorageArtifact extends AbstractDbArtifact {

    private final CloudBlockBlob blob;

    AzureStorageArtifact(final CloudBlockBlob blob, final String artifactId, final DbArtifactHash hashes,
            final Long size, final String contentType) {
        super(artifactId, hashes, size, contentType);
        Assert.notNull(blob, "Azure storage blob cannot be null");
        this.blob = blob;
    }

    @Override
    public InputStream getFileInputStream() {
        try {
            return blob.openInputStream();
        } catch (final StorageException e) {
            throw new ArtifactStoreException("Failed to open stream on Azure Storage blob", e);
        }
    }

}
