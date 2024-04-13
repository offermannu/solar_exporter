<%@page import="com.zfabrik.util.runtime.Foundation"%>

<p style="color: gray; font-size: x-small; padding-top: 1em; margin-top:2em; border-top:1px solid gray">Served from <c:out value="<%=Foundation.getProperties().getProperty(Foundation.PROCESS_WORKER)%>"/></p>
</body>
</html>
