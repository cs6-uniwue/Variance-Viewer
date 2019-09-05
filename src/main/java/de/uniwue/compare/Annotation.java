package de.uniwue.compare;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.uniwue.wa.server.editor.AnnotationWrapper;

/**
 * Document annotations of TEI documents.
 * Annotations in TEI documents include all Tags in the original TEI xml,
 * with pointer to Text inside the extracted text content.
 * e.g. 
 * <p rend="xxl">Text</p>
 * =>
 * Annotation{begin:0,end:4,type:p,features:{rend:xxl}}
 */
public class Annotation {

	private final int begin, end;
	private final String type;
	private final Map<String, String> features;

	public Annotation(AnnotationWrapper annotationWrapper) {
		this.begin = (int) annotationWrapper.getBegin();
		this.end = (int) annotationWrapper.getEnd();
		this.type = annotationWrapper.getType();

		features = new HashMap<String, String>();
		
		for (String key : annotationWrapper.getFeatures().keySet())
			features.put(key, annotationWrapper.getFeatures().get(key).toString());
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}

	public Map<String, String> getFeatures() {
		return features;
	}

	public String getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Annotation other = (Annotation) obj;
		if (begin != other.begin)
			return false;
		if (end != other.end)
			return false;
		if (features == null) {
			if (other.features != null)
				return false;
		} else if (!features.equals(other.features))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		String featureString = features.entrySet().stream().map(e -> e.getKey()+":"+e.getValue())
				.collect(Collectors.joining(","));
		return "Annotation{begin:"+begin+",end:"+end+",type:"+type+",features:{"+featureString+"}}";
	}

}
