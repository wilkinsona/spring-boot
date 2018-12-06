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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.gradle.api.artifacts.transform.ArtifactTransform;

/**
 * Gradle {@link ArtifactTransform} to extract given ZIP file input.
 *
 * @author Eric Wendelin
 */
public class ExplodeZip extends ArtifactTransform {

	@Override
	public List<File> transform(File input) {
		try (ZipFile zipFile = new ZipFile(input)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File entryDestination = new File(getOutputDirectory(), entry.getName());
				if (entry.isDirectory()) {
					entryDestination.mkdirs();
				}
				else {
					File parentDir = entryDestination.getParentFile();
					parentDir.mkdirs();
					try (InputStream in = zipFile.getInputStream(entry)) {
						OutputStream out = new FileOutputStream(entryDestination);
						IOUtils.copy(in, out);
						out.close();

						if (parentDir.getName().equals("bin")) {
							entryDestination.setExecutable(true);
						}
					}
				}
			}
		}
		catch (IOException ex) {
			throw new RuntimeException("Unable to unzip " + input.getAbsolutePath()
					+ " - " + ex.getMessage());
		}

		return new ArrayList<>(Collections.singleton(getOutputDirectory()));
	}

}
