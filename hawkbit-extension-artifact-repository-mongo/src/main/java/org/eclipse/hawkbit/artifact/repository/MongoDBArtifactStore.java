/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.validation.annotation.Validated;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientException;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

/**
 * The file management based on MongoDb GridFS.
 *
 */
@Validated
public class MongoDBArtifactStore extends AbstractArtifactRepository {
    /**
     * The mongoDB field which holds the filename of the file to download.
     * hawkBit update-server uses the SHA hash as a filename and lookup in the
     * mongoDB.
     */
    private static final String FILENAME = "filename";

    /**
     * The mongoDB field which holds the tenant of the file to download.
     */
    private static final String TENANT = "tenant";

    /**
     * Query by {@link TenantAware} field.
     */
    private static final String TENANT_QUERY = "metadata." + TENANT;

    /**
     * The mongoDB field which holds the SHA1 hash, stored in the meta data
     * object.
     */
    private static final String SHA1 = "sha1";

    private static final String ID = "_id";

    private final GridFsOperations gridFs;

    MongoDBArtifactStore(final GridFsOperations gridFs) {
        this.gridFs = gridFs;
    }

    /**
     * Retrieves a {@link GridFSDBFile} from the store by it's SHA1 hash.
     *
     * @param sha1Hash
     *            the sha1-hash of the file to lookup.
     * 
     * @return The DbArtifact object or {@code null} if no file exists.
     */
    @Override
    public AbstractDbArtifact getArtifactBySha1(final String tenant, final String sha1Hash) {

        GridFSDBFile found = gridFs.findOne(new Query()
                .addCriteria(Criteria.where(FILENAME).is(sha1Hash).and(TENANT_QUERY).is(sanitizeTenant(tenant))));

        // fallback pre-multi-tenancy
        if (found == null) {
            found = gridFs.findOne(
                    new Query().addCriteria(Criteria.where(FILENAME).is(sha1Hash).and(TENANT_QUERY).exists(false)));
        }

        return map(found);
    }

    @Override
    public void deleteBySha1(final String tenant, final String sha1Hash) {
        try {
            deleteArtifact(gridFs.findOne(new Query()
                    .addCriteria(Criteria.where(FILENAME).is(sha1Hash).and(TENANT_QUERY).is(sanitizeTenant(tenant)))));
        } catch (final MongoException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }
    }

    private void deleteArtifact(final GridFSDBFile dbFile) {
        if (dbFile != null) {
            try {
                gridFs.delete(new Query().addCriteria(Criteria.where(ID).is(dbFile.getId())));
            } catch (final MongoClientException e) {
                throw new ArtifactStoreException(e.getMessage(), e);
            }
        }
    }

    @Override
    protected AbstractDbArtifact store(final String tenant, final String sha1Hash16, final String mdMD5Hash16,
            final String contentType, final File file) throws IOException {

        // upload if it does not exist already, check if file exists, not
        // tenant specific.
        final GridFSDBFile result = gridFs
                .findOne(new Query().addCriteria(Criteria.where(FILENAME).is(sha1Hash16).and(TENANT_QUERY).is(tenant)));
        if (result == null) {
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                final BasicDBObject metadata = new BasicDBObject();
                metadata.put(SHA1, sha1Hash16);
                metadata.put(TENANT, tenant);
                return map(gridFs.store(inputStream, sha1Hash16, contentType, metadata));
            }
        }

        return map(result);
    }

    /**
     * Maps a single {@link GridFSFile} to {@link AbstractDbArtifact}.
     *
     * @param tenant
     *            the tenant
     * @param dbFile
     *            the mongoDB gridFs file.
     * @return a mapped artifact from the given dbFile
     */
    private static GridFsArtifact map(final GridFSFile fsFile) {
        if (fsFile == null) {
            return null;
        }

        return new GridFsArtifact(fsFile);
    }

    @Override
    public void deleteByTenant(final String tenant) {
        try {
            gridFs.delete(new Query().addCriteria(Criteria.where(TENANT_QUERY).is(sanitizeTenant(tenant))));
        } catch (final MongoClientException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }
    }
}
