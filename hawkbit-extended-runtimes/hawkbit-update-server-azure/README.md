# hawkBit update server with Azure extensions

Reference runtime that packages all the extensions together in a ready to go deployment for Microsoft Azure.

The runtime includes:

- hawkBit DDI, management API and management UI
- [Azure SQL Database](https://azure.microsoft.com/en-us/services/sql-database/) for the metadata repository
- [Azure Storage](https://azure.microsoft.com/en-us/services/storage/) for software artifact binary persistence

## Quick start

### Prerequisites

- An [Azure subscription](https://azure.microsoft.com/en-us/get-started/).
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) installed to setup the infrastructure.
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) and [helm](https://helm.sh/docs/using_helm/#installing-helm) installed to deploy hawkBit into [Azure Kubernetes Service (AKS)](https://docs.microsoft.com/en-us/azure/aks/intro-kubernetes).
- [Docker](https://www.docker.com) installed

### Build hawkBit Azure extension pack

```bash
git clone https://github.com/eclipse/hawkbit-extensions.git
cd hawkbit-extensions
mvn clean install
```

### Package and push the docker image

In this example we expect that you have a [Azure Container Registry (ACR)](https://azure.microsoft.com/en-us/services/container-registry/), are logged in and have the credentials extracted. However, pushing your image to Docker Hub works as well.

```bash
acr_resourcegroupname=YOUR_ACR_RG
acr_registry_name=YOUR_ACR_NAME
acr_login_server=$acr_registry_name.azurecr.io
cd hawkbit-extended-runtimes/hawkbit-update-server-azure
docker build -t $acr_login_server/hawkbit-update-server-azure:latest .
docker push $acr_login_server/hawkbit-update-server-azure:latest
```

### Deploy hawkBit to your Azure environment

First we are going to setup the basic Azure infrastructure: [AKS](https://docs.microsoft.com/en-us/azure/aks/intro-kubernetes), [SQL Database](https://azure.microsoft.com/en-us/services/sql-database/) and [Storage](https://azure.microsoft.com/en-us/services/storage/).

As described [here](https://docs.microsoft.com/en-gb/azure/aks/kubernetes-service-principal) we will create an explicit service principal first. You can add roles to this principal later, e.g. to access a [Azure Container Registry (ACR)](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-intro).

```bash
service_principal=`az ad sp create-for-rbac --name http://hawkBitServicePrincipal --skip-assignment --output tsv`
app_id_principal=`echo $service_principal|cut -f1 -d ' '`
password_principal=`echo $service_principal|cut -f4 -d ' '`
object_id_principal=`az ad sp show --id $app_id_principal --query objectId --output tsv`
acr_id_access_registry=`az acr show --resource-group $acr_resourcegroupname --name $acr_registry_name --query "id" --output tsv`
```

Note: it might take a few seconds until the principal is available to the cluster in the following steps. So maybe time to get up and stretch a bit.

```bash
az role assignment create --assignee $app_id_principal --scope $acr_id_access_registry --role Reader

resourcegroup_name=hawkbit
az group create --name $resourcegroup_name --location "westeurope"
```

With the next command we will use the provided [Azure Resource Manager (ARM)](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-overview) templates to setup the AKS cluster. This might take a while. So maybe time to try out this meditation thing :smile:

```bash
cd deployment
unique_solution_prefix=myprefix
az group deployment create --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --template-file arm/hawkBitInfrastructureDeployment.json --parameters uniqueSolutionPrefix=$unique_solution_prefix servicePrincipalObjectId=$object_id_principal servicePrincipalClientId=$app_id_principal servicePrincipalClientSecret=$password_principal
```

Retrieve secrets from the deployment:

```bash
aks_cluster_name=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.aksClusterName.value -o tsv`
ip_address=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.publicIPAddress.value -o tsv`
public_fqdn=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.publicIPFQDN.value -o tsv`
db_password=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.dbAdministratorLoginPassword.value -o tsv`
db_user=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.dbAdministratorLogin.value -o tsv`
db_url=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.dbUri.value -o tsv`
storage_url=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.storageConnectionString.value -o tsv`
eh_connection=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.ehNamespaceConnectionString.value -o tsv`
eh_ns=`az group deployment show --name hawkBitBasicInfrastructure --resource-group $resourcegroup_name --query properties.outputs.ehNamespaceName.value -o tsv`
```

Now you can set your cluster in `kubectl`.

```bash
az aks get-credentials --resource-group $resourcegroup_name --name $aks_cluster_name
```

Next deploy helm on your cluster. It will take a moment until tiller is booted up. So maybe time again to get up and stretch a bit.

```bash
kubectl apply -f helm/helm-rbac.yaml
helm init --service-account tiller
```

Next we prepare the k8s environment and our chart for deployment.

```bash
k8s_namespace=hawkbitns
kubectl create namespace $k8s_namespace
```

Deploy Nginx as Kubernetes Ingress controller.

```bash
helm upgrade hawkbit-ingress stable/nginx-ingress \
    --namespace $k8s_namespace \
    --set controller.service.loadBalancerIP=$ip_address \
    --set controller.replicaCount=2 \
    --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-resource-group"=$resourcegroup_name \
    --install
```

Deploy cert manager for [Let's Encrypt](https://letsencrypt.org) certificates.

```bash
helm upgrade hawkbit-cert-manager stable/cert-manager \
    --namespace $k8s_namespace \
    --set ingressShim.defaultIssuerName=letsencrypt-prod \
    --set ingressShim.defaultIssuerKind=ClusterIssuer \
    --version v0.5.2 --install
```

Now install hawkBit:

```bash
helm upgrade hawkbit helm/hawkbit \
    --install \
    --wait \
    --namespace $k8s_namespace \
    --set image.repository=$acr_login_server/hawkbit-update-server-azure \
    --set ingress.hosts={$public_fqdn} \
    --set db.password=$db_password \
    --set db.username=$db_user \
    --set db.url=$db_url \
    --set storage.url=$storage_url \
    --set eventHubs.connectionString=$eh_connection \
    --set eventHubs.namespace=$eh_ns \
    --set insights.enabled=false
```

Now check your deployment.

```bash
> kubectl get pods -n $k8s_namespace
NAME                                                            READY   STATUS    RESTARTS   AGE
hawkbit-5ddf667cc8-hdlj2                                        1/1     Running   0          62s
hawkbit-5ddf667cc8-m9szw                                        1/1     Running   0          62s
hawkbit-ingress-nginx-ingress-controller-57658b4744-9gwmh       1/1     Running   0          46m
hawkbit-ingress-nginx-ingress-controller-57658b4744-nhmnn       1/1     Running   0          46m
hawkbit-ingress-nginx-ingress-default-backend-7c5b8cf46-kxg8p   1/1     Running   0          46m
```

Now you can open the management UI with the defined DNS name. However, it will take some time until the application is booted up and the certificates are issued.

In case it does not open up you can take a look at the logs using `kubectl logs`.

### Cleanup your resources

If you no longer need any of the resources you created in this quick start, you can execute the az group delete command to remove the resource group and all resources it contains. This command deletes the running container as well.

```bash
az group delete --name $resourcegroupname
```
