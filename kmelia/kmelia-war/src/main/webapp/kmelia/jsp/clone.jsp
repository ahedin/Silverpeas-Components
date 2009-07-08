<%
response.setHeader("Cache-Control","no-store"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires",-1); //prevents caching at the proxy server
%>

<%@ include file="checkKmelia.jsp" %>
<%@ include file="modelUtils.jsp" %>
<%@ include file="attachmentUtils.jsp" %>
<%@ include file="publicationsList.jsp.inc" %>
<%@ include file="topicReport.jsp.inc" %>
<%@ include file="tabManager.jsp.inc" %>

<%@ page import="com.silverpeas.publicationTemplate.*"%>
<%@ page import="com.silverpeas.form.*"%>

<%!
 //Icons
String folderSrc;
String publicationSrc;
String pubValidateSrc;
String pubUnvalidateSrc;
String fullStarSrc;
String emptyStarSrc;
String authorLinks;
String sameSubject;
String attachedFiles;
String mandatorySrc;
String deleteSrc;
String seeAlsoSrc;
String seeAlsoDeleteSrc;
String pathUpdateSrc;
String alertSrc;
String deletePubliSrc;
String clipboardCopySrc;
String hLineSrc;
String pdfSrc;
String pubDraftInSrc;
String pubDraftOutSrc;
String inDraftSrc;
String outDraftSrc;
String validateSrc;
String refusedSrc;

void displayViewWysiwyg(String id, String spaceId, String componentId, HttpServletRequest request, HttpServletResponse response) throws KmeliaException {
    try {
        getServletConfig().getServletContext().getRequestDispatcher("/wysiwyg/jsp/htmlDisplayer.jsp?ObjectId="+id+"&SpaceId="+spaceId+"&ComponentId="+componentId).include(request, response);
    } catch (Exception e) {
	  throw new KmeliaException("JSPpublicationManager.displayViewWysiwyg()",SilverpeasException.ERROR,"root.EX_DISPLAY_WYSIWYG_FAILED", e);
    }
}

// Fin des d�clarations
%>

<%
  	String creatorName	= "";
  	String creationDate	= "";
  	String updateDate	= "";
  	String updaterName	= "";
  	String status		= "";
  	String author 		= "";
	
  	ResourceLocator uploadSettings 		= new ResourceLocator("com.stratelia.webactiv.util.uploads.uploadSettings", resources.getLanguage());
  	ResourceLocator publicationSettings = new ResourceLocator("com.stratelia.webactiv.util.publication.publicationSettings", resources.getLanguage());
  
	//R�cup�ration des param�tres
	String 					profile 		= (String) request.getAttribute("Profile");
	String 					action 			= (String) request.getAttribute("Action");
	String 					checkPath 		= (String) request.getAttribute("CheckPath");
	UserCompletePublication userPubComplete = (UserCompletePublication) request.getAttribute("Publication");
	
	if (action == null)
		action = "ViewClone";

	CompletePublication 		pubComplete 	= userPubComplete.getPublication();
	PublicationDetail 			pubDetail 		= pubComplete.getPublicationDetail();
	UserDetail 					ownerDetail 	= userPubComplete.getOwner();
	String						pubName			= pubDetail.getName();
	String 						id 				= pubDetail.getPK().getId();
	
	String 		linkedPathString 	= kmeliaScc.getSessionPath();
  	
	//Icons
	folderSrc 			= m_context + "/util/icons/component/kmeliaSmall.gif";
	publicationSrc		= m_context + "/util/icons/publication.gif";
	pubValidateSrc		= m_context + "/util/icons/publicationValidate.gif";
	pubUnvalidateSrc	= m_context + "/util/icons/publicationUnvalidate.gif";
	fullStarSrc			= m_context + "/util/icons/starFilled.gif";
	emptyStarSrc		= m_context + "/util/icons/starEmpty.gif";
	deleteSrc			= m_context + "/util/icons/delete.gif";
	seeAlsoSrc			= "icons/linkedAdd.gif";
	seeAlsoDeleteSrc	= "icons/linkedDel.gif";
	pathUpdateSrc		= m_context + "/util/icons/kmelia_addto_topic.gif";
	alertSrc			= m_context + "/util/icons/alert.gif";
	deletePubliSrc		= m_context + "/util/icons/publicationDelete.gif";
	clipboardCopySrc	= m_context + "/util/icons/copy.gif";
	pdfSrc              = m_context + "/util/icons/publication_to_pdf.gif";
	hLineSrc			= m_context + "/util/icons/colorPix/1px.gif";
	inDraftSrc			= m_context + "/util/icons/masque.gif";
	outDraftSrc			= m_context + "/util/icons/visible.gif";
	validateSrc			= m_context + "/util/icons/ok.gif";
	refusedSrc			= m_context + "/util/icons/wrong.gif";

	String screenMessage = "";

	//Vrai si le user connecte est le createur de cette publication ou si il est admin
	boolean isOwner = false;

	if (action.equals("Unvalidate")) {
		screenMessage += ("<TABLE ALIGN=CENTER CELLPADDING=2 CELLSPACING=0 BORDER=0 WIDTH=\"98%\" CLASS=intfdcolor><tr><td>");
		screenMessage += ("<TABLE ALIGN=CENTER CELLPADDING=5 CELLSPACING=0 BORDER=0 WIDTH=\"100%\" CLASS=intfdcolor4><tr>");
		screenMessage += ("<td align=center>"+resources.getString("kmelia.CloneUnvalidate")+"</td>");
		screenMessage += ("</tr></TABLE></td></tr></TABLE>");
	    action = "ViewPublication";
	}
	else if (action.equals("GeneratePdf")) {
		String link = (String) request.getAttribute("Link");
	    out.println("<BODY marginheight=5 marginwidth=5 leftmargin=5 topmargin=5 onLoad=\"compileResult('"+link+"')\">");
	    out.println("</BODY>");
	}

	if (action.equals("ValidateView")) {
    	kmeliaScc.setSessionOwner(true);
        action = "UpdateView";
        isOwner = true;
    } else {
        if (profile.equals("admin") || profile.equals("publisher") || profile.equals("supervisor") || (ownerDetail != null && kmeliaScc.getUserDetail().getId().equals(ownerDetail.getId()) && profile.equals("writer")))
            isOwner = true;

        if (isOwner) {
            kmeliaScc.setSessionOwner(true);
        } else {
		    //modification pour acc�der � l'onglet voir aussi
            kmeliaScc.setSessionOwner(false);
        }
	}

    creationDate = resources.getOutputDate(pubDetail.getCreationDate());
  	
  	status	= pubDetail.getStatus();
  	author 	= pubDetail.getAuthor();
  	
  	String creatorId = pubDetail.getCreatorId();
	creatorName	= resources.getString("kmelia.UnknownUser");
	if (creatorId != null && creatorId.length() > 0)
	{
		UserDetail creator = kmeliaScc.getUserDetail(creatorId);
		if (creator != null)
			creatorName = creator.getDisplayedName();
	}
	
	String 	updaterId = pubDetail.getUpdaterId();
	updaterName = resources.getString("kmelia.UnknownUser");
	if (updaterId != null && updaterId.length() > 0)
	{
		UserDetail updater = kmeliaScc.getUserDetail(updaterId);
		if (updater != null)
			updaterName = updater.getDisplayedName();
	}

%>
<HTML>
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<TITLE></TITLE>
<%
out.println(gef.getLookStyleSheet());
%>
<script type="text/javascript" src="<%=m_context%>/wysiwyg/jsp/FCKeditor/fckeditor.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/animation.js"></script>
<script language="javascript">

var refusalMotiveWindow = window;
var publicVersionsWindow = window;
var suspendMotiveWindow = window;
var attachmentWindow = window;

function clipboardCopy() {
  top.IdleFrame.location.href = '../..<%=kmeliaScc.getComponentUrl()%>copy.jsp?Id=<%=id%>';
}

function compileResult(fileName) {
    SP_openWindow(fileName, "PdfGeneration","770", "550", "toolbar=no, directories=no, menubar=no, locationbar=no ,resizable, scrollbars");
}

function generatePdf(id)
{
 document.toRouterForm.action = "<%=routerUrl%>GeneratePdf";
 document.toRouterForm.PubId.value = id;
 document.toRouterForm.submit();
}

function pubDeleteConfirm(id) {
	closeWindows();
    if(window.confirm("<%=resources.getString("ConfirmDeletePub")%> ?")){
          document.toRouterForm.action = "<%=routerUrl%>DeletePublication";
          document.toRouterForm.PubId.value = id;
          document.toRouterForm.submit();
    }
}

function deleteCloneConfirm() {
    if(window.confirm("<%=Encode.javaStringToJsString(resources.getString("kmelia.ConfirmDeleteClone"))%>")){
          document.toRouterForm.action = "<%=routerUrl%>DeleteClone";
          document.toRouterForm.submit();
    }
}

function pubValidate(id) {
	document.toRouterForm.action = "<%=routerUrl%>ValidatePublication";
	document.toRouterForm.submit();
}

function pubUnvalidate(id) {
	document.pubForm.PubId.value = id;
	url = "WantToRefusePubli?PubId="+id;
    windowName = "refusalMotiveWindow";
	larg = "550";
	haut = "350";
    windowParams = "directories=0,menubar=0,toolbar=0, alwaysRaised";
    if (!refusalMotiveWindow.closed && refusalMotiveWindow.name== "refusalMotiveWindow")
        refusalMotiveWindow.close();
    refusalMotiveWindow = SP_openWindow(url, windowName, larg, haut, windowParams);
}

function pubSuspend(id) {
	document.pubForm.PubId.value = id;
	url = "WantToSuspendPubli?PubId="+id;
    windowName = "suspendMotiveWindow";
	larg = "550";
	haut = "350";
    windowParams = "directories=0,menubar=0,toolbar=0, alwaysRaised";
    if (!suspendMotiveWindow.closed && suspendMotiveWindow.name== "suspendMotiveWindow")
        suspendMotiveWindow.close();
    suspendMotiveWindow = SP_openWindow(url, windowName, larg, haut, windowParams);
}

function topicGoTo(id) {
	closeWindows();
	location.href="GoToTopic?Id="+id;
}

function closeWindows() {
    if (window.publicationWindow != null)
        window.publicationWindow.close();
    if (window.publicVersionsWindow != null)
    	window.publicVersionsWindow.close();
}

function viewPublicVersions(docId) {
	url = "<%=m_context+URLManager.getURL("VersioningPeas", spaceId, componentId)%>ListPublicVersionsOfDocument?DocId="+docId;
    windowName = "publicVersionsWindow";
	larg = "550";
	haut = "350";
    windowParams = "directories=0,menubar=0,toolbar=0, alwaysRaised";
    if (!publicVersionsWindow.closed && publicVersionsWindow.name== "publicVersionsWindow")
        publicVersionsWindow.close();
    publicVersionsWindow = SP_openWindow(url, windowName, larg, haut, windowParams);
}

</script>
</HEAD>

<BODY class="yui-skin-sam" onUnload="closeWindows()" onLoad="openSingleAttachment()">

<% 
		
        Window window = gef.getWindow();
        Frame frame = gef.getFrame();

        BrowseBar browseBar = window.getBrowseBar();
        browseBar.setDomainName(spaceLabel);
        browseBar.setComponentName(componentLabel, "javascript:onClick=topicGoTo('0')");
        browseBar.setPath(linkedPathString);
		browseBar.setExtraInformation(pubName);

        OperationPane operationPane = window.getOperationPane();

        //operationPane.addOperation(alertSrc, resources.getString("GML.notify"), "javaScript:onClick=goToOperationInAnotherWindow('ToAlertUser', '"+id+"', 'ViewAlert')");
		//operationPane.addLine();
		if (isOwner) {
			if (!"supervisor".equals(profile))
			{
                operationPane.addOperation(deletePubliSrc, resources.getString("kmelia.DeleteClone"), "javaScript:deleteCloneConfirm();");
                //operationPane.addLine();
            }
		}
		//operationPane.addOperation(pdfSrc, resources.getString("GML.generatePDF"), "javascript:generatePdf('"+id+"')");
        //operationPane.addLine();
        //operationPane.addOperation(clipboardCopySrc, resources.getString("GML.copy"), "javaScript:clipboardCopy()");
        if (isOwner) {
            if (profile.equals("admin") || profile.equals("publisher")) {
				if ("Valid".equals(pubDetail.getStatus())) {
					operationPane.addLine();
					operationPane.addOperation(pubUnvalidateSrc, resources.getString("PubUnvalidate?"), "javaScript:pubUnvalidate('"+id+"')");
				} else if ("ToValidate".equals(pubDetail.getStatus()) || "Clone".equals(pubDetail.getStatus())) {
					operationPane.addLine();
					operationPane.addOperation(pubValidateSrc, resources.getString("PubValidate?"), "javaScript:pubValidate('"+id+"')");
					operationPane.addOperation(pubUnvalidateSrc, resources.getString("PubUnvalidate?"), "javaScript:pubUnvalidate('"+id+"')");
				}
            }
            if (profile.equals("supervisor"))
            {
            	operationPane.addLine();
				operationPane.addOperation(pubUnvalidateSrc, resources.getString("kmelia.PubSuspend"), "javaScript:pubSuspend('"+id+"')");
            }
        }
        out.println(window.printBefore());

        displayAllOperations(id, kmeliaScc, gef, "ViewClone", resources, out);
        
        out.println(frame.printBefore());

        if (screenMessage != null && screenMessage.length()>0)
	    	out.println("<center>"+screenMessage+"</center>");
               
        InfoDetail 			infos 	= pubComplete.getInfoDetail();
    	ModelDetail 		model 	= pubComplete.getModelDetail();
	
	    int type 	= 0;
	    if (kmeliaScc.isVersionControlled())
	        type = 1; // Versioning
        
        /*********************************************************************************************************************/
		/** Affichage du header de la publication																			**/
		/*********************************************************************************************************************/
    	out.println("<TABLE border=\"0\" width=\"98%\" align=center>");
    	out.println("<TR><TD align=\"left\">");

    	out.println("<span class=\"txtnav\"><b>"+Encode.convertHTMLEntities(pubDetail.getName())+"</b></span>");
		if (!"user".equals(profile))
		{
			if ("ToValidate".equals(status))
				out.println("<img src=\""+outDraftSrc+"\" alt=\""+resources.getString("PubStateToValidate")+"\" align=\"absmiddle\">");
			else if ("Draft".equals(status))
				out.println("<img src=\""+inDraftSrc+"\" alt=\""+resources.getString("PubStateDraft")+"\" align=\"absmiddle\">");
			else if ("Valid".equals(status))
				out.println("<img src=\""+validateSrc+"\" alt=\""+resources.getString("PublicationValidated")+"\" align=\"absmiddle\">");
			else if ("UnValidate".equals(status))
				out.println("<img src=\""+refusedSrc+"\" alt=\""+resources.getString("PublicationRefused")+"\" align=\"absmiddle\">");
		}
		
		out.println("<br><b>"+Encode.javaStringToHtmlParagraphe(Encode.convertHTMLEntities(pubDetail.getDescription()))+"<b><BR><BR>");

		out.println("</TD></TR></table>");
		
		/*********************************************************************************************************************/
		/** Affichage du contenu de la publication																			**/
		/*********************************************************************************************************************/
		out.println("<TABLE border=\"0\" width=\"98%\" align=center>");
		out.println("<TR><TD valign=\"top\">");
    	if (WysiwygController.haveGotWysiwyg(spaceId, componentId, id)) {
        	out.flush();
        	getServletConfig().getServletContext().getRequestDispatcher("/wysiwyg/jsp/htmlDisplayer.jsp?ObjectId="+id+"&SpaceId="+spaceId+"&ComponentId="+componentId).include(request, response);
    	} else if (infos != null && model != null) {
       	    displayViewInfoModel(out, model, infos, resources, publicationSettings, m_context);
    	} else {
	    	Form			xmlForm 	= (Form) request.getAttribute("XMLForm");
			DataRecord		xmlData		= (DataRecord) request.getAttribute("XMLData");
			String			currentLang = (String) request.getAttribute("Language");
			if (xmlForm != null)
			{
				PagesContext xmlContext = new PagesContext("myForm", "0", resources.getLanguage(), false, componentId, kmeliaScc.getUserId());
				xmlContext.setObjectId(id);
				xmlContext.setNodeId(kmeliaScc.getSessionTopic().getNodeDetail().getNodePK().getId());
				xmlContext.setBorderPrinted(false);
				xmlContext.setContentLanguage(currentLang);
				
		    	xmlForm.display(out, xmlContext, xmlData);
		    }
    	}
    	out.println("</TD>");
    	
    	/*********************************************************************************************************************/
		/** Affichage des fichiers joints																					**/
		/*********************************************************************************************************************/
   
   		//DLE
		boolean showTitle 				= true;
		boolean showFileSize 			= true;
		boolean showDownloadEstimation 	= true;
		boolean showInfo 				= true;
		if ("no".equals(resources.getSetting("showTitle")))
			showTitle = false;	        
		if ("no".equals(resources.getSetting("showFileSize")))
			showFileSize = false;
		if ("no".equals(resources.getSetting("showDownloadEstimation")))
			showDownloadEstimation = false;	        
		if ("no".equals(resources.getSetting("showInfo")))
			showInfo = false;
		boolean showIcon = true;
		if (infos != null) {
		    if (!"bottom".equals(resources.getSetting("attachmentPosition"))) {
				out.println("<TD width=\"25%\" valign=\"top\" align=\"center\">");
				out.println("<A NAME=attachments></a>");
		   	}
		   	else {
				out.println("</TR><TR>");
				out.println("<TD valign=\"top\" align=\"left\">");
				out.println("<A NAME=attachments></a>");
		    }
			try
			{
				out.flush();									
				if (kmeliaScc.isVersionControlled())
					getServletConfig().getServletContext().getRequestDispatcher("/versioningPeas/jsp/displayDocuments.jsp?Id="+id+"&ComponentId="+componentId+"&Context=Images&AttachmentPosition="+resources.getSetting("attachmentPosition")+"&ShowIcon="+showIcon+"&ShowTitle="+showTitle+"&ShowFileSize="+showFileSize+"&ShowDownloadEstimation="+showDownloadEstimation+"&ShowInfo="+showInfo+"&UpdateOfficeMode="+kmeliaScc.getUpdateOfficeMode()).include(request, response);
				else
					getServletConfig().getServletContext().getRequestDispatcher("/attachment/jsp/displayAttachments.jsp?Id="+id+"&ComponentId="+componentId+"&Context=Images&AttachmentPosition="+resources.getSetting("attachmentPosition")+"&ShowIcon="+showIcon+"&ShowTitle="+showTitle+"&ShowFileSize="+showFileSize+"&ShowDownloadEstimation="+showDownloadEstimation+"&ShowInfo="+showInfo+"&UpdateOfficeMode="+kmeliaScc.getUpdateOfficeMode()).include(request, response);
			}
			catch (Exception e)
			{
				throw new KmeliaException("JSPpublicationManager.displayUserModelAndAttachmentsView()",SilverpeasException.ERROR,"root.EX_DISPLAY_ATTACHMENTS_FAILED", e);
			}
			out.println("</TD>");
		    out.println("</TR>");
		}
    	out.println("</TABLE>");
    	
    	out.println("<CENTER>");
    	out.print("<span class=\"txtBaseline\">");
    	if (kmeliaScc.isAuthorUsed() && pubDetail.getAuthor() != null && !pubDetail.getAuthor().equals(""))
		{
			out.print("<BR>");
			out.print(resources.getString("GML.author")+" : "+pubDetail.getAuthor());
		}
    	out.print("<BR>");
		out.print(creatorName+" - "+resources.getOutputDate(pubDetail.getCreationDate()));
		if (updaterId != null)
		{
			out.print(" | ");
			out.print(resources.getString("kmelia.LastModification")+" : "+updaterName+" - "+resources.getOutputDate(pubDetail.getUpdateDate()));
		}
		
		out.println("</CENTER>");
        
		out.flush();

        out.println(frame.printAfter());
        out.println(window.printAfter());
%>
<FORM NAME="pubForm" ACTION="<%=routerUrl%>clone.jsp" METHOD="POST">
	<input type="hidden" name="Action">
	<input type="hidden" name="PubId">
	<input type="hidden" name="Profile" value="<%=profile%>">
</FORM>
<FORM NAME="refusalForm" action="<%=routerUrl%>Unvalidate">
  	<input type="hidden" name="PubId" value="<%=id%>">
  	<input type="hidden" name="Motive" value="">
</FORM>
<FORM NAME="defermentForm" ACTION="<%=routerUrl%>SuspendPublication" METHOD="POST">
  	<input type="hidden" name="PubId" value="<%=id%>">
  	<input type="hidden" name="Motive" value="">
</FORM>
<FORM name="toRouterForm">
	<input type="hidden" name="PubId">
</FORM>
</BODY>
</HTML>