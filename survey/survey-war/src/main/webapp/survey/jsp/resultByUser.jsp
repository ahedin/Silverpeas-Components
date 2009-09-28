<%
response.setHeader("Cache-Control","no-store"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires",-1); //prevents caching at the proxy server
%>

<%@ include file="checkSurvey.jsp" %>
<%@ include file="surveyUtils.jsp.inc" %>


<%
	//R�cup�ration des param�tres
	QuestionContainerDetail survey 	= (QuestionContainerDetail) request.getAttribute("Survey");
	String profile 					= (String) request.getAttribute("Profile");
	Collection resultsByUser		= (Collection) request.getAttribute("ResultUser");
	String userName					= (String) request.getAttribute("UserName");
	String userId					= (String) request.getAttribute("UserId");
	
	ResourceLocator settings = new ResourceLocator("com.stratelia.webactiv.survey.surveySettings", surveyScc.getLanguage());
	String m_context = GeneralPropertiesManager.getGeneralResourceLocator().getString("ApplicationURL");
	String iconsPath = GeneralPropertiesManager.getGeneralResourceLocator().getString("ApplicationURL");

%>

<HTML>
<HEAD>
<TITLE></TITLE>
<style>

			html, body, div, dl, dt, dd, ul, ol, li, h1, h2, h3, h4, h5, h6, pre, form, fieldset, textarea, p, blockquote, th, td, img, hr, embed, object {
				margin:0px;
				padding:0px;
				font-family:Arial, Helvetica, sans-serif;
			}
			
			body {
				margin:20px 0 0 20px;
			}
		
			/* tableau Doodle */
			.questionResults th {
				border-top:#CCC 1px solid;
			}
			
			.questionResults th, .questionResults td, .questionResults tr {
				padding:5px;
				font-size:12px;
			}
			
			.questionResults .questionResults-top th {
				background-color:#E4E4E4;
				/*border:#CCC 1px solid;*/
				width:150px;
			}
			
			.questionResults .questionResults-top .questionResults-vide, .questionResults tbody .questionResults-vide {
				background-color:#FFFFFF;
				border:none;
				padding:3px;
			}
			
			.questionResults .displayUserName , .questionResults .displayUserName a {
				/*display:block;*/
				background-color:#E4E4E4;
				/*border:#CCC 1px solid;*/
				margin-right:5px;
				font-size:12px;
				font-weight:bold;
			}
			
			.questionResults tbody tr td {
				background-color:#FFFFFF;
				/*border:#E9E9E9 1px solid;*/
				min-height:33px;
			}
			
			.questionResults tbody .questionResults-Oui {
				background-color:#D5FAC5;
				/*border:1px solid #7CCC24;*/
				text-align:center;
				font-size:12px;
			}
			
			.questionResults tbody .questionResults-Non {
				background-color:#FECBCB;
				/*border:1px solid #FD433E;*/
				text-align:center;
				font-size:12px;
			}
			
			.questionResults .labelAnswer {
				text-align:right;
				width:50%;
			}		
		</style>
<%
	out.println(gef.getLookStyleSheet());
%>
<script type="text/javascript" src="<%=iconsPath%>/util/javaScript/animation.js"></script>
<script language="JavaScript1.2">
        
	function viewUsers(id)
	{
    	url = "ViewListResult?AnswerId="+id;
 		windowName = "users";
 		larg = "550";
 		haut = "250";
 		windowParams = "directories=0,menubar=0,toolbar=0,resizable=1,scrollbars=1,alwaysRaised";
 		suggestions = SP_openWindow(url, windowName, larg , haut, windowParams);
 		suggestions.focus();
    }
 				   
</script>
</HEAD>
<BODY>
<%     
	Window window = gef.getWindow();
    Frame frame = gef.getFrame();

    String surveyPart = displaySurveyResult(userName, userId, "user", resultsByUser, "C", survey, gef, m_context, surveyScc, resources, false, settings, frame);
    String action = "ViewResult";
    window.addBody(frame.printBefore()+"<center>"+""+"</center><BR>"+surveyPart);

    out.println(window.print());
%>

</BODY>
</HTML>