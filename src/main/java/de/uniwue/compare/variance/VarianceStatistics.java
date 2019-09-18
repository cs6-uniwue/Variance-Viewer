package de.uniwue.compare.variance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.variance.types.Variance;

public class VarianceStatistics {

	private final LinkedHashMap<Variance, Integer> varianceCounts;
	private int globalChanges;
	
	public VarianceStatistics(List<ConnectedContent> contents, Collection<Variance> variances) {
		this.varianceCounts = new LinkedHashMap<>();
		this.globalChanges = 0;
		
		Map<String, Variance> varianceMap = variances.stream()
				.collect(Collectors.toMap(Variance::getName, Function.identity()));
		
		Map<Variance, Integer> counter = new HashMap<>();
		for(ConnectedContent content : contents) {
			String varianceString = content.getVarianceType();
			if (varianceMap.containsKey(varianceString)) {
				Variance variance = varianceMap.get(varianceString);
				counter.putIfAbsent(variance, 0);
				
				counter.put(variance, counter.get(variance) + 1);
				globalChanges++;
			}
		}
		
		//Sort
		List<Entry<Variance, Integer>> sortedEntries = new ArrayList<>(counter.entrySet());
        sortedEntries.sort((e1,e2) -> e2.getValue().compareTo(e1.getValue()));
        for (Entry<Variance, Integer> entry : sortedEntries) 
            varianceCounts.put(entry.getKey(), entry.getValue());
	}
	
	
	public int getGlobalChanges() {
		return globalChanges;
	}
	
	public LinkedHashMap<Variance, Integer> getVarianceCounts() {
		return varianceCounts;
	}

	
}
