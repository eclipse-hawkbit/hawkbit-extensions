/**
 * Copyright (c) 2018 Microsoft and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Azure Storage configuration properties for the artifact repository
 * implementation.
 */
@ConfigurationProperties("org.eclipse.hawkbit.repository.azure")
public class AzureStorageRepositoryProperties {

    private String containerName = "artifactrepository";

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(final String containerName) {
        this.containerName = containerName;
    }
}
