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

### Package the docker image

```bash
docker build -t hawkbit/hawkbit-update-server-azure .
```

### Setup your azure environment (Azure CLI)

Setup an azure resource group including [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/) and [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) account. 

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
export app_name=hawkbit01
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

az storage account create \
    --name $storage_name \
    --resource-group $resourcegroupname \
    --location $location \
    --sku Standard_LRS \
    --kind StorageV2

export storage_access_key=`az storage account keys list --account-name $storage_name --resource-group $resourcegroupname --output=tsv|cut  -f3| head -1`


```

Now run your hawkBit container

```bash
az container create --resource-group $resourcegroupname --name $app_name --image hawkbit/hawkbit-update-server-azure --cpu 1 --memory 1 \
    --dns-name-label $app_name --ports 8080 \
    --environment-variables SPRING_JPA_DATABASE=SQL_SERVER \
    SPRING_DATASOURCE_URL=jdbc:sqlserver://"$db_servername".database.windows.net:1433\;database="$db_name"\;user="$db_adminlogin"@"$db_servername"\;password="$db_password"\;encrypt=true\;trustServerCertificate=false\;hostNameInCertificate=*.database.windows.net\;loginTimeout=30 \
    SPRING_DATASOURCE_USER="$db_adminlogin"@"$db_name" \
    SPRING_DATASOURCE_PASSWORD="$db_password" \
    SPRING_DATASOURCE_DRIVERCLASSNAME=com.microsoft.sqlserver.jdbc.SQLServerDriver \
    AZURE_STORAGE_CONNECTION_STRING="$storage_access_key"
```

## Status and roadmap

| Module | Description | Status | Integrated with runtime |
|---|:---:|:---:|:---:|
| hawkbit-extension-artifact-repository-azure  | Artifact repository based on Azure Storage blobs | :white_check_mark: | :white_check_mark: |