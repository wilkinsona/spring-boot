/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.data.jdbc.autoconfigure;

import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.IdGeneratingEntityCallback;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.QueryMappingConfiguration;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.dialect.DialectResolver;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcConfiguration;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's JDBC Repositories.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling JDBC repositories
 * using the {@link EnableJdbcRepositories @EnableJdbcRepositories} annotation and
 * providing an {@link AbstractJdbcConfiguration} subclass.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 4.0.0
 * @see EnableJdbcRepositories
 */
@AutoConfiguration(after = { JdbcTemplateAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class })
@ConditionalOnBean({ NamedParameterJdbcOperations.class, PlatformTransactionManager.class })
@ConditionalOnClass({ NamedParameterJdbcOperations.class, AbstractJdbcConfiguration.class })
@ConditionalOnBooleanProperty(name = "spring.data.jdbc.repositories.enabled", matchIfMissing = true)
@EnableConfigurationProperties(DataJdbcProperties.class)
public final class DataJdbcRepositoriesAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JdbcRepositoryConfigExtension.class)
	@Import(DataJdbcRepositoriesRegistrar.class)
	static class JdbcRepositoriesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(AbstractJdbcConfiguration.class)
	static class SpringBootJdbcConfiguration {

		@Bean
		@ConditionalOnMissingBean
		RelationalManagedTypes jdbcManagedTypes(ApplicationContext applicationContext) throws ClassNotFoundException {
			return RelationalManagedTypes.fromIterable(new EntityScanner(applicationContext).scan(Table.class));
		}

		@Bean
		@ConditionalOnMissingBean
		JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy,
				JdbcCustomConversions customConversions, RelationalManagedTypes jdbcManagedTypes) {
			return JdbcConfiguration.createMappingContext(jdbcManagedTypes, customConversions,
					namingStrategy.orElse(null));
		}

		@Bean
		@ConditionalOnMissingBean
		IdGeneratingEntityCallback idGeneratingBeforeSaveCallback(JdbcMappingContext mappingContext,
				NamedParameterJdbcOperations operations, JdbcDialect dialect) {
			return new IdGeneratingEntityCallback(mappingContext, dialect, operations);
		}

		@Bean
		@ConditionalOnMissingBean
		JdbcConverter jdbcConverter(JdbcMappingContext mappingContext, NamedParameterJdbcOperations operations,
				@Lazy RelationResolver relationResolver, JdbcCustomConversions conversions, JdbcDialect dialect) {
			return JdbcConfiguration.createConverter(mappingContext, operations, relationResolver, conversions,
					dialect);
		}

		@Bean
		@ConditionalOnMissingBean
		JdbcCustomConversions jdbcCustomConversions(JdbcDialect dialect) {
			return JdbcConfiguration.createCustomConversions(dialect, Collections.emptyList());
		}

		@Bean
		@ConditionalOnMissingBean
		JdbcAggregateTemplate jdbcAggregateTemplate(ApplicationContext applicationContext,
				JdbcMappingContext mappingContext, JdbcConverter converter, DataAccessStrategy dataAccessStrategy) {
			return new JdbcAggregateTemplate(applicationContext, mappingContext, converter, dataAccessStrategy);
		}

		@Bean
		@ConditionalOnMissingBean
		DataAccessStrategy dataAccessStrategyBean(NamedParameterJdbcOperations operations, JdbcConverter jdbcConverter,
				JdbcMappingContext context, JdbcDialect dialect) {
			return JdbcConfiguration.createDataAccessStrategy(operations, jdbcConverter,
					QueryMappingConfiguration.EMPTY, dialect);
		}

		@Bean
		@ConditionalOnMissingBean
		JdbcDialect jdbcDialect(DataJdbcProperties properties,
				ObjectProvider<NamedParameterJdbcOperations> operations) {
			DataJdbcDatabaseDialect dialect = properties.getDialect();
			return (dialect != null) ? dialect.getJdbcDialect(() -> operations.getObject().getJdbcOperations())
					: DialectResolver.getDialect(operations.getObject().getJdbcOperations());
		}

	}

}
