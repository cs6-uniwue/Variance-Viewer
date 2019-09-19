<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:webpage>
	<style>
	// Colors
	<c:forEach items="${variancetypes}" var="variancetype">
		.display-switches .display-switch-checkbox label.${variancetype.getName()} {
			background: ${variancetype.getColor()}ff;
		}
		.lineSet .line .line-fragment.ACTIVE.${variancetype.getName()}.CHANGE .highlight {
			background-color: ${variancetype.getColor()}ff;
		}
		.lineSet .line .line-fragment.ACTIVE.${variancetype.getName()}.INSERT .highlight {
			background-color: ${variancetype.getColor()}cc;
		}
		.lineSet .line .line-fragment.ACTIVE.${variancetype.getName()}.DELETE .highlight {
			background-color: ${variancetype.getColor()}cc;
		}
		.lineSet .line .line-fragment.ACTIVE.${variancetype.getName()} span:not(.fillerspace) {
			background-color: ${variancetype.getColor()}77;
		}
	</c:forEach>
	</style>

	<article id="legend">
		<button class="openDownload">Download <u>⇓</u></button>
		<button id="statistics-info" class="openStatistics">Statistics
			<svg
				xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:cc="http://creativecommons.org/ns#"
				xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
				xmlns:svg="http://www.w3.org/2000/svg"
				xmlns="http://www.w3.org/2000/svg"
				width="206mm"
				height="206mm"
				viewBox="0 0 206 206"
				version="1.1"
				id="statistics-icon">
				<path
				transform="scale(0.26458332)"
				d="M 379.59375 6.8652344 C 170.09455 6.8651663 0.0019153297 176.95783 0.001953125 386.45703 C 0.0028602116 595.95561 170.09523 766.0489 379.59375 766.04883 C 464.98936 766.04847 543.83531 737.7836 607.28516 690.11133 L 687.45703 770.2832 A 28.349291 28.349291 0 0 0 727.54492 770.2832 L 769.01953 728.80664 A 28.349291 28.349291 0 0 0 769.01953 688.71875 L 687.99219 607.68945 C 732.78802 545.38303 759.1852 468.97885 759.18555 386.45703 C 759.18558 176.9585 589.09233 6.8661301 379.59375 6.8652344 z M 379.59375 71.353516 C 553.76414 71.353629 694.69802 212.28669 694.69727 386.45703 C 694.69715 560.62681 553.76353 701.56043 379.59375 701.56055 C 205.42342 701.56134 64.498122 560.62742 64.498047 386.45703 C 64.497291 212.28606 205.42282 71.352752 379.59375 71.353516 z M 492.60742 230.69141 A 28.349291 28.349291 0 0 0 464.26172 259.03711 L 464.26172 522.50586 A 28.349291 28.349291 0 0 0 492.60742 550.85156 L 551.33203 550.85156 A 28.349291 28.349291 0 0 0 579.67773 522.50586 L 579.67773 259.03711 A 28.349291 28.349291 0 0 0 551.33203 230.69141 L 492.60742 230.69141 z M 341.19141 316.01172 A 28.349291 28.349291 0 0 0 312.84375 344.35742 L 312.84375 532.48438 A 28.349291 28.349291 0 0 0 341.19141 560.83203 L 399.89258 560.83203 A 28.349291 28.349291 0 0 0 428.23828 532.48438 L 428.23828 344.35742 A 28.349291 28.349291 0 0 0 399.89258 316.01172 L 341.19141 316.01172 z M 189.78125 390.08789 A 28.349291 28.349291 0 0 0 161.43359 418.43555 L 161.43359 541.22461 A 28.349291 28.349291 0 0 0 189.78125 569.57227 L 248.44531 569.57227 A 28.349291 28.349291 0 0 0 276.79102 541.22461 L 276.79102 418.43555 A 28.349291 28.349291 0 0 0 248.44531 390.08789 L 189.78125 390.08789 z "
				style="stroke-width:56.69291687;stroke-linecap:round;stroke-linejoin:round;" />
			</svg>
		</button>
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
					<input id="checkbox${variancetype.getName()}" type="checkbox" class="display-switch" data-type="${variancetype.getName()}" checked="checked">
					<label for="checkbox${variancetype.getName()}" class="switch ${variancetype.getName()}"></label>
				</span>
				<c:out value="${variancetype.getName()}"></c:out>
			</li>
			</c:forEach>
		</ul>
	</article>
	<article id="compare-container">
		<p class="title">
			<c:out value="${document1name}" />
			<a href="#" class="documenttype openHelp">
				(<c:out value="${document1type}" />)
			</a>
		</p>
		<p class="title">
			<c:out value="${document2name}" />
			<a href="#" class="documenttype openHelp">
				(<c:out value="${document2type}" />)
			</a>


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
							<t:token token="${content}"/>
						</c:forEach>
						&nbsp;
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
						&nbsp;
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
							<input id="checkbox${variancetype.getName()}" type="checkbox" class="display-switch" data-type="${variancetype.getName()}" checked="checked">
							<label for="checkbox${variancetype.getName()}" class="switch ${variancetype.getName()}"></label>
						</span>
						<c:out value="${variancetype.getName()}"></c:out>
					</li>
					</c:forEach>
				</ul> 
			</section>
		</form>
		<section id="browser-warning" class="warning">Your browser does not completely support this format.<br> You may need to manually set the save name and enable "Print&nbspBackground&nbspColor" in your print options.</section>
		<section id="format-warning" class="warning">The compared documents are not both of type TEI.<br>It is possible that the resulting TEI may not be valid, since it can contain invalid symbols inside the text blocks e.g. "&lt;", "&gt;".</section>
		<section id="download-confirm">
			<button id="download-cancel" type="button">Cancel</button>
			<button id="download-save" type="button">Save</button>
		</section>
	</article>
	<article id="help">
		<section id="help-header">
			Document type help
			<hr>
		</section>
		<section id="help-body">
		The Variance Viewer is build to compare the content of TEI documents or plain text documents.
		</br>
		Comparing two TEI documents will thereby read and interpret the documents, 
		with all "rend" attributes being interpreted as typographical changes in a TEI document.
		</br>
		The comparison will, if one or both documents are not of type TEI, compare both documents as plain text documents. 
		</section>
		<section id="help-confirm">
			<button id="help-close" type="button">Close</button>
		</section>
	</article>
	<article id="statistics">
		<section id="statistics-header">
			Statistics
			<hr>
		</section>
		<section id="statistics-body">
			<c:forEach items="${statistics.getVarianceCounts()}" var="count">
				<span>${count.key.getName()}</span>
				<div style="width:${100*count.value/statistics.getGlobalChanges()}%;background-color:${count.key.getColor()}"></div>
				<span style="text-align:right">${count.value}/${statistics.getGlobalChanges()}</span>
				<span>(${Math.round(100*count.value/statistics.getGlobalChanges()*100)/100}%)</span>
			</c:forEach>
		</section>
		<section id="statistics-confirm">
			<button id="statistics-close" type="button">Close</button>
		</section>
	</article>
	<script type="text/javascript">
		var format = "${format}";
		var exportJSON = ${exportJSON};
	</script>
	<script type="text/javascript" src='<c:url value="/resources/js/underscore-min.js" />'></script>
	<script type="text/javascript" src='<c:url value="/resources/js/viewer.js" />'></script>
</t:webpage>