/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

/**
 * An exception that is thrown as soon as an S3 object could not be found in a S3 bucket.
 */
public class S3ArtifactNotFoundException extends RuntimeException {

    private final String bucket;
    private final String key;

    /**
     * Constructor with individual error message and information about the searched
     * artifact.
     *
     * @param message
     *            use an individual error message here.
     *
     * @param bucket
     *            the bucket of the searched artifact.
     * @param key
     *            the key of the searched artifact (mostly kind of
     *            'tenant/sha1hash').
     */
    public S3ArtifactNotFoundException(final String message, final String bucket, final String key) {
        super(message);
        this.bucket = bucket;
        this.key = key;
    }

    /**
     * Constructor with individual error message with a cause and information about
     * the searched artifact.
     *
     * @param message
     *            use an individual error message here.
     *
     * @param cause
     *            the cause of the exception.
     * @param bucket
     *            the bucket of the searched artifact.
     * @param key
     *            the key of the searched artifact (mostly kind of
     *            'tenant/sha1hash').
     */
    public S3ArtifactNotFoundException(final String message, final Throwable cause, final String bucket,
            final String key) {
        super(message, cause);
        this.bucket = bucket;
        this.key = key;
    }

    /**
     * Constructor with a cause and information about the searched artifact.
     * 
     * @param cause
     *            the cause of the exception.
     * @param bucket
     *            the bucket of the searched artifact.
     * @param key
     *            the key of the searched artifact (mostly kind of
     *            'tenant/sha1hash').
     */
    public S3ArtifactNotFoundException(final Throwable cause, final String bucket, final String key) {
        super(cause);
        this.bucket = bucket;
        this.key = key;
    }

    /**
     * @return key (mostly kind of 'tenant/sha1hash').
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the bucket name
     */
    public String getBucket() {
        return bucket;
    }
}
