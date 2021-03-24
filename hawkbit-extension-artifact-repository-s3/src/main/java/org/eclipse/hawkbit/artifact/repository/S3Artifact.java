/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.io.BaseEncoding;
import org.apache.http.client.methods.HttpRequestBase;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.amazonaws.services.s3.AmazonS3;

/**
 * An {@link AbstractDbArtifact} implementation which retrieves the
 * {@link InputStream} from the {@link AmazonS3} client.
 */
public final class S3Artifact extends AbstractDbArtifact {

    private static final Logger LOG = LoggerFactory.getLogger(S3Artifact.class);

    private final AmazonS3 amazonS3;
    private final S3RepositoryProperties s3Properties;
    private final String key;
    private S3Object s3Object;
    private WrappedS3InputStream s3InputStream;

    private S3Artifact(final S3Object s3Object, final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties,
            final String key, final String artifactId, final DbArtifactHash hashes, final Long size,
            final String contentType) {
        this(amazonS3, s3Properties, key, artifactId, hashes, size, contentType);
        this.s3Object = s3Object;
        this.s3InputStream = WrappedS3InputStream.wrap(s3Object.getObjectContent());
    }

    private S3Artifact(final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties, final String key,
            final String artifactId, final DbArtifactHash hashes, final Long size, final String contentType) {
        super(artifactId, hashes, size, contentType);
        Assert.notNull(amazonS3, "S3 cannot be null");
        Assert.notNull(s3Properties, "Properties cannot be null");
        Assert.notNull(key, "Key cannot be null");
        this.amazonS3 = amazonS3;
        this.s3Properties = s3Properties;
        this.key = key;
    }

    /**
     * Get an S3Artifact for an already existing binary in the repository based on
     * the given key.
     *
     * @param amazonS3
     *            connection to the AmazonS3
     * @param s3Properties
     *            used to retrieve the bucket name
     * @param key
     *            of the artifact
     * @param artifactId
     *            sha1Hash to create the {@link DbArtifactHash}
     * @return an instance of {@link S3Artifact}
     * @throws S3ArtifactNotFoundException
     *             in case that no artifact could be found for the given values
     */
    public static S3Artifact get(final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties, final String key,
            final String artifactId) {
        final S3Object s3Object = getS3ObjectOrThrowException(amazonS3, s3Properties.getBucketName(), key);

        final ObjectMetadata objectMetadata = s3Object.getObjectMetadata();
        final DbArtifactHash artifactHash = createArtifactHash(artifactId, objectMetadata);
        return new S3Artifact(s3Object, amazonS3, s3Properties, key, artifactId, artifactHash,
                objectMetadata.getContentLength(), objectMetadata.getContentType());
    }

    /**
     * Create a new instance of {@link S3Artifact}. In this case it is not checked
     * if an artifact with the given values exists. The S3 object is empty.
     *
     * @param amazonS3
     *            connection to the AmazonS3
     * @param s3Properties
     *            used to retrieve the bucket name
     * @param key
     *            of the artifact
     * @param hashes
     *            instance of {@link DbArtifactHash}
     * @param size
     *            of the artifact
     * @param contentType
     *            of the artifact
     * @return an instance of {@link S3Artifact} with an empty {@link S3Object}
     */
    public static S3Artifact create(final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties,
            final String key, final DbArtifactHash hashes, final Long size, final String contentType) {
        return new S3Artifact(amazonS3, s3Properties, key, hashes.getSha1(), hashes, size, contentType);
    }

    /**
     * Verify if the {@link S3Object} exists
     *
     * @return result of {@link AmazonS3}#doesObjectExist
     */
    public boolean exists() {
        return amazonS3.doesObjectExist(s3Properties.getBucketName(), key);
    }

    @Override
    public String toString() {
        return "S3Artifact [key=" + key + ", getArtifactId()=" + getArtifactId() + ", getHashes()=" + getHashes()
                + ", getSize()=" + getSize() + ", getContentType()=" + getContentType() + "]";
    }

    @Override
    public InputStream getFileInputStream() {
        LOG.debug("Get file input stream for s3 object with key {}", key);
        refreshS3ObjectIfNeeded();
        return s3InputStream;
    }
    
    private void refreshS3ObjectIfNeeded() {
        if (s3Object == null || s3InputStream == null) {
            LOG.info("Initialize S3Object in bucket {} with key {}", s3Properties.getBucketName(), key);
            s3Object = amazonS3.getObject(s3Properties.getBucketName(), key);
            s3InputStream = WrappedS3InputStream.wrap(s3Object.getObjectContent());
        }
    }

    private static S3Object getS3ObjectOrThrowException(AmazonS3 amazonS3, String bucketName, String key) {
        final S3Object s3Object = amazonS3.getObject(bucketName, key);
        if (s3Object == null) {
            throw new S3ArtifactNotFoundException("Cannot find s3 object by given arguments.", bucketName, key);
        }
        return s3Object;
    }

    private static DbArtifactHash createArtifactHash(final String artifactId, ObjectMetadata metadata) {
        return new DbArtifactHash(artifactId, BaseEncoding.base16().lowerCase()
                .encode(Base64.getDecoder().decode(sanitizeEtag(metadata.getETag()))), null);
    }

    private static String sanitizeEtag(final String etag) {
        // base64 alphabet consist of alphanumeric characters and + / = (see RFC
        // 4648)
        return etag.trim().replaceAll("[^A-Za-z0-9+/=]", "");
    }

    /**
     * Wrapper to abort the http request of the S3 input stream before closing it
     */
    static final class WrappedS3InputStream extends S3ObjectInputStream {

        /**
         * Constructor
         */
        private WrappedS3InputStream(InputStream in, HttpRequestBase httpRequest) {
            super(in, httpRequest);
        }

        /**
         * Wrap an input stream of type {@link S3ObjectInputStream} to abort a
         * connection before closing the stream
         * 
         * @param inputStream
         *            the {@link S3ObjectInputStream}
         * @return an instance of {@link WrappedS3InputStream}
         */
        public static WrappedS3InputStream wrap(final S3ObjectInputStream inputStream) {
            return new WrappedS3InputStream(inputStream, inputStream.getHttpRequest());
        }

        @Override
        public void close() throws IOException {
            super.abort();
            super.close();
        }
    }
}
