package de.uniwue.compare;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Settings object representing all variance rules and display settings.
 *
 */
public class Settings {

	private final List<String> punctuation;
	private final Map<String, String> graphemes;
	private final Map<String, String> abbreviations;
	private final List<String> contentTags;
	private final String externalCss;
	private final static String[] tags = new String[] { ":punctuations:", ":graphemes:", ":abbreviations:",
			":contenttags:", ":css:" };

	/**
	 * Create Settings object from a valid settingsString. Will raise
	 * IllegalArgumentException if settingsString is not valide.
	 * 
	 * @param settingsString
	 */
	public Settings(String settingsString) {
		validate(settingsString);
		this.punctuation = Arrays.asList(getSettingsString("punctuations", settingsString).split("(?!^)"));
		this.graphemes = readRules(getSettingsString("graphemes", settingsString));
		this.abbreviations = readRules(getSettingsString("abbreviations", settingsString));
		this.contentTags = Arrays.asList(getSettingsString("contenttags", settingsString).split("\\s+"));
		this.externalCss = getSettingsString("css", settingsString);
	}

	/**
	 * Validate settingsString and will raise IllegalArgumentException if
	 * settingsString is not valid. Exceptions includes additional information about
	 * the error.
	 * 
	 * @param settingsString
	 * @return
	 */
	private static boolean validate(String settingsString) {
		// Check tag count
		for (String tag : tags) {
			final int occurances = countOccurrances(settingsString, tag);
			if (occurances != 2 && occurances != 0) {
				throw new IllegalArgumentException(
						String.format("Tag '%s' has been opened, but not been closed.", tag));
			} else if (occurances > 0) {
				// Check invalid tag position
				String tagContent = settingsString.substring(settingsString.indexOf(tag),
						settingsString.lastIndexOf(tag));
				for (String otherTag : tags) {
					if (otherTag != tag && tagContent.contains(otherTag)) {
						throw new IllegalArgumentException(String.format(
								"Tag '%s' was opened inside '%s'." + " Tags can not be definde inside others.",
								otherTag, tag));
					}
				}
			}
		}

		return true;
	}

	/**
	 * Count the occurrences of a :tag: inside a text
	 * 
	 * @param text
	 * @param tag
	 * @return
	 */
	private static int countOccurrances(String text, String tag) {
		return (int) ((text.length() - text.replaceAll(tag, "").length()) / tag.length());
	}

	public Map<String, String> getAbbreviations() {
		return abbreviations;
	}

	/**
	 * Get all graphemic rules
	 * 
	 * @return
	 */
	public Map<String, String> getGraphemes() {
		return graphemes;
	}

	/**
	 * Get all punctuations to consider
	 * 
	 * @return
	 */
	public List<String> getPunctuation() {
		return punctuation;
	}

	/**
	 * Get all tags that should be compared via their content
	 * 
	 * @return
	 */
	public List<String> getContentTags() {
		return contentTags;
	}

	/**
	 * Get css string that should be added to the existing css
	 * 
	 * @return
	 */
	public String getExternalCss() {
		return externalCss;
	}

	/**
	 * Get the content between the start and end of a settings :tag:
	 * 
	 * @param settingsTag    Tag to search content in
	 * @param settingsString Complete settings string/content of the settings file
	 * @return Settings in between the start and end of a tag
	 */
	private String getSettingsString(String settingsTag, String settingsString) {
		String settingsTagString = "";
		// Check if setting exists and isn't empty, because of costly extraction via .*
		if (doesSettingExist(settingsTag, settingsString) && !isSettingEmpty(settingsTag, settingsString)) {
			// Get Settings content
			String settingPattern = Pattern.quote(":" + settingsTag + ":");
			Matcher matcher = Pattern.compile(settingPattern + "\\n((.*|\\n)*)\\n" + settingPattern)
					.matcher(settingsString);

			if (matcher.find())
				return matcher.group(1) != null ? matcher.group(1) : "";
		}

		return settingsTagString;
	}

	/**
	 * Read the rules from a settings tag (e.g. Graphemics, Abbreviations)
	 * 
	 * @param settings Settings in between a tags start and end
	 * @return Map with all rules (Key:From, Value:To)
	 */
	private Map<String, String> readRules(String settings) {
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

	/**
	 * Check if a setting does exist inside a settings file
	 * 
	 * @param settingTag     Settings tag to search
	 * @param settingsString Complete settings content of all settings
	 * @return True if exists, else False
	 */
	private boolean doesSettingExist(String settingTag, String settingsString) {
		return settingsString.contains(":" + settingTag + ":");
	}

	/**
	 * Check if a settings tag contains content
	 * 
	 * @param settingTag     Tag to check
	 * @param settingsString Complete setting content of all settings
	 * @return True if empty, else False
	 */
	private boolean isSettingEmpty(String settingTag, String settingsString) {
		return Pattern.compile(Pattern.quote(":" + settingTag + ":(\\s)*:" + settingTag + ":")).matcher(settingsString)
				.find();
	}
}