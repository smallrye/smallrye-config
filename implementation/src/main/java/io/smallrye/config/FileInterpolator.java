package io.smallrye.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author kg6zvp
 */
public class FileInterpolator {

	public static URL interpolate(URL reference) {
		try {
			return interpolateURL(reference).toURI().toURL();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static File interpolate(File input) {
		try {
			return interpolateURL(input.toURI().toURL());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static File interpolateURL(URL reference) throws IOException {
		URLConnection conn = reference.openConnection();
		conn.setUseCaches(false);
		File tFile = File.createTempFile(reference.getFile(), "interpolated");
	
		InputStream inputStream = null;
		BufferedWriter bufferedWriter = null;

		try {
			inputStream = conn.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			bufferedWriter = new BufferedWriter(new FileWriter(tFile));

			String currentLine = null;
			while((currentLine = bufferedReader.readLine()) != null) {
				bufferedWriter.append(StringInterpolator.interpolate(currentLine) + "\n");
			}
		} catch (IOException e) {
		} finally {
			if(inputStream != null)
				inputStream.close();
			if(bufferedWriter != null)
				bufferedWriter.close();
		}
		return tFile;
	}
}