# hawkBit update server with Azure extensions

Reference runtime that packages all the extensions together in a ready to go deployment for Microsoft Azure.

The runtime includes:

- hawkBit DDI, management API and management UI
- [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/) for the metadata repository
- [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) for software artifact binary persistence

## Quick start

### Build hawkBit Azure extension pack

```bash
git clone https://github.com/eclipse/hawkbit-extensions.git
cd hawkbit-extensions
mvn clean install
```

### Package and push the docker image

In this example we expect that you have a [Azure Container Registry (ACR)](https://azure.microsoft.com/en-us/services/container-registry/), are logged in and have the credentials extracted. However, pushing your image to Docker Hub works as well.

```bash
cd hawkbit-extended-runtimes/hawkbit-update-server-azure
docker build -t <YourAcrLoginServer>/hawkbit-update-server-azure:latest .
docker push <YourAcrLoginServer>/hawkbit-update-server-azure:latest
```

### Quick start your azure environment ([Azure CLI](https://shell.azure.com))

- [hawkBit single node](single_node.md) with Azure Container Instances (ACI)
- [hawkBit cluster](cluster.md) with Azure Kubernetes Service (AKS)

### Cleanup your resources

If you no longer need any of the resources you created in this quick start, you can execute the az group delete command to remove the resource group and all resources it contains. This command deletes the running container as well.

```bash
az group delete --name $resourcegroupname
```
