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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link BomPlugin}.
 *
 * @author Eric Wendelin
 */
public class BomPluginIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File buildFile;

    @Before
    public void setup() {
        try {
            buildFile = temporaryFolder.newFile("build.gradle");
        } catch (IOException e) {
            fail("Could not create build.gradle file in temporary folder");
        }
    }

    @Test
    public void testCreatesProcessBomTask() throws Exception {
        try (FileWriter out = new FileWriter(buildFile)) {
            out.write("plugins { id 'org.springframework.boot.bom' }\n");
        }
        BuildResult buildResult = runGradle(temporaryFolder.getRoot(), "tasks", "--all");
        assertTrue(buildResult.getOutput().contains("processBom"));
    }

    @Test
    public void testWritesDeclaredPropertiesToPom() throws Exception {
        final String propertyName = "maven.version";
        final String propertyValue = "3.5.4";

        // TODO: use some util to make this prettier
        try (FileWriter out = new FileWriter(buildFile)) {
            out.write("plugins {\n" +
                    "    id 'org.springframework.boot.bom'\n" +
                    "}\n" +
                    "bom {\n" +
                    "    property '"+propertyName+"', '"+propertyValue+"'\n" +
                    "}\n");
        }

        runGradle(temporaryFolder.getRoot(), "generatePomFileForBomPublication", "-s");

        File generatedPomXml = new File(temporaryFolder.getRoot(), "build/publications/bom/pom-default.xml");
        assertTrue(generatedPomXml.canRead());

        assertBomProperty(propertyValue, generatedPomXml, propertyName);
    }

    @Test
    public void testWriteDeclaredDependenciesToPom() throws Exception {
        final String groupId = "ch.qos.logback";
        final String artifactId = "logback-core";
        final String version = "1.2.3";

        try (FileWriter out = new FileWriter(buildFile)) {
            out.write("plugins {\n" +
                    "    id 'org.springframework.boot.bom'\n" +
                    "}\n" +
                    "bom {\n" +
                    "    dependency '"+groupId+"', '"+artifactId+"', '"+version+"'" +
                    "}\n");
        }

        runGradle(temporaryFolder.getRoot(), "generatePomFileForBomPublication", "-s");

        File generatedPomXml = new File(temporaryFolder.getRoot(), "build/publications/bom/pom-default.xml");
        assertTrue(generatedPomXml.canRead());
        assertBomDependency(groupId, artifactId, version, generatedPomXml);
    }

    @Test
    public void testWriteDeclaredBomImportsToPom() throws Exception {
        final String groupId = "org.junit";
        final String artifactId = "junit-bom";
        final String version = "5.3.2";

        try (FileWriter out = new FileWriter(buildFile)) {
            out.write("plugins {\n" +
                    "    id 'org.springframework.boot.bom'\n" +
                    "}\n" +
                    "bom {\n" +
                    "    bomImport '"+groupId+"', '"+artifactId+"', '"+version+"'" +
                    "}\n");
        }

        runGradle(temporaryFolder.getRoot(), "generatePomFileForBomPublication", "-s");

        File generatedPomXml = new File(temporaryFolder.getRoot(), "build/publications/bom/pom-default.xml");
        assertTrue(generatedPomXml.canRead());
        assertBomImport(groupId, artifactId, version, generatedPomXml);
    }

    @Ignore("this is failing for an unknown reason")
    @Test
    public void testResolvesDependencyVersionsUsingProperties() throws Exception {
        try (FileWriter out = new FileWriter(buildFile)) {
            out.write("plugins {\n" +
                    "    id 'org.springframework.boot.bom'\n" +
                    "}\n" +
                    "bom {\n" +
                    "    property 'logback.version', '1.2.3'\n" +
                    "    property 'junit-jupiter.version', '5.3.2'\n" +
                    "    dependency 'ch.qos.logback', 'logback-core', '\\${logback.version}'\n" +
                    "    bomImport 'org.junit', 'junit-bom', '\\${junit-jupiter.version}'\n" +
                    "}\n");
        }

        runGradle(temporaryFolder.getRoot(), "generatePomFileForBomPublication", "-s");

        File generatedPomXml = new File(temporaryFolder.getRoot(), "build/publications/bom/pom-default.xml");
        assertTrue(generatedPomXml.canRead());

        assertBomDependency("ch.qos.logback", "logback-core", "1.2.3", generatedPomXml);
        assertBomImport("org.junit", "junit-bom", "5.3.2", generatedPomXml);
    }

    private void assertBomProperty(String propertyValue, File generatedPomXml, String propertyName) throws Exception {
        Document bom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(generatedPomXml);
        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals(propertyValue, xpath.evaluate("//properties/" + propertyName, bom, XPathConstants.STRING));
    }

    private void assertBomDependency(String groupId, String artifactId, String version, File generatedPomXml) throws Exception {
        Document bom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(generatedPomXml);
        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals(groupId, xpath.evaluate("//artifactId[text()='" + artifactId + "']/../groupId/text()", bom, XPathConstants.STRING));
        assertEquals(artifactId, xpath.evaluate("//artifactId[text()='" + artifactId + "']/../artifactId/text()", bom, XPathConstants.STRING));
        assertEquals(version, xpath.evaluate("//artifactId[text()='" + artifactId + "']/../version/text()", bom, XPathConstants.STRING));
        assertEquals("compile", xpath.evaluate("//artifactId[text()='" + artifactId + "']/../scope/text()", bom, XPathConstants.STRING));
    }

    private void assertBomImport(String groupId, String artifactId, String version, File generatedPomXml) throws Exception {
        Document bom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(generatedPomXml);
        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals(groupId, xpath.evaluate("//artifactId[text()='" + artifactId + "']/../groupId/text()", bom, XPathConstants.STRING));
        assertEquals(artifactId, xpath.evaluate("//artifactId[text()='" + artifactId + "']/../artifactId/text()", bom, XPathConstants.STRING));
        assertEquals(version, xpath.evaluate("//artifactId[text()='" + artifactId + "']/../version/text()", bom, XPathConstants.STRING));
        assertEquals("pom", xpath.evaluate("//artifactId[text()='" + artifactId + "']/../type/text()", bom, XPathConstants.STRING));
        assertEquals("import", xpath.evaluate("//artifactId[text()='" + artifactId + "']/../scope/text()", bom, XPathConstants.STRING));
    }

    private BuildResult runGradle(File projectDir, String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(args)
                .withPluginClasspath()
                .build();
    }
}
