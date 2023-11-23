/**
 * Copyright (c) 2019 Rico Pahlisch and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * The Spring auto-configuration to register the necessary beans for the GCS
 * artifact repository implementation.
 */
@Configuration
@ConditionalOnProperty(prefix = "org.eclipse.hawkbit.artifact.repository.gcs", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GcsRepositoryProperties.class)
public class GcsRepositoryAutoConfiguration {

    /**
     * GSC storage service
     * 
     * @param gcsProperties
     *            properties to initialize GCS storage service
     * @return the {@link Storage} if no other {@link Storage} bean is registered.
     */
    @Bean
    @ConditionalOnMissingBean
    public Storage gcsStorage(final GcsRepositoryProperties gcsProperties) {
        if (gcsProperties.getCredentialsLocation() != null) {
            try {
                Credentials credentials = GoogleCredentials
                        .fromStream(gcsProperties.getCredentialsLocation().getInputStream());
                return StorageOptions.newBuilder().setCredentials(credentials)
                        .setProjectId(gcsProperties.getProjectId()).build().getService();
            } catch (IOException e) {
                throw new GcpInitialisationFailedException(e);
            }
        } else {
            return StorageOptions.getDefaultInstance().getService();
        }
    }

    /**
     * GCS implementation for artifact repository
     * 
     * @param gcsProperties
     *            properties to initialize GCS storage service
     * @return google GCS repository {@link ArtifactRepository} implementation.
     */
    @Bean
    public ArtifactRepository artifactRepository(final GcsRepositoryProperties gcsProperties) {
        return new GcsRepository(gcsStorage(gcsProperties), gcsProperties);
    }
}
