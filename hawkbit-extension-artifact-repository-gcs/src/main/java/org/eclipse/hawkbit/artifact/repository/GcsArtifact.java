/**
 * Copyright (c) 2019 Rico Pahlisch and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import static java.nio.channels.Channels.newInputStream;

import java.io.InputStream;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.springframework.util.Assert;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;

/**
 * An {@link AbstractDbArtifact} implementation which retrieves the
 * {@link InputStream} from the {@link Storage} client.
 */
public class GcsArtifact extends AbstractDbArtifact {

    private final Storage gcsStorage;
    private final GcsRepositoryProperties gcsProperties;
    private final String key;

    GcsArtifact(final Storage gcsStorage, final GcsRepositoryProperties gcsProperties, final String key,
            final String artifactId, final DbArtifactHash hashes, final Long size, final String contentType) {
        super(artifactId, hashes, size, contentType);
        Assert.notNull(gcsStorage, "GCS cannot be null");
        Assert.notNull(gcsProperties, "Properties cannot be null");
        Assert.notNull(key, "Key cannot be null");
        this.gcsStorage = gcsStorage;
        this.gcsProperties = gcsProperties;
        this.key = key;
    }

    @Override
    public InputStream getFileInputStream() {
        return newInputStream(gcsStorage.reader(BlobId.of(gcsProperties.getBucketName(), key)));
    }

    @Override
    public String toString() {
        return "GcsArtifact [key=" + key + ", getArtifactId()=" + getArtifactId() + ", getHashes()=" + getHashes()
                + ", getSize()=" + getSize() + ", getContentType()=" + getContentType() + "]";
    }
}
