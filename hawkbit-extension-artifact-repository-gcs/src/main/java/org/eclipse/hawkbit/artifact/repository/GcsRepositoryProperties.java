/**
 * Copyright (c) 2019 Rico Pahlisch and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * The google configuration properties for the GCS artifact repository
 * implementation.
 */
@ConfigurationProperties("org.eclipse.hawkbit.repository.gcs")
public class GcsRepositoryProperties {

    private String bucketName = "artifactrepository";
    private Resource credentialsLocation;
    private String projectId;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(final String bucketName) {
        this.bucketName = bucketName;
    }

    public Resource getCredentialsLocation() {
        return credentialsLocation;
    }

    public void setCredentialsLocation(Resource credentialsLocation) {
        this.credentialsLocation = credentialsLocation;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
