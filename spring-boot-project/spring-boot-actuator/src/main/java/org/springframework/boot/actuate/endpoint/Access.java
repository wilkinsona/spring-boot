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

package org.springframework.boot.actuate.endpoint;

/**
 * Permitted level of access to an endpoint and its operations.
 *
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public enum Access {

	/**
	 * Access to the endpoint is disabled.
	 */
	DISABLED,

	/**
	 * Access to the endpoint is limited to read operations.
	 */
	READ_ONLY,

	/**
	 * Access to the endpoint is unrestricted.
	 */
	UNRESTRICTED

}