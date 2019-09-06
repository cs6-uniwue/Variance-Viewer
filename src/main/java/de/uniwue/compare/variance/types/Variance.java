package de.uniwue.compare.variance.types;

import java.util.Arrays;
import java.util.List;

/**
 * Variance with predefined types and user defined names 
 */
public class Variance {

	protected final String name;
	protected final VarianceType type;
	protected final String color;
	protected final int priority;
	
	
	public Variance(String name, VarianceType type, String color, int priority) {
		this.name = name;
		this.type = type;
		this.color = color;
		this.priority = priority;
	}
	
	public String getName() {
		return name;
	}
	public VarianceType getType() {
		return type;
	}

	public String getColor() {
		return color;
	}
	
	public int getPriority() {
		return priority;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || getClass() != obj.getClass())
			return false;

		Variance other = (Variance) obj;
		if (name == null) {
			if(other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		if (type != other.type)
			return false;

		return true;
	}
	
	public static List<Variance> getBaseVariances() {
		return Arrays.asList(new Variance[] {
				new VarianceTypography("#03a9f4", 0),
				new VarianceContent("#8bc34a",0),
				new VarianceLineSeparation("#e0e1e0",0)
		});
	}
	
}
