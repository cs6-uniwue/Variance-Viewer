package de.uniwue.translate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;

import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.ContentType;
import de.uniwue.compare.VarianceType;
import de.uniwue.compare.token.Token;
import de.uniwue.wa.server.editor.AnnotationWrapper;
import de.uniwue.wa.server.editor.FeatureWrapper;
import de.uniwue.wa.server.editor.TextAnnotationStruct;
import de.uniwue.wa.server.editor.TypeWrapper;

public class DiffExporter {

	private static final String GROUPID = "de.uniwue.diff";
	private static Collection<TypeWrapper> diffTypes;

	/**
	 * Convert a String Document to an AthenJSON
	 * 
	 * @param originalDocumenat
	 * @param diffContents
	 * @return
	 */
	public static String convertToAthenJSONString(String originalDocumenat,
						List<ConnectedContent> diffContents,
						List<VarianceType> outputVarianceTypes) {
		TextAnnotationStruct originalDocument = new TextAnnotationStruct(null, originalDocumenat, new ArrayList<>(),
				new ArrayList<>());

		return convertToAthenJSONString(originalDocument, diffContents, outputVarianceTypes);
	}

	/**
	 * Add differences to a TextAnnotationStruct and convert it to AthenJSON
	 * 
	 * @param originalDocument
	 * @param diffContents
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String convertToAthenJSONString(TextAnnotationStruct originalDocument,
			List<ConnectedContent> diffContents, List<VarianceType> outputVarianceTypes) {
		String text = originalDocument.getText();

		// ** Annotations **
		List<AnnotationWrapper> originalAnnotations = originalDocument.getAnnotations();
		List<AnnotationWrapper> annotations = new ArrayList<>();

		Optional<Token> firstToken = diffContents.stream().flatMap(t -> t.getOriginal().stream()).findFirst();
		int textPosition = firstToken.isPresent() ? firstToken.get().getBegin() : 0;
		
		int idCounter = originalDocument.getAnnotations().size();
		int totalDelta = 0;
		int delta = 0;
		for (ConnectedContent content : diffContents) {
			if(outputVarianceTypes.contains(content.getVarianceType())) {
				switch (content.getContentType()) {
				case CHANGE:
					text = insertRevised(text, content.getRevisedAsText(),
							(int) (totalDelta + content.getOriginal().peekLast().getEnd()));
					final int changeId = idCounter++;
					final int insertId = idCounter++;
					final int deleteId = idCounter++;
					final AnnotationWrapper change = convertChange(content, totalDelta, changeId, insertId, deleteId);
					if (change != null) {
						final AnnotationWrapper delete = convertDelete(content, totalDelta, deleteId);
						if (delete != null)
							annotations.add(delete);

						annotations.add(change);
						final AnnotationWrapper insert = convertInsert(content,
								(totalDelta + content.getOriginal().peekLast().getEnd()), insertId);
						if (insert != null)
							annotations.add(insert);

						delta = (int) content.getRevisedAsText().length();
						totalDelta += delta;
						moveByCurDelta(originalAnnotations, delta, insert.getBegin());
					}
					break;
				case INSERT:
					text = insertRevised(text, content.getRevisedAsText(), textPosition);
					final AnnotationWrapper insert = convertInsert(content, textPosition, idCounter++);
					if (insert != null)
						annotations.add(insert);

					delta = (int) content.getRevisedAsText().length();
					totalDelta += delta;
					moveByCurDelta(originalAnnotations, delta, textPosition);
					break;
				case DELETE:
					final AnnotationWrapper delete = convertDelete(content, totalDelta, idCounter++);
					if (delete != null)
						annotations.add(delete);
					break;
				case EQUAL: // Ignore
				}

			}
			// Move position
			LinkedList<Token> original = content.getOriginal();
			if (original.size() > 0)
				textPosition = original.getLast().getEnd() + totalDelta;
		}

		annotations.addAll(originalAnnotations);
		
		// ** Types **
		List<TypeWrapper> types = null;
		// Access private field types
		try {
			Field f = originalDocument.getClass().getDeclaredField("types");
			f.setAccessible(true);
			types = (List<TypeWrapper>) f.get(originalDocument);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			System.err.println("Error: TextAnnotationStruct has changed. Can't access types via reflection anymore.");
			e.printStackTrace();
		}
		types.addAll(getTypeWrapper());

		return new Gson().toJson(new TextAnnotationStruct(null, text, annotations, types));
	}

	private static String insertRevised(String text, String revised, int insertPosition) {
		return text.substring(0, insertPosition) + revised + text.substring(insertPosition);
	}

	/**
	 * Converts a connected content DELETE into a list of Athen annotations
	 * 
	 * @param content
	 *            DELETE connected content
	 * @param delta
	 *            Position delta between original text and merged text
	 * @return List of Athen annotations reflecting the delete
	 */
	private static AnnotationWrapper convertDelete(ConnectedContent content, int delta, int id) {
		if (!(content.getContentType().equals(ContentType.DELETE)
				|| content.getContentType().equals(ContentType.CHANGE)))
			throw new IllegalArgumentException("Can not convert Delete if content parameter is not a delete.");

		final LinkedList<Token> original = content.getOriginal();

		if (!original.isEmpty()) {
			// Delete Wrapper
			final HashMap<String, Object> deleteFeatures = new HashMap<>();
			deleteFeatures.put("variance-type", GROUPID + ".variance." + content.getVarianceType().toString());
			deleteFeatures.put("annotations", original.getFirst().getAnnotationsString());

			AnnotationWrapper deleteAnno = new AnnotationWrapper(GROUPID + ".type.DELETE",
					(int) original.getFirst().getBegin() + delta, (int) original.getLast().getEnd() + delta,
					deleteFeatures);
			deleteAnno.setId(id);
			return deleteAnno;
		}

		return null;
	}

	private static int moveByCurDelta(List<AnnotationWrapper> annotations, int delta, long textPosition) {
		if (delta > 0) {
			annotations.forEach(a -> {
				if (a.getBegin() >= textPosition)
					a.setBegin((int) (a.getBegin() + delta));

				if (a.getEnd() >= textPosition)
					a.setEnd((int) (a.getEnd() + delta));
			});
		}
		return delta;
	}

	private static AnnotationWrapper convertInsert(ConnectedContent content, long position, int id) {
		if (!(content.getContentType().equals(ContentType.INSERT)
				|| content.getContentType().equals(ContentType.CHANGE)))
			throw new IllegalArgumentException("Can not convert Insert if content parameter is not a insert.");

		final LinkedList<Token> revised = content.getRevised();

		if (!revised.isEmpty()) {
			// Insert Wrapper
			final HashMap<String, Object> insertFeatures = new HashMap<>();
			insertFeatures.put("variance-type", GROUPID + ".variance." + content.getVarianceType().toString());
			insertFeatures.put("annotations", revised.getFirst().getAnnotationsString());

			AnnotationWrapper insertAnno = new AnnotationWrapper(GROUPID + ".type.INSERT", (int) position,
					(int) (position + content.getRevisedAsText().length()), insertFeatures);
			insertAnno.setId(id);
			return insertAnno;
		}

		return null;
	}

	private static AnnotationWrapper convertChange(ConnectedContent content, int totalDelta, int changeId, int insertId,
			int deleteId) {
		if (!content.getContentType().equals(ContentType.CHANGE))
			throw new IllegalArgumentException("Can not convert Change if content parameter is not a change.");

		final LinkedList<Token> original = content.getOriginal();

		if (!original.isEmpty()) {
			// Change Wrapper
			final HashMap<String, Object> changeFeatures = new HashMap<>();
			changeFeatures.put("variance-type", GROUPID + ".variance." + content.getVarianceType().toString());
			changeFeatures.put("insert", new JSONID(insertId));
			changeFeatures.put("delete", new JSONID(deleteId));
			AnnotationWrapper changeAnno = new AnnotationWrapper(GROUPID + ".type.CHANGE",
					(int) original.getFirst().getBegin() + totalDelta,
					(int) (original.getLast().getEnd() + totalDelta + content.getRevisedAsText().length()),
					changeFeatures);
			changeAnno.setId(changeId);
			return changeAnno;
		}

		return null;
	}

	/**
	 * Get (and Init) Type Wrapper
	 * 
	 * @return
	 */
	private static Collection<TypeWrapper> getTypeWrapper() {
		if (diffTypes != null)
			return diffTypes;

		// Init type wrapper
		diffTypes = new HashSet<TypeWrapper>();

		// variance-type
		final String range_primitive = "RANGE_PRIMITIVE";
		final List<FeatureWrapper> insertDeleteFeatures = new ArrayList<FeatureWrapper>();
		insertDeleteFeatures.add(new FeatureWrapper("variance-type", range_primitive));
		insertDeleteFeatures.add(new FeatureWrapper("annotations", range_primitive));

		final String range_annotation = "RANGE_ANNOTATION";
		final List<FeatureWrapper> changeFeatures = new ArrayList<FeatureWrapper>();
		changeFeatures.add(new FeatureWrapper("delete", range_annotation));
		changeFeatures.add(new FeatureWrapper("insert", range_annotation));

		// INSERT,DELETE,CHANGE
		diffTypes.add(new TypeWrapper(GROUPID + ".type.INSERT", insertDeleteFeatures));
		diffTypes.add(new TypeWrapper(GROUPID + ".type.DELETE", insertDeleteFeatures));
		diffTypes.add(new TypeWrapper(GROUPID + ".type.CHANGE", changeFeatures));

		return diffTypes;
	}

	public static class JSONID {
		public int jsonId;

		public JSONID(int jsonId) {
			this.jsonId = jsonId;
		}
	}
}
