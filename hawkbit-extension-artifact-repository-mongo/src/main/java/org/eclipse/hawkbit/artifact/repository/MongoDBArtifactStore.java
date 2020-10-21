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
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
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

            return createGridFsArtifact(found);
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

    private void deleteArtifact(final GridFSFile file) {
        if (file != null) {
            try {
                gridFs.delete(new Query().addCriteria(Criteria.where(ID).is(file.getId())));
            } catch (final MongoClientException e) {
                throw new ArtifactStoreException(e.getMessage(), e);
            }
        }
    }

    @Override
    protected AbstractDbArtifact store(final String tenant, final DbArtifactHash base16Hashes, final String contentType,
            final String tempFile) throws IOException {

        final GridFSFile result = gridFs.findOne(new Query().addCriteria(
                Criteria.where(FILENAME).is(base16Hashes.getSha1()).and(TENANT_QUERY).is(sanitizeTenant(tenant))));

        if (result == null) {
            try {
                final GridFSFile temp = loadTempFile(tempFile);

                final Document metadata = new Document();
                metadata.put(SHA1, base16Hashes.getSha1());
                metadata.put(TENANT, tenant);
                metadata.put(FILENAME, base16Hashes.getSha1());
                metadata.put(CONTENT_TYPE, contentType);

                final GridFsResource resource = gridFs.getResource(temp);
                final ObjectId id = gridFs.store(resource.getInputStream(), base16Hashes.getSha1(), contentType, metadata);
                final GridFSFile file = gridFs.findOne(new Query().addCriteria(Criteria.where(ID).is(id)));

                return createGridFsArtifact(file, contentType, base16Hashes);

            } catch (final MongoClientException e) {
                throw new ArtifactStoreException(e.getMessage(), e);
            }
        }

        return createGridFsArtifact(result, contentType, base16Hashes);
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
     * Maps a single {@link GridFSFile} to a {@link GridFsArtifact}.
     *
     * @param file
     *            the {@link GridFSFile} object.
     * 
     * @return a mapped artifact from the given file
     */
    private GridFsArtifact createGridFsArtifact(final GridFSFile file) {
        if (file == null) {
            return null;
        }
        return createGridFsArtifact(file, getContentType(file),
                new DbArtifactHash(file.getFilename(), file.getMD5(), null));
    }

    /**
     * Maps a single {@link GridFSFile} to {@link GridFsArtifact}.
     *
     * @param file
     *            the {@link GridFSFile} object.
     * @param contentType
     *            the content type of the artifact
     * @param hashes
     *            the {@link DbArtifactHash} object of the artifact
     * @return a mapped artifact from the given file
     */
    private GridFsArtifact createGridFsArtifact(final GridFSFile file, final String contentType,
            final DbArtifactHash hashes) {
        if (file == null) {
            return null;
        }
        return new GridFsArtifact(file.getId().toString(), hashes, file.getLength(), contentType, () -> {
            try {
                return gridFs.getResource(file).getInputStream();
            } catch (final IllegalStateException | IOException e) {
                throw new ArtifactStoreException(e.getMessage(), e);
            }
        });
    }

    @SuppressWarnings("squid:S2589")
    // False positive: file.getMetadata() can return null
    private static final String getContentType(final GridFSFile file) {
        final Document metadata = file.getMetadata();
        String contentType = null;
        if (metadata != null) {
            contentType = metadata.getString(CONTENT_TYPE);
        }
        if (contentType == null) {
            try {
                contentType = file.getContentType();
            } catch (final MongoGridFSException e) {
                throw new ArtifactStoreException("Could not determine content type for file " + file.getId(), e);
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

    @Override
    public boolean existsByTenantAndSha1(final String tenant, final String sha1Hash) {
        final GridFSFile artifact = gridFs.findOne(new Query()
                .addCriteria(Criteria.where(FILENAME).is(sha1Hash).and(TENANT_QUERY).is(sanitizeTenant(tenant))));

        return artifact != null;
    }
}
