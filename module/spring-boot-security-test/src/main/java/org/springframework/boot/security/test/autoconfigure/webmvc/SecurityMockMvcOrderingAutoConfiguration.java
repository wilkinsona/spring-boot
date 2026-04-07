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

package org.springframework.boot.security.test.autoconfigure.webmvc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcAutoConfiguration;

/**
 * Auto-configuration to ensure that {@link MockMvcAutoConfiguration} has defined a
 * {@link DispatcherServletPath} bean before the condition in
 * {@link ServletWebSecurityAutoConfiguration} that matches against such a bean is
 * evaluated.
 *
 * @author Andy Wilkinson
 */
@AutoConfiguration(afterName = "org.springframework.boot.webmvc.test.autoconfigure.MockMvcAutoConfiguration",
		before = ServletWebSecurityAutoConfiguration.class)
final class SecurityMockMvcOrderingAutoConfiguration {

}
