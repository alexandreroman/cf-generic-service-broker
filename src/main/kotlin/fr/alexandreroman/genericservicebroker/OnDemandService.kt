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

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

interface OnDemandService {
    fun supports(url: String): Boolean
    fun bind(url: String, serviceId: String): String
    fun unbind(url: String, serviceId: String)
}

@Component
@Primary
@Qualifier("postgres")
class PostgresOnDemandService : OnDemandService {
    override fun supports(url: String) =
            url.startsWith("jdbc:postgresql")

    private fun createJdbcDataSource(url: String): HikariDataSource {
        val ds = HikariDataSource()
        ds.jdbcUrl = url
        return ds
    }

    private fun toSchema(serviceId: String) = "instance_" + serviceId.replace("-", "_")

    override fun bind(url: String, serviceId: String): String {
        val ds = createJdbcDataSource(url)
        val schema = toSchema(serviceId)
        ds.use {
            val jdbcTemplate = JdbcTemplate(ds)
            jdbcTemplate.update("CREATE SCHEMA IF NOT EXISTS $schema")
        }
        return "$url&currentSchema=$schema"
    }

    override fun unbind(url: String, serviceId: String) {
        val ds = createJdbcDataSource(url)
        ds.use {
            val jdbcTemplate = JdbcTemplate(ds)
            val schema = toSchema(serviceId)
            jdbcTemplate.update("DROP SCHEMA IF EXISTS $schema CASCADE")
        }
    }
}
