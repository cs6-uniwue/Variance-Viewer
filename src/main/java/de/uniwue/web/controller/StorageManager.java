package de.uniwue.web.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletContext;

import org.springframework.web.multipart.MultipartFile;

public class StorageManager {

	public static String getSettings(MultipartFile settingsFile, ServletContext servletContext) {
		if (!settingsFile.isEmpty())
			try {
				return new String(settingsFile.getBytes(), "UTF-8");
			} catch (IOException e2) {
				// punctuationFile is faulty
				System.err.println("Settings file is faulty. Fallback to default.");
			}
		
		return getDefault(servletContext);
	}

	public static String getDefault(ServletContext servletContext) {
		try {
			String path = servletContext.getRealPath("WEB-INF" + File.separator + "defaults.txt");
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, "UTF-8");
		} catch (IOException e) {
			// default file is empty or faulty
			System.err.println("Default settings file could not be found or is faulty. Fallback to empty.");
		}
		return "";
	}
}
