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
package org.springframework.boot.build.bom;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.build.assertj.NodeAssert;

/**
 * Tests for {@link BomPlugin}.
 *
 * @author Eric Wendelin
 * @author Andy Wilkinson
 */
public class BomPluginIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File buildFile;

    @Before
    public void setup() throws IOException {
        this.buildFile = this.temporaryFolder.newFile("build.gradle");
    }

    @Test
    public void processBomTaskIsCreatedWhenPluginIsApplied() throws Exception {
        try (FileWriter out = new FileWriter(this.buildFile)) {
            out.write("plugins { id 'org.springframework.boot.bom' }\n");
        }
        BuildResult buildResult = runGradle(temporaryFolder.getRoot(), "tasks", "--all");
        assertTrue(buildResult.getOutput().contains("processBom"));
    }

    @Test
    public void declaredPropertiesAreIncludedInGeneratedPom() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
        	out.println("plugins {");
        	out.println("    id 'org.springframework.boot.bom'");
            out.println("}");
            out.println("bom {");
            out.println("    property 'maven.version', '3.5.4'");
            out.println("}");
        }
        generatePom((pom) -> {
        	assertThat(pom).textAtPath("//properties/maven.version").isEqualTo("3.5.4");
        });
    }

    @Test
    public void declaredDependenciesAreIncludedInGeneratedPom() throws Exception {
    	try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
        	out.println("plugins {");
        	out.println("    id 'org.springframework.boot.bom'");
            out.println("}");
            out.println("bom {");
            out.println("    dependency 'ch.qos.logback', 'logback-core', '1.2.3'");
            out.println("}");
        }
        generatePom((pom) -> {
        	NodeAssert dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency");
        	assertThat(dependency).textAtPath("groupId").isEqualTo("ch.qos.logback");
        	assertThat(dependency).textAtPath("artifactId").isEqualTo("logback-core");
        	assertThat(dependency).textAtPath("version").isEqualTo("1.2.3");
        	assertThat(dependency).textAtPath("scope").isEqualTo("compile");
        	assertThat(dependency).textAtPath("type").isNullOrEmpty();
        });
    }

    @Test
    public void declaredBomImportsAreIncludedInGeneratedPom() throws Exception {
    	try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
        	out.println("plugins {");
        	out.println("    id 'org.springframework.boot.bom'");
            out.println("}");
            out.println("bom {");
            out.println("    bomImport 'org.junit', 'junit-bom', '5.3.2'");
            out.println("}");
        }
        generatePom((pom) -> {
        	NodeAssert dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency");
        	assertThat(dependency).textAtPath("groupId").isEqualTo("org.junit");
        	assertThat(dependency).textAtPath("artifactId").isEqualTo("junit-bom");
        	assertThat(dependency).textAtPath("version").isEqualTo("5.3.2");
        	assertThat(dependency).textAtPath("scope").isEqualTo("import");
        	assertThat(dependency).textAtPath("type").isEqualTo("pom");
        });
    }

    @Test
    public void dependencyVersionsUsePropertyReferences() throws Exception {
    	try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
        	out.println("plugins {");
        	out.println("    id 'org.springframework.boot.bom'");
            out.println("}");
            out.println("bom {");
            out.println("    property 'logback.version', '1.2.3'");
            out.println("    property 'junit-jupiter.version', '5.3.2'");
            out.println("    dependency 'ch.qos.logback', 'logback-core', '${logback.version}'");
            out.println("    bomImport 'org.junit', 'junit-bom', '${junit-jupiter.version}'");
            out.println("}");
        }
    	generatePom((pom) -> {
        	assertThat(pom).textAtPath("//properties/logback.version").isEqualTo("1.2.3");
        	assertThat(pom).textAtPath("//properties/junit-jupiter.version").isEqualTo("5.3.2");
        	assertThat(pom).textAtPath("//dependencyManagement/dependencies/dependency/artifactId[text()='logback-core']/../version").isEqualTo("${logback.version}");
        	assertThat(pom).textAtPath("//dependencyManagement/dependencies/dependency/artifactId[text()='junit-bom']/../version").isEqualTo("${junit-jupiter.version}");
        });
    }

    private BuildResult runGradle(File projectDir, String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(args)
                .withPluginClasspath()
                .build();
    }

    private void generatePom(Consumer<NodeAssert> consumer) {
        runGradle(this.temporaryFolder.getRoot(), "generatePomFileForBomPublication", "-s");
        File generatedPomXml = new File(this.temporaryFolder.getRoot(), "build/publications/bom/pom-default.xml");
        assertThat(generatedPomXml).isFile();
        consumer.accept(new NodeAssert(generatedPomXml));
    }
}
