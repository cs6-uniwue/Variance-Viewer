package de.uniwue.compare.variance.types;

import java.util.HashMap;
import java.util.Map;

/**
 * Variance with replacement rules e.g. 
 * Bsp -> Beispiel
 * 
 * Variance between text
 * Bsp: this is an example 		| Beispiel: this is an example 
 * Would be VarianceReplacement of Bsp to Beispiel
 */
public class VarianceReplacement extends Variance{
	
	protected final Map<String, String> rules;
	
	public VarianceReplacement(String name, String color, int priority, Map<String, String> rules) {
		super(name, VarianceType.REPLACEMENT, color, priority);
		this.rules = rules;
	}
	
	public Map<String, String> getRules() {
		return new HashMap<>(rules);
	}
}
