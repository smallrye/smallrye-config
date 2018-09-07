package io.smallrye.config;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

/**
 * @author kg6zvp
 */
public class FileInterpolatorTest {
	File testFile;

	@Before
	public void setProperties() throws IOException {
		System.setProperty("smallrye.test.var", "ice cream");
		System.setProperty("smallrye.test.var2", "chocolate");
	}

	@Before
	public void createTestFile() throws IOException {
		testFile = File.createTempFile(UUID.randomUUID().toString(), "filetest");

		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(testFile))) {
			bufferedWriter.write("random line\n");
			bufferedWriter.write("I want some vanilla ${smallrye.test.var} with ${smallrye.test.var2} chips\n");
		}
	}

	@Test
	public void testURLInterpolation() throws IOException {
		URL originalURL = testFile.toURI().toURL();
		URL interpolatedURL = FileInterpolator.interpolate(originalURL);
		assertNotEquals(originalURL, interpolatedURL);

		URLConnection conn = interpolatedURL.openConnection();
		conn.setUseCaches(false);

		List<String> lines = new LinkedList<>();

		try(InputStream inputStream = conn.getInputStream()) {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String currentLine = null;
			while( (currentLine = bufferedReader.readLine()) != null) {
				lines.add(currentLine);
			}
		}

		assertEquals(2, lines.size());
		assertEquals("random line", lines.get(0));
		assertEquals("I want some vanilla ice cream with chocolate chips", lines.get(1));
	}


	@Test
	public void testFileInterpolation() throws IOException {
		File interpolatedFile = FileInterpolator.interpolate(testFile);

		List<String> lines = Files.readAllLines(interpolatedFile.toPath());
		assertEquals(2, lines.size());
		assertEquals("random line", lines.get(0));
		assertEquals("I want some vanilla ice cream with chocolate chips", lines.get(1));
	}

	@After
	public void cleanup() {
		System.clearProperty("smallrye.test.var");
		System.clearProperty("smallrye.test.var2");

		//delete
		testFile.delete();
	}
}
