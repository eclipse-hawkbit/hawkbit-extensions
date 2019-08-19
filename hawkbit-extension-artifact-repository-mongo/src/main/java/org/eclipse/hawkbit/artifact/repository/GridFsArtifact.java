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
import java.util.function.Supplier;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;

import com.mongodb.gridfs.GridFSDBFile;

/**
 * A wrapper object for the {@link AbstractDbArtifact} object which returns the
 * {@link InputStream} directly from {@link GridFSDBFile#getInputStream()} which
 * retrieves when calling {@link #getFileInputStream()} always a new
 * {@link InputStream} and not the same.
 */
public class GridFsArtifact extends AbstractDbArtifact {

    private final Supplier<InputStream> inputStreamSupplier;

    /**
     * @param artifactId
     *            id of the artifact
     * @param hashes
     *            base16 hashes of the artifact
     * @param size
     *            size of the artifact
     * @param contentType
     *            content type of the artifact
     * @param inputStreamSupplier
     *            the supplier of the input stream
     */
    public GridFsArtifact(final String artifactId, final DbArtifactHash hashes, final long size,
            final String contentType, final Supplier<InputStream> inputStreamSupplier) {
        super(artifactId, hashes, size, contentType);
        this.inputStreamSupplier = inputStreamSupplier;
    }

    @Override
    public InputStream getFileInputStream() {
        return inputStreamSupplier.get();
    }

}
