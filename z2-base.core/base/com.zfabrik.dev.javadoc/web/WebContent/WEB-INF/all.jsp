<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@page import="com.zfabrik.util.html.Escaper"%><html>
<head>
</head>
<body>
<h1>Javadoc Browser</h1>
<c:choose>
<c:when test="${notfound}">
<p>The Javadocs you requested could not be found. Please try any of the ones below:</p>
</c:when>
<c:otherwise>
<p>Available Java components. Please choose whether you want to look for API or implementation Javadocs by clicking on the corresponding link.</p>
</c:otherwise>
</c:choose>


<table style="border:none;">
<c:forEach items="${components}" var="c">
<c:if test="${apis.contains(c) || impls.contains(c)}">
<tr><td><c:out value="${c}"/></td>
<% 
String cn = Escaper.urlEncode((String) pageContext.getAttribute("c"),'!'); 
String u1 = request.getContextPath()+"/"+cn+"/api";
String u2 = request.getContextPath()+"/"+cn+"/impl";
%>
<c:choose>
    <c:when test="${apis.contains(c)}">
        <td><a href="<%=u1%>">API</a></td>
    </c:when>
    <c:otherwise>
        <td></td>
    </c:otherwise>
</c:choose>
<c:choose>
    <c:when test="${impls.contains(c)}">
		<td><a href="<%=u2%>">Impl</a></td>
    </c:when>
    <c:otherwise>
        <td></td>
    </c:otherwise>
</c:choose>
</tr>
</c:if>
</c:forEach>
</table>

</body>

</html>