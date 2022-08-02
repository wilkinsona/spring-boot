/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.configurationprocessor.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuildExtension;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the configuration properties annotation processor.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleBuildExtension.class)
public class ConfigurationProcessorIntegrationTests {

	private GradleBuild gradleBuild = new GradleBuild().bootVersion(loadVersionFromGradleProperties())
			.scriptProperty("mavenRepository", new File("build/maven-repository").getAbsolutePath());

	@Test
	void emptyProjectBuildsCleanly() {
		BuildResult result = this.gradleBuild.build("compileJava", "--console=plain");
		assertThat(result.task(":compileJava").getOutcome()).isEqualTo(TaskOutcome.NO_SOURCE);
		assertThat(result.task(":preserveSpringConfigurationMetadata").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	void whenGetterIsAnnotatedThenPropertyTypeIsCorrect() throws IOException {
		copySource("AnnotatedGetterProperties.java");
		compileApplication((properties) -> {
			assertThat(properties).hasSize(1);
			assertThat(properties).containsKey("annotated.name");
			assertThat(properties.get("annotated.name").getType()).isEqualTo("java.lang.String");
		});
	}

	@Test
	void whenNewClassIsAddedAndCompilationIsIncrementalThenAllPropertyDescriptionsAreAvailable()
			throws FileNotFoundException, IOException {
		copySource("ExampleProperties.java");
		compileApplication((properties) -> {
			assertThat(properties).hasSize(1);
			assertThat(properties).containsKey("example.name");
			assertThat(properties.get("example.name").getDescription()).isEqualTo("Description of example's name.");
		});
		copySource("MoreProperties.java");
		compileApplication((properties) -> {
			assertThat(properties).hasSize(2);
			assertThat(properties).containsKey("more.name");
			assertThat(properties.get("more.name").getDescription()).isEqualTo("Description of more's name.");
			assertThat(properties).containsKey("example.name");
			assertThat(properties.get("example.name").getDescription()).isEqualTo("Description of example's name.");
		});
	}

	@Test
	void whenOnlyConfigurationPropertyClassIsRemovedThenMetadataIsDeleted() throws FileNotFoundException, IOException {
		copySource("ExampleProperties.java");
		compileApplication((properties) -> {
			assertThat(properties).hasSize(1);
			assertThat(properties).containsKey("example.name");
			assertThat(properties.get("example.name").getDescription()).isEqualTo("Description of example's name.");
		});
		new File(this.gradleBuild.getProjectDir(), "src/main/java/com/example/ExampleProperties.java").delete();
		copySource("Plain.java");
		BuildResult result = this.gradleBuild.build("compileJava");
		assertThat(result.task(":compileJava").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(new File(this.gradleBuild.getProjectDir(),
				"build/classes/java/main/META-INF/spring-configuration-metadata.json")).doesNotExist();

	}

	private String loadVersionFromGradleProperties() {
		File gradlePropertiesFile = findGradleProperties();
		try (Reader reader = new FileReader(gradlePropertiesFile)) {
			Properties gradleProperties = new Properties();
			gradleProperties.load(new FileReader(gradlePropertiesFile));
			String bootVersion = gradleProperties.getProperty("version");
			return bootVersion;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private File findGradleProperties() {
		File candidate = new File("gradle.properties").getAbsoluteFile();
		while (!candidate.exists()) {
			candidate = new File(candidate.getParentFile().getParentFile(), "gradle.properties");
		}
		return candidate;
	}

	private void copySource(String name) throws IOException {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/com/example");
		examplePackage.mkdirs();
		FileCopyUtils.copy(new File("src/test/java/com/example/" + name), new File(examplePackage, name));
	}

	private void compileApplication(Consumer<Map<String, ConfigurationMetadataProperty>> propertiesConsumer)
			throws IOException, FileNotFoundException {
		BuildResult result = this.gradleBuild.build("compileJava");
		assertThat(result.task(":compileJava").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Map<String, ConfigurationMetadataProperty> properties = readConfigurationProperties();
		propertiesConsumer.accept(properties);
	}

	private Map<String, ConfigurationMetadataProperty> readConfigurationProperties()
			throws IOException, FileNotFoundException {
		ConfigurationMetadataRepository repository = ConfigurationMetadataRepositoryJsonBuilder
				.create(new FileInputStream(new File(this.gradleBuild.getProjectDir(),
						"build/classes/java/main/META-INF/spring-configuration-metadata.json")))
				.build();
		Map<String, ConfigurationMetadataProperty> properties = repository.getAllProperties();
		return properties;
	}

}
