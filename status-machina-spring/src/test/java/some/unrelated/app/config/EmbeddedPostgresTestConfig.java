/*
 *  Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package some.unrelated.app.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Boots a real (binary) PostgreSQL instance for integration tests and exposes it as the application's
 * {@link DataSource}. Spring Boot's standard Liquibase and JPA auto-configuration then run against this
 * datasource, so the PostgreSQL-targeted Liquibase changesets execute on an actual PostgreSQL server
 * without requiring Docker.
 *
 * <p>Zonky's {@code @AutoConfigureEmbeddedDatabase} Spring integration is intentionally avoided here:
 * its Liquibase support does not yet work with Spring Boot 4. We only use the embedded-postgres binary
 * launcher and let Spring Boot own the rest of the wiring.
 */
@Configuration
public class EmbeddedPostgresTestConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }
}
