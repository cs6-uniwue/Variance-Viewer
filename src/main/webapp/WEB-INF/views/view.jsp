<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:webpage>
	<article id="legend">
		<button class="openDownload">Download</button>
		<ul class="display-switches">
			<li class="display-switch-wrapper ALL">
				<span class="display-switch-checkbox">
					<input id="checkboxALL" type="checkbox" class="display-switch-all" checked="checked">
					<label for="checkboxALL" class="switch ALL"></label>
				</span>
				<c:out value="ALL"></c:out>
			</li>
			<c:forEach items="${variancetypes}" var="variancetype">
			<li class="display-switch-wrapper">
				<span class="display-switch-checkbox">
					<input id="checkbox${variancetype}" type="checkbox" class="display-switch" data-type="${variancetype}" checked="checked">
					<label for="checkbox${variancetype}" class="switch ${variancetype}"></label>
				</span>
				<c:out value="${variancetype}"></c:out>
			</li>
			</c:forEach>
		</ul>
	</article>
	<article id="compare-container">
		<p class="title">
			<c:out value="${document1name}" />
		</p>
		<p class="title">
			<c:out value="${document2name}" />
		</p>
		<div class="pagetop"></div>
		<div class="pagetop"></div>
		<c:forEach items="${allLines}" var="connectedLines">
			<div class="lineSet">
				<c:forEach items="${connectedLines.getOriginal()}" var="line">
					<div class="line">
						<div class="line-nr"><c:out value="${line.getLineNr()}"></c:out></div>
						<div class="line-content">
						<c:forEach items="${line.getContent()}" var="content">
							<t:token token="${content}" />
						</c:forEach>
						</div>
					</div>
				</c:forEach>
			</div>
			<div class="lineSet">
				<c:forEach items="${connectedLines.getRevised()}" var="line">
					<div class="line">
						<div class="line-nr"><c:out value="${line.getLineNr()}"></c:out></div>
						<div class="line-content">
						<c:forEach items="${line.getContent()}" var="content">
							<t:token token="${content}" />
						</c:forEach>
						</div>
					</div>
				</c:forEach>
			</div>
		</c:forEach>
	</article> 

	<article id="blur"></article>	
	<article id="download">
		<section id="download-header">
			Save Result
			<hr>
		</section>
		<form>
			<section id="download-settings">
				<span>Save as:</span> 
				<input type="text" name="filename" id="download-filename" value="Variance_${document1name}_${document2name}">
				<span>Format:</span> 
				<select name="filetype" id="download-filetype">
					<option value="tei">TEI (XML)</option>
					<option value="jsonAthen">Athen (JSON)</option>
					<option value="pdf">PDF</option>
				</select>

			</section>
			<section id="download-switches">
				<ul class="display-switches">
					<li class="display-switch-wrapper ALL">
						<span class="display-switch-checkbox">
							<input id="checkboxALL" type="checkbox" class="display-switch-all" checked="checked">
							<label for="checkboxALL" class="switch ALL"></label>
						</span>
						<c:out value="ALL"></c:out>
					</li>
					<c:forEach items="${variancetypes}" var="variancetype">
					<li class="display-switch-wrapper">
						<span class="display-switch-checkbox">
							<input id="checkbox${variancetype}" type="checkbox" class="display-switch" data-type="${variancetype}" checked="checked">
							<label for="checkbox${variancetype}" class="switch ${variancetype}"></label>
						</span>
						<c:out value="${variancetype}"></c:out>
					</li>
					</c:forEach>
				</ul> 
			</section>
		</form>
		<section id="browser-warning">Your browser does not completely support this format.<br> You may need to manually set the save name and enable "Print&nbspBackground&nbspColor" in your print options.</section>
		<section id="download-confirm">
			<button id="download-cancel" type="button">Cancel</button>
			<button id="download-save" type="button">Save</button>
		</section>
	</article>
	<script type="text/javascript">
		var format = "${format}";
		var exportJSON = ${exportJSON};
	</script>
	<script type="text/javascript" src="resources/js/underscore-min.js"></script>
	<script type="text/javascript" src="resources/js/webEditorIO.js"></script>
	<script type="text/javascript" src="resources/js/viewer.js"></script>
</t:webpage>