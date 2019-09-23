package de.uniwue.web.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import de.uniwue.compare.Annotation;
import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.Diff;
import de.uniwue.compare.DocumentType;
import de.uniwue.compare.Settings;
import de.uniwue.compare.variance.VarianceClassifier;
import de.uniwue.compare.variance.VarianceStatistics;
import de.uniwue.compare.variance.types.Variance;
import de.uniwue.translate.DiffExporter;
import de.uniwue.translate.TEIToAthenConverter;
import de.uniwue.translate.XMLCleaner;
import de.uniwue.wa.server.editor.TextAnnotationStruct;
import de.uniwue.web.view.LineCreator;

@Controller
public class NavigationController {
	@Autowired
	ServletContext servletContext;

	@RequestMapping(value = "/")
	public String home(Model model) {
		return "home";
	}

	@RequestMapping(value = "/home")
	public String home2(Model model) {
		return "redirect:/";
	}

	@RequestMapping(value = "/view", method = RequestMethod.POST)
	public String view(Model model,
			@RequestParam("file1") MultipartFile file1,
			@RequestParam("file2") MultipartFile file2,
			@RequestParam("settings") String settingsType,
			@RequestParam(value = "settingsFile", required = false) MultipartFile settingsFile) {

		if (!file1.isEmpty() && !file2.isEmpty()) {
			// Read normalize files / settings
			Settings settings = null;
			switch(settingsType) {
			case "default":
				settings = new Settings(StorageManager.getDefault(servletContext));
				break;
			case "user":
				if (!settingsFile.isEmpty()) {
					try {
						settings = new Settings(StorageManager.getSettings(settingsFile, servletContext));
					} catch (IllegalArgumentException e) {
						// Invalid settingsString
						model.addAttribute("warning", "Invalid settings file. " + e.getMessage() + " Redirected to home.");
						return home(model);
					}
				} else {
					model.addAttribute("warning", "User settings file was selected, but now file has been provided. Redirected to home.");
					return home(model);
				}
				break;
			default: 
				model.addAttribute("warning", "Unkown settings file was selected. Redirected to home.");
				return home(model);
			}

			// Compare document files
			try {
				String file1Type = file1.getContentType();
				String file2Type = file2.getContentType();
				String tei1Content = "";
				String tei2Content = "";
				// Base document type
				DocumentType document1Type = file1Type.equals("text/xml") ? DocumentType.XML : DocumentType.PLAINTEXT;
				DocumentType document2Type = file2Type.equals("text/xml") ? DocumentType.XML : DocumentType.PLAINTEXT;
				
				// Check for TEI document type
				if (document1Type.equals(DocumentType.XML)) {
					tei1Content = XMLCleaner.clean(new String(file1.getBytes(), "UTF-8"));
					document1Type = TEIToAthenConverter.isTEI(new ByteArrayInputStream(tei1Content.getBytes()))
							? DocumentType.TEI
							: DocumentType.XML;
				} else {
					tei1Content = new String(file1.getBytes(), "UTF-8");
				}
				if (document2Type.equals(DocumentType.XML)) {
					tei2Content = XMLCleaner.clean(new String(file2.getBytes(), "UTF-8"));
					document2Type = TEIToAthenConverter.isTEI(new ByteArrayInputStream(tei2Content.getBytes()))
							? DocumentType.TEI
							: DocumentType.XML;
				} else {
					tei2Content = new String(file2.getBytes(), "UTF-8");
				}

				// Compare and create response
				return compare(model,
						file1.getOriginalFilename(),
						file2.getOriginalFilename(),
						tei1Content, tei2Content,
						document1Type, document2Type,
						settings);
				
			} catch (IOException e1) {
				return "redirect:/404";
			}
		} else {
			return "redirect:/404";
		}
	}

	@RequestMapping(value = "/view", method = RequestMethod.GET)
	public String view2(Model model) {
		return "redirect:/";
	}

	@RequestMapping(value = "demo/view", method = RequestMethod.GET)
	public String demoview(Model model, @RequestParam("demo") int demo) {
		// Initialize
		File file1 = null;
		File file2 = null;
		Settings settings;
		DocumentType document1Type = DocumentType.PLAINTEXT;
		DocumentType document2Type = DocumentType.PLAINTEXT;
		final String fs = File.separator;
		
		// Select demo
		switch(demo) {
		case 0:
			file1 = StorageManager.getFile("demo"+ fs +"demo1"+ fs +"test1.txt", servletContext);
			file2 = StorageManager.getFile("demo"+ fs +"demo1"+ fs +"test2.txt", servletContext);
			settings = new Settings(StorageManager.getDefault(servletContext));
			break;
		default:
			return "redirect:/404";
		}

		// Load demo
		try {
			// Compare and create response
			return compare(model,
					file1.getName(),
					file2.getName(),
					new String(Files.readAllBytes(file1.toPath()), StandardCharsets.UTF_8),
					new String(Files.readAllBytes(file2.toPath()), StandardCharsets.UTF_8),
					document1Type, document2Type,
					settings);
		} catch (IOException e) {
			e.printStackTrace();
			return "redirect:/404";
		}
	}
	
	@RequestMapping(value = "demo/download/demo{demo}.zip", method = RequestMethod.GET)
	public ResponseEntity<byte[]> downloaddemo(Model model, @PathVariable("demo") int demo) {
		// Initialize
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		File file1 = null;
		File file2 = null;
		File settings;
		final String fs = File.separator;
		
		// Select demo
		switch(demo) {
		case 0:
			file1 = StorageManager.getFile("demo"+ fs +"demo1"+ fs +"test1.txt", servletContext);
			file2 = StorageManager.getFile("demo"+ fs +"demo1"+ fs +"test2.txt", servletContext);
			settings = StorageManager.getFile("defaults.txt", servletContext);
			break;
		default:
			return new ResponseEntity<byte[]>(new byte[0], headers, HttpStatus.BAD_REQUEST);
		}

		try {
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ZipOutputStream zip = new ZipOutputStream(baos);
		    addToZip(zip, file1);
		    addToZip(zip, file2);
		    addToZip(zip, settings);
		    zip.close();
    		
			headers.setContentType(MediaType.TEXT_PLAIN);
			headers.setContentLength(baos.toByteArray().length);
			return new ResponseEntity<byte[]>(baos.toByteArray(), headers, HttpStatus.OK);
		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity<byte[]>(new byte[0], headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private void addToZip(ZipOutputStream zip, File file) throws IOException {
		    byte[] filebyte = Files.readAllBytes(file.toPath());
		    ZipEntry entry = new ZipEntry(file.getName());
		    entry.setSize(filebyte.length);
		    
		    zip.putNextEntry(entry);
		    zip.write(filebyte);
		    zip.closeEntry();
	}
	
	@RequestMapping(value = "/default.txt", method = RequestMethod.GET)
	public ResponseEntity<byte[]> defaultSettings(Model model) {
		final String defaultSettings = StorageManager.getDefault(servletContext);
		final byte[] settingsBytes = defaultSettings.getBytes();

		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.setContentType(MediaType.TEXT_PLAIN);
		headers.setContentLength(settingsBytes.length);

		return new ResponseEntity<byte[]>(settingsBytes, headers, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/404")
	public String error404(Model model) {
		model.addAttribute("warning", "Unable to find requested page. Redirected to home.");
		return home(model);
	}

	@RequestMapping(value = "/400")
	public String error400(Model model) {
		model.addAttribute("warning", "Unable to understand your request. Redirected to home.");
		return home(model);
	}

	@RequestMapping(value = "/500")
	public String error500(Model model) {
		model.addAttribute("warning", "Sorry the server had an internal error. Redirected to home.");
		return home(model);
	}

	@Bean(name = "multipartResolver")
	public CommonsMultipartResolver multipartResolver() {
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxUploadSize(10485760);
		return multipartResolver;
	}

	
	public String compare(Model model,
			String filename1, String filename2,
			String file1, String file2,
			DocumentType document1Type, DocumentType document2Type,
			Settings settings) {

		List<Variance> variancetypes = new ArrayList<>(settings.getVariances());
		List<String> outputVarianceTypes = variancetypes.stream().map(v -> v.getName()).collect(Collectors.toList());

		// Compare document files
		if (document1Type.equals(DocumentType.TEI) && document2Type.equals(DocumentType.TEI)) {
			// Convert to Athen
			TextAnnotationStruct document1 = TEIToAthenConverter
					.convertTEIToAthen(new ByteArrayInputStream(file1.getBytes()));
			TextAnnotationStruct document2 = TEIToAthenConverter
					.convertTEIToAthen(new ByteArrayInputStream(file2.getBytes()));

			Collection<Annotation> annotations1 = document1.getAnnotations().stream()
					.map(a -> new Annotation(a)).collect(Collectors.toList());
			Collection<Annotation> annotations2 = document2.getAnnotations().stream()
					.map(a -> new Annotation(a)).collect(Collectors.toList());

			// Find differences
			List<ConnectedContent> differences = Diff.compareXML(document1.getText(), document2.getText(),
					annotations1, annotations2, settings);

			model.addAttribute("exportJSON",
					DiffExporter.convertToAthenJSONString(document1, differences, outputVarianceTypes));
			model.addAttribute("allLines", LineCreator.patch(differences));
			
			model.addAttribute("statistics", new VarianceStatistics(differences, settings.getVariances()));
		} else {
			// Interpret as plain text
			String content1 = file1;
			content1 = content1.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
			String content2 = file2;
			content2 = content2.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
			List<ConnectedContent> differences = Diff.comparePlainText(content1, content2, settings);

			outputVarianceTypes.remove(Variance.TYPOGRAPHY.getName());
			
			model.addAttribute("exportJSON",
					DiffExporter.convertToAthenJSONString(content1, differences, outputVarianceTypes));
			model.addAttribute("allLines", LineCreator.patch(differences));

			model.addAttribute("statistics", new VarianceStatistics(differences, settings.getVariances()));

			variancetypes.remove(Variance.TYPOGRAPHY);

		}
		model.addAttribute("variancetypes", VarianceClassifier.sortVariances(variancetypes));
		model.addAttribute("document1name", filename1);
		model.addAttribute("document2name", filename2);
		model.addAttribute("document1type", document1Type);
		model.addAttribute("document2type", document2Type);
		model.addAttribute("externalCSS", settings.getExternalCss());
		return "view";
	}
}
