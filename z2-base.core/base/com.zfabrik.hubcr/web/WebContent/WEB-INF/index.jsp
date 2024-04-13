<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@page import="java.io.PrintWriter"%>
<html>
<head>
<title>Hub CR Management Utility</title>
</head>
<body>
<h1>Hub Component Repository Management Utility</h1>
<a href="?">refresh</a>

<p>Currently: 
<c:choose>
<c:when test="${not empty revision}"><fmt:formatDate value="${revision}" type="both"/></c:when>
<c:otherwise>Initial (no DB)</c:otherwise>
</c:choose> 
</p>

<%-- handle error display --%>
<c:if test="${not empty errors}">
<c:forEach var="e" items="${errors}">
<p><pre style="color:red">
<% ((Exception) pageContext.getAttribute("e")).printStackTrace(new PrintWriter(out)); %>
</pre></p>
</c:forEach>
</c:if>

<c:if test="${not empty messages}">
<p><pre>
<c:forEach var="m" items="${messages}">
<c:out value="${m}"></c:out>
</c:forEach>
</pre></p>
</c:if>

<form action="?" method="post">
<input type="submit" name="scan" value="Scan and update Hub CR"/>
</form>

</body>
</html>