/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test class for the {@link S3Repository}.
 */

@Feature("Unit Tests - S3 Repository")
@Story("S3 Artifact Repository")
@ExtendWith(MockitoExtension.class)
public class S3RepositoryTest {

    private static final String TENANT = "test_tenant";

    @Mock
    private AmazonS3 amazonS3Mock;

    @Mock
    private S3Object s3ObjectMock;

    @Mock
    private ObjectMetadata s3ObjectMetadataMock;

    @Mock
    private PutObjectResult putObjectResultMock;

    @Captor
    private ArgumentCaptor<ObjectMetadata> objectMetaDataCaptor;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

    private final S3RepositoryProperties s3Properties = new S3RepositoryProperties();
    private S3Repository s3RepositoryUnderTest;

    @BeforeEach
    public void before() {
        amazonS3Mock = mock(AmazonS3.class);
        s3RepositoryUnderTest = new S3Repository(amazonS3Mock, s3Properties);
    }

    @Test
    @Description("Verifies that the amazonS3 client is called to put the object to S3 with the correct inputstream and meta-data")
    public void storeInputStreamCallAmazonS3Client() throws IOException, NoSuchAlgorithmException {
        final byte[] rndBytes = randomBytes();
        final String knownSHA1 = getSha1OfBytes(rndBytes);
        final String knownContentType = "application/octet-stream";

        when(amazonS3Mock.putObject(any(), any(), any(), any())).thenReturn(putObjectResultMock);

        // test
        storeRandomBytes(rndBytes, knownContentType);

        // verify
        Mockito.verify(amazonS3Mock).putObject(eq(s3Properties.getBucketName()),
                eq(TENANT.toUpperCase() + "/" + knownSHA1), inputStreamCaptor.capture(),
                objectMetaDataCaptor.capture());

        final ObjectMetadata recordedObjectMetadata = objectMetaDataCaptor.getValue();
        assertThat(recordedObjectMetadata.getContentType()).isEqualTo(knownContentType);
        assertThat(recordedObjectMetadata.getContentMD5()).isNotNull();
        assertThat(recordedObjectMetadata.getContentLength()).isEqualTo(rndBytes.length);
    }

    @Test
    @Description("Verifies that the amazonS3 client is called to retrieve the correct artifact from S3 and the mapping to the DBArtifact is correct")
    public void getArtifactBySHA1Hash() {
        final String knownSHA1Hash = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
        final long knownContentLength = 100;
        final String knownContentType = "application/octet-stream";

        when(amazonS3Mock.getObject(anyString(), anyString())).thenReturn(s3ObjectMock);
        when(s3ObjectMock.getObjectMetadata()).thenReturn(s3ObjectMetadataMock);
        when(s3ObjectMetadataMock.getContentLength()).thenReturn(knownContentLength);
        when(s3ObjectMetadataMock.getContentType()).thenReturn(knownContentType);

        // test
        final AbstractDbArtifact artifactBySha1 = s3RepositoryUnderTest.getArtifactBySha1(TENANT, knownSHA1Hash);

        // verify
        assertThat(artifactBySha1.getArtifactId()).isEqualTo(knownSHA1Hash);
        assertThat(artifactBySha1.getContentType()).isEqualTo(knownContentType);
        assertThat(artifactBySha1.getSize()).isEqualTo(knownContentLength);
        assertThat(artifactBySha1.getHashes().getSha1()).isEqualTo(knownSHA1Hash);
    }

    @Test
    @Description("Verifies that the amazonS3 client is not called to put the object to S3 due the artifact already exists on S3")
    public void artifactIsNotUploadedIfAlreadyExists() throws NoSuchAlgorithmException, IOException {
        final byte[] rndBytes = randomBytes();
        final String knownSHA1 = getSha1OfBytes(rndBytes);
        final String knownContentType = "application/octet-stream";

        when(amazonS3Mock.putObject(any(), any(), any(), any())).thenReturn(putObjectResultMock);

        // test
        storeRandomBytes(rndBytes, knownContentType);

        // verify
        Mockito.verify(amazonS3Mock, never()).putObject(eq(s3Properties.getBucketName()), eq(knownSHA1),
                inputStreamCaptor.capture(), objectMetaDataCaptor.capture());

    }

    @Test
    @Description("Verifies that null is returned if the given hash does not exists on S3")
    public void getArtifactBySha1ReturnsNullIfFileDoesNotExists() {
        final String knownSHA1Hash = "0815";

        // test
        final AbstractDbArtifact artifactBySha1NotExists = s3RepositoryUnderTest.getArtifactBySha1(TENANT,
                knownSHA1Hash);

        // verify
        assertThat(artifactBySha1NotExists).isNull();
    }

    @Test
    @Description("Verifies that given SHA1 hash is checked and if not match will throw exception")
    public void sha1HashValuesAreNotTheSameThrowsException() throws IOException, NoSuchAlgorithmException {

        final byte[] rndBytes = randomBytes();
        final String knownContentType = "application/octet-stream";
        final String wrongSHA1Hash = "wrong";
        final String knownMD5 = getMd5OfBytes(rndBytes);
        final String knownSHA256 = getSha256OfBytes(rndBytes);

        // test
        try {
            storeRandomBytes(rndBytes, knownContentType, new DbArtifactHash(wrongSHA1Hash, knownMD5, knownSHA256));
            fail("Expected an HashNotMatchException, but didn't throw");
        } catch (final HashNotMatchException e) {
            assertThat(e.getHashFunction()).isEqualTo(HashNotMatchException.SHA1);
        }
    }

    @Test
    @Description("Verifies that given MD5 hash is checked and if not match will throw exception")
    public void md5HashValuesAreNotTheSameThrowsException() throws IOException, NoSuchAlgorithmException {

        final byte[] rndBytes = randomBytes();
        final String knownContentType = "application/octet-stream";
        final String knownSHA1 = getSha1OfBytes(rndBytes);
        final String wrongMD5 = "wrong";
        final String knownSHA256 = getSha256OfBytes(rndBytes);

        // test
        try {
            storeRandomBytes(rndBytes, knownContentType, new DbArtifactHash(knownSHA1, wrongMD5, knownSHA256));
            fail("Expected an HashNotMatchException, but didn't throw");
        } catch (final HashNotMatchException e) {
            assertThat(e.getHashFunction()).isEqualTo(HashNotMatchException.MD5);
        }
    }

    @Test
    @Description("Verifies that given SHA256 hash is checked and if not match will throw exception")
    public void sha256HashValuesAreNotTheSameThrowsException() throws IOException, NoSuchAlgorithmException {

        final byte[] rndBytes = randomBytes();
        final String knownContentType = "application/octet-stream";
        final String knownSHA1 = getSha1OfBytes(rndBytes);
        final String knownMD5 = getMd5OfBytes(rndBytes);
        final String wrongSHA256 = "wrong";

        // test
        try {
            storeRandomBytes(rndBytes, knownContentType, new DbArtifactHash(knownSHA1, knownMD5, wrongSHA256));
            fail("Expected an HashNotMatchException, but didn't throw");
        } catch (final HashNotMatchException e) {
            assertThat(e.getHashFunction()).isEqualTo(HashNotMatchException.SHA256);
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
            s3RepositoryUnderTest.store(TENANT, content, knownFileName, contentType, hashes);
        }
    }

    private static String getSha1OfBytes(final byte[] bytes) throws IOException, NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        return getHashOfBytes(bytes, messageDigest);
    }

    private static String getMd5OfBytes(final byte[] bytes) throws IOException, NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        return getHashOfBytes(bytes, messageDigest);
    }

    private static String getSha256OfBytes(final byte[] bytes) throws IOException, NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return getHashOfBytes(bytes, messageDigest);
    }

    private static String getHashOfBytes(final byte[] bytes, final MessageDigest messageDigest) throws IOException {
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
}
