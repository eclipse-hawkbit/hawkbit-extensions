# hawkBit cluster with [Azure Kubernetes Service (AKS)](https://azure.microsoft.com/en-us/services/kubernetes-service/)

Setup an Azure resource group including [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/), [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) account for hawkBit's repository and [Azure Event Hubs for Apache Kafka](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview) for inner cluster communication.

```bash
# Your ACR registry name
export acr_registry_name=yourACr
export acr_login_server=yourACr.azurecr.io
export acr_resourcegroupname=yourACrsGroup

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
# The AKS IP name
export aks_ip_name=hawkbit-ip-$RANDOM

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
# Grant access to your ACR registry
export service_principal_access_registry=`az ad sp create-for-rbac --skip-assignment --output tsv`
export app_id_access_registry=`echo $service_principal_access_registry|cut -f1 -d ' '`
export password_access_registry=`echo $service_principal_access_registry|cut -f4 -d ' '`
export acr_id_access_registry=`az acr show --resource-group $acr_resourcegroupname --name $acr_registry_name --query "id" --output tsv`

# Wait until service principal is available
sleep 60
az role assignment create --assignee $app_id_access_registry --scope $acr_id_access_registry --role Reader

# Create cluster and get credentials for kubectl
az aks create --resource-group $resourcegroupname --name $aks_cluster_name --node-count 2 --enable-addons monitoring --generate-ssh-keys --service-principal $app_id_access_registry --client-secret $password_access_registry

# Get credentials and configures the Kubernetes CLI to use them.
az aks get-credentials --resource-group $resourcegroupname --name $aks_cluster_name

export node_resource_group=`az aks show --resource-group $resourcegroupname --name $aks_cluster_name --query nodeResourceGroup -o tsv`

# Get static public IP with DNS name
az network public-ip create --resource-group $node_resource_group --name $aks_ip_name --allocation-method static --dns-name $app_name
export public_ip_address=`az network public-ip show --resource-group $node_resource_group --name $aks_ip_name --query ipAddress -o tsv`
export public_fqdn=`az network public-ip show --resource-group $node_resource_group --name $aks_ip_name --query dnsSettings.fqdn -o tsv`

# Store your secrets
export db_url=jdbc:sqlserver://"$db_servername".database.windows.net:1433\;database="$db_name"\;user="$db_adminlogin"@"$db_servername"\;password="$db_password"\;encrypt=true\;trustServerCertificate=false\;hostNameInCertificate=*.database.windows.net\;loginTimeout=30\; > secrets.env
export db_username="$db_adminlogin"@"$db_servername" >> secrets.env
export storage_url=DefaultEndpointsProtocol=https\;AccountName="$storage_name"\;AccountKey="$storage_access_key"\;EndpointSuffix=core.windows.net >> secrets.env
export eventhubs_host="$events_hubs_namespace_name".servicebus.windows.net >> secrets.env

echo db_url="$db_url" > secrets.env
echo db_username="$db_username" >> secrets.env
echo db_password="$db_password" >> secrets.env
echo storage_url="$storage_url" >> secrets.env
echo eventhubs_host="$eventhubs_host" >> secrets.env
echo event_hubs_access_key="$event_hubs_access_key" >> secrets.env

kubectl create namespace hawkbit
kubectl create secret generic hawkbit-infra-secrets --from-env-file=secrets.env --namespace hawkbit
```

Next deploy helm on your cluster. For this follow the guide [here](https://docs.microsoft.com/en-us/azure/aks/kubernetes-helm).

Deploy Nginx as Kubernetes Ingress controller.

```bash
helm install stable/nginx-ingress \
    --namespace hawkbit \
    --set controller.service.loadBalancerIP=$public_ip_address \
    --set controller.replicaCount=2 \
    --name hawkbit-ingress
```

Deploy cert manager for [Let's Encrypt](https://letsencrypt.org) certificates.

```bash
helm install stable/cert-manager \
    --namespace hawkbit \
    --name hawkbit-cert-manager \
    --set ingressShim.defaultIssuerName=letsencrypt-prod \
    --set ingressShim.defaultIssuerKind=ClusterIssuer \
    --version v0.5.2
```

Deploy hawkbit.

```bash
cd helm
helm install ./hawkbit/ \
    --name hawkbit \
    --namespace hawkbit \
    --set image.repository=$acr_login_server/hawkbit-update-server-azure \
    --set ingress.hosts={$public_fqdn}
```

Now check your deployment and identify the public IP address (might take a moment).

```bash
> kubectl get services -n hawkbit
NAME                                            TYPE           CLUSTER-IP    EXTERNAL-IP   PORT(S)                      AGE
hawkbit                                         LoadBalancer   10.0.185.59   <pending>     8080:30324/TCP               14s
hawkbit-ingress-nginx-ingress-controller        LoadBalancer   10.0.75.142   52.174.35.7   80:31811/TCP,443:31615/TCP   45m
hawkbit-ingress-nginx-ingress-default-backend   ClusterIP      10.0.65.126   <none>        80/TCP                       45m


> kubectl get pods -n hawkbit
NAME                                                            READY   STATUS    RESTARTS   AGE
hawkbit-5ddf667cc8-hdlj2                                        1/1     Running   0          62s
hawkbit-5ddf667cc8-m9szw                                        1/1     Running   0          62s
hawkbit-ingress-nginx-ingress-controller-57658b4744-9gwmh       1/1     Running   0          46m
hawkbit-ingress-nginx-ingress-controller-57658b4744-nhmnn       1/1     Running   0          46m
hawkbit-ingress-nginx-ingress-default-backend-7c5b8cf46-kxg8p   1/1     Running   0          46m
```

Now you can open the management UI with the DNS name. However, it will take some time until the application is booted up and the certificates are issued.

In case it does not open up you can take a look at the logs using `kubectl logs`.
