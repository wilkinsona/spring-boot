/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.build.mavenexec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MavenExecPlugin}.
 *
 * @author Eric Wendelin
 */
public class MavenExecPluginIntegrationTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File buildFile;

	@Before
	public void setup() throws IOException {
		this.buildFile = this.temporaryFolder.newFile("build.gradle");
	}

	@Test
	public void canExecuteMavenBuilds() throws Exception {
		writePom(this.temporaryFolder.getRoot());
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("import org.springframework.boot.build.mavenexec.MavenExec");
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.mavenexec'");
			out.println("}");
			out.println("task install(type: MavenExec)");
		}

		BuildResult buildResult = runGradle(this.temporaryFolder.getRoot(), "install",
				"-s");
		assertThat(buildResult.task(":install").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void mavenVersionIsCustomizable() throws Exception {
		writePom(this.temporaryFolder.getRoot());
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("import org.springframework.boot.build.mavenexec.MavenExec");
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.mavenexec'");
			out.println("}");
			out.println("maven {");
			out.println("    version = '3.5.4'");
			out.println("}");
			out.println("task install(type: MavenExec)");
		}

		BuildResult buildResult = runGradle(this.temporaryFolder.getRoot(), "install",
				"-s");
		assertThat(buildResult.getOutput()).contains("3.5.4");
	}

	private BuildResult runGradle(File projectDir, String... args) {
		return GradleRunner.create().withProjectDir(projectDir).withArguments(args)
				.withPluginClasspath().build();
	}

	private File writePom(File projectDir) throws IOException {
		File pomXml = new File(projectDir, "pom.xml");
		try (PrintWriter out = new PrintWriter(new FileWriter(pomXml))) {
			out.println(
					"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			out.println(
					"  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">");
			out.println("  <modelVersion>4.0.0</modelVersion>");
			out.println("  <groupId>com.mycompany.app</groupId>");
			out.println("  <artifactId>my-app</artifactId>");
			out.println("  <version>1.0-SNAPSHOT</version>");
			out.println("  <properties>");
			out.println("    <maven.compiler.source>1.8</maven.compiler.source>");
			out.println("    <maven.compiler.target>1.8</maven.compiler.target>");
			out.println("  </properties>");
			out.println("  <dependencies>");
			out.println("    <dependency>");
			out.println("      <groupId>commons-io</groupId>");
			out.println("      <artifactId>commons-io</artifactId>");
			out.println("      <version>2.6</version>");
			out.println("      <type>pom</type>");
			out.println("    </dependency>");
			out.println("  </dependencies>");
			out.println("</project>");
		}

		return pomXml;
	}

}
