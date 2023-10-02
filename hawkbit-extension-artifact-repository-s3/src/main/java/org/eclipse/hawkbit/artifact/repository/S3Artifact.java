/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;

/**
 * An {@link AbstractDbArtifact} implementation which retrieves the
 * {@link InputStream} from the {@link AmazonS3} client.
 */
public class S3Artifact extends AbstractDbArtifact {

    private final AmazonS3 amazonS3;
    private final S3RepositoryProperties s3Properties;
    private final String key;

    S3Artifact(final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties, final String key,
            final String artifactId, final DbArtifactHash hashes, final Long size, final String contentType) {
        super(artifactId, hashes, size, contentType);
        Assert.notNull(amazonS3, "S3 cannot be null");
        Assert.notNull(s3Properties, "Properties cannot be null");
        Assert.notNull(key, "Key cannot be null");
        this.amazonS3 = amazonS3;
        this.s3Properties = s3Properties;
        this.key = key;
    }

    @Override
    public InputStream getFileInputStream() {
        var req = new GetObjectRequest(s3Properties.getBucketName(), key);
        var obj = amazonS3.getObject(req);

        return obj.getObjectContent();
    }

    @Override
    public InputStream getFileInputStream(long start, long end) {
        var req = new GetObjectRequest(s3Properties.getBucketName(), key).withRange(start, end);
        var obj = amazonS3.getObject(req);

        return obj.getObjectContent();
    }

    @Override
    public String toString() {
        return "S3Artifact [key=" + key + ", getArtifactId()=" + getArtifactId() + ", getHashes()=" + getHashes()
                + ", getSize()=" + getSize() + ", getContentType()=" + getContentType() + "]";
    }
}
