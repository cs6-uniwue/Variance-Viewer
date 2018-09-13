package de.uniwue.compare;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import de.uniwue.wa.server.editor.AnnotationWrapper;

public class Annotation {

	private int begin, end;
	private String type;
	private Map<String, String> features;

	public Annotation(JSONObject json) {
		this.begin = (int) json.get("begin");
		this.end = (int) json.get("end");
		this.type = (String) json.get("type");

		features = new HashMap<String, String>();
		JSONObject featuresJSON = (JSONObject) json.get("features");
		if (featuresJSON != null)
			for (Object key : featuresJSON.keySet())
				features.put((String) key, (String) featuresJSON.get(key));
	}
	
	public Annotation(AnnotationWrapper annotationWrapper) {
		this.begin = (int) annotationWrapper.getBegin();
		this.end = (int) annotationWrapper.getEnd();
		this.type = annotationWrapper.getType();

		features = new HashMap<String, String>();
		
		for (String key : annotationWrapper.getFeatures().keySet())
			features.put(key, annotationWrapper.getFeatures().get(key).toString());
	}
	public Annotation(int begin, int end, String type, Map<String, String> features) {
		this.begin = begin;
		this.end = end;
		this.features = features;
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
		return "{begin:"+begin+",end:"+end+",type:"+type+",features:[..]}";
	}

}
