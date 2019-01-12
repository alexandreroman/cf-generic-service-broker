/*
 * Copyright (c) 2019 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.genericservicebroker

import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException
import org.springframework.cloud.servicebroker.model.binding.*
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.Plan
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition
import org.springframework.cloud.servicebroker.model.instance.*
import org.springframework.cloud.servicebroker.service.CatalogService
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService
import org.springframework.cloud.servicebroker.service.ServiceInstanceService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

/**
 * Custom [CatalogService] implementation.
 */
@Component
class GenericCatalogService(private val props: ServiceProperties) : CatalogService {
    override fun getCatalog() =
            Catalog.builder().serviceDefinitions(getServiceDefinition(SERVICE_DEF_ID)).build()

    override fun getServiceDefinition(serviceId: String?): ServiceDefinition? {
        var svc = ServiceDefinition.builder()
                .id(SERVICE_DEF_ID)
                .bindable(true)
                .name(props.id)
                .description(props.description)
                .instancesRetrievable(true)
                .bindingsRetrievable(false)
                .planUpdateable(false)
                .plans(standardPlan())
                .metadata(mapOf(
                        "displayName" to props.name,
                        "imageUrl" to props.icon,
                        "providerDisplayName" to props.provider,
                        "longDescription" to props.description,
                        "documentationUrl" to "https://github.com/alexandreroman/generic-service-broker",
                        "supportUrl" to "https://github.com/alexandreroman/generic-service-broker/issues"
                ))
        val tag = createTag()
        if (tag != null) {
            svc = svc.tags(tag)
        }
        return svc.build()
    }

    private fun createTag(): String? {
        if (props.tag.isNotEmpty()) {
            return props.tag
        }
        if (props.url.startsWith("jdbc:")) {
            val i = "jdbc:".length
            val j = props.url.indexOf(":", i)
            return props.url.substring(i, j)
        }
        return null
    }

    private fun standardPlan() =
            Plan.builder()
                    .id(STANDARD_PLAN_ID)
                    .name("standard")
                    .description("Standard plan")
                    .bindable(true)
                    .free(true)
                    .metadata(mapOf(
                            "shareable" to "true"
                    ))
                    .build()
}

/**
 * [ServiceInstanceService] implementation, managing custom service instances
 * through the Open Service Broker API.
 */
@Service
class GenericServiceInstanceService(
        private val instanceRepo: GenericServiceInstanceRepository,
        private val props: ServiceProperties,
        private val onDemandServices: List<OnDemandService>) : ServiceInstanceService {
    override fun createServiceInstance(req: CreateServiceInstanceRequest?): CreateServiceInstanceResponse {
        checkServiceDefinitionAndPlan(req!!.serviceDefinitionId, req.planId)

        val instanceExisted = instanceRepo.findById(req.serviceInstanceId).isPresent
        if (instanceExisted) {
            throw ServiceInstanceExistsException(req.serviceInstanceId, req.serviceDefinitionId)
        }

        var newUrl = props.url
        for (ods in onDemandServices) {
            if (ods.supports(props.url)) {
                newUrl = ods.bind(props.url, req.serviceInstanceId)
                break
            }
        }

        val instance = GenericServiceInstance(req.serviceInstanceId, newUrl)
        instanceRepo.save(instance)

        return CreateServiceInstanceResponse.builder()
                .async(false).instanceExisted(instanceExisted).build()
    }

    override fun deleteServiceInstance(req: DeleteServiceInstanceRequest?): DeleteServiceInstanceResponse {
        checkServiceDefinitionAndPlan(req!!.serviceDefinitionId, req.planId)
        if (instanceRepo.existsById(req.serviceInstanceId)) {
            instanceRepo.deleteById(req.serviceInstanceId)
        }
        for (ods in onDemandServices) {
            if (ods.supports(props.url)) {
                ods.unbind(props.url, req.serviceInstanceId)
                break
            }
        }
        return DeleteServiceInstanceResponse.builder()
                .async(false).build()
    }

    override fun getLastOperation(request: GetLastServiceOperationRequest?): GetLastServiceOperationResponse {
        return GetLastServiceOperationResponse.builder()
                .operationState(OperationState.SUCCEEDED)
                .build()
    }

    override fun getServiceInstance(req: GetServiceInstanceRequest?): GetServiceInstanceResponse {
        val instanceExisted = instanceRepo.findById(req!!.serviceInstanceId).isPresent
        if (!instanceExisted) {
            throw ServiceInstanceDoesNotExistException(req.serviceInstanceId)
        }

        return GetServiceInstanceResponse.builder()
                .serviceDefinitionId(SERVICE_DEF_ID)
                .planId(STANDARD_PLAN_ID)
                .build()
    }

    override fun updateServiceInstance(request: UpdateServiceInstanceRequest?): UpdateServiceInstanceResponse {
        return UpdateServiceInstanceResponse.builder()
                .async(false)
                .build()
    }
}

/**
 * [ServiceInstanceBindingService] implementation, providing access to custom service URL.
 */
@Service
class GenericServiceInstanceBindingService(
        private val instanceRepo: GenericServiceInstanceRepository) : ServiceInstanceBindingService {
    override fun createServiceInstanceBinding(req: CreateServiceInstanceBindingRequest?): CreateServiceInstanceBindingResponse {
        checkServiceDefinitionAndPlan(req!!.serviceDefinitionId, req.planId)

        val instance = instanceRepo.getOne(req.serviceInstanceId)
        val svc = CreateServiceInstanceAppBindingResponse.builder()
                .bindingExisted(false)
        createCredentials(instance).forEach { k, v -> svc.credentials(k, v) }
        return svc.build()
    }

    override fun deleteServiceInstanceBinding(req: DeleteServiceInstanceBindingRequest?): DeleteServiceInstanceBindingResponse {
        return DeleteServiceInstanceBindingResponse.builder()
                .async(false).build()
    }

    override fun getServiceInstanceBinding(req: GetServiceInstanceBindingRequest?) =
            throw UnsupportedOperationException()

    private fun createCredentials(instance: GenericServiceInstance): Map<String, String> {
        if (instance.url.startsWith("jdbc:")) {
            return mapOf(
                    "jdbcUrl" to instance.url,
                    "uri" to instance.url.removePrefix("jdbc:")
            )
        }
        return mapOf("uri" to instance.url)
    }
}
