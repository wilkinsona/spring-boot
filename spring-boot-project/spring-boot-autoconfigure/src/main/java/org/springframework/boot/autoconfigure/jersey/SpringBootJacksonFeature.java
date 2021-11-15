/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.jersey;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.InternalProperties;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * {@link Feature} for registering Jackson with Jersey. Alternative to
 * {@link JacksonFeature} that avoids a dependency on JAX-B.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public class SpringBootJacksonFeature implements Feature {

	private static final String FEATURE_ID = SpringBootJacksonFeature.class.getSimpleName();

	@Override
	public boolean configure(FeatureContext context) {
		Configuration configuration = context.getConfiguration();
		if (alternativeJsonFeatureRegistered(configuration)) {
			return false;
		}
		context.property(PropertiesHelper.getPropertyNameForRuntime(InternalProperties.JSON_FEATURE,
				configuration.getRuntimeType()), FEATURE_ID);
		if (!configuration.isRegistered(JacksonJsonProvider.class)) {
			context.register(JacksonJsonProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
		}
		return true;
	}

	private boolean alternativeJsonFeatureRegistered(Configuration configuration) {
		String jsonFeature = CommonProperties.getValue(configuration.getProperties(), configuration.getRuntimeType(),
				InternalProperties.JSON_FEATURE, FEATURE_ID, String.class);
		return !FEATURE_ID.equalsIgnoreCase(jsonFeature);
	}

}
