/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.validation.annotation.Validated;

import com.mongodb.MongoClientException;
import com.mongodb.MongoException;
import com.mongodb.MongoGridFSException;
import com.mongodb.client.gridfs.model.GridFSFile;

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

    private static final String CONTENT_TYPE = "contentType";

    private final GridFsOperations gridFs;

    MongoDBArtifactStore(final GridFsOperations gridFs) {
        this.gridFs = gridFs;
    }

    /**
     * Retrieves an artifact from the store by its SHA1 hash.
     *
     * @param sha1Hash
     *            the sha1-hash of the file to lookup.
     * 
     * @return The DbArtifact object or {@code null} if no file exists.
     */
    @Override
    public AbstractDbArtifact getArtifactBySha1(final String tenant, final String sha1Hash) {

        try {

            GridFSFile found = gridFs.findOne(new Query()
                    .addCriteria(Criteria.where(FILENAME).is(sha1Hash).and(TENANT_QUERY).is(sanitizeTenant(tenant))));

            // fallback pre-multi-tenancy
            if (found == null) {
                found = gridFs.findOne(
                        new Query().addCriteria(Criteria.where(FILENAME).is(sha1Hash).and(TENANT_QUERY).exists(false)));
            }

            return map(found);
        } catch (final MongoClientException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }
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

    private void deleteArtifact(final GridFSFile dbFile) {
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
            final String contentType, final String tempFile) throws IOException {

        final GridFSFile result = gridFs.findOne(new Query()
                .addCriteria(Criteria.where(FILENAME).is(sha1Hash16).and(TENANT_QUERY).is(sanitizeTenant(tenant))));

        if (result == null) {
            try {
                final GridFSFile temp = loadTempFile(tempFile);

                final Document metadata = new Document();
                metadata.put(SHA1, sha1Hash16);
                metadata.put(TENANT, tenant);
                metadata.put(FILENAME, sha1Hash16);
                metadata.put(CONTENT_TYPE, contentType);

                final GridFsResource resource = gridFs.getResource(temp);
                final ObjectId id = gridFs.store(resource.getInputStream(), sha1Hash16, contentType, metadata);
                final GridFSFile file = gridFs.findOne(new Query().addCriteria(Criteria.where(ID).is(id)));

                return map(file, contentType);

            } catch (final MongoClientException e) {
                throw new ArtifactStoreException(e.getMessage(), e);
            }
        }

        return map(result, contentType);
    }

    private GridFSFile loadTempFile(final String tempFile) {
        return gridFs.findOne(new Query().addCriteria(Criteria.where(FILENAME).is(getTempFilename(tempFile))));
    }

    @Override
    protected String storeTempFile(final InputStream content) {
        final String fileName = findUnusedTempFileName();

        try {
            gridFs.store(content, getTempFilename(fileName));
        } catch (final MongoClientException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }

        return fileName;
    }

    private String findUnusedTempFileName() {
        String fileName;
        do {
            fileName = UUID.randomUUID().toString();
        } while (loadTempFile(fileName) != null);

        return fileName;
    }

    @Override
    protected void deleteTempFile(final String tempFile) {
        try {
            deleteArtifact(loadTempFile(tempFile));
        } catch (final MongoException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }

    }

    private static String getTempFilename(final String fileName) {
        return "TMP_" + fileName;
    }

    /**
     * Maps a single {@link GridFSFile} to {@link AbstractDbArtifact}.
     *
     * @param dbFile
     *            the mongoDB gridFs file.
     * 
     * @return a mapped artifact from the given dbFile
     */
    private GridFsArtifact map(final GridFSFile dbFile) {
        if (dbFile == null) {
            return null;
        }
        return map(dbFile, getContentType(dbFile));
    }

    /**
     * Maps a single {@link GridFSFile} to {@link AbstractDbArtifact}.
     *
     * @param dbFile
     *            the mongoDB gridFs file.
     * @param contentType
     *            the content type of the artifact
     * @return a mapped artifact from the given dbFile
     */
    private GridFsArtifact map(final GridFSFile dbFile, final String contentType) {
        if (dbFile == null) {
            return null;
        }
        return new GridFsArtifact(dbFile, contentType, () -> {
            try {
                return gridFs.getResource(dbFile).getInputStream();
            } catch (final IllegalStateException | IOException e) {
                throw new ArtifactStoreException(e.getMessage(), e);
            }
        });
    }

    private static final String getContentType(final GridFSFile dbFile) {
        final Document metadata = dbFile.getMetadata();
        String contentType = null;
        if (metadata != null) {
            contentType = metadata.getString(CONTENT_TYPE);
        }
        if (contentType == null) {
            try {
                contentType = dbFile.getContentType();
            } catch (final MongoGridFSException e) {
                throw new ArtifactStoreException("Could not determine content type for file " + dbFile.getId(), e);
            }
        }
        return contentType;
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
