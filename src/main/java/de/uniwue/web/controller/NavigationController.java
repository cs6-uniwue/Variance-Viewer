package de.uniwue.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import de.uniwue.compare.Annotation;
import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.Diff;
import de.uniwue.compare.Settings;
import de.uniwue.translate.DiffExporter;
import de.uniwue.translate.TEIToAthenConverter;
import de.uniwue.wa.server.editor.TextAnnotationStruct;
import de.uniwue.web.view.LineCreator;
import difflib.PatchFailedException;
import de.uniwue.compare.VarianceType;

@Controller
public class NavigationController {
	@Autowired
	ServletContext servletContext;
	List<VarianceType> outputVarianceTypes = Arrays.asList(VarianceType.CONTENT, VarianceType.ABBREVIATION,
															VarianceType.GRAPHEMICS, VarianceType.NONE,
															VarianceType.PARATEXT, VarianceType.PUNCTUATION,
															VarianceType.TYPOGRAPHY);

	@RequestMapping(value = "/")
	public String home(Model model) {
		return "home";
	}

	@RequestMapping(value = "/home")
	public String home2(Model model) {
		return "redirect:/";
	}

	@RequestMapping(value = "/view", method = RequestMethod.POST)
	public String view(Model model, @RequestParam("file1") MultipartFile file1,
			@RequestParam("file2") MultipartFile file2,
			@RequestParam(value = "settingsFile", required = false) MultipartFile settingsFile) {
		if (!file1.isEmpty() && !file2.isEmpty()) {
			// Read normalize files / settings
			Settings settings;
			if(!settingsFile.isEmpty()) {
				try {
					String settingsContent = StorageManager.getSettings(settingsFile, servletContext);
					settings = new Settings(settingsContent);
				} catch(IllegalArgumentException e) {
					// Invalid settingsString
					model.addAttribute("warning", "Invalid settings file. " + e.getMessage() + " Redirected to home.");
					return home(model);
				}
			} else {
				settings = new Settings(StorageManager.getDefault(servletContext));
			}

			// Compare document files
			try {
				String file1Type = file1.getContentType();
				String file2Type = file2.getContentType();

				boolean filesAreTEI = file1Type.equals("text/xml") && file2Type.equals("text/xml")
						&& TEIToAthenConverter.isTEI(file1.getInputStream())
						&& TEIToAthenConverter.isTEI(file2.getInputStream());

				if (filesAreTEI) {
					TextAnnotationStruct document1 = TEIToAthenConverter.convertTEIToAthen(file1.getInputStream());
					TextAnnotationStruct document2 = TEIToAthenConverter.convertTEIToAthen(file2.getInputStream());
					Collection<Annotation> annotations1 = document1.getAnnotations().stream()
							.map(a -> new Annotation(a)).collect(Collectors.toList());
					Collection<Annotation> annotations2 = document2.getAnnotations().stream()
							.map(a -> new Annotation(a)).collect(Collectors.toList());
					List<ConnectedContent> differences = Diff.compareXML(document1.getText(), document2.getText(),
							annotations1, annotations2, settings);

					model.addAttribute("format", "tei");
					model.addAttribute("exportJSON", DiffExporter.convertToAthenJSONString(document1, differences, outputVarianceTypes));
					model.addAttribute("allLines", LineCreator.patch(differences));
				} else {
					// Interpret as plain text
					String content1 = new String(file1.getBytes(), "UTF-8");
					String content2 = new String(file2.getBytes(), "UTF-8");
					List<ConnectedContent> differences = Diff.comparePlainText(content1, content2, settings);

					model.addAttribute("format", "txt");
					model.addAttribute("exportJSON", DiffExporter.convertToAthenJSONString(content1, differences, outputVarianceTypes));
					model.addAttribute("allLines", LineCreator.patch(differences));
				}
				List<VarianceType> variancetypes = new ArrayList<VarianceType>();
				for (VarianceType variancetype : VarianceType.values())
					if (!variancetype.equals(VarianceType.NONE))
						variancetypes.add(variancetype);

				model.addAttribute("variancetypes", variancetypes);
				model.addAttribute("document1name", file1.getOriginalFilename());
				model.addAttribute("document2name", file2.getOriginalFilename());
				model.addAttribute("externalCSS", settings.getExternalCss());
			} catch (IOException e1) {
				return "redirect:/404";
			} catch (PatchFailedException e) {
				return "redirect:/404";
			}

			return "view";
		} else {
			return "redirect:/404";
		}
	}

	@RequestMapping(value = "/view", method = RequestMethod.GET)
	public String view2(Model model) {
		return "redirect:/";
	}

	@RequestMapping(value = "/default", method = RequestMethod.GET)
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

}
