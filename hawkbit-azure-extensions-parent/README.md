# hawkBit extensions for Microsoft Azure

This module combines a set of extensions for optimized integration of hawkBit update server into Microsoft Azure.

In addition it includes a reference runtime, i.e. hawkbit-update-server-azure, that packages all the extensions together in a ready to go deployment.

## Quick start

### Build hawkBit azure extension pack

```bash
git clone https://github.com/eclipse/hawkbit-extensions.git
cd hawkbit-extensions
mvn clean install
```

### Package the docker image

```bash
docker build -t hawkbit/hawkbit-update-server-azure .
```

### Setup your azure environment (Azure CLI)

Setup an azure resource group including [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/) and [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) account

```bash
TODO
```

## Status and roadmap

| Module | Description | Status | Integrated with runtime |
|---|:---:|:---:|:---:|
| hawkbit-extension-artifact-repository-azure  | Artifact repository based on Azure Storage blobs | :white_check_mark: | :white_check_mark: |
