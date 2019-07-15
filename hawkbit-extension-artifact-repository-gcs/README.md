# Eclipse hawkBit - Artifact Repository Google GCS
HawkBit Artifact Repository is a library for storing binary artifacts and metadata into the Goolge GCS service.


## Using Artifact Repository GCS Extension
The module contains a spring-boot autoconfiguration for easily integration into spring-boot projects.
For using this extension in the hawkbit-example-application you just need to add the maven dependency.
```
<dependency>
  <groupId>org.eclipse.hawkbit</groupId>
  <artifactId>hawkbit-extension-artifact-repository-gcs</artifactId>
  <version>${project.version}</version>
</dependency>
```

## Configuration of the GCS Extension

#### Bucket
All files are stored in a bucket configured via property `org.eclipse.hawkbit.repository.gcs.bucketName` (see GcsRepositoryProperties).
The name of the object stored in the GCS bucket is the SHA1-hash of the binary file.

#### GCS Credentials
The extension is using the default gcloud properties which looks for credentials in this order:

1. Environment variable (GOOGLE_APPLICATION_CREDENTIALS)
2. to override the location you can set `org.eclipse.hawkbit.repository.gcs.credentialsLocation` and `org.eclipse.hawkbit.repository.gcs.projectId`

For more information check the [Gcloud credentials guide](https://cloud.google.com/docs/authentication/getting-started).
