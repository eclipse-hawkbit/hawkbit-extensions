/**
 * Copyright (c) 2018 Microsoft and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.microsoft.azure.storage.CloudStorageAccount;

/**
 * The Spring auto-configuration to register the necessary beans for the Azure
 * Storage artifact repository implementation.
 */
@Configuration
@EnableConfigurationProperties(AzureStorageRepositoryProperties.class)
@PropertySource("classpath:/hawkbit-azure-storage-defaults.properties")
public class AzureStorageRepositoryAutoConfiguration {

    @Bean
    CloudStorageAccount cloudStorageAccount(final AzureStorageRepositoryProperties properties)
            throws InvalidKeyException, URISyntaxException {
        return CloudStorageAccount.parse(properties.getConnectionString());
    }

    /**
     * @return Azure storage repository based {@link ArtifactRepository}
     *         implementation.
     */
    @Bean
    ArtifactRepository artifactRepository(final CloudStorageAccount storageAccount,
            final AzureStorageRepositoryProperties properties) {
        return new AzureStorageRepository(storageAccount, properties);
    }
}
