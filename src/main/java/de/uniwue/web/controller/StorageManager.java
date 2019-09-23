package de.uniwue.web.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletContext;

import org.springframework.web.multipart.MultipartFile;

public class StorageManager {

	public static String getSettings(File settingsFile, ServletContext servletContext) {
		try {
			byte[] encoded = Files.readAllBytes(settingsFile.toPath());
			return normalize(new String(encoded, "UTF-8"));
		} catch (IOException e2) {
			// SettingsFile is faulty
			System.err.println("Settings file is faulty. Fallback to default.");
		}
		return getDefault(servletContext);
	}

	public static String getSettings(MultipartFile settingsFile, ServletContext servletContext) {
		if (!settingsFile.isEmpty())
			try {
				return normalize(new String(settingsFile.getBytes(), "UTF-8"));
			} catch (IOException e2) {
				// SettingsFile is faulty
				System.err.println("Settings file is faulty. Fallback to default.");
			}
		
		return getDefault(servletContext);
	}

	public static String getDefault(ServletContext servletContext) {
		try {
			String path = servletContext.getRealPath("WEB-INF" + File.separator + "defaults.txt");
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return normalize(new String(encoded, "UTF-8"));
		} catch (IOException e) {
			// default file is empty or faulty
			System.err.println("Default settings file could not be found or is faulty. Fallback to empty.");
		}
		return "";
	}

	public static File getFile(String path, ServletContext servletContext) {
			return new File(servletContext.getRealPath("WEB-INF" + File.separator + path));
	}
	
	private static String normalize(String content) {
			content = content.replaceAll("\\r\\n", "\n");
			content = content.replaceAll("\\r", "\n");
			return content;	
	}
}
