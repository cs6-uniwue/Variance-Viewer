package de.uniwue.compare.variance.types;

import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * Variance with replacement distance e.g. 
 * Distance 1
 * 
 * Variance between text
 * Text: this is an example 		| Test: this is an example 
 * Would be VarianceDistance of Text to Test with a distance of 1
 */
public class VarianceDistance extends Variance {
	
	protected final int distanceMin, distanceMax;
	
	public VarianceDistance(String name, String color, int priority, int distanceMin, int distanceMax) {
		super(name, VarianceType.DISTANCE, color, priority);
		this.distanceMin = distanceMin;
		this.distanceMax = distanceMax;
	}
	
	public int getDistanceMax() {
		return distanceMax;
	}
	
	public int getDistanceMin() {
		return distanceMin;
	}
	
	/**
	 * Calculate the levenshtein distance between two words
	 * 
	 * @param word1 
	 * @param word2
	 * @return
	 */
	public static int distance(String word1, String word2) {
		LevenshteinDistance calculator = LevenshteinDistance.getDefaultInstance();
		return calculator.apply(word1, word2);
	}
}
