/*
 *
 *  * Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.spring.jpa.autoconfig;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("io.statusmachina")
@EnableJpaRepositories("io.statusmachina.spring.jpa.repo")
@EntityScan("io.statusmachina.spring.jpa.model")
@EnableConfigurationProperties(StatusMachinaProperties.class)
public class StatusMachinaAutoConfiguration {
}
