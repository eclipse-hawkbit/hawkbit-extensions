# hawkBit cluster with [Azure Kubernetes Service (AKS)](https://azure.microsoft.com/en-us/services/kubernetes-service/)

Setup an Azure resource group including [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/), [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) account for hawkBit's repository and [Azure Event Hubs for Apache Kafka](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview) for inner cluster communication.

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
# The event hubs namespace name
export events_hubs_namespace_name=hawkbit-ns
# The AKS cluster name
export aks_cluster_name=hawkbit-cluster-$RANDOM

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

# Now create a an Event Hubs namespace with Kafka support
az eventhubs namespace create --name $events_hubs_namespace_name --resource-group $resourcegroupname -l $location --enable-kafka

# Get namespace connection string
export event_hubs_access_key=`az eventhubs namespace authorization-rule keys list --resource-group $resourcegroupname --namespace-name $events_hubs_namespace_name --name RootManageSharedAccessKey --output=tsv|cut -f4`
```

Now create your Kubernetes cluster.

```bash
# First grant access to your ACR registry
export service_principal_access_registry=`az ad sp create-for-rbac --skip-assignment --output tsv`
export app_id_access_registry=`echo $service_principal_access_registry|cut -f1 -d ' '`
export password_access_registry=`echo $service_principal_access_registry|cut -f4 -d ' '`
export acr_id_access_registry=`az acr show --resource-group kaisGroup --name <YourAcrRegistry> --query "id" --output tsv`

az role assignment create --assignee $app_id_access_registry --scope $acr_id_access_registry --role Reader

# Create cluster and get credentials for kubectl
az aks create --resource-group $resourcegroupname --name $aks_cluster_name --node-count 1 --enable-addons monitoring --generate-ssh-keys --service-principal $app_id_access_registry --client-secret $password_access_registry

# Get credentials and configures the Kubernetes CLI to use them.
az aks get-credentials --resource-group $resourcegroupname --name $aks_cluster_name

# Store your secrets
export ho db_url=jdbc:sqlserver://"$db_servername".database.windows.net:1433\;database="$db_name"\;user="$db_adminlogin"@"$db_servername"\;password="$db_password"\;encrypt=true\;trustServerCertificate=false\;hostNameInCertificate=*.database.windows.net\;loginTimeout=30\; > secrets.env
export db_username="$db_adminlogin"@"$db_servername" >> secrets.env
export storage_url=DefaultEndpointsProtocol=https\;AccountName="$storage_name"\;AccountKey="$storage_access_key"\;EndpointSuffix=core.windows.net >> secrets.env
export eventhubs_host="$events_hubs_namespace_name".servicebus.windows.net >> secrets.env

echo db_url="$db_url" > secrets.env
echo db_username="$db_username" >> secrets.env
echo db_password="$db_password" >> secrets.env
echo storage_url="$storage_url" >> secrets.env
echo eventhubs_host="$eventhubs_host" >> secrets.env
echo event_hubs_access_key=$event_hubs_access_key >> secrets.env

kubectl create secret generic hawkbit-infra-secrets --from-env-file=secrets.env

# Deploy containers and service
kubectl apply -f azure-hawkbit-aks.yaml
```

Now check your deployment and identify the public IP address (might take a moment).

```bash
> kubectl get services
NAME                    TYPE           CLUSTER-IP   EXTERNAL-IP     PORT(S)          AGE
hawkbit-update-server   LoadBalancer   10.0.30.89   51.145.128.61   8080:31066/TCP   8m
kubernetes              ClusterIP      10.0.0.1     <none>          443/TCP          34m


> kubectl get pods
NAME                                     READY     STATUS    RESTARTS   AGE
hawkbit-update-server-68f6b7bf67-4sdpk   1/1       Running   0          8m
hawkbit-update-server-68f6b7bf67-hrw2z   1/1       Running   0          8m
```

Now you can open the management UI with the IP address

In case it does not open up you can take a look at the logs using `kubectl logs`.

Obviously this simple example if far away for productive use. Next steps could be:

- [Deploy an HTTPS ingress controller](https://docs.microsoft.com/en-us/azure/aks/ingress)
- Split hawkBit into multiple apps/services (e.g. one each for DDI, Management API and Management UI)
