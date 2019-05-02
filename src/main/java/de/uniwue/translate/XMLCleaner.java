package de.uniwue.translate;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMLCleaner {
	public static InputStream clean(InputStream inputStream) {
		// Read into String
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.out.println(e);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e);
		}

		// Clean xml string
		String xmlString = clean(stringBuilder.toString());

		// Convert back to inputStream
		return new ByteArrayInputStream(xmlString.getBytes());
	}

	public static String clean(String xmlString) {
		// Extract xml header before removing processing instructions
		Pattern pattern = Pattern.compile("<\\?xml(?:[^<>])*\\?>");
        Matcher matcher = pattern.matcher(xmlString);
        String header = matcher.find() ? matcher.group() : "";
    
		// Remove Processing Instructions <?*?>
		// (Included in Athen TEI String but not in annotations to Athen TEI)
		String cleanXML = xmlString.replaceAll("<\\?(?:[^<>])*\\?>", "");

		// Remove comments <!--.*-->
		cleanXML = cleanXML.replaceAll("<!--.*-->", "");
		
		// Reattach header
		cleanXML = header + cleanXML;

		// Fuzzy cleaning with error correction
		cleanXML = cleanXML.replaceAll("<<", "<");
		cleanXML = cleanXML.replaceAll(">>", ">");

		return cleanXML;
	}

}
