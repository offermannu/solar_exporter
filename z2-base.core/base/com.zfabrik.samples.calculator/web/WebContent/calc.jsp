<jsp:useBean id="calc" class="com.zfabrik.samples.calculator.impl.Calculator"
/><jsp:setProperty property="params" name="calc" value="${param}"
/><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Z2 Samples Calculator</title>
<style>
	.canvas {
		position: relative;
		background: #f9830c;
		margin: 100px auto 0;
		width: 300px;
		padding: 20px 10px 75px;
		border: 2px solid black;
	}
	.display {
		margin: 0px auto;
		text-align: center;
        font-family: sans;		
	}
	.keypad {
		margin: 10px auto;
	}
	h1 {
		font-size: large;
	}
	input {
		width: 250px;
		border: solid white;
		border-width: 5px 15px; 
		padding: 5px;
		background: grey;
		height: 32px;
		font-family: monospace;
		font-size: 17px;
		font-weight: bold;
		color: white;
		text-align: right;
	}
	button {
		width: 52px;
		margin: 10px;
		font-size: 14px;
		border: solid 1px white;
		padding: 4px 0;
		background: white;
	}
}
</style>
<script type="text/javascript">
	function handleKeyPress(evt) {
		evt = evt || window.event;
  		var chCode = evt.which || evt.keyCode;
		if (chCode == 13) {
			document.getElementById("enter").click();
		} else {
			var btns = document.getElementsByTagName("button");
			for (var idx = 0; idx < btns.length; idx++) {
				if (btns[idx].attributes.title.nodeValue.charCodeAt(0) == chCode) {
					btns[idx].click();
				}
			}		
		}
	}
</script>
</head>
<body onkeypress="handleKeyPress(event)">
	<form method="post">
		<div class="canvas">
			<div class="display">
				<h1>Z2 Sample Calculator</h1>
				<input id="result" readonly="readonly" value="${calc.result}"/>
			</div>
			
			<div class="keypad">
				<!--  	
					<button name="op" value="b"> does not work in IE7! 
					We code the op-value into the name: <button name="op=b"> which requires just a simply split on the server side.
					the num field is fine because value equals inner-text  
				-->
				<button type="submit" name="op=open" title="(">(</button>
				<button type="submit" name="op=close" title=")">)</button>
				<button type="submit" name="op=C" title="c">C</button>
				<button type="submit" name="op=CE" title="C">CE</button>
	
	<!-- uncomment me
				<button type="submit" name="op=sqrt" title="r">&radic;</button>
				<button type="submit" name="op=pow" title="^">x<sup>y</sup></button>
				<button type="submit" name="op=ln" title="l">ln</button>
				<button type="submit" name="op=epowx" title="e">e<sup>x</sup></button>
	-->
				<button type="submit" name="op=pi" title="p">&pi;</button>
				<button type="submit" name="op=inv" title="i">1/x</button>
				<button type="submit" name="op=sqr" title="s">x&sup2;</button>
				<button type="submit" name="op=div" title="/">/</button>
	
				<button type="submit" name="num" value="7" title="7">7</button>
				<button type="submit" name="num" value="8" title="8">8</button>
				<button type="submit" name="num" value="9" title="9">9</button>
				<button type="submit" name="op=mult" title="*">*</button>
	
				<button type="submit" name="num" value="4" title="4">4</button>
				<button type="submit" name="num" value="5" title="5">5</button>
				<button type="submit" name="num" value="6" title="6">6</button>
				<button type="submit" name="op=sub" title="-">&minus;</button>
	
				<button type="submit" name="num" value="1" title="1">1</button>
				<button type="submit" name="num" value="2" title="2">2</button>
				<button type="submit" name="num" value="3" title="3">3</button>
				<button type="submit" name="op=add" title="+">+</button>
	
				<button type="submit" name="num" value="0" title="0">0</button>
				<button type="submit" name="op=dot" title=".">.</button>
				<button type="submit" name="op=sgn" title="#">&plusmn;</button>
				<button id="enter" type="submit" name="op=eq" title="=">=</button>
			</div>
			
			<div style="clear: both;"/>
			<input type="hidden" name="stack" value="${calc.stack}"/>
			<input type="hidden" name="serStack" value="${calc.serStack}"/>
		</div>
	</form>
</body>

</html>