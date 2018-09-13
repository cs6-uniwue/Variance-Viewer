package de.uniwue.compare;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {

	private final List<String> punctuation;
	private final Map<String, String> graphemes;
	private final Map<String, String> abbreviations;
	private final List<String> contentTags;
	private final String externalCss;

	public Settings(String settingsFile) {
		this.punctuation = Arrays.asList(getSettingsString("punctuations", settingsFile).split("(?!^)"));
		this.graphemes = readSettings(getSettingsString("graphemes", settingsFile));
		this.abbreviations = readSettings(getSettingsString("abbreviations", settingsFile));
		this.contentTags = Arrays.asList(getSettingsString("contenttags", settingsFile).split("\\s+"));
		this.externalCss = getSettingsString("css", settingsFile);
	}

	public Map<String, String> getAbbreviations() {
		return abbreviations;
	}

	public Map<String, String> getGraphemes() {
		return graphemes;
	}

	public List<String> getPunctuation() {
		return punctuation;
	}

	public List<String> getContentTags() {
		return contentTags;
	}
	
	public String getExternalCss() {
		return externalCss;
	}

	private String getSettingsString(String setting, String settingsFile) {
		String settingsString = "";
		// Check if setting exists and isn't empty, because of costly extraction via .*
		if (doesSettingExist(setting, settingsFile) && !isSettingEmpty(setting, settingsFile)) {
			// Get Settings content
			String settingPattern = Pattern.quote(":" + setting + ":");
			Matcher matcher = Pattern.compile(settingPattern + "\\n((.*|\\n)*)\\n" + settingPattern).matcher(settingsFile);

			if (matcher.find())
				return matcher.group(1) != null ? matcher.group(1) : "";
		}

		return settingsString;
	}

	private Map<String, String> readSettings(String settings) {
		Map<String, String> converted = new HashMap<String, String>();

		for (String line : settings.split(System.lineSeparator())) {
			String[] linecontent = line.split(" ");
			if (linecontent.length == 2)
				converted.put(linecontent[0], linecontent[1]);
			else
				System.err.println("Invalid line in settings.");
		}

		return converted;
	}

	private boolean doesSettingExist(String setting, String settingsFile) {
		return settingsFile.contains(":" + setting + ":");
	}

	private boolean isSettingEmpty(String setting, String settingsFile) {
		return Pattern.compile(Pattern.quote(":" + setting + ":(\\s)*:" + setting + ":")).matcher(settingsFile).find();
	}
}