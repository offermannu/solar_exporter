<%@page session="false" %>
<%@include file="include/decl.jsp"%>
<%
String sbase = request.getParameter("base");
long base=0;
if (sbase!=null && (sbase=sbase.trim()).length()>0) {
  base = Long.parseLong(sbase);
}
%>
<html>
<head>
<title><c:out value="${title}" /></title>
<link rel="stylesheet" type="text/css" href="${microweb_context_path}/mimes/bare.css" /> 
<style>
#control {
    position:absolute; 
    padding: 0.5em 0.5em 0.5em 0.5em; 
    display: flex;
    align-items: center;
    top:0; 
    left:0; 
    right:0; 
    height:1.5em;
    font-family: sans;
    background-color: #f9830c;
    vertical-align:middle;
}
#control SPAN {
    font-size: 70%;
    margin: 0 0.5em 0 0.5em;
}
#control button {
    border: solid 1px white;
    flex-grow: 0;
    margin: 0 0.2em;
}
#control button.right  {
    margin-left: auto;
}
#content {
    overflow: auto; 
    top: 3em; 
    bottom:0.5em; 
    left:0; 
    right:0;
    padding: 0 1em 5px 1em; 
    position: absolute; 
}
#content pre {
    margin: 0px;
}
</style>
<script type="text/javascript">

var bufferLength=10000;
var pollInterval=1000;
var base = ${base};
var active=false;
var errored=false;

function init() {
	start();
}

function start() {
	if (!active) {
		if (base<0) {
			info("Log streaming not available (handler not registered)!");
		} else {
		    active=true;
			plan();
			info("Log streaming active");
		}
	}
}

function plan() {
    if (active) {
        progress();
       	setTimeout(poll,pollInterval);
    } else {
    	unprogress();    	
    }
}

function stop() {
    if (active) {
    	active=false;
        plan();
        info("Log streaming paused");
    }
}

function clearContent() {
    var content = document.getElementById("content");
	while (content.firstChild) {
	    content.removeChild(content.firstChild);
    }
}

function poll() {
	get(
	   '<c:url value=""><c:param name="getLines"/><c:param name="base"/></c:url>'+base,
	   function(xhr, event) {
            // ok
            var result = JSON.parse(xhr.responseText);
            if (result.actualBase!=base) {
                appendContent("WARNING: Missed "+(result.actualBase-base)+" lines"); 
            }
            for (var i=0;i<result.lines.length;i++) {
                appendContent(result.lines[i]);
            }
            base = result.actualBase+result.lines.length;
	        plan();
	    },
	    function(xhr, event) {
	        plan();
	    }
    );	
}

function sync() {
    get('<c:url value=""><c:param name="sync"/></c:url>');  
}

function get(url, success, error) {
    var xhr = new XMLHttpRequest();
    xhr.addEventListener("error", function(event) {
        appendContent("ERROR: "+xhr.status+ "/"+ xhr.statusText);
        errored=true;
        if (error) { error.call(this, xhr, event); }
    });
    xhr.addEventListener("abort", function(event) {
        appendContent("ERROR: "+xhr.status+ "/"+ xhr.statusText); 
        errored=true;
        if (error) { error.call(this, xhr, event); }
    });
    xhr.addEventListener("load", function(event) {
        // response received
        if (xhr.status==200) {
            if (errored) {
                appendContent("INFO: Reconnected"); 
                errored=false;
            }
            if (success) { success.call(this, xhr, event); }
        } else {
            appendContent("ERROR: "+xhr.status+ "/"+ xhr.statusText); 
            errored=true;
            if (error) { error.call(this, xhr, event); }
        }
    }); 
    xhr.open('GET',url ,true);
    xhr.send();
}

function appendContent(line) {
	var content = document.getElementById("content");
	
	// cut off head
	if (content.childNodes.length>bufferLength) {
		content.removeChild(content.firstChild);
	}
	
	// append to end
    var pre = document.createElement("pre");
    var text = document.createTextNode(line);
    pre.appendChild(text); 
    content.appendChild(pre)
    content.scrollTop = content.scrollHeight;
}

function info(line) {
	var info = document.getElementById("info");
    info.innerHTML=line;
}

var p1 = "\u25c9";
var p2 = "\u25cb";

function progress() {
    var e = document.getElementById("progress");
    if (p1==e.innerHTML) {
    	e.innerHTML=p2;
    } else {
        e.innerHTML=p1;
    }
}

function unprogress() {
    var e = document.getElementById("progress");
    e.innerHTML="";
}

</script>
</head>
<body style="border:none" onload="init();">
<div id="control">
<button onclick="start()">Continue</button>
<button onclick="stop()">Stop</button>
<button onclick="clearContent()">Clear</button>
<span id="info"></span>
<span id="progress"></span>
<button class="right" onclick="sync()">Sync</button>
<a href="<c:url value="admin"/>"><button>Adm</button></a>
</div>
<div id="content"></div>
</body>
</html>
