# hawkBit extensions for Microsoft Azure

This module combines a set of extensions for optimized integration of hawkBit update server into Microsoft Azure.

In addition we provide a pre-packaged [runtime including the Azure extensions](/hawkbit-extended-runtimes/hawkbit-update-server-azure).

## Status and roadmap

| Maven Module                                |                                                             Description                                                             |       Status       | Integrated with runtime |
| ------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------: | :----------------: | :---------------------: |
| hawkbit-extension-artifact-repository-azure | Artifact repository based on [Azure Blob storage](https://docs.microsoft.com/en-us/azure/storage/blobs/storage-blobs-introduction). | :white_check_mark: |   :white_check_mark:    |
|                                             |             Cluster messaging support based on [Azure Event Hubs](https://docs.microsoft.com/en-us/azure/event-hubs/).              | :white_check_mark: |   :white_check_mark:    |
|                                             |        Kubernetes deployment support based on [Azure Kubernetes Service (AKS)](https://docs.microsoft.com/en-us/azure/aks/).        | :white_check_mark: |   :white_check_mark:    |
