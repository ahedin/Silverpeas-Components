<%--

    Copyright (C) 2000 - 2013 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have received a copy of the text describing
    the FLOSS exception, and it is also available here:
    "http://www.silverpeas.org/docs/core/legal/floss_exception.html"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://www.silverpeas.com/tld/viewGenerator" prefix="view"%>
<%@ include file="check.jsp" %>

<%
String 		dateDebProject  = (String) request.getAttribute("DateDebProject"); //au format dd/MM/yyyy
String 		role 			= (String) request.getAttribute("Role");
TaskDetail 	task 			= (TaskDetail) request.getAttribute("Task");
TaskDetail 	actionMere 		= (TaskDetail) request.getAttribute("ActionMere");
List		previousTasks	= (List) request.getAttribute("PreviousTasks");
TaskDetail	taskInProgress	= (TaskDetail) request.getAttribute("TaskInProgress");

String 	nom 		= task.getNom();
String 	charge 		= new Float(task.getCharge()).toString();
String 	consomme 	= new Float(task.getConsomme()).toString();
String 	raf 		= new Float(task.getRaf()).toString();
int 	avancement	= task.getAvancement();
String 	description = task.getDescription();
String 	dateDebut 	= task.getUiDateDebut();
String 	dateFin 	= task.getUiDateFin();
String 	respoName	= task.getResponsableFullName();
String 	respoId		= new Integer(task.getResponsableId()).toString();
int	   	status		= task.getStatut();
int	   	previousId	= task.getPreviousTaskId();
String previousName = task.getPreviousTaskName();
Collection resources = task.getResources();
int		taskId		= task.getId();

String 	resourceId 	= "";

if (taskInProgress != null)
{
	nom 		= taskInProgress.getNom();
	charge 		= new Float(taskInProgress.getCharge()).toString();
	consomme 	= new Float(taskInProgress.getConsomme()).toString();
	raf 		= new Float(taskInProgress.getRaf()).toString();
	avancement	= taskInProgress.getAvancement();
	description = taskInProgress.getDescription();
	dateDebut 	= taskInProgress.getUiDateDebut();
	dateFin 	= taskInProgress.getUiDateFin();
	respoName 	= taskInProgress.getResponsableFullName();
	respoId		= new Integer(taskInProgress.getResponsableId()).toString();
	status		= taskInProgress.getStatut();
	previousId	= taskInProgress.getPreviousTaskId();
	resources 	= taskInProgress.getResources();
	taskId 		= taskInProgress.getId();
}
if (description == null || "null".equals(description))
	description = "";
if (previousName == null || "null".equals(previousName))
	previousName = "";
	
boolean	isOrganisateur = "admin".equals(role);
%>

<html>
<head>
<view:looknfeel/>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/checkForm.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/animation.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/dateUtils.js"></script>

<script type="text/javascript" src="<%=m_context%>/util/ajax/prototype.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/ajax/rico.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/ajax/ricoAjax.js"></script>


<script type="text/javascript">

var language = '<%=resource.getLanguage()%>';

function editDate(indiceElem)
{
	//emptyEndDate();
	chemin = "ToChooseDate?JSFunction=setDate&InputId="+indiceElem;
	largeur = "180";
	hauteur = "200";
	SP_openWindow(chemin,"Calendrier_Todo",largeur,hauteur,"");
}

function setDate(day)
{
	document.getElementById("DateDebut").value = day;
	reloadDateFinAndOccupations();
}

function isFieldDateCorrect(input)
{
	var date = input.value;
    if (!isWhitespace(date)) {
    	if (!isDateOK(date, '<%=resource.getLanguage()%>')) {
    		return false;
    	}
    }
    return true;
}
function dateDansPeriodeMere(beginDate)
{
	<%
		if (actionMere != null)
		{
			out.println("var beginDateMere = \""+actionMere.getUiDateDebut()+"\";");
			out.println("var endDateMere = \""+actionMere.getUiDateFin()+"\";");
	%>     		
     		return isDate1AfterDate2(beginDate, beginDateMere, '<%=resource.getLanguage()%>') && isDate1AfterDate2(endDateMere, beginDate, '<%=resource.getLanguage()%>');
	<%
		}
		else
		{
			out.println("return true;");
		}
	%>
}
function isCorrectForm() {
	 var i=0;
	 var str = "";
	 var hasNext = 1;
   var errorMsg 			= "";
   var errorNb 			= 0;
	 while (hasNext == 1)
	 {
		 try
		 {
			 var resourceId = document.getElementById("Resource"+i).value;
       var chargeResource = document.getElementById("Charge"+i).value;
       if (!isInteger(chargeResource))
       {
          errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.ResourceCharge")%>' <%=resource.getString("GML.MustContainsNumber")%>\n";
          errorNb++;
       }
			 str += resourceId;
			 str += "_";
			 str += chargeResource;
			 str += ",";
			 i++;
		 }
		 catch (e)
		 {
			 hasNext = 0;
		 }
	 }
	 //str = resourceId0_pourcentage0,resourceId1_pourcentage1,resourceId2_pourcentage2
	 document.actionForm.allResources.value = str;
     
     var name 				= document.actionForm.Nom.value;
     var responsable		= document.actionForm.ResponsableId.value;
     var charge 			= document.actionForm.Charge.value;
     var consomme			= document.actionForm.Consomme.value;
     var raf				= document.actionForm.Raf.value;

     var beginDate 			= document.actionForm.DateDebut.value;
     var endDate 			= document.actionForm.DateFin.value;

     if (isWhitespace(name)) {
           errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheNom")%>' <%=resource.getString("GML.MustBeFilled")%>\n";
           errorNb++;
     }
     if (isWhitespace(responsable)) {
           errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheResponsable")%>' <%=resource.getString("GML.MustBeFilled")%>\n";
           errorNb++;
     }
     if (isWhitespace(charge) || !isNumericField(charge)) {
           errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheCharge")%>' <%=resource.getString("GML.MustContainsFloat")%>\n";
           errorNb++;
     }
     if (!isNumericField(consomme)) {
           errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheConsomme")%>' <%=resource.getString("GML.MustContainsFloat")%>\n";
           errorNb++;
     }
     if (!isNumericField(raf)) {
           errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheResteAFaire")%>' <%=resource.getString("GML.MustContainsFloat")%>\n";
           errorNb++;
     }
     if (isWhitespace(beginDate)) {
           errorMsg +="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheDateDebut")%>' <%=resource.getString("GML.MustBeFilled")%>\n";
           errorNb++;
     } else {
           if (!isDateOK(beginDate, '<%=resource.getLanguage()%>')) {
               errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheDateDebut")%>' <%=resource.getString("GML.MustContainsCorrectDate")%>\n";
               errorNb++;
           } else {
	 	   			var beginDateMin 	= extractBeginDateFromPrevious();
	 	   			if (!isDate1AfterDate2(beginDate, beginDateMin, language))
	 				{
						errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheDateDebut")%>' <%=resource.getString("GML.MustContainsPostOrEqualDateTo")%> "+beginDateMin+"\n";
	           			errorNb++;
	 				}
		 	    
               <% if (actionMere != null) { %>
	     	   		if (!dateDansPeriodeMere(beginDate))
	     	   		{
	     	   			errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheDateDebut")%>' <%=resource.getString("GML.MustContainsDateBetween1")%> <%=actionMere.getUiDateDebut()%> <%=resource.getString("GML.MustContainsDateBetween2")%> <%=actionMere.getUiDateFin()%> \n";
		               	errorNb++;
	     	   		}
	     	   <% } %>
           }
     }
     switch(errorNb) {
        case 0 :
            result = true;
            break;
        case 1 :
            errorMsg = "<%=resource.getString("GML.ThisFormContains")%> 1 <%=resource.getString("GML.error")%> : \n" + errorMsg;
            window.alert(errorMsg);
            result = false;
            break;
        default :
            errorMsg = "<%=resource.getString("GML.ThisFormContains")%> " + errorNb + " <%=resource.getString("GML.errors")%> :\n" + errorMsg;
            window.alert(errorMsg);
            result = false;
            break;
     }
     return result;
}


function isCorrectFormResponsable() {
	   var i=0;
	   var str = "";
	   //while(document.getElementById("Resource"+i).value != null)
	   var hasNext = 1;
     var errorMsg 			= "";
     var errorNb 			= 0;
	   while (hasNext == 1)
	   {
	     try
	     {
	       var resourceId = document.getElementById("Resource"+i).value;
         var chargeResource = document.getElementById("Charge"+i).value;
         if (!isInteger(chargeResource))
         {
            errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.ResourceCharge")%>' <%=resource.getString("GML.MustContainsNumber")%>\n";
            errorNb++;
         }
			   str += resourceId;
			   str += "_";
			   str += chargeResource;
			   str += ",";
			   i++;
	     }
	     catch (e)
	     {
	       hasNext = 0;
	     }
	   }
	 //str = resourceId0_pourcentage0,resourceId1_pourcentage1,resourceId2_pourcentage2
	   document.actionForm.allResources.value = str;
	   
     var consomme			= document.actionForm.Consomme.value;
     var raf				= document.actionForm.Raf.value;
     
	if (!isNumericField(consomme)) {
	      errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheConsomme")%>' <%=resource.getString("GML.MustContainsFloat")%>\n";
	      errorNb++;
	}
	if (!isNumericField(raf)) {
	      errorMsg+="  - <%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheResteAFaire")%>' <%=resource.getString("GML.MustContainsFloat")%>\n";
	      errorNb++;
	}
     switch(errorNb) {
        case 0 :
            result = true;
            break;
        case 1 :
            errorMsg = "<%=resource.getString("GML.ThisFormContains")%> 1 <%=resource.getString("GML.error")%> : \n" + errorMsg;
            window.alert(errorMsg);
            result = false;
            break;
        default :
            errorMsg = "<%=resource.getString("GML.ThisFormContains")%> " + errorNb + " <%=resource.getString("GML.errors")%> :\n" + errorMsg;
            window.alert(errorMsg);
            result = false;
            break;
     }
     return result;
}

function sendTaskData() {
    if (isCorrectForm()) {
    	document.getElementById("HiddenDateFin").value = document.getElementById("DisplayDateFin").innerHTML;
    	
    	document.actionForm.PreviousId.value = extractPreviousId();
    	document.actionForm.submit();
    }
}

function sendTaskDataResponsable() {
    if (isCorrectFormResponsable()) {
		document.actionForm.submit();
    }
}

function callUserPanel()
{
	SP_openWindow('ToUserPanel','', '750', '550','scrollbars=yes, resizable, alwaysRaised');
}

function selectResources()
{
	var i=0;
	 var str = "";
	 var hasNext = 1;
	 while (hasNext == 1)
	 {
		 try
		 {
			 str += document.getElementById("Resource"+i).value;
			 str += "_";
			 str += document.getElementById("Charge"+i).value;
			 str += ",";
			 i++;
		 }
		 catch (e)
		 {
			 hasNext = 0;
		 }
	 }
	var url = "ToSelectResources?allResources="+str;
	SP_openWindow(url, '750', '550','scrollbars=yes, resizable, alwaysRaised');
}

function extractPreviousId()
{
	index = document.actionForm.Precedente.selectedIndex;
    var str = document.actionForm.Precedente.options[index].value;
    s = str.split("_");
    return s[0];
}
function extractBeginDateFromPrevious()
{
	index = document.actionForm.Precedente.selectedIndex;
    var str = document.actionForm.Precedente.options[index].value;
    s = str.split("_");
    return s[1];
}

function changePrevious()
{
    document.actionForm.DateDebut.value = extractBeginDateFromPrevious();
    //emptyEndDate();
}

/*function emptyEndDate()
{
	document.actionForm.DateFin.value = "";
	document.actionForm.DF.value = "";
}*/
</script>

<script language="JavaScript1.2">
function init()
{
	ajaxEngine.registerRequest('refreshTask', '<%=m_context%>/RAjaxProjectManagerServlet/dummy');
	
	var cpt = 0;
	var nomore = 0;
	while (nomore != 1)
	{
		//alert('reset '+cpt);
		try {
			var inputCharge = document.getElementById('Charge'+cpt);
			if (inputCharge.addEventListener){
				//inputCharge.onBlur = "reloadOccupations()";
				inputCharge.setAttribute("onblur", "reloadOccupations();");
				//inputCharge.addEventListener("onblur", reloadOccupations, false);
			} else if (inputCharge.attachEvent) {
				inputCharge.attachEvent('onblur', reloadOccupations);
			}
			ajaxEngine.registerAjaxElement('Occupation'+cpt);
			cpt++;
		} 
		catch (e) 
		{
			//alert("erreur #"+cpt);
			nomore = 1;
		}
	}
	
	ajaxEngine.registerAjaxElement('DisplayDateFin');
}

function reloadOccupation(i)
{
	//alert("reload "+i);
	var userId = document.getElementById("Resource"+i).value;
	var charge = document.getElementById("Charge"+i).value;
	var dateDebut = document.getElementById("DateDebut").value;
	var dateFin = document.getElementById("DisplayDateFin").innerHTML;

	ajaxEngine.sendRequest('refreshTask','ElementId=Occupation'+i,"ComponentId=<%=componentId%>", "TaskId=<%=taskId%>", "UserId="+userId, "UserCharge="+charge, "BeginDate="+dateDebut, "EndDate="+dateFin, "Action=ProcessUserOccupation");
}

function doNothing()
{
	alert("doNothing");
	return;
}

function reloadOccupations()
{
	init();
	//alert("reloadOccupations");
	var cpt = 0;
	var nomore = 0;
	while (nomore != 1)
	{
		try {
			//alert("cpt = " + cpt);
			reloadOccupation(cpt);
			cpt++;
			
			var delai = 0;
			var d = new Date();
			while (delai < 800000)
			{
				delai++;
			}
		} 
		catch (e) 
		{
			//alert("error = " + e);
			nomore = 1;
		}
	}
}

function reloadDateFinAndOccupations()
{
	//mise � jour de la date de fin
	if (isFieldDateCorrect(document.getElementById("DateDebut")))
	{
		var charge = document.getElementById("Charge").value;
		var dateDebut = document.getElementById("DateDebut").value;
		ajaxEngine.sendRequest('refreshTask','ElementId=DisplayDateFin',"ComponentId=<%=componentId%>", "TaskId=<%=taskId%>", "Charge="+charge, "BeginDate="+dateDebut, "Action=ProcessEndDate");
	
		var dateFin = document.getElementById("DisplayDateFin").innerHTML;
	
		/*var delai = 0;
		var d = new Date();
		while (delai < 4000000)
		{
			delai++;
		}*/
		
		setTimeout("reloadOccupations();", 100);
	}
	else
	{
		window.alert("<%=resource.getString("GML.theField")%> '<%=resource.getString("projectManager.TacheDateDebut")%>' <%=resource.getString("GML.MustContainsCorrectDate")%>\n");

	}
}
</script>

</head>
<body bgcolor="#ffffff" leftmargin="5" topmargin="5" marginwidth="5" marginheight="5" onLoad="init()">
<%
browseBar.setDomainName(spaceLabel);
browseBar.setComponentName(componentLabel, "Main");
browseBar.setExtraInformation(resource.getString("projectManager.ModifierTache"));

operationPane.addOperation(resource.getIcon("projectManager.userPanel"), resource.getString("projectManager.SelectionnerRessources"), "javascript:selectResources()");

out.println(window.printBefore());
out.println(frame.printBefore());

Board board = gef.getBoard();
out.println(board.printBefore());
%>
<form name="actionForm" action="UpdateTask" method="post">
<table cellpadding="5">
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheNumero")%> :</td>
    <td><%=task.getChrono()%></td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheOrganisateur")%> :</td>
    <td><%=task.getOrganisateurFullName()%></td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheResponsable")%> :</td>
    <td>
    	<% if (isOrganisateur) { %>
	    	<input type="text" name="Responsable" value="<%=task.getResponsableFullName()%>" size="60" disabled>
	    	<a href="javascript:callUserPanel()"><img src="<%=resource.getIcon("projectManager.userPanel")%>" alt="<%=resource.getString("projectManager.SelectionnerResponsable")%>" border="0" align="middle"></a>&nbsp;
	    	<IMG src="<%=resource.getIcon("projectManager.mandatoryField")%>" width="5" height="5" border="0">
	    	<input type="hidden" name="ResponsableName" value="<%=task.getResponsableFullName()%>">
	    	<input type="hidden" name="ResponsableId" value="<%=task.getResponsableId()%>">&nbsp;
    	<% } else { %>
    		<%=task.getResponsableFullName()%>
    	<% } %>
    </td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheNom")%> <%=resource.getString("projectManager.Tache")%> :</td>
    <td>
    	<% if (isOrganisateur) { %>
	    	<input type="text" name="Nom" value="<%=nom%>" size="60" maxlength="150">&nbsp;<IMG src="<%=resource.getIcon("projectManager.mandatoryField")%>" width="5" height="5" border="0">
	    <% } else { %>
    		<%=nom%>
    	<% } %>
    </td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TachePrecedente")%> :</td>
    <td>
    	<% if (isOrganisateur) { %>
	    	<select name="Precedente" size="1" onChange="javaScript:changePrevious();">
	    	<%
	    		out.println("<option value=\"-1_"+dateDebProject+"\">"+resource.getString("GML.noneF")+"</option>");
	    		
	    		TaskDetail 	previousTask 		= null;
	    		String 		previousSelected 	= "";
	    		for (int t=0; t<previousTasks.size(); t++)
	    		{
	    			previousTask = (TaskDetail) previousTasks.get(t);
	    			if (previousTask.getId() != task.getId())
	    			{
		    			if (previousTask.getId() == previousId)
		    				previousSelected = "selected";
		    				
		   				out.println("<option value=\""+previousTask.getId()+"_"+previousTask.getUiDateDebutPlus1()+"\""+previousSelected+">"+previousTask.getNom()+"</option>");
		   				previousSelected = "";
		   			}
	    		}
	    	%>
	    	</select>
	    <% } else { %>
	    	<%=previousName%>
	    <% } %>
    </td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheCharge")%> :</td>
    <td>
    	<% if (isOrganisateur) { %>
	    	<input type="text" id="Charge" name="Charge" value="<%=charge%>" size="10" maxlength="10" onBlur="javascript:reloadDateFinAndOccupations();">&nbsp;<%=resource.getString("projectManager.TacheJours")%>&nbsp;<IMG src="<%=resource.getIcon("projectManager.mandatoryField")%>" width="5" height="5" border="0">
	    <% } else { %>
    		<%=charge%>
    	<% } %>
    </td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheConsomme")%> :</td>
    <td><input type="text" name="Consomme" value="<%=consomme%>" size="10" maxlength="10" onBlur="this.value=formatNumericField(this.value);">&nbsp;<%=resource.getString("projectManager.TacheJours")%></td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheResteAFaire")%> :
		<input type="hidden" name="TaskId" value="<%=task.getId()%>"></td>
    <td><input type="text" name="Raf" value="<%=raf%>" size="10" maxlength="10" onBlur="this.value=formatNumericField(this.value);">&nbsp;<%=resource.getString("projectManager.TacheJours")%></td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheAvancement")%> :</td>
    <td><%=avancement%> %</td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheStatut")%> :</td>
    <td><select name="Statut" size="1">
    	<%
    		String 	label 	= "";
    		String 	statusSelected;
    		for (int s=0; s<6; s++)
    		{
    			switch (s)
    			{
    				case 0 : label = resource.getString("projectManager.TacheStatutEnCours");
    						 break;
    				case 1 : label = resource.getString("projectManager.TacheStatutGelee");
    						 break;
    				case 2 : label = resource.getString("projectManager.TacheStatutAbandonnee");
    						 break;
    				case 3 : label = resource.getString("projectManager.TacheStatutRealisee");
    						 break;
    				case 4 : label = resource.getString("projectManager.TacheStatutEnAlerte");
    						 break;    						     						     						 
    				case 5 : label = resource.getString("projectManager.TacheAvancementND");
    						 break;    						     						     						 
    			}
    			statusSelected = "";
    			if (s==status)
    				statusSelected = "selected";
    				
   				out.println("<option value=\""+s+"\" "+statusSelected+">"+label+"</option>");
    		}
    	%>
    	</select>
    </td>
</tr>
<tr>
	<td class="txtlibform" valign="top"><%=resource.getString("projectManager.TacheDescription")%> :</td>
    <td>
    	<% if (isOrganisateur) { %>
	    	<textarea name="Description" rows="5" cols="60"><%=description%></textarea>
	    <% } else { %>
    		<%=description%>
    	<% } %>
    </td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheDateDebut")%> :</td>
    <td>
    	<% if (isOrganisateur) { %>
	    	<input type="text" name="DateDebut" id="DateDebut" size="12" maxlength="10" value="<%=dateDebut%>" onBlur="javascript:reloadDateFinAndOccupations();">&nbsp;<img src="<%=resource.getIcon("projectManager.mandatoryField")%>" width="5" height="5" valign="absmiddle">&nbsp;&nbsp;<a href="javascript:onClick=editDate(11)"><img src="<%=resource.getIcon("projectManager.calendrier")%>" border="0" align="absmiddle" alt="<%=resource.getString("GML.viewCalendar")%>"></a>&nbsp;<span class="txtnote"><%=resource.getString("GML.dateFormatExemple")%></span>
	    <% } else { %>
	       <%=resource.getOutputDate(task.getDateDebut())%>
    		<input type="hidden" id="DateDebut" value="<%=resource.getOutputDate(task.getDateDebut())%>"/>
    	<% } %>
    </td>
</tr>
<tr>
	<td class="txtlibform"><%=resource.getString("projectManager.TacheDateFin")%> :</td>
    <td>
    	<% if (isOrganisateur) { %>
    		<span id="DisplayDateFin"><%=dateFin%></span>
    		<input type="hidden" id="HiddenDateFin" name="DateFin" value="<%=dateFin%>"><input type="hidden" name="PreviousId"><input type="hidden" name="Id" value="<%=task.getId()%>">
    	<% } else { %>
    		<span id="DisplayDateFin"><%=resource.getOutputDate(task.getDateFin())%></span>
    	<% } %>
    </td>
</tr>
<!-- affichage des ressources --> 
<tr>
<td class="txtlibform"><%=resource.getString("projectManager.TacheResources")%> :</td>
<td><input type="hidden" name="allResources" /><div id="resources"> 
<%
	String resourceName = "";
	
	int resourceCharge = 0;
	int occupation = 0;

	if (resources != null) {
		Iterator it = resources.iterator();
		int cpt = 0;
		out.println("<table id=\"tableResources\">");
		while(it.hasNext())
		{
			TaskResourceDetail resourceDetail = (TaskResourceDetail) it.next();
			resourceName = resourceDetail.getUserName();
			resourceId = resourceDetail.getUserId();  
			resourceCharge = resourceDetail.getCharge();
			occupation = resourceDetail.getOccupation();
			String couleur = "green";
			if (occupation > 100)
				couleur = "red";
					
			%>
			<tr>
				<td><input type="hidden" id="Resource<%=cpt%>" name="Resource<%=cpt%>" value="<%=resourceId%>"><span><%=resourceName%></span></td>
				<td>&nbsp;<input type="text" id="Charge<%=cpt%>" name="Charge<%=cpt%>" value="<%=resourceCharge%>" size="3"> %</td>
				<td>&nbsp;&nbsp;&nbsp;<span id="Occupation<%=cpt%>"><font color="<%=couleur%>"><%=occupation%> %</font></span></td>
			</tr>
			<%
			cpt++;
		}
		out.println("</table>");
	} %>
	</div></td>
</tr>
</table>
</form>
<%
out.println(board.printAfter());
%>
<center><br>
<%
ButtonPane buttonPane = gef.getButtonPane();
if (role.equals("admin"))
	buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.validate"), "javascript:onClick=sendTaskData()", false));
else
	buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.validate"), "javascript:onClick=sendTaskDataResponsable()", false));
buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.cancel"), "Main", false));
out.println(buttonPane.print());
%>
<br></center>
<%
out.println(frame.printAfter());
out.println(window.printAfter());
%>
</body>
</html>