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

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationFilter;

/**
 * An {@link OperationFilter} that filters based on the allowed {@link EndpointAccess
 * access}.
 *
 * @param <O> the operation type
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public class AllowedAccessOperationFilter<O extends Operation> implements OperationFilter<O> {

	private final EndpointAccess allowedAccess;

	public AllowedAccessOperationFilter(EndpointAccess permittedAccess) {
		this.allowedAccess = permittedAccess;
	}

	@Override
	public boolean match(O operation, EndpointId endpointId) {
		return this.allowedAccess.allow(operation.getType());
	}

}
