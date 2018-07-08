/**
 * Copyright (c) 2018 Microsoft Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The AWS S3 configuration properties for the S3 artifact repository
 * implementation.
 */
@ConfigurationProperties("org.eclipse.hawkbit.repository.azure")
public class AzureStorageRepositoryProperties {

    private String containerName = "artifactrepository";
    private boolean serverSideEncryption = false;

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(final String containerName) {
        this.containerName = containerName;
    }

    public boolean isServerSideEncryption() {
        return serverSideEncryption;
    }

    public void setServerSideEncryption(final boolean serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }
}
