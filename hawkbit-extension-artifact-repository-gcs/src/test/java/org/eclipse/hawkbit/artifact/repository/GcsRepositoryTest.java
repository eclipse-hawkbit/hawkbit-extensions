/**
 * Copyright (c) 2019 Rico Pahlisch and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test class for the {@link GcsRepository}.
 */
@RunWith(MockitoJUnitRunner.class)
@Feature("Unit Tests - GCS Repository")
@Story("GCS Artifact Repository")
public class GcsRepositoryTest {

    private static final String TENANT = "test_tenant";
    private final GcsRepositoryProperties gcpProperties = new GcsRepositoryProperties();
    @Mock
    private Storage gcsStorageMock;
    @Mock
    private Blob gcpObjectMock;
    @Mock
    private Blob putObjectResultMock;
    @Captor
    private ArgumentCaptor<byte[]> inputStreamCaptor;
    @Captor
    private ArgumentCaptor<BlobInfo> blobCaptor;
    private GcsRepository gcsRepositoryUnderTest;

    private static String getSha1OfBytes(final byte[] bytes) throws IOException, NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

        try (InputStream input = new ByteArrayInputStream(bytes);
                OutputStream output = new DigestOutputStream(new ByteArrayOutputStream(), messageDigest)) {
            ByteStreams.copy(input, output);
            return BaseEncoding.base16().lowerCase().encode(messageDigest.digest());
        }
    }

    private static byte[] randomBytes() {
        final byte[] randomBytes = new byte[20];
        final Random ran = new Random();
        ran.nextBytes(randomBytes);
        return randomBytes;
    }

    @Before
    public void before() {
        gcsStorageMock = mock(Storage.class);
        gcsRepositoryUnderTest = new GcsRepository(gcsStorageMock, gcpProperties);
    }

    @Test
    @Description("Verifies that the gcs storage client is called to put the object to GCS with the correct inputstream and meta-data")
    public void storeInputStreamCallGcsStorageClient() throws IOException, NoSuchAlgorithmException {
        final byte[] rndBytes = randomBytes();
        final String knownSHA1 = getSha1OfBytes(rndBytes);
        final String knownContentType = "application/octet-stream";

        when(gcsStorageMock.get(anyString(), anyString())).thenReturn(gcpObjectMock);
        when(gcsStorageMock.create(any(), (byte[]) any())).thenReturn(putObjectResultMock);

        // test
        storeRandomBytes(rndBytes, knownContentType);

        // verify
        Mockito.verify(gcsStorageMock).create(blobCaptor.capture(), inputStreamCaptor.capture());

        final BlobInfo recordedObjectMetadata = blobCaptor.getValue();
        assertThat(recordedObjectMetadata.getContentType()).isEqualTo(knownContentType);
        assertThat(recordedObjectMetadata.getMd5()).isNotNull();
    }

    @Test
    @Description("Verifies that the gcs storage client is called to retrieve the correct artifact from GCS and the mapping to the DBArtifact is correct")
    public void getArtifactBySHA1Hash() {
        final String knownSHA1Hash = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
        final long knownContentLength = 100;
        final String knownContentType = "application/octet-stream";
        final String knownMd5 = "098f6bcd4621d373cade4e832627b4f6";
        final String knownMdBase16 = BaseEncoding.base16().lowerCase().encode(knownMd5.getBytes());
        final String knownMd5Base64 = BaseEncoding.base64().encode(knownMd5.getBytes());

        when(gcsStorageMock.get(anyString(), anyString())).thenReturn(gcpObjectMock);
        when(gcpObjectMock.exists()).thenReturn(true);
        when(gcpObjectMock.getSize()).thenReturn(knownContentLength);
        when(gcpObjectMock.getMd5()).thenReturn(knownMd5Base64);
        when(gcpObjectMock.getContentType()).thenReturn(knownContentType);

        // test
        final AbstractDbArtifact artifactBySha1 = gcsRepositoryUnderTest.getArtifactBySha1(TENANT, knownSHA1Hash);

        // verify
        assertThat(artifactBySha1.getArtifactId()).isEqualTo(knownSHA1Hash);
        assertThat(artifactBySha1.getContentType()).isEqualTo(knownContentType);
        assertThat(artifactBySha1.getSize()).isEqualTo(knownContentLength);
        assertThat(artifactBySha1.getHashes().getSha1()).isEqualTo(knownSHA1Hash);
        assertThat(artifactBySha1.getHashes().getMd5()).isEqualTo(knownMdBase16);
    }

    @Test
    @Description("Verifies that the gcs storage client is not called to put the object to GCS due the artifact already exists on GCS")
    public void artifactIsNotUploadedIfAlreadyExists() throws NoSuchAlgorithmException, IOException {
        final byte[] rndBytes = randomBytes();
        final String knownSHA1 = getSha1OfBytes(rndBytes);
        final String knownContentType = "application/octet-stream";

        when(gcsStorageMock.get(anyString(), anyString())).thenReturn(gcpObjectMock);
        when(gcpObjectMock.exists()).thenReturn(true);

        // test
        storeRandomBytes(rndBytes, knownContentType);

        // verify
        Mockito.verify(gcsStorageMock, never()).create(blobCaptor.capture(), inputStreamCaptor.capture());
    }

    @Test
    @Description("Verifies that null is returned if the given hash does not exists on GCS")
    public void getArtifactBySha1ReturnsNullIfFileDoesNotExists() {
        final String knownSHA1Hash = "0815";

        // test
        final AbstractDbArtifact artifactBySha1NotExists = gcsRepositoryUnderTest.getArtifactBySha1(TENANT,
                knownSHA1Hash);

        // verify
        assertThat(artifactBySha1NotExists).isNull();
    }

    @Test
    @Description("Verifies that given SHA1 hash are checked and if not match will throw exception")
    public void sha1HashValuesAreNotTheSameThrowsException() throws IOException {

        final byte[] rndBytes = randomBytes();
        final String knownContentType = "application/octet-stream";
        final String wrongSHA1Hash = "wrong";
        final String wrongMD5 = "wrong";

        // test
        try {
            storeRandomBytes(rndBytes, knownContentType, new DbArtifactHash(wrongSHA1Hash, wrongMD5));
            fail("Expected an HashNotMatchException, but didn't throw");
        } catch (final HashNotMatchException e) {
            assertThat(e.getHashFunction()).isEqualTo(HashNotMatchException.SHA1);
        }
    }

    @Test
    @Description("Verifies that given MD5 hash are checked and if not match will throw exception")
    public void md5HashValuesAreNotTheSameThrowsException() throws IOException, NoSuchAlgorithmException {

        final byte[] rndBytes = randomBytes();
        final String knownContentType = "application/octet-stream";
        final String knownSHA1 = getSha1OfBytes(rndBytes);
        final String wrongMD5 = "wrong";

        // test
        try {
            storeRandomBytes(rndBytes, knownContentType, new DbArtifactHash(knownSHA1, wrongMD5));
            fail("Expected an HashNotMatchException, but didn't throw");
        } catch (final HashNotMatchException e) {
            assertThat(e.getHashFunction()).isEqualTo(HashNotMatchException.MD5);
        }
    }

    private void storeRandomBytes(final byte[] rndBytes, final String contentType)
            throws IOException, NoSuchAlgorithmException {
        storeRandomBytes(rndBytes, contentType, null);
    }

    private void storeRandomBytes(final byte[] rndBytes, final String contentType, final DbArtifactHash hashes)
            throws IOException {
        final String knownFileName = "randomBytes";
        try (InputStream content = new BufferedInputStream(new ByteArrayInputStream(rndBytes))) {
            gcsRepositoryUnderTest.store(TENANT, content, knownFileName, contentType, hashes);
        }
    }
}
