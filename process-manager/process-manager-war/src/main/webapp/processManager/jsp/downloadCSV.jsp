<%@ include file="checkProcessManager.jsp" %>
<%
	String 	csvFileName = (String) request.getAttribute("CSVFilename");
	Long 	csvFileSize = (Long) request.getAttribute("CSVFileSize");
	String 	csvFileURL 	= (String) request.getAttribute("CSVFileURL");
%>

<html>
	<head>
		<%
			out.println(gef.getLookStyleSheet());
		%>
	</head>
	<body>
		<%
			browseBar.setDomainName(spaceLabel);
		  	browseBar.setComponentName(componentLabel);
			browseBar.setExtraInformation(resource.getString("GML.export"));
			
			out.println(window.printBefore());
			out.println(frame.printBefore());
			out.println(board.printBefore());
		%>
		<table>
			<tr>
				<td class="txtlibform"><%=resource.getString("GML.size")%> :</td>
				<td><%=FileRepositoryManager.formatFileSize(csvFileSize.longValue())%></td>
			</tr>
			<tr>
				<td class="txtlibform"><%=resource.getString("GML.csvFile")%> :</td>
				<td><a href="<%=csvFileURL%>"><%=csvFileName%></a></td>
			</tr>
		</table>
		<%
			out.println(board.printAfter());
			ButtonPane buttonPane = gef.getButtonPane();
			Button button = (Button) gef.getFormButton(resource.getString("GML.close"), "javaScript:window.close();", false);
			buttonPane.addButton(button);
			out.println("<BR><center>"+buttonPane.print()+"</center><BR>");
			out.println(frame.printAfter());
			out.println(window.printAfter());
		%>
	</body>
</html>