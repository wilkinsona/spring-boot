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

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.SecurityContext;

/**
 * Strategy interface that can be used to restrict access to an endpoint and its
 * operations.
 *
 * @author Andy Wilkinson
 * @since 3.4.0
 */
@FunctionalInterface
public interface EndpointAccessFilter {

	/**
	 * Determines whether to allow access, returning {@code false} when access is not
	 * allowed.
	 * @param securityContext the current security context
	 * @param endpointId the id of the endpoint whose operation is being invoked
	 * @param operation the operation being invoked or {@code null} if the operation is
	 * not known}
	 * @return {@code true} to allow access, otherwise {@code false}.
	 */
	boolean allow(SecurityContext securityContext, EndpointId endpointId, WebOperation operation);

}
