# hawkBit extensions for Microsoft Azure

This module combines a set of extensions for optimized integration of hawkBit update server into Microsoft Azure.

In addition it includes a reference runtime, i.e. hawkbit-update-server-azure, that packages all the extensions together in a ready to go deployment.

The runtime includes:

* hawkBit DDI, management API and management UI
* [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/) for the metadata repositoy
* [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) for software artifact binary persistence

## Quick start

### Build hawkBit azure extension pack

```bash
git clone https://github.com/eclipse/hawkbit-extensions.git
cd hawkbit-extensions
mvn clean install
```

### Package and push the docker image

In this example we expect that you have a [Azure Container Registry (ARC)](https://azure.microsoft.com/en-us/services/container-registry/),  are logged in and have the credentials extracted. However, pushing your image to Docker Hub works as well.

```bash
cd hawkbit-azure-extensions-parent/hawkbit-update-server-azure
docker build -t <YourAcrLoginServer>/hawkbit-update-server-azure:latest .
docker push <YourAcrLoginServer>/hawkbit-update-server-azure:latest
```

### Quick start your azure environment ([Azure CLI](https://shell.azure.com))

Setup an Azure resource group including [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/) and [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) account.

```bash
# The data center and resource name for your resources
export resourcegroupname=hawkBitTestResource
export location=westeurope
# The logical server name: Use a random value or replace with your own value (do not capitalize)
export db_servername=hawkbitsql
# Set an admin login and password for your database
export db_adminlogin=ServerAdmin
export db_password=passwdYouShouldChange-$RANDOM
# The ip address range that you want to allow to access your DB
export db_startip="0.0.0.0"
export db_endip="0.0.0.0"
# The database name
export db_name=hawkBitDb-$RANDOM
# The server DNS name
export app_name=hawkbit-hello-$RANDOM
# The storage account name
export storage_name=hawkbitstorage$RANDOM

# Create a resource group
az group create --name $resourcegroupname --location $location

# Create a logical server
az sql server create --name $db_servername --resource-group $resourcegroupname --location $location \
    --admin-user $db_adminlogin --admin-password $db_password

# Configure a server firewall rule
az sql server firewall-rule create --resource-group $resourcegroupname --server $db_servername \
    -n AllowYourIp --start-ip-address $db_startip --end-ip-address $db_endip

# Create a database
az sql db create --resource-group $resourcegroupname --server $db_servername \
    --name $db_name --service-objective S0

# Create the storage account
az storage account create \
    --name $storage_name \
    --resource-group $resourcegroupname \
    --location $location \
    --sku Standard_LRS \
    --kind StorageV2

# Extract the storage access key
export storage_access_key=`az storage account keys list --account-name $storage_name --resource-group $resourcegroupname --output=tsv|cut  -f3| head -1`
```

Now run your hawkBit container. We are using in this case [Azure Container Instances (ACI)](https://azure.microsoft.com/en-us/services/container-instances/).

```bash
az container create --resource-group $resourcegroupname --name $app_name --image <YourAcrLoginServer>/hawkbit-update-server-azure --registry-login-server <YourAcrLoginServer> --registry-username <YourAcrName> --registry-password <YourAcrPassword> --cpu 1 --memory 1 \
    --dns-name-label $app_name --ports 8080 \
    --environment-variables SPRING_JPA_DATABASE=SQL_SERVER \
    SPRING_DATASOURCE_URL=jdbc:sqlserver://"$db_servername".database.windows.net:1433\;database="$db_name"\;user="$db_adminlogin"@"$db_servername"\;password="$db_password"\;encrypt=true\;trustServerCertificate=false\;hostNameInCertificate=*.database.windows.net\;loginTimeout=30 \
    SPRING_DATASOURCE_USER="$db_adminlogin"@"$db_name" \
    SPRING_DATASOURCE_PASSWORD="$db_password" \
    SPRING_DATASOURCE_DRIVERCLASSNAME=com.microsoft.sqlserver.jdbc.SQLServerDriver \
    AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=https\;AccountName="$storage_name"\;AccountKey="$storage_access_key"\;EndpointSuffix=core.windows.net
```

### Check your work and cleanup your resources

Once the deployment succeeds, display the container's fully qualified domain name (FQDN) with the az container show command:

```bash
az container show --resource-group $resourcegroupname --name $app_name --query ipAddress.fqdn
```

Now, you should be able to open the hawkBit management UI on the returned DNS name on port 8080.

In case it does not open up you can take a look at the logs using:

```bash
az container logs --resource-group $resourcegroupname --name $app_name
```

If you no longer need any of the resources you created in this quick start, you can execute the az group delete command to remove the resource group and all resources it contains. This command deletes the running container as well.

```bash
az group delete --name $resourcegroupname
```

## Status and roadmap

| Module | Description | Status | Integrated with runtime |
|---|:---:|:---:|:---:|
| hawkbit-extension-artifact-repository-azure  | Artifact repository based on Azure Storage blobs | :white_check_mark: | :white_check_mark: |