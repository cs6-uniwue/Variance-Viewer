package de.uniwue.compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.uniwue.compare.variance.types.Variance;
import de.uniwue.compare.variance.types.VarianceDistance;
import de.uniwue.compare.variance.types.VarianceMissing;
import de.uniwue.compare.variance.types.VarianceReplacement;

public class Settings {

	private List<String> contentTags;
	private String externalCss;
	private	List<Variance> variances;
	private final static List<String> baseTags = Arrays.asList(new String[] { "css", "contenttags" });
	private final static List<String> legacyTags = Arrays.asList(new String[] {
													"punctuations", "graphemes", "abbreviations"});
	
	public Settings(String settingsString) {
		variances = new ArrayList<>();
		fromString(settingsString);
		variances.addAll(Variance.getBaseVariances());
	}
	
	
	private void fromString(String rawString) {
		String settingsString = Arrays.stream(rawString.split(SpecialCharacter.LINE_BREAKS_REGEX))
									.filter(l -> !(l == null || l == "" || l.startsWith("#")))
									.collect(Collectors.joining("\n"));
		
		Pattern settingsPattern = Pattern.compile(":(.+):((?:\\r|\\n|.)*?):(.+):");
		Pattern complexTagPattern = Pattern.compile("(.+)\\[(.+)\\|(.+)\\]");
		
	
		Matcher settingsMatcher = settingsPattern.matcher(settingsString);
		while (settingsMatcher.find()) {
			final String settingHead = settingsMatcher.group(1);
			
			// Process start tag
			Matcher complexTagMatcher = complexTagPattern.matcher(settingHead);
			String tag = null;
			String variance = null;
			String color = null;
			if (complexTagMatcher.find()) {
				tag = complexTagMatcher.group(1);
				variance = complexTagMatcher.group(2);
				color = complexTagMatcher.group(3);
			} else {
				// Is simple tag
				if (baseTags.contains(settingHead) || legacyTags.contains(settingHead)) {
					tag = settingHead;
				} else {
					Integer[] textposition = calculateTextPosition(settingsMatcher.start(), settingsString);
					throw new IllegalArgumentException(String.format(
							"Unkown starting tag '%s' at line '%d'. Tags must either be complex with ':<tagname>[<variance>|<color>]:' , "+
							"or one of the base tags 'css', 'contenttags', or a legacy tag like 'punktuations', 'graphemes', 'abbreviations'.",
							settingHead, textposition[0]));
				}
			}
			

			// Check if settings starts and ends with the same tag
			final String endTag = settingsMatcher.group(3);
			if (!endTag.equals(tag)) {
				Integer[] textposition = calculateTextPosition(settingsMatcher.end(), settingsString);
				throw new IllegalArgumentException(String.format(
						"Expected tag '%s' at line '%d', encountered '%s'.",
						tag, textposition[0], endTag));
			}
			
			
			// Process setting
			final String settingBody = settingsMatcher.group(2);
			Integer[] textposition = calculateTextPosition(settingsMatcher.start(), settingsString);
			if (variance != null && color != null) {
				// Complex variance type
				switch(variance.toLowerCase()) {
					case "missing":
					case "m":
						variances.add(new VarianceMissing(tag, color, 0, readMissing(settingBody, textposition)));
						break;
					case "replacement":
					case "r":
						variances.add(new VarianceReplacement(tag, color, 0, readReplacementRules(settingBody, textposition)));
						break;
					case "distance":
					case "d":
						final Integer[] range = readIntegerRange(settingBody, textposition);
						variances.add(new VarianceDistance(tag, color, 0, range[0], range[1]));
						break;
					default:
						throw new IllegalArgumentException(String.format(
								"Unkown variance type '%s' at line %d for tag '%s'.",
								variance, textposition[0], tag));
				}
			} else {
				switch(tag.toLowerCase()) {
					case "css":
						externalCss = settingBody;
						break;
					case "contenttags":
						contentTags = Arrays.asList(settingBody.trim().split(SpecialCharacter.WHITESPACES_REGEX));
						break;
					// Legacy settings
					case "punctuations":
						variances.add(new VarianceMissing("PUNKTUATION", "#f44336", 0,
													Arrays.asList(settingBody.trim().split("(?!^)"))));
						break;
					case "graphemes":
						variances.add(new VarianceReplacement("GRAPHEMICS", "#ffb74d", 0, 
													readReplacementRules(settingBody, textposition)));
						break;
					case "abbreviations":
						variances.add(new VarianceReplacement("ABBREVIATION", "#9c27b0", 1,
													readReplacementRules(settingBody, textposition)));
						break;
					default: 
						throw new IllegalArgumentException(String.format(
								"Unkown setting '%s' at line %d.",
								tag, textposition[0]));
				}
			}
		}
	}
	
	/**
	 * Get all variances defined in the settings file
	 * 
	 * @return
	 */
	public List<Variance> getVariances() {
		return new ArrayList<>(variances);
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
	 * Calculate the line and column position of a position inside a text String.
	 * e.g.
	 * "Test
	 * Text"
	 * position 6 ("T")
	 * => line = 1, column = 0
	 * 
	 * @param position
	 * @param document
	 * @return
	 */
	private static Integer[] calculateTextPosition(int position, String document) {
		final int lastNewLine = document.substring(0, position).lastIndexOf(System.lineSeparator());
		final int line = document.substring(0, position).split(System.lineSeparator()).length - 1 ;
		final int column = position - lastNewLine + 1;
		
		return new Integer[] {line, column};
	}
	
	/**
	 * Read an integer range of a setting 
	 * 
	 * @param settings Settings body in between a tags start and end
	 * @return range value [start,end] provided
	 */
	private Integer[] readIntegerRange(String settings, Integer[] startposition) {
		String[] rangeValues = settings.trim().split("\\s");
		if (rangeValues.length != 2) {
			throw new IllegalArgumentException(String.format(
					"Range values found at line %d consist of %d parts instead of 2 parts.",
					startposition[0], rangeValues.length));
		}
		try{
			return new Integer[] {
					Integer.parseInt(rangeValues[0].trim()),
					Integer.parseInt(rangeValues[1].trim())
			};
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format(
					"Range values found at line %d must consist of two integer number.",
					startposition[0]));
		}
	}

	/**
	 * Read the rules from a Replacement settings tag 
	 * 
	 * @param settings Settings body in between a tags start and end
	 * @return Map with all rules (Key:From, Value:To)
	 */
	private Map<String, String> readReplacementRules(String settings, Integer[] startposition) {
		Map<String, String> converted = new HashMap<String, String>();

		int linecounter = 0;
		for (String line : settings.split(System.lineSeparator())) {
			if (!line.trim().isEmpty()) {
				String[] linecontent = line.split("\\s");
				if (linecontent.length == 2) {
					converted.put(linecontent[0], linecontent[1]);
				} else {
					// Replacement rules can only consist of two parts and do not allow more than one whitespace
					throw new IllegalArgumentException(String.format(
							"Replacement rule at line %d does consist of %d parts instead of 2.",
							startposition[0] + linecounter, linecontent.length));
				}
			}
			linecounter++;
		}

		return converted;
	}

	/**
	 * Read the rules from a Missing settings tag
	 * 
	 * @param settings Settings body in between a tags start and end
	 * @return List with all rules 
	 */
	private List<String> readMissing(String settings, Integer[] startposition) {
		return Arrays.stream(settings.split(SpecialCharacter.WHITESPACES_REGEX+"+"))
			.filter(s -> !s.isEmpty()).collect(Collectors.toList());
	}
}
