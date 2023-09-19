/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.lang.reflect.InaccessibleObjectException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

/**
 * @author awilkinson
 */
public class UrlFactoryHygieneTestExecutionListener implements TestExecutionListener {

	private static final String TOMCAT_URL_STREAM_HANDLER_FACTORY = "org.apache.catalina.webresources.TomcatURLStreamHandlerFactory";

	private static Path report = Path.of("url-factory-hygiene.txt");

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		try {
			Object factory = ReflectionTestUtils.getField(URL.class, "factory");
			if (factory != null && factory.getClass().getClassLoader() != getClass().getClassLoader()) {
				try {
					Files.write(report,
							List.of(testIdentifier.toString() + " " + factory.getClass() + " "
									+ factory.getClass().getClassLoader() + " " + getClass().getClassLoader()),
							StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				}
				catch (IOException ex) {
					ex.printStackTrace(System.err);
				}
			}
			cleanUp();
		}
		catch (InaccessibleObjectException ex) {
			System.err.println("You need to open some stuff");
		}
	}

	private void cleanUp() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent(TOMCAT_URL_STREAM_HANDLER_FACTORY, classLoader)) {
			Class<?> factoryClass = ClassUtils.resolveClassName(TOMCAT_URL_STREAM_HANDLER_FACTORY, classLoader);
			ReflectionTestUtils.setField(factoryClass, "instance", null);
		}
		ReflectionTestUtils.setField(URL.class, "factory", null);
	}

}
