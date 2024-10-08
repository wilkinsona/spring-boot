/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.access;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.core.env.PropertyResolver;

/**
 * An {@link OperationFilter} that filters based on the allowed {@link Access access}
 * configured using the following properties:
 * <ol>
 * <li>{@code management.endpoints.<id>.access}
 * <li>{@code management.endpoints.<id>.enabled} (deprecated)
 * <li>{@code management.endpoints.default-access}
 * <li>{@code management.endpoints.enabled-by-default} (deprecated)
 * </ol>
 *
 * @param <O> the operation type
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public class AccessPropertiesOperationFilter<O extends Operation> implements OperationFilter<O> {

	private final PropertyResolver properties;

	private final Access endpointsDefaultAccess;

	private final Map<EndpointId, Access> accessCache = new ConcurrentHashMap<>();

	public AccessPropertiesOperationFilter(PropertyResolver properties) {
		this.properties = properties;
		this.endpointsDefaultAccess = determineDefaultAccess(properties);
	}

	private static Access determineDefaultAccess(PropertyResolver properties) {
		Access defaultAccess = properties.getProperty("management.endpoints.default-access", Access.class);
		if (defaultAccess != null) {
			return defaultAccess;
		}
		Boolean endpointsEnabledByDefault = properties.getProperty("management.endpoints.enabled-by-default",
				Boolean.class);
		if (endpointsEnabledByDefault != null) {
			return endpointsEnabledByDefault ? Access.UNRESTRICTED : Access.DISABLED;
		}
		return null;
	}

	@Override
	public boolean match(O operation, EndpointId endpointId, Access defaultAccess) {
		Access access = this.accessCache.computeIfAbsent(endpointId, (id) -> accessFor(id, defaultAccess));
		return switch (access) {
			case DISABLED -> false;
			case READ_ONLY -> operation.getType() == OperationType.READ;
			case UNRESTRICTED -> true;
		};
	}

	private Access accessFor(EndpointId endpointId, Access endpointDefaultAccess) {
		Access access = this.properties.getProperty("management.endpoint.%s.access".formatted(endpointId),
				Access.class);
		if (access != null) {
			return access;
		}
		Boolean enabled = this.properties.getProperty("management.endpoint.%s.enabled".formatted(endpointId),
				Boolean.class);
		if (enabled != null) {
			return (enabled) ? Access.UNRESTRICTED : Access.DISABLED;
		}
		return (this.endpointsDefaultAccess != null) ? this.endpointsDefaultAccess : endpointDefaultAccess;
	}

}
