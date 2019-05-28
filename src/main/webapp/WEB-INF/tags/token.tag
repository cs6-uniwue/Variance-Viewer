<%@tag description="Main Webpage Tag" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>
<%@ attribute name="token" required="true" type="de.uniwue.web.view.Content"%>
<c:choose>
	<c:when test="${token.getContentType() ne 'EQUAL'}">
	<span class="line-fragment ACTIVE trimmed <c:out value='${token.getContentType()} ${token.getVarianceType()}'/>">
			<c:forEach var="i" begin="0" end="${token.getHighlight().size()-1}" varStatus="status">
				<c:set var="highlight" value="${token.getHighlight().get(i)}"/>
				<c:set var="annotation" value="${token.getAnnotationsString()}"/>
				<c:set var="content" value="${token.getContent()}"/>
				<%-- Check for first highlight to include start to highlight --%>
				<c:if test="${i == 0 && highlight[0] > 0}">
					<span class="${token.getAnnotationsString()}"><c:out value="${content.substring(0,highlight[0])}"/></span>
				</c:if>
				<%-- Check for multiple highlights to include contents between the highlights --%>
				<c:if test="${i > 0}">
					<c:set var="oldHighlight" value="${token.getHighlight().get(i-1)}"/>
					<c:if test="${highlight[0] > oldHighlight[1]+1}">
						<span class="${annotation}"><c:out value="${content.substring(oldHighlight[1],highlight[0])}"/></span>
					</c:if>
				</c:if>
				<span class="highlight ${token.getAnnotationsString()}">
					<c:set var="highlightedText" value="${content.substring(highlight[0],highlight[1])}"/>
					<c:out value="${highlightedText}"/>
					<c:if test="${token.getVarianceType() eq 'SEPARATION' && highlightedText.length() > 0 && highlightedText.trim().length() == 0}">&nbsp;</c:if>
				</span>
				<c:if test="${status.isLast() && highlight[1] < content.length()}">
					<span class="${annotation}"><c:out value="${content.substring(highlight[1])}"/></span>
				</c:if>
			</c:forEach>
			<span class="fillerspace">&nbsp;</span>
	</span>
	</c:when>
	<c:otherwise>
	<span class="line-fragment ${token.getAnnotationsString()}">
		<c:out value="${token.getContent()}"/>
	</span>
	</c:otherwise>
</c:choose>
<jsp:doBody />