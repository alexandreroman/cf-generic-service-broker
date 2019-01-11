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

import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException

/**
 * Service identifier.
 */
const val SERVICE_DEF_ID = "1eff5a72-603f-4953-aa15-c3bbf9d64543"

/**
 * Standard plan identifier.
 */
const val STANDARD_PLAN_ID = "332fbace-eb47-42d5-9ad9-dcc728fe9aaf"

inline fun checkServiceDefinitionAndPlan(serviceDefId: String, planId: String) {
    if (serviceDefId != SERVICE_DEF_ID) {
        throw ServiceBrokerInvalidParametersException("Unsupported service definition: $serviceDefId")
    }
    if (planId != STANDARD_PLAN_ID) {
        throw ServiceBrokerInvalidParametersException("Unsupported plan: $planId")
    }
}
