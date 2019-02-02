/**
 * Copyright (c) 2018 Microsoft and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Azure Storage configuration properties for the artifact repository
 * implementation.
 */
@ConfigurationProperties("org.eclipse.hawkbit.repository.azure")
public class AzureStorageRepositoryProperties {

    @NotEmpty
    private String containerName = "artifactrepository";

    @Min(1)
    private int concurrentRequestCount = 8;

    @NotEmpty
    private String connectionString;

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(final String containerName) {
        this.containerName = containerName;
    }

    public int getConcurrentRequestCount() {
        return concurrentRequestCount;
    }

    public void setConcurrentRequestCount(final int concurrentRequestCount) {
        this.concurrentRequestCount = concurrentRequestCount;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(final String connectionString) {
        this.connectionString = connectionString;
    }

}
