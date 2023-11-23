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

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.amazonaws.services.s3.model.SSEAlgorithm;

/**
 * The AWS S3 configuration properties for the S3 artifact repository
 * implementation.
 */
@ConfigurationProperties("org.eclipse.hawkbit.repository.s3")
public class S3RepositoryProperties {

    private String bucketName = "artifactrepository";
    private boolean serverSideEncryption = false;
    private String serverSideEncryptionAlgorithm = SSEAlgorithm.AES256.getAlgorithm();

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(final String bucketName) {
        this.bucketName = bucketName;
    }

    public boolean isServerSideEncryption() {
        return serverSideEncryption;
    }

    public void setServerSideEncryption(final boolean serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }

    public String getServerSideEncryptionAlgorithm() {
        return serverSideEncryptionAlgorithm;
    }

    public void setServerSideEncryptionAlgorithm(final String serverSideEncryptionAlgorithm) {
        this.serverSideEncryptionAlgorithm = serverSideEncryptionAlgorithm;
    }
}
