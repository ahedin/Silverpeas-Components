package com.stratelia.webactiv.kmelia.servlets;

import java.io.File;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

import com.silverpeas.form.DataRecord;
import com.silverpeas.form.Form;
import com.silverpeas.form.FormException;
import com.silverpeas.form.PagesContext;
import com.silverpeas.form.RecordSet;
import com.silverpeas.publicationTemplate.PublicationTemplate;
import com.silverpeas.publicationTemplate.PublicationTemplateException;
import com.silverpeas.publicationTemplate.PublicationTemplateImpl;
import com.silverpeas.publicationTemplate.PublicationTemplateManager;
import com.silverpeas.util.EncodeHelper;
import com.silverpeas.util.ForeignPK;
import com.silverpeas.util.StringUtil;
import com.silverpeas.util.ZipManager;
import com.silverpeas.util.i18n.I18NHelper;
import com.silverpeas.util.web.servlet.FileUploadUtil;
import com.stratelia.silverpeas.peasCore.ComponentContext;
import com.stratelia.silverpeas.peasCore.ComponentSessionController;
import com.stratelia.silverpeas.peasCore.MainSessionController;
import com.stratelia.silverpeas.peasCore.URLManager;
import com.stratelia.silverpeas.peasCore.servlets.ComponentRequestRouter;
import com.stratelia.silverpeas.selection.Selection;
import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.silverpeas.util.SilverpeasSettings;
import com.stratelia.silverpeas.versioning.model.Document;
import com.stratelia.silverpeas.versioning.model.DocumentVersion;
import com.stratelia.silverpeas.versioning.util.VersioningUtil;
import com.stratelia.silverpeas.wysiwyg.control.WysiwygController;
import com.stratelia.webactiv.beans.admin.ProfileInst;
import com.stratelia.webactiv.kmelia.KmeliaSecurity;
import com.stratelia.webactiv.kmelia.control.KmeliaSessionController;
import com.stratelia.webactiv.kmelia.control.ejb.KmeliaHelper;
import com.stratelia.webactiv.kmelia.model.FileFolder;
import com.stratelia.webactiv.kmelia.model.TopicDetail;
import com.stratelia.webactiv.kmelia.model.UserCompletePublication;
import com.stratelia.webactiv.kmelia.model.UserPublication;
import com.stratelia.webactiv.util.DateUtil;
import com.stratelia.webactiv.util.FileRepositoryManager;
import com.stratelia.webactiv.util.FileServerUtils;
import com.stratelia.webactiv.util.GeneralPropertiesManager;
import com.stratelia.webactiv.util.ResourceLocator;
import com.stratelia.webactiv.util.attachment.control.AttachmentController;
import com.stratelia.webactiv.util.attachment.model.AttachmentDetail;
import com.stratelia.webactiv.util.fileFolder.FileFolderManager;
import com.stratelia.webactiv.util.node.model.NodeDetail;
import com.stratelia.webactiv.util.node.model.NodePK;
import com.stratelia.webactiv.util.publication.info.model.InfoDetail;
import com.stratelia.webactiv.util.publication.info.model.InfoImageDetail;
import com.stratelia.webactiv.util.publication.info.model.InfoTextDetail;
import com.stratelia.webactiv.util.publication.info.model.ModelDetail;
import com.stratelia.webactiv.util.publication.model.Alias;
import com.stratelia.webactiv.util.publication.model.CompletePublication;
import com.stratelia.webactiv.util.publication.model.PublicationDetail;
import com.stratelia.webactiv.util.publication.model.PublicationPK;

public class KmeliaRequestRouter extends ComponentRequestRouter {

	/**
     * This method creates a KmeliaSessionController instance
	 *
	 * @param		mainSessionCtrl		The MainSessionController instance
	 * @param		context				Context of current component instance
	 * @return		a KmeliaSessionController instance
     */
	public ComponentSessionController createComponentSessionController(MainSessionController mainSessionCtrl, ComponentContext context) {
		ComponentSessionController component = (ComponentSessionController) new KmeliaSessionController(mainSessionCtrl, context);
		return component;
	}

	/**
     * This method has to be implemented in the component request rooter class.
     * returns the session control bean name to be put in the request object
     * ex : for almanach, returns "almanach"
     */
	public String getSessionControlBeanName() {
		return "kmelia";
	}

    /**
     * This method has to be implemented by the component request rooter
     * it has to compute a destination page
     * @param function The entering request function ( : "Main.jsp")
     * @param componentSC The component Session Control, build and initialised.
	 * @param request The entering request. The request rooter need it to get parameters
     * @return The complete destination URL for a forward (ex : "/almanach/jsp/almanach.jsp?flag=user")
     */
	public String getDestination(String function, ComponentSessionController componentSC, HttpServletRequest request) {
		SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination()", "root.MSG_GEN_PARAM_VALUE","function = "+function);
		String destination = "";
		String rootDestination = "/kmelia/jsp/";
		boolean profileError = false;
		boolean kmaxMode = false;
		boolean toolboxMode = false;
		try
		{
			KmeliaSessionController kmelia = (KmeliaSessionController) componentSC;
			SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination()", "root.MSG_GEN_PARAM_VALUE","getComponentRootName() = "+kmelia.getComponentRootName());
			if ("kmax".equals(kmelia.getComponentRootName()))
			{
				kmaxMode = true;
				kmelia.isKmaxMode = true;
			}
			request.setAttribute("KmaxMode", new Boolean(kmaxMode));
			
			toolboxMode = KmeliaHelper.isToolbox(kmelia.getComponentId());
			
			//Set language choosen by the user
			setLanguage(request, kmelia);
			
			if (function.startsWith("Main")) {
				resetWizard(kmelia);
				if (kmaxMode)
				{
					destination = getDestination("KmaxMain", componentSC, request);
					kmelia.setSessionTopic(null);
					kmelia.setSessionPath("");
				}
				else
					destination = getDestination("GoToTopic", componentSC, request);
			}
			else if (function.startsWith("portlet")) 
			{
				kmelia.setSessionPublication(null);
                String  flag = componentSC.getUserRoleLevel();
				if (kmaxMode)
					destination = rootDestination + "kmax_portlet.jsp?Profile=" + flag;
				else
					destination = rootDestination + "portlet.jsp?Profile=user";
			}

			else if (function.equals("FlushTrashCan"))
			{
				kmelia.flushTrashCan();
				if (kmaxMode)
					destination = getDestination("KmaxMain", kmelia, request);
				else
					destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			
			else if (function.equals("ViewPublicationsToValidate"))
			{
				String flag = kmelia.getProfile();
				destination = rootDestination + "publicationsToValidate.jsp?Profile="+flag;
			}
			
			else if (function.equals("GoToDirectory"))
			{
				String topicId = request.getParameter("Id");
				
				String path = null;
				if (StringUtil.isDefined(topicId))
				{
					NodeDetail topic = kmelia.getNodeHeader(topicId);
					path = topic.getPath();
				}
				else
				{
					path = request.getParameter("Path");
				}
				
				FileFolder folder = new FileFolder(path);
				
				/*String separator = "\\";
				if (path.indexOf('/') != -1)
					separator = "/";
				
				List lPath = new ArrayList(); 
				StringTokenizer tokenizer = new StringTokenizer(path, separator);
				while (tokenizer.hasMoreTokens())
				{
					lPath.add(tokenizer.nextToken());
				}*/
				
				request.setAttribute("Directory", folder);
				//request.setAttribute("Path", lPath);
				request.setAttribute("LinkedPathString", kmelia.getSessionPath());
				
				destination = rootDestination + "repository.jsp";
			}

			else if (function.equals("GoToTopic")) 
			{
				String topicId 	= (String) request.getAttribute("Id");
				if (!StringUtil.isDefined(topicId))
				{
					topicId = request.getParameter("Id");
					if (!StringUtil.isDefined(topicId))
						topicId = "0";
				}
				
				TopicDetail currentTopic = kmelia.getTopic(topicId, true);
				
				processPath(kmelia, null);
				
				kmelia.setSessionPublication(null);
				resetWizard(kmelia);
				
				request.setAttribute("CurrentTopic", currentTopic);
				request.setAttribute("PathString", kmelia.getSessionPathString());
				request.setAttribute("LinkedPathString", kmelia.getSessionPath());
				request.setAttribute("Treeview", kmelia.getTreeview());
				request.setAttribute("DisplayNBPublis", new Boolean(kmelia.displayNbPublis()));
				
				if ("noRights".equalsIgnoreCase(currentTopic.getNodeDetail().getUserRole()))
				{
					destination = rootDestination + "toCrossTopic.jsp";
				}
				else
				{
					request.setAttribute("Profile", kmelia.getUserTopicProfile(topicId));
					request.setAttribute("IsGuest", new Boolean(kmelia.getUserDetail().isAccessGuest()));
					request.setAttribute("RightsOnTopicsEnabled", new Boolean(kmelia.isRightsOnTopicsEnabled()));
					
					destination = rootDestination + "topicManager.jsp";
				}
			}
			
			else if (function.equals("GoToCurrentTopic"))
			{
				if (kmelia.getSessionTopic() != null)
				{
					String id = kmelia.getSessionTopic().getNodePK().getId();
					request.setAttribute("Id", id);

					destination = getDestination("GoToTopic", kmelia, request);
				}
				else
					destination = getDestination("Main", kmelia, request);
			}

			else if (function.startsWith("searchResult")) {
				resetWizard(kmelia);
				String id 					= request.getParameter("Id");
				String type 				= request.getParameter("Type");
				String fileAlreadyOpened	= request.getParameter("FileOpened");
				if (type.equals("Publication") || type.equals("com.stratelia.webactiv.calendar.backbone.TodoDetail"))
				{
					KmeliaSecurity security = new KmeliaSecurity(kmelia.getOrganizationController());
					try
					{
						boolean accessAuthorized = security.isAccessAuthorized(kmelia.getComponentId(), kmelia.getUserId(), id, "Publication");
						if (accessAuthorized)
						{
							if (kmaxMode)
							{
								request.setAttribute("FileAlreadyOpened", fileAlreadyOpened);
								
								destination = getDestination("ViewPublication", kmelia, request);
							}
							else if (toolboxMode)
							{
								processPath(kmelia, id);
								
								//we have to find which page contains the right publication
								List				publications	= new ArrayList(kmelia.getSessionTopic().getPublicationDetails());
								UserPublication		publication		= null;
								int					pubIndex		= -1;
								for (int p=0; p<publications.size()&&pubIndex==-1; p++)
								{
									publication = (UserPublication) publications.get(p);
									if (id.equals(publication.getPublication().getPK().getId()))
										pubIndex = p;
								}
								int nbPubliPerPage = kmelia.getNbPublicationsPerPage();
								int ipage = pubIndex/nbPubliPerPage;
								kmelia.setIndexOfFirstPubToDisplay(Integer.toString(ipage*nbPubliPerPage));
								
								request.setAttribute("PubIdToHighlight", id);
								
								destination = getDestination("GoToCurrentTopic", kmelia, request);
							}
							else
							{
								request.setAttribute("FileAlreadyOpened", fileAlreadyOpened);
								
								processPath(kmelia,id);
								destination = getDestination("ViewPublication", kmelia, request);
							}
						} else
							destination = "/admin/jsp/accessForbidden.jsp";
					}
					catch (Exception e)
					{
						destination = getDocumentNotFoundDestination(kmelia, request);
					}
				}
				else if (type.equals("Node"))
				{
					if (kmaxMode)
					{
						//Simuler l'action d'un utilisateur ayant s�lectionn� la valeur id d'un axe
						//SearchCombination est un chemin /0/4/i
						NodeDetail node = kmelia.getNodeHeader(id);
						String path = node.getPath()+id;
						
						request.setAttribute("SearchCombination", path);
						
						destination = getDestination("KmaxSearch", componentSC, request);
					}
					else
					{
						try
						{
							request.setAttribute("Id", id);
							destination = getDestination("GoToTopic", componentSC, request);
						}
						catch (Exception e)
						{
							destination = getDocumentNotFoundDestination(kmelia, request);
						}
					}
				}
				else if (type.equals("Wysiwyg"))
				{
					if (id.startsWith("Node")) {
						id = id.substring(5, id.length());
						request.setAttribute("Id", id);
						destination = getDestination("GoToTopic", componentSC, request);
					} else {
						/*if (kmaxMode)
							destination = getDestination("KmaxViewPublication", kmelia, request);
						else*/
							destination = getDestination("ViewPublication", kmelia, request);
					}
				}
				else
				{
					request.setAttribute("Id", "0");
					destination = getDestination("GoToTopic", componentSC, request);
				}
			}
			else if (function.startsWith("GoToFilesTab")) {
				String id = request.getParameter("Id");
				try
				{
		            UserCompletePublication userPubComplete = kmelia.getUserCompletePublication(id);
					kmelia.setSessionPublication(userPubComplete);
					kmelia.setSessionOwner(true);
					processPath(kmelia,id);
		            destination = getDestination("ViewAttachments", kmelia, request);
				}
				catch (Exception e)
				{
					destination = getDocumentNotFoundDestination(kmelia, request);
				}
			}
				
			else if (function.startsWith("publicationManager")) {
				String flag = kmelia.getProfile();
				request.setAttribute("Wizard", kmelia.getWizard());
				destination = rootDestination + "publicationManager.jsp?Profile=" + flag;
			}
			
			else if (function.equals("ToAddTopic"))
			{
				String isLink = request.getParameter("IsLink");
				if (StringUtil.isDefined(isLink))
					request.setAttribute("IsLink", new Boolean(true));
				
				request.setAttribute("Path", kmelia.getSessionPathString());
				request.setAttribute("PathLinked", kmelia.getSessionPath());
				request.setAttribute("Translation", kmelia.getCurrentLanguage());
				request.setAttribute("PopupDisplay", new Boolean(true));
				
				if (kmelia.isRightsOnTopicsEnabled())
				{
					request.setAttribute("PopupDisplay", new Boolean(false));
					request.setAttribute("Profiles", kmelia.getTopicProfiles());
					
					//Rights of the component
					request.setAttribute("RightsDependsOn", "ThisComponent");					
				}
				
				destination = rootDestination + "addTopic.jsp";
			}
			
			else if (function.equals("ToUpdateTopic"))
			{
				String id = request.getParameter("Id");
				
				NodeDetail node = kmelia.getSubTopicDetail(id);
				
				request.setAttribute("NodeDetail", node);
				request.setAttribute("Path", kmelia.getSessionPathString());
				request.setAttribute("PathLinked", kmelia.getSessionPath());
				request.setAttribute("Translation", kmelia.getCurrentLanguage());
				request.setAttribute("PopupDisplay", new Boolean(true));
				
				if (kmelia.isRightsOnTopicsEnabled())
				{
					request.setAttribute("PopupDisplay", new Boolean(false));
					request.setAttribute("Profiles", kmelia.getTopicProfiles(id));
					
					if (node.haveInheritedRights())
					{
						request.setAttribute("RightsDependsOn", "AnotherTopic");
					}
					else if (node.haveLocalRights())
					{
						request.setAttribute("RightsDependsOn", "ThisTopic");
					}
					else
					{
						//Rights of the component
						request.setAttribute("RightsDependsOn", "ThisComponent");					
					}
				}
				
				destination = rootDestination + "updateTopicNew.jsp";
			}
			
			else if (function.equals("AddTopic"))
			{
				String name 		= request.getParameter("Name");
		        String description 	= request.getParameter("Description");
		        String alertType 	= request.getParameter("AlertType");
		        String rightsUsed 	= request.getParameter("RightsUsed");
		        String path			= request.getParameter("Path");
		        
		        NodeDetail topic = new NodeDetail("-1", name, description, null, null, null, "0", "X");
		        I18NHelper.setI18NInfo(topic, request);
		        
		        if (StringUtil.isDefined(path))
		        {
		        	topic.setType(NodeDetail.FILE_LINK_TYPE);
		        	topic.setPath(path);
		        }

		        int rightsDependsOn = -1;
		        if (StringUtil.isDefined(rightsUsed))
		        {
		        	if (rightsUsed.equalsIgnoreCase("father"))
		        	{
		        		NodeDetail father = kmelia.getSessionTopic().getNodeDetail();
			        	rightsDependsOn = father.getRightsDependsOn();
		        	}
		        	else
		        	{
		        		rightsDependsOn = 0;
		        	}
		        	topic.setRightsDependsOn(rightsDependsOn);
		        }
		        
		        NodePK nodePK = kmelia.addSubTopic(topic, alertType);
		        
		        if (kmelia.isRightsOnTopicsEnabled())
		        {
		        	if (rightsDependsOn == 0)
		        	{
		        		request.setAttribute("NodeId", nodePK.getId());
		        		destination = getDestination("ViewTopicProfiles", componentSC, request);
		        	}
		        	else
		        	{
		        		destination = getDestination("GoToCurrentTopic", componentSC, request);
		        	}
		        }
		        else
		        {
		        	request.setAttribute("urlToReload", "GoToCurrentTopic");
		        	destination = rootDestination + "closeWindow.jsp";
		        }
			}
			else if (function.equals("UpdateTopic"))
			{
				String name 		= request.getParameter("Name");
		        String description 	= request.getParameter("Description");
		        String alertType 	= request.getParameter("AlertType");
		        String id			= request.getParameter("ChildId");
		        String path			= request.getParameter("Path");
		        
		        NodeDetail topic = new NodeDetail(id, name, description, null, null, null, "0", "X");
		        I18NHelper.setI18NInfo(topic, request);
		        
		        if (StringUtil.isDefined(path))
		        {
		        	topic.setType(NodeDetail.FILE_LINK_TYPE);
		        	topic.setPath(path);
		        }
		        
		        kmelia.updateTopicHeader(topic, alertType);
		        
		        if (kmelia.isRightsOnTopicsEnabled())
		        {
		        	int rightsUsed = Integer.parseInt(request.getParameter("RightsUsed"));
		        	
		        	topic = kmelia.getNodeHeader(id);
		        	
		        	if (topic.getRightsDependsOn() != rightsUsed)
		        	{
		        		//rights dependency have changed
		        		if (rightsUsed == -1)
		        		{
		        			kmelia.updateTopicDependency(topic, false);
		        			destination = getDestination("GoToCurrentTopic", componentSC, request);
		        		}
		        		else
		        		{
		        			kmelia.updateTopicDependency(topic, true);
		        			
		        			request.setAttribute("NodeId", id);
		        			
		        			destination = getDestination("ViewTopicProfiles", componentSC, request);
		        		}
		        	}
		        	else
		        	{
		        		destination = getDestination("GoToCurrentTopic", componentSC, request);
		        	}
		        }
		        else
		        {
		        	request.setAttribute("urlToReload", "GoToCurrentTopic");
		        	destination = rootDestination + "closeWindow.jsp";
		        }
			}
			else if (function.equals("DeleteTopic"))
			{
				String id = (String) request.getParameter("Id");
				
				kmelia.deleteTopic(id);
				
				destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			else if (function.equals("ViewClone"))
			{
				PublicationDetail pubDetail = kmelia.getSessionPublication().getPublication().getPublicationDetail();

				//Reload clone and put it into session
				String cloneId = pubDetail.getCloneId();
				UserCompletePublication userPubComplete = kmelia.getUserCompletePublication(cloneId);
				kmelia.setSessionClone(userPubComplete);
				
				request.setAttribute("Publication", userPubComplete);
				request.setAttribute("Profile", kmelia.getProfile());
				
				putXMLDisplayerIntoRequest(userPubComplete.getPublication().getPublicationDetail(), kmelia, request);
				
				destination = rootDestination + "clone.jsp";
			}
			else if (function.equals("ViewPublication"))
			{
				String id = request.getParameter("PubId");
				if (!StringUtil.isDefined(id))
				{
					id = request.getParameter("Id");
					if (!StringUtil.isDefined(id))
						id = (String) request.getAttribute("PubId");
				}
				
				String checkPath = request.getParameter("CheckPath");
				if (checkPath != null && "1".equals(checkPath))
				{
					processPath(kmelia,id);
				}
							
				UserCompletePublication userPubComplete = null;
				if (StringUtil.isDefined(id))
				{
					userPubComplete = kmelia.getUserCompletePublication(id);
					kmelia.setSessionPublication(userPubComplete);
					
					PublicationDetail pubDetail = userPubComplete.getPublication().getPublicationDetail();
					if (pubDetail.haveGotClone())
					{
						UserCompletePublication clone = kmelia.getUserCompletePublication(pubDetail.getCloneId());
						kmelia.setSessionClone(clone);
					}
				}
				else
				{
					userPubComplete = kmelia.getSessionPublication();
					id = userPubComplete.getPublication().getPublicationDetail().getPK().getId();
				}
				if (toolboxMode)
				{
					destination = rootDestination + "publicationManager.jsp?Action=UpdateView&PubId="+id+"&Profile="+kmelia.getProfile();
				}
				else
				{
					List publicationLanguages = kmelia.getPublicationLanguages(); //languages of publication header and attachments
					if (publicationLanguages.contains(kmelia.getCurrentLanguage()))
						request.setAttribute("ContentLanguage", kmelia.getCurrentLanguage());
					else
						request.setAttribute("ContentLanguage", checkLanguage(kmelia, userPubComplete.getPublication().getPublicationDetail()));
					request.setAttribute("Languages", publicationLanguages);
					
					request.setAttribute("Publication", userPubComplete);
					request.setAttribute("PubId", id);
					request.setAttribute("ValidationStep", kmelia.getValidationStep());
					request.setAttribute("ValidationType", new Integer(kmelia.getValidationType()));
					
					//check if user is writer with approval right (versioning case)
					request.setAttribute("WriterApproval", new Boolean(kmelia.isWriterApproval(id)));
					
					//check is requested publication is an alias
					checkAlias(kmelia, userPubComplete);
				
					if (userPubComplete.isAlias())
					{
						request.setAttribute("Profile", "user");
						request.setAttribute("IsAlias", "1");
					}
					else
						request.setAttribute("Profile", kmelia.getProfile());
					
					request.setAttribute("Wizard", kmelia.getWizard());
					
					request.setAttribute("Rang", new Integer(kmelia.getRang()));
					if (kmelia.getSessionPublicationsList() != null)
						request.setAttribute("NbPublis", new Integer(kmelia.getSessionPublicationsList().size()));
					else
						request.setAttribute("NbPublis", new Integer(1));

					putXMLDisplayerIntoRequest(userPubComplete.getPublication().getPublicationDetail(), kmelia, request);
					
					String fileAlreadyOpened = (String) request.getAttribute("FileAlreadyOpened");
					boolean alreadyOpened = "1".equals(fileAlreadyOpened);
					if (!alreadyOpened && kmelia.openSingleAttachmentAutomatically() && !kmelia.isCurrentPublicationHaveContent())
					{
						PublicationPK pubPK = userPubComplete.getPublication().getPublicationDetail().getPK();
						String url = null;
						if (kmelia.isVersionControlled())
						{
							VersioningUtil versioning = new VersioningUtil();
							List documents = versioning.getDocuments(new ForeignPK(pubPK));
							if (documents.size() == 1)
							{
								Document document = (Document) documents.get(0);
								DocumentVersion documentVersion = versioning.getLastPublicVersion(document.getPk());
								if (documentVersion != null)
									url = versioning.getDocumentVersionURL(document.getInstanceId(), documentVersion.getLogicalName(), document.getPk().getId(), documentVersion.getPk().getId());
							}
						}
						else
						{
							Vector attachments = AttachmentController.searchAttachmentByPKAndContext(pubPK, "Images");
							if (attachments.size() == 1)
							{
								AttachmentDetail attachment = (AttachmentDetail) attachments.get(0);
								url = attachment.getAttachmentURL();
							}
						}
						request.setAttribute("SingleAttachmentURL", url);
					}
	
					destination = rootDestination + "publication.jsp";
				}
			}
			else if (function.equals("PreviousPublication"))
			{
				// r�cup�ration de la publication pr�c�dente
				String pubId = kmelia.getPrevious();
				request.setAttribute("PubId", pubId);
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("NextPublication"))
			{
				// r�cup�ration de la publication suivante
				String pubId = kmelia.getNext();
				request.setAttribute("PubId", pubId);
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.startsWith("copy")) {
				String objectType = request.getParameter("Object");
				String objectId   = request.getParameter("Id");
				if (StringUtil.isDefined(objectType) && "Node".equalsIgnoreCase(objectType))
					kmelia.copyTopic(objectId);
				else
					kmelia.copyPublication(objectId);
				destination = URLManager.getURL(URLManager.CMP_CLIPBOARD) + "Idle.jsp?message=REFRESHCLIPBOARD" ;
			}
			else if (function.startsWith("cut")) {
				String objectType = request.getParameter("Object");
				String objectId   = request.getParameter("Id");
				if (StringUtil.isDefined(objectType) && "Node".equalsIgnoreCase(objectType))
					kmelia.cutTopic(objectId);
				else
					kmelia.cutPublication(objectId);
				destination = URLManager.getURL(URLManager.CMP_CLIPBOARD) + "Idle.jsp?message=REFRESHCLIPBOARD" ;
			}
			else if (function.startsWith("paste")) {
				//processPublicationsPaste(kmelia);
				kmelia.paste();
				destination = URLManager.getURL(URLManager.CMP_CLIPBOARD) + "Idle.jsp";
			}

			/*************************************************************/
			/** SCO - 26/12/2002 Integration AlertUser et AlertUserPeas **/
			/*************************************************************/
			else if(function.startsWith("ToAlertUser")){ //utilisation de alertUser et alertUserPeas
				SilverTrace.debug("kmelia","KmeliaRequestRooter.getDestination()","root.MSG_GEN_PARAM_VALUE","ToAlertUser: function = "+function+" spaceId="+kmelia.getSpaceId()+" componentId="+ kmelia.getComponentId());
				try{
					destination = kmelia.initAlertUser();
				}
				catch(Exception e){
					SilverTrace.warn("kmelia","KmeliaRequestRooter.getDestination()","root.EX_USERPANEL_FAILED","function = "+function, e);
				}
				SilverTrace.debug("kmelia","KmeliaRequestRooter.getDestination()","root.MSG_GEN_PARAM_VALUE","ToAlertUser: function = "+function+"=> destination="+destination);
			}
			/*************************************************************/
			else if (function.equals("ReadingControl"))
			{
				PublicationDetail publication = kmelia.getSessionPublication().getPublication().getPublicationDetail();
				request.setAttribute("LinkedPathString", kmelia.getSessionPath());
				request.setAttribute("Publication", publication);

		        // param�tre du wizard
				request.setAttribute("Wizard", kmelia.getWizard());
				
				destination = rootDestination + "readingControlManager.jsp";
			}
			else if (function.startsWith("ViewAttachments"))
			{
				String flag = kmelia.getProfile();
				
				if (kmelia.isCloneNeeded())
				{
					kmelia.clonePublication();
				}
				//put current publication
				request.setAttribute("CurrentPublicationDetail", kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail());
				
				// Param�tres du wizard
				setWizardParams(request, kmelia);
				
				//Param�tres de i18n
				List attachmentLanguages = kmelia.getAttachmentLanguages();
				if (attachmentLanguages.contains(kmelia.getCurrentLanguage()))
					request.setAttribute("Language", kmelia.getCurrentLanguage());
				else
					request.setAttribute("Language", checkLanguage(kmelia));
				request.setAttribute("Languages", attachmentLanguages);
				destination = rootDestination + "attachmentManager.jsp?profile=" + flag;
			}
			else if (function.equals("DeletePublication"))
			{
				String pubId = (String) request.getParameter("PubId");
				kmelia.deletePublication(pubId);
				
				if (kmaxMode)
					destination = getDestination("Main", kmelia, request);
				else
					destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			else if (function.equals("DeleteClone"))
			{
				kmelia.deleteClone();
				
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("ViewValidationSteps"))
			{
				request.setAttribute("LinkedPathString", kmelia.getSessionPath());
				request.setAttribute("Publication", kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail());
				request.setAttribute("ValidationSteps", kmelia.getValidationSteps());
				
				request.setAttribute("Role", kmelia.getProfile());
				
				destination = rootDestination + "validationSteps.jsp";
			}
			else if (function.equals("ValidatePublication"))
			{
				//String pubId = (String) request.getParameter("PubId");
				String pubId = kmelia.getSessionPublication().getPublication().getPublicationDetail().getPK().getId();
				SilverTrace.debug("kmelia","KmeliaRequestRooter.getDestination()","root.MSG_GEN_PARAM_VALUE","function = "+function+" pubId="+pubId);

				boolean validationComplete = kmelia.validatePublication(pubId);
			
				if (validationComplete)
					request.setAttribute("Action", "ValidationComplete");
				else
					request.setAttribute("Action", "ValidationInProgress");
				
				request.setAttribute("PubId", pubId);
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("ForceValidatePublication"))
			{
				String pubId = kmelia.getSessionPublication().getPublication().getPublicationDetail().getPK().getId();

				kmelia.forcePublicationValidation(pubId);
			
				request.setAttribute("Action", "ValidationComplete");
				
				request.setAttribute("PubId", pubId);
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("WantToRefusePubli"))
			{
				PublicationDetail pubDetail = kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail();

				request.setAttribute("PublicationToRefuse", pubDetail);

				destination = rootDestination + "refusalMotive.jsp";
			}
			else if (function.equals("Unvalidate"))
			{
				String motive	= request.getParameter("Motive");
				
				String pubId = kmelia.getSessionPublication().getPublication().getPublicationDetail().getPK().getId();
				SilverTrace.debug("kmelia","KmeliaRequestRooter.getDestination()","root.MSG_GEN_PARAM_VALUE","function = "+function+" pubId="+pubId);

				kmelia.unvalidatePublication(pubId, motive);
			
				request.setAttribute("Action", "Unvalidate");
				
				if (kmelia.getSessionClone() != null)
					destination = getDestination("ViewClone", kmelia, request);
				else
					destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("WantToSuspendPubli"))
			{
				String pubId = request.getParameter("PubId");

				PublicationDetail pubDetail = kmelia.getPublicationDetail(pubId);

				request.setAttribute("PublicationToSuspend", pubDetail);

				destination = rootDestination + "defermentMotive.jsp";
			}
			else if (function.equals("SuspendPublication"))
			{
				String motive	= request.getParameter("Motive");
				String pubId	= request.getParameter("PubId");

				kmelia.suspendPublication(pubId, motive);

				request.setAttribute("Action", "Suspend");
				
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("DraftIn"))
			{
				kmelia.draftInPublication();

				String pubId = kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail().getPK().getId();
				String flag = kmelia.getProfile();
				String from = request.getParameter("From");
				if (StringUtil.isDefined(from))
					destination = getDestination(from, componentSC, request);
				else
					destination = rootDestination + "publicationManager.jsp?Action=UpdateView&PubId="+pubId+"&Profile="+flag;
			}
			else if (function.equals("DraftOut"))
			{
				kmelia.draftOutPublication();

				String pubId 	= kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail().getPK().getId();
				String flag 	= kmelia.getProfile();
				String from = request.getParameter("From");
				if (StringUtil.isDefined(from))
					destination = getDestination(from, componentSC, request);
				else
					destination = rootDestination + "publicationManager.jsp?Action=UpdateView&PubId="+pubId+"&Profile="+flag;
			}
			else if (function.equals("ToTopicWysiwyg"))
			{
				String topicId		= request.getParameter("Id");
				String subTopicId	= request.getParameter("ChildId");
				String flag			= kmelia.getProfile();
				
				NodeDetail topic = kmelia.getSubTopicDetail(subTopicId);

				destination = URLManager.getHttpMode()+kmelia.getServerNameAndPort()+URLManager.getApplicationURL()+"/wysiwyg/jsp/htmlEditor.jsp?";
				destination += "SpaceId="+kmelia.getSpaceId();
				destination += "&SpaceName="+URLEncoder.encode(kmelia.getSpaceLabel(), "ISO-8859-1");
				destination += "&ComponentId="+kmelia.getComponentId();
				destination += "&ComponentName="+URLEncoder.encode(kmelia.getComponentLabel(), "ISO-8859-1");
				destination += "&BrowseInfo="+URLEncoder.encode(kmelia.getSessionPathString()+" > "+topic.getName()+" > "+kmelia.getString("TopicWysiwyg"), "ISO-8859-1");
				destination += "&ObjectId=Node_"+subTopicId;
				destination += "&Language=fr";
				destination += "&ReturnUrl="+URLEncoder.encode(URLManager.getApplicationURL()+URLManager.getURL(kmelia.getSpaceId(), kmelia.getComponentId())+"FromTopicWysiwyg?Action=Search&Id="+topicId+"&ChildId="+subTopicId+"&Profile="+flag, "ISO-8859-1");
			}
			else if (function.equals("FromTopicWysiwyg"))
			{
				String subTopicId	= request.getParameter("ChildId");

				kmelia.processTopicWysiwyg(subTopicId);
				
				destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			else if (function.equals("TopicUp"))
			{
				String subTopicId = request.getParameter("ChildId");

				kmelia.changeSubTopicsOrder("up", subTopicId);
				
				destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			else if (function.equals("TopicDown"))
			{
				String subTopicId = request.getParameter("ChildId");

				kmelia.changeSubTopicsOrder("down", subTopicId);
				
				destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			else if (function.equals("ChangeTopicStatus"))
			{
				String subTopicId	= request.getParameter("ChildId");
				String newStatus	= request.getParameter("Status");
				String recursive	= request.getParameter("Recursive");

				if (recursive != null && recursive.equals("1"))
				{
					kmelia.changeTopicStatus(newStatus, subTopicId, true);
				}
				else
				{
					kmelia.changeTopicStatus(newStatus, subTopicId, false);
				}

				destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			else if (function.equals("ViewOnly"))
			{
				String id = request.getParameter("documentId");
				destination = rootDestination + "publicationViewOnly.jsp?Id="+id;
			}
			else if (function.equals("SeeAlso"))
			{
				String action	= request.getParameter("Action");
				if (!StringUtil.isDefined(action))
					action = "LinkAuthorView";
				
				request.setAttribute("Action", action);
				
				//check if requested publication is an alias
				UserCompletePublication userPubComplete = kmelia.getSessionPublication();
				checkAlias(kmelia, userPubComplete);
			
				if (userPubComplete.isAlias())
				{
					request.setAttribute("Profile", "user");
					request.setAttribute("IsAlias", "1");
				}
				else
					request.setAttribute("Profile", kmelia.getProfile());
				
				// param�tres du wizard
				request.setAttribute("Wizard", kmelia.getWizard());
						
				destination = rootDestination + "seeAlso.jsp";
			}
			else if (function.equals("DeleteSeeAlso"))
			{
				String[] 	pubIds 	= request.getParameterValues("PubIds");

				List 	infoLinks	= new ArrayList();
				String	id			= null;
				for (int p=0; pubIds != null && p<pubIds.length; p++)
				{
					id = pubIds[p];
					infoLinks.add(id);
				}

				if (infoLinks.size()>0)
					kmelia.deleteInfoLinks(kmelia.getSessionPublication().getId(), infoLinks);
				
				destination = getDestination("SeeAlso", kmelia, request);
			}
			else if (function.equals("ImportFileUpload"))
			{
				destination = processFormUpload(kmelia, request, rootDestination, false);
			}
			else if (function.equals("ImportFilesUpload"))
			{
				destination = processFormUpload(kmelia, request, rootDestination, true);
			}
			else if (function.equals("ExportAttachementsToPDF"))
			{
				String topicId	= (String) request.getParameter("TopicId");
				//build an exploitable list by importExportPeas
				SilverTrace.info("kmelia", "KmeliaSessionController.getAllVisiblePublicationsByTopic()", "root.MSG_PARAM_VALUE", "topicId =" + topicId);
				List publicationsIds = kmelia.getAllVisiblePublicationsByTopic(topicId);
				request.setAttribute("selectedResultsWa", publicationsIds);
				request.setAttribute("RootId", topicId);
				//Go to importExportPeas
				destination = "/RimportExportPeas/jsp/ExportPDF";
			}
			else if (function.equals("DeleteVignette"))
			{
				String pubId = request.getParameter("PubId");

				kmelia.deleteVignette(pubId);

				destination = rootDestination + "publicationManager.jsp?Action=UpdateView&PubId="+pubId+"&Profile="+kmelia.getProfile();
			}
			else if (function.equals("NewPublication")) 
			{
				destination = rootDestination + "publicationManager.jsp?Action=New&CheckPath=0&Profile="+kmelia.getProfile();
			}
			else if (function.equals("AddPublication"))
			{
				List parameters = FileUploadUtil.parseRequest(request);
				
				List vignetteParams = processVignette(parameters, kmelia);
				
				PublicationDetail pubDetail = getPublicationDetail(parameters, vignetteParams, kmelia);
					    
				String newPubId = kmelia.createPublication(pubDetail);
		        request.setAttribute("PubId", newPubId);
		        processPath(kmelia,newPubId);
		        
		        String wizard = kmelia.getWizard();
		        if (wizard.equals("progress"))
		        {
		        	UserCompletePublication userPubComplete = kmelia.getUserCompletePublication(newPubId);
					kmelia.setSessionPublication(userPubComplete);
					String position	= FileUploadUtil.getParameter(parameters, "Position");

					setWizardParams(request, kmelia);
					
					request.setAttribute("Position", position);
					request.setAttribute("Publication", userPubComplete);
					request.setAttribute("Profile", kmelia.getProfile());
		        	
		        	destination = getDestination("WizardNext", kmelia, request);
		        }
		        else
		        {
		        	destination = getDestination("ViewPublication", kmelia, request);
		        }
			}
			else if (function.equals("UpdatePublication"))
			{
				List parameters = FileUploadUtil.parseRequest(request);
				
				List vignetteParams = processVignette(parameters, kmelia);
				
				PublicationDetail pubDetail = getPublicationDetail(parameters, vignetteParams, kmelia);
		        kmelia.updatePublication(pubDetail);
		        	        
		        String id = pubDetail.getPK().getId();
		        
		        String wizard = kmelia.getWizard();
		        if (wizard.equals("progress"))
		        {
		        	UserCompletePublication userPubComplete = kmelia.getUserCompletePublication(id);
		        	String position	= FileUploadUtil.getParameter(parameters, "Position");
					
		        	setWizardParams(request, kmelia);
					
		        	request.setAttribute("Position", position);
					request.setAttribute("Publication", userPubComplete);
					request.setAttribute("Profile", kmelia.getProfile());
		        	
		        	destination = getDestination("WizardNext", kmelia, request);
		        }
		        else
		        {
			        if (kmelia.getSessionClone() != null)
			        {
			        	destination = getDestination("ViewClone", kmelia, request);
			        }
			        else
			        {
			        	request.setAttribute("PubId", id);
				        request.setAttribute("CheckPath", "1");
				        destination = getDestination("ViewPublication", kmelia, request);
			        }
		        }
			}
			else if (function.equals("SelectValidator"))
			{
				destination = kmelia.initUPToSelectValidator("");
			}
			else if (function.equals("Comments"))
			{
				String id 	= (String) request.getParameter("PubId");
				String flag = kmelia.getProfile();
				if (!kmaxMode)
					processPath(kmelia,id);
				
				// param�tre du wizard
			    request.setAttribute("Wizard",kmelia.getWizard());
				
				destination = rootDestination + "comments.jsp?PubId="+id+"&Profile="+flag;
			}
			else if (function.equals("PublicationPaths"))
			{
				// param�tre du wizard
			    request.setAttribute("Wizard", kmelia.getWizard());
			    
			    PublicationDetail publication = kmelia.getSessionPublication().getPublication().getPublicationDetail();
			    String pubId = publication.getPK().getId();
			    request.setAttribute("Publication", publication);
			    request.setAttribute("PathList",kmelia.getPublicationFathers(pubId));
			    request.setAttribute("LinkedPathString", kmelia.getSessionPath());
			    request.setAttribute("Topics", kmelia.getAllTopics());
			    
			    List aliases = kmelia.getAliases();
			    request.setAttribute("Aliases", aliases);
			    request.setAttribute("OtherComponents", kmelia.getOtherComponents(aliases));
			    
			    destination = rootDestination + "publicationPaths.jsp";
			}
			else if (function.equals("SetPath"))
			{
				String[] topics = request.getParameterValues("topicChoice");
				
				Alias alias = null;
				List aliases = new ArrayList();
				for (int i = 0; topics != null && i < topics.length; i++)
				{
					String topicId = topics[i];
					SilverTrace.debug("kmelia", "KmeliaRequestRouter.setPath()", "root.MSG_GEN_PARAM_VALUE", "topicId = " + topicId);
					StringTokenizer tokenizer = new StringTokenizer(topicId,",");
					String nodeId = tokenizer.nextToken();
					String instanceId = tokenizer.nextToken();
					
					alias = new Alias(nodeId, instanceId);
					alias.setUserId(kmelia.getUserId());
					aliases.add(alias);
				}
				kmelia.setAliases(aliases);
				
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("SelectPath"))
			{
				// modification de la liste des emplacements de la publication
				String[] topics = request.getParameterValues("topicChoice");
				String pubId = request.getParameter("PubId");
				setPubPath(kmelia, pubId, topics);
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.equals("ShowAliasTree"))
			{
				String componentId = request.getParameter("ComponentId");
				
				request.setAttribute("Tree", kmelia.getAliasTreeview(componentId));
			    request.setAttribute("Aliases", kmelia.getAliases());
				
				destination = rootDestination + "treeview4PublicationPaths.jsp";
			}
			else if (function.equals("AddLinksToPublication"))
			{
				String id 		= (String) request.getParameter("PubId");
				String topicId 	= (String) request.getParameter("TopicId");
				
				processPublicationsToLink(kmelia, request);
				
				int nb = kmelia.addPublicationsToLink(id);
				
				request.setAttribute("NbLinks", Integer.toString(nb));
							
				destination = rootDestination + "publicationLinksManager.jsp?Action=Add&Id="+topicId;
			}
			else if (function.equals("ExportComponent"))
			{
				if (kmaxMode)
				{
					destination = getDestination("KmaxExportComponent", kmelia, request);					
				}
				else
				{
					//build an exploitable list by importExportPeas
					List publicationsIds = kmelia.getAllVisiblePublications();
					request.setAttribute("selectedResultsWa", publicationsIds);
					request.setAttribute("RootId", "0");
					//Go to importExportPeas
					destination = "/RimportExportPeas/jsp/ExportItems";
				}
			}
			else if (function.equals("ExportTopic"))
			{
				if (kmaxMode)
				{
					destination = getDestination("KmaxExportPublications", kmelia, request);					
				}
				else
				{
					// r�cup�ration du topicId
					String topicId	= (String) request.getParameter("TopicId");
					//build an exploitable list by importExportPeas
					SilverTrace.info("kmelia", "KmeliaSessionController.getAllVisiblePublicationsByTopic()", "root.MSG_PARAM_VALUE", "topicId =" + topicId);
					List publicationsIds = kmelia.getAllVisiblePublicationsByTopic(topicId);
					request.setAttribute("selectedResultsWa", publicationsIds);
					request.setAttribute("RootId", topicId);
					//Go to importExportPeas
					destination = "/RimportExportPeas/jsp/ExportItems";
				}
			}
			else if (function.equals("ToPubliContent"))
			{
				CompletePublication completePublication = kmelia.getSessionPublication().getPublication();
				if (kmelia.getSessionClone() != null)
					completePublication = kmelia.getSessionClone().getPublication();
							
				if (completePublication.getModelDetail() != null)
				{
					destination = getDestination("ToDBModel", kmelia, request);
				} 
				else if (WysiwygController.haveGotWysiwyg(kmelia.getSpaceId(), kmelia.getComponentId(), completePublication.getPublicationDetail().getPK().getId())) 
				{
					destination = getDestination("ToWysiwyg", kmelia, request);
				} 
				else
				{
					String infoId = completePublication.getPublicationDetail().getInfoId();
					if (infoId == null || "0".equals(infoId))
					{
						List usedModels = (List) kmelia.getModelUsed();
						if (usedModels.size() == 1)
						{
							String modelId = (String) usedModels.get(0);
							if ("WYSIWYG".equals(modelId))
							{
								//Wysiwyg content
								destination = getDestination("ToWysiwyg", kmelia, request);
							}
							else if (isInteger(modelId))
							{
								//DB template
								ModelDetail model = kmelia.getModelDetail(modelId);
								request.setAttribute("ModelDetail", model);
								destination = getDestination("ToDBModel", kmelia, request);
							}
							else
							{
								//XML template
								setXMLForm(request, kmelia, modelId);
								
								//put current publication
								request.setAttribute("CurrentPublicationDetail", kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail());
								
								// Parametres du Wizard
								setWizardParams(request, kmelia);
								
								destination = rootDestination + "xmlForm.jsp";
							}
						}
						else
						{
							destination = getDestination("ListModels", kmelia, request);
						}
					}
					else
					{
						destination = getDestination("GoToXMLForm", kmelia, request);
					}
				}
			}
			else if (function.equals("ListModels"))
			{
				Collection modelUsed = kmelia.getModelUsed();
				Collection listModelXml = new ArrayList();
				List templates = new ArrayList();
				try {
					templates = PublicationTemplateManager.getPublicationTemplates();
					// recherche de la liste des mod�les utilisables
					PublicationTemplate xmlForm;
					Iterator iterator = templates.iterator();
				    while (iterator.hasNext()) 
				    {
				        xmlForm = (PublicationTemplate) iterator.next();
				        // recherche si le mod�le est dans la liste
				        if (modelUsed.contains(xmlForm.getFileName()))
				        {
				        	listModelXml.add(xmlForm);
				        }
				    }
				    
				    request.setAttribute("XMLForms", listModelXml);
				} catch (Exception e) {
					SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination(ListModels)", "root.MSG_GEN_PARAM_VALUE","", e);
				}
				
				//put dbForms
				Collection dbForms = kmelia.getAllModels();
				// recherche de la liste des mod�les utilisables
				Collection listModelForm = new ArrayList();
				ModelDetail modelDetail;
				Iterator iterator = dbForms.iterator();
			    while (iterator.hasNext()) 
			    {
			        modelDetail = (ModelDetail) iterator.next();
			        // recherche si le mod�le est dans la liste
			        if (modelUsed.contains(modelDetail.getId()))
			        {
			        	listModelForm.add(modelDetail);
			        }
			    }
				request.setAttribute("DBForms", listModelForm);
				
				// recherche si modele Wysiwyg utilisable
				boolean wysiwygValid = false;
				if (modelUsed.contains("WYSIWYG"))
		        {
		        	wysiwygValid = true;
		        }
				request.setAttribute("WysiwygValid", new Boolean(wysiwygValid));
				
				// s'il n'y a pas de mod�les selectionn�s, les pr�senter tous
				if (((listModelXml == null) || (listModelXml.isEmpty())) && ((listModelForm == null) || (listModelForm.isEmpty())) && (!wysiwygValid))
				{
					request.setAttribute("XMLForms", templates);
					request.setAttribute("DBForms", dbForms);
					request.setAttribute("WysiwygValid", new Boolean(true));
				}
				//put current publication
				request.setAttribute("CurrentPublicationDetail", kmelia.getSessionPublication().getPublication().getPublicationDetail());
				
				// Param�tres du wizard
				setWizardParams(request, kmelia);
				destination = rootDestination + "modelsList.jsp";
			}
			else if (function.equals("ModelUsed"))
			{
				try {
					List templates = PublicationTemplateManager.getPublicationTemplates();
					request.setAttribute("XMLForms", templates);
				} catch (Exception e) {
					SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination(ModelUsed)", "root.MSG_GEN_PARAM_VALUE","", e);
				}
				
				//put dbForms
				Collection dbForms = kmelia.getAllModels();
				request.setAttribute("DBForms", dbForms);
				
				Collection modelUsed = kmelia.getModelUsed();
				request.setAttribute("ModelUsed", modelUsed);
				
				destination = rootDestination + "modelUsedList.jsp";
			}
			else if (function.equals("SelectModel")) 
			{
                Object o = request.getParameterValues("modelChoice");
                if (o != null)
                {
                	String[] models = (String[]) o;
                   	kmelia.addModelUsed(models);
                }
                destination = getDestination("Main", kmelia, request);
			}
			else if (function.equals("ToWysiwyg"))
			{
				if (kmelia.isCloneNeeded())
				{
					kmelia.clonePublication();
				}
				//put current publication
				request.setAttribute("CurrentPublicationDetail", kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail());
				
				// Parametres du Wizard
				setWizardParams(request, kmelia);
				
				request.setAttribute("CurrentLanguage", checkLanguage(kmelia));
				
				destination = rootDestination + "toWysiwyg.jsp";
			}
			else if (function.equals("FromWysiwyg"))
			{
				String id = request.getParameter("PubId");
				
				// Parametres du Wizard
				String wizard = kmelia.getWizard();
				setWizardParams(request, kmelia);
				
				if (wizard.equals("progress"))
				{
					request.setAttribute("Position", "Content");
					destination = getDestination("WizardNext", kmelia, request);
				}
				else
				{
					if (kmelia.getSessionClone() != null && id.equals(kmelia.getSessionClone().getPublication().getPublicationDetail().getPK().getId()))
					{
						destination = getDestination("ViewClone", componentSC, request);
					}
					else
					{
						destination = getDestination("ViewPublication", componentSC, request);
					}
				}
			}
			else if (function.equals("ToDBModel"))
			{
				String modelId = request.getParameter("ModelId");
				if (StringUtil.isDefined(modelId))
				{
					ModelDetail model = kmelia.getModelDetail(modelId);
					request.setAttribute("ModelDetail", model);
				}
				
				//put current publication
				request.setAttribute("CompletePublication", kmelia.getSessionPubliOrClone().getPublication());
				
				// Param�tres du wizard
				setWizardParams(request, kmelia);
				
				destination = rootDestination + "modelManager.jsp";
			}
			else if (function.equals("UpdateDBModelContent"))
			{
				ResourceLocator publicationSettings = kmelia.getPublicationSettings();

				ArrayList textDetails = new ArrayList();
				ArrayList imageDetails = new ArrayList();
				String theText = null;
				int textOrder = 0;
				int imageOrder = 0;
				String logicalName = "";
				String physicalName = "";
				long size = 0;
				String type = "";
				String mimeType = "";
				File dir = null;
				InfoDetail infos = null;
				boolean imageTrouble = false;
				
				ResourceLocator settings = new ResourceLocator("com.stratelia.webactiv.util.attachment.Attachment", "");
		        boolean runOnUnix = settings.getBoolean("runOnSolaris", false);

				List parameters 		= FileUploadUtil.parseRequest(request);
				String modelId 			= FileUploadUtil.getParameter(parameters, "ModelId");
				
				// Parametres du Wizard
				setWizardParams(request, kmelia);
				
				Iterator iter = parameters.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
					if (item.isFormField() && item.getFieldName().startsWith("WATXTVAR")) {
						theText = item.getString();
				        textOrder = new Integer(item.getFieldName().substring(8, item.getFieldName().length())).intValue();
				        textDetails.add(new InfoTextDetail(null, new Integer(textOrder).toString(), null, theText));
					}
					else if (!item.isFormField())
					{
						logicalName = item.getName();
						if (logicalName != null && logicalName.length() > 0) {
					        // the part actually contained a file
							
					        if (runOnUnix)
			    			{
					        	logicalName = logicalName.replace('\\', File.separatorChar);
			    				SilverTrace.info("kmelia", "KmeliaRequestRouter.UpdateDBModelContent", "root.MSG_GEN_PARAM_VALUE", "fileName on Unix = "+logicalName);
			    			}
							
					        logicalName 	= logicalName.substring(logicalName.lastIndexOf(File.separator)+1, logicalName.length());
							type 			= logicalName.substring(logicalName.lastIndexOf(".")+1, logicalName.length());
							physicalName 	= new Long(new Date().getTime()).toString() + "." +type;
					        mimeType 		= item.getContentType();
					        size			= item.getSize();
					        
					        dir = new File(FileRepositoryManager.getAbsolutePath(kmelia.getComponentId())+publicationSettings.getString("imagesSubDirectory")+ File.separator +physicalName);
					        if (type.equalsIgnoreCase("gif") || type.equalsIgnoreCase("jpg") || type.equalsIgnoreCase("jpeg") || type.equalsIgnoreCase("png")) {
					          item.write(dir);
					          imageOrder++;
					          if (size > 0) {
					              imageDetails.add(new InfoImageDetail(null, new Integer(imageOrder).toString(), null, physicalName, logicalName, "", mimeType, size));
					              imageTrouble = false;
					          } else {
					              imageTrouble = true;
					          }
					        } else {
					          imageTrouble = true;
					        }
					    }  else {
					          // the field did not contain a file
					    }
					}
				}

				infos = new InfoDetail(null, textDetails, imageDetails, null, "");
				
				CompletePublication completePub = kmelia.getSessionPubliOrClone().getPublication();
				
				if (completePub.getModelDetail() == null)
					kmelia.createInfoModelDetail("useless", modelId, infos);
				else
					kmelia.updateInfoDetail("useless", infos);
								
				if (imageTrouble)
					request.setAttribute("ImageTrouble", new Boolean(true));
				
				String wizard = kmelia.getWizard();
				if (wizard.equals("progress"))
				{
					request.setAttribute("Position", "Content");
					destination = getDestination("WizardNext", kmelia, request);
				}
				else
					destination = getDestination("ToDBModel", kmelia, request);
			}
			else if (function.equals("GoToXMLForm"))
			{
				String xmlFormName = request.getParameter("Name");
				
				setXMLForm(request, kmelia, xmlFormName);
								
				//put current publication
				request.setAttribute("CurrentPublicationDetail", kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail());				
				
				// Parametres du Wizard
				setWizardParams(request, kmelia);
				
				destination = rootDestination + "xmlForm.jsp";
			}
			else if (function.equals("UpdateXMLForm"))
			{
				if (kmelia.isCloneNeeded())
				{
					kmelia.clonePublication();
				}
				
				List items = FileUploadUtil.parseRequest(request);
				
				PublicationDetail pubDetail = kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail();
				
				String xmlFormShortName = null;
				
				//Is it the creation of the content or an update ?
				String infoId = pubDetail.getInfoId();
				if (infoId == null || "0".equals(infoId))
				{					
					String 	xmlFormName = FileUploadUtil.getParameter(items, "Name");
					
					//The publication have no content
					//We have to register xmlForm to publication
					xmlFormShortName = xmlFormName.substring(xmlFormName.indexOf("/")+1, xmlFormName.indexOf("."));
					pubDetail.setInfoId(xmlFormShortName);
					kmelia.updatePublication(pubDetail);
				}
				else
				{
					xmlFormShortName = pubDetail.getInfoId();
				}
				
				String pubId = pubDetail.getPK().getId();
								
				PublicationTemplate pub = PublicationTemplateManager.getPublicationTemplate(kmelia.getComponentId()+":"+xmlFormShortName);
				
				RecordSet 	set 	= pub.getRecordSet();			
				Form 		form 	= pub.getUpdateForm();
				
				String language = checkLanguage(kmelia, pubDetail);
				
   				DataRecord data = set.getRecord(pubId, language);
   				if (data == null) {
   					data = set.getEmptyRecord();
					data.setId(pubId);
					data.setLanguage(language);
   				}
   				
   				PagesContext context = new PagesContext("myForm", "3", kmelia.getLanguage(), false, kmelia.getComponentId(), kmelia.getUserId());
   				if (!kmaxMode)
   					context.setNodeId(kmelia.getSessionTopic().getNodeDetail().getNodePK().getId());
   				context.setObjectId(pubId);
   				context.setContentLanguage(kmelia.getCurrentLanguage());
   				
				form.update(items, data, context);
				set.save(data);
				
				//update publication to change updateDate and updaterId
				kmelia.updatePublication(pubDetail);
				
				// Parametres du Wizard
				setWizardParams(request, kmelia);
				
				if (kmelia.getWizard().equals("progress"))
				{
					// on est en mode Wizard
					request.setAttribute("Position", "Content");
					destination = getDestination("WizardNext", kmelia, request);
				}
				else
				{
					if (kmelia.getSessionClone() != null)
						destination = getDestination("ViewClone", kmelia, request);
					else if (kmaxMode)
						destination = getDestination("ViewAttachments", kmelia, request);
					else
						destination = getDestination("ViewPublication", kmelia, request);
				}
			}
			else if (function.equals("GeneratePdf"))
			{
				String pubId = request.getParameter("PubId");
				String name,link = "";
				name = kmelia.generatePdf(pubId);
			    link =  FileServerUtils.getUrlToTempDir(name, name, "application/pdf");

				request.setAttribute("Action", "GeneratePdf");
				request.setAttribute("Link", link);
				
				destination = getDestination("ViewPublication", kmelia, request);
			}
			else if (function.startsWith("ToOrderPublications"))
			{
				List publications = kmelia.getSessionPublicationsList();
				
				request.setAttribute("Publications", publications);
				request.setAttribute("Path", kmelia.getSessionPath());
				
				destination = rootDestination + "orderPublications.jsp";
			}
			else if (function.startsWith("OrderPublications"))
			{
				String sortedIds = request.getParameter("sortedIds");
				
				StringTokenizer tokenizer = new StringTokenizer(sortedIds, ",");
				List ids = new ArrayList();
				while (tokenizer.hasMoreTokens())
				{
					ids.add(tokenizer.nextToken());
				}
				
				kmelia.orderPublications(ids);
				
				destination = getDestination("GoToCurrentTopic", kmelia, request);
			}
			else if (function.startsWith("Wizard"))
			{
				destination = processWizard(function, kmelia, request, rootDestination);
			}
			else if (function.equals("ViewPdcPositions"))
			{
				// Parametres du Wizard
				setWizardParams(request, kmelia);
				destination = rootDestination + "pdcPositions.jsp";
			}
			else if (function.equals("ViewTopicProfiles"))
			{
				String role = request.getParameter("Role");
				if (!StringUtil.isDefined(role))
					role = "admin";
				
				String id = request.getParameter("NodeId");
				if (!StringUtil.isDefined(id))
					id = (String) request.getAttribute("NodeId");
							
				request.setAttribute("Profiles", kmelia.getTopicProfiles(id));
				
				NodeDetail topic = kmelia.getNodeHeader(id);
				ProfileInst profile = null;
				if (topic.haveInheritedRights())
				{
					profile = kmelia.getTopicProfile(role, Integer.toString(topic.getRightsDependsOn()));
					
					request.setAttribute("RightsDependsOn", "AnotherTopic");
				}
				else if (topic.haveLocalRights())
				{
					profile = kmelia.getTopicProfile(role, Integer.toString(topic.getRightsDependsOn()));
					
					request.setAttribute("RightsDependsOn", "ThisTopic");
				}
				else
				{
					profile = kmelia.getProfile(role);
					
					//Rights of the component
					request.setAttribute("RightsDependsOn", "ThisComponent");					
				}
				
				request.setAttribute("CurrentProfile", profile);
				request.setAttribute("Groups", kmelia.groupIds2Groups(profile.getAllGroups()));
				request.setAttribute("Users", kmelia.userIds2Users(profile.getAllUsers()));
				request.setAttribute("Path", kmelia.getSessionPath());
				request.setAttribute("NodeDetail", topic);
				
				destination = rootDestination + "topicProfiles.jsp";
			}
			else if (function.equals("TopicProfileSelection"))
			{
				String role 	= request.getParameter("Role");
				String nodeId 	= request.getParameter("NodeId");
	        	try {
	            	kmelia.initUserPanelForTopicProfile(role, nodeId);
	            }
	            catch(Exception e){
					SilverTrace.warn("jobStartPagePeas","JobStartPagePeasRequestRouter.getDestination()","root.EX_USERPANEL_FAILED","function = "+function, e);
				}
				destination = Selection.getSelectionURL(Selection.TYPE_USERS_GROUPS);
			}
			else if (function.equals("TopicProfileSetUsersAndGroups"))
			{
				String role 	= request.getParameter("Role");
				String nodeId 	= request.getParameter("NodeId");
	        	
				kmelia.updateTopicRole(role, nodeId);
	        	
	        	request.setAttribute("urlToReload", "ViewTopicProfiles?Role="+role+"&NodeId="+nodeId);
	        	destination = rootDestination + "closeWindow.jsp";
			}
			else if (function.equals("TopicProfileRemove"))
			{
				String profileId = request.getParameter("Id");
				
	        	kmelia.deleteTopicRole(profileId);
	        	
	        	destination = getDestination("ViewTopicProfiles", componentSC, request);
			}
			else if (function.equals("CloseWindow"))
			{
				destination = rootDestination + "closeWindow.jsp";
			}
			/***************************
			 *  Kmax mode
			 **************************/
			else if (function.equals("KmaxMain")) 
			{
				destination = rootDestination + "kmax.jsp?Action=KmaxView&Profile="+kmelia.getProfile();
			}
			else if (function.equals("KmaxAxisManager")) 
			{
				destination = rootDestination + "kmax_axisManager.jsp?Action=KmaxViewAxis&Profile="+kmelia.getProfile();
			}
			else if (function.equals("KmaxAddAxis")) 
			{
			    String newAxisName = request.getParameter("Name");
			    String newAxisDescription = request.getParameter("Description");
			    NodeDetail axis = new NodeDetail("-1", newAxisName, newAxisDescription, DateUtil.today2SQLDate(), kmelia.getUserId(), null, "0", "X");
	            //I18N
		        I18NHelper.setI18NInfo(axis, request);
			    kmelia.addAxis(axis);
			    
			    request.setAttribute("urlToReload", "KmaxAxisManager");
				destination = rootDestination + "closeWindow.jsp";
			}
			else if (function.equals("KmaxUpdateAxis")) 
			{
				String axisId = request.getParameter("AxisId");
				String newAxisName = request.getParameter("AxisName");
				String newAxisDescription = request.getParameter("AxisDescription");
				NodeDetail axis = new NodeDetail(axisId, newAxisName, newAxisDescription, null, null, null, "0", "X");
		        //I18N
		        I18NHelper.setI18NInfo(axis, request);
				kmelia.updateAxis(axis);
				destination = getDestination("KmaxAxisManager", componentSC, request);
			}
			else if (function.equals("KmaxDeleteAxis")) 
			{
			    String axisId = request.getParameter("AxisId");
			    kmelia.deleteAxis(axisId);
				destination = getDestination("KmaxAxisManager", componentSC, request);
			}
			else if (function.equals("KmaxManageAxis")) 
			{
			    String axisId = request.getParameter("AxisId");
				String translation = request.getParameter("Translation");
				request.setAttribute("Translation", translation);
				destination = rootDestination + "kmax_axisManager.jsp?Action=KmaxManageAxis&Profile="+kmelia.getProfile()+"&AxisId="+axisId;
			}
			else if (function.equals("KmaxManagePosition")) 
			{
			    String positionId = request.getParameter("PositionId");
				String translation = request.getParameter("Translation");
				request.setAttribute("Translation", translation);
				destination = rootDestination + "kmax_axisManager.jsp?Action=KmaxManagePosition&Profile="+kmelia.getProfile()+"&PositionId="+positionId;
			}
			else if (function.equals("KmaxAddPosition")) 
			{
			    String axisId = request.getParameter("AxisId");
			    String newPositionName = request.getParameter("Name");
			    String newPositionDescription = request.getParameter("Description");
				String translation = request.getParameter("Translation");
			    NodeDetail position = new NodeDetail("toDefine", newPositionName, newPositionDescription, null, null, null, "0", "X");
	            //I18N
		        I18NHelper.setI18NInfo(position, request);
			    kmelia.addPosition(axisId, position);
			    request.setAttribute("AxisId", axisId);
				request.setAttribute("Translation", translation);
			    destination = getDestination("KmaxManageAxis", componentSC, request);
			}
			else if (function.equals("KmaxUpdatePosition")) 
			{
			    String positionId = request.getParameter("PositionId");
			    String positionName = request.getParameter("PositionName");
			    String positionDescription = request.getParameter("PositionDescription");
				NodeDetail position = new NodeDetail(positionId, positionName, positionDescription, null, null, null, "0", "X");
	            //I18N
		        I18NHelper.setI18NInfo(position, request);
			    kmelia.updatePosition(position);
				destination = getDestination("KmaxAxisManager", componentSC, request);
			}
			else if (function.equals("KmaxDeletePosition")) 
			{
			    String positionId = request.getParameter("PositionId");
			    kmelia.deletePosition(positionId);
				destination = getDestination("KmaxAxisManager", componentSC, request);
			}
			else if (function.equals("KmaxViewUnbalanced")) 
			{
				List publications = kmelia.getUnbalancedPublications();
				kmelia.setSessionPublicationsList(publications);
				kmelia.orderPubs();
				
				destination = rootDestination + "kmax.jsp?Action=KmaxViewUnbalanced&Profile="+kmelia.getProfile();
			}
			else if (function.equals("KmaxSearch")) 
			{
				String axisValuesStr 	= request.getParameter("SearchCombination");
				if (!StringUtil.isDefined(axisValuesStr))
					axisValuesStr 	= (String) request.getAttribute("SearchCombination");
				String timeCriteria 	= request.getParameter("TimeCriteria");
				
				SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination()", "root.MSG_GEN_PARAM_VALUE","axisValuesStr = "+axisValuesStr+" timeCriteria="+timeCriteria);
				ArrayList combination = kmelia.getCombination(axisValuesStr);
				List publications = null;
				if (StringUtil.isDefined(timeCriteria) && !timeCriteria.equals("X"))
					publications = kmelia.search(combination, new Integer(timeCriteria).intValue());
				else
					publications = kmelia.search(combination);
				SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination()", "root.MSG_GEN_PARAM_VALUE","publications = "+publications+" Combination="+combination+" timeCriteria="+timeCriteria);

				kmelia.setIndexOfFirstPubToDisplay("0");
				kmelia.orderPubs();
				kmelia.setSessionCombination(combination);
				kmelia.setSessionTimeCriteria(timeCriteria);

				destination = rootDestination + "kmax.jsp?Action=KmaxSearchResult&Profile="+kmelia.getProfile();
			}
			else if (function.equals("KmaxSearchResult")) 
			{
				if (kmelia.getSessionCombination() == null)
					destination = getDestination("KmaxMain", kmelia, request);
				else
					destination = rootDestination + "kmax.jsp?Action=KmaxSearchResult&Profile="+kmelia.getProfile();
			}
			else if (function.equals("KmaxViewCombination")) 
			{
				setWizardParams(request, kmelia);
				
				request.setAttribute("CurrentCombination", kmelia.getCurrentCombination());
				destination = rootDestination + "kmax_viewCombination.jsp?Profile="+kmelia.getProfile();
			}
			else if (function.equals("KmaxAddCoordinate")) 
			{
				String pubId = request.getParameter("PubId");
				String axisValuesStr = request.getParameter("SearchCombination");
				StringTokenizer st = new StringTokenizer(axisValuesStr, ",");
				ArrayList combination = new ArrayList();
				String axisValue = "";
				while (st.hasMoreTokens()) {
				       axisValue = st.nextToken();
					   //axisValue is xx/xx/xx where xx are nodeId
				   axisValue = axisValue.substring(axisValue.lastIndexOf('/')+1, axisValue.length());
				   combination.add(axisValue);
				}
				kmelia.addPublicationToCombination(pubId, combination);
				//Store current combination
				kmelia.setCurrentCombination(kmelia.getCombination(axisValuesStr));
	        	destination = getDestination("KmaxViewCombination", kmelia, request);
			}
			else if (function.equals("KmaxDeleteCoordinate")) 
			{
				String coordinateId = request.getParameter("CoordinateId");
				String pubId = request.getParameter("PubId");
				SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination()", "root.MSG_GEN_PARAM_VALUE","coordinateId = "+coordinateId+" PubId="+pubId);
			    kmelia.deletePublicationFromCombination(pubId, coordinateId);
	        	destination = getDestination("KmaxViewCombination", kmelia, request);
			}
			else if (function.equals("KmaxExportComponent"))
			{
				//build an exploitable list by importExportPeas
				List publicationsIds = kmelia.getAllVisiblePublications();
				request.setAttribute("selectedResultsWa", publicationsIds);

				//Go to importExportPeas
				destination = "/RimportExportPeas/jsp/KmaxExportComponent";
			}
			else if (function.equals("KmaxExportPublications"))
			{
				//build an exploitable list by importExportPeas
			    List publicationsIds = kmelia.getCurrentPublicationsList();
				ArrayList combination = kmelia.getSessionCombination();
		        //get the time axis
				String timeCriteria = null;
				if (kmelia.isTimeAxisUsed() && StringUtil.isDefined(kmelia.getSessionTimeCriteria()))
				{
			        ResourceLocator timeSettings = new ResourceLocator("com.stratelia.webactiv.kmelia.multilang.timeAxisBundle", kmelia.getLanguage());
			        if (kmelia.getSessionTimeCriteria().equals("X"))
				        timeCriteria = null;
		        	else
		        		timeCriteria = "<b>" + kmelia.getString("TimeAxis") + "</b> > " + timeSettings.getString(kmelia.getSessionTimeCriteria(),"");
				}
				request.setAttribute("selectedResultsWa", publicationsIds);
				request.setAttribute("Combination", combination);
				request.setAttribute("TimeCriteria", timeCriteria);
				//Go to importExportPeas
				destination = "/RimportExportPeas/jsp/KmaxExportPublications";
			}
			
			/************ End Kmax Mode  *****************/
			else {
				destination = rootDestination + function;
			}

			if (profileError) {
				String sessionTimeout = GeneralPropertiesManager.getGeneralResourceLocator().getString("sessionTimeout");
				destination = sessionTimeout;
			}
			SilverTrace.info("kmelia","KmeliaRequestRouter.getDestination()", "root.MSG_GEN_PARAM_VALUE","destination = " + destination);
		}
		catch (Exception exce_all){
			request.setAttribute("javax.servlet.jsp.jspException", exce_all);
			return "/admin/jsp/errorpageMain.jsp";
		}
		return destination;
	}
	
	private void setPubPath(KmeliaSessionController kmelia, String pubId, String[] topics) throws RemoteException
	{
		kmelia.deletePublicationFromAllTopics(pubId);
		if (topics != null)
		{
			for (int i = 0; i < topics.length; i++)
			{
				String topicId = topics[i];
				SilverTrace.debug("kmelia", "KmeliaRequestRouter.setPubPath()", "root.MSG_GEN_PARAM_VALUE", "topicId = " + topicId);
				kmelia.addPublicationToTopic(pubId, topicId);
			}
		}
	}
	
	private String getDocumentNotFoundDestination(KmeliaSessionController kmelia, HttpServletRequest request)
	{
		request.setAttribute("ComponentId", kmelia.getComponentId());
		return "/admin/jsp/documentNotFound.jsp";
	}
	
	private PublicationDetail getPublicationDetail(List parameters, List vignetteParams, KmeliaSessionController kmelia) throws Exception
	{
		String id 					= FileUploadUtil.getParameter(parameters, "PubId");
	    String status 				= FileUploadUtil.getParameter(parameters, "Status");
		String name 				= FileUploadUtil.getParameter(parameters, "Name");
	    String description 			= FileUploadUtil.getParameter(parameters, "Description");
	    String keywords 			= FileUploadUtil.getParameter(parameters, "Keywords");
	    String beginDate 			= FileUploadUtil.getParameter(parameters, "BeginDate");
	    String endDate 				= FileUploadUtil.getParameter(parameters, "EndDate");
	    String version 				= FileUploadUtil.getParameter(parameters, "Version");
	    String importance 			= FileUploadUtil.getParameter(parameters, "Importance");
	    String beginHour 			= FileUploadUtil.getParameter(parameters, "BeginHour");
	    String endHour 				= FileUploadUtil.getParameter(parameters, "EndHour");
	    String author 				= FileUploadUtil.getParameter(parameters, "Author");
	    String targetValidatorId 	= FileUploadUtil.getParameter(parameters, "ValideurId");
	    String tempId 				= FileUploadUtil.getParameter(parameters, "TempId");
	    String infoId				= FileUploadUtil.getParameter(parameters, "InfoId");
	    
	    Date jBeginDate = null;
	    Date jEndDate = null;
	    
	    if (beginDate != null && !beginDate.trim().equals("")) {
	    	jBeginDate = DateUtil.stringToDate(beginDate, kmelia.getLanguage());
	    }
	    if (endDate != null && !endDate.trim().equals("")) {
	    	jEndDate = DateUtil.stringToDate(endDate, kmelia.getLanguage());
	    }
	    
	    String pubId = "X";
	    if (StringUtil.isDefined(id))
	    	pubId = id;
	    PublicationDetail pubDetail = new PublicationDetail(pubId, name, description, null, jBeginDate, jEndDate, null, importance, version, keywords, "", null, author);
        pubDetail.setBeginHour(beginHour);
        pubDetail.setEndHour(endHour);
        pubDetail.setStatus(status);
        
        if (StringUtil.isDefined(targetValidatorId))
        	pubDetail.setTargetValidatorId(targetValidatorId);
        
        if (vignetteParams != null)
		{
			pubDetail.setImage((String) vignetteParams.get(0));
			pubDetail.setImageMimeType((String) vignetteParams.get(1));
		}
        
        pubDetail.setCloneId(tempId);
        
        if (StringUtil.isDefined(infoId))
        	pubDetail.setInfoId(infoId);
        
        I18NHelper.setI18NInfo(pubDetail, parameters);
        
        return pubDetail;
	}
	
	private static boolean isInteger(String id)
	{
		try
		{
			Integer.parseInt(id);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	private List processVignette(List parameters, KmeliaSessionController kmelia) throws Exception
	{
		FileItem file = FileUploadUtil.getFile(parameters, "WAIMGVAR0");
		
		String 	physicalName 	= null;
		String 	mimeType 		= null;
		List    result			= null;
		
		if (file != null)
		{
			String 	logicalName 	= file.getName();
			String 	type 			= null;
			File   	dir 			= null;
			
			if (logicalName != null)
			{
				logicalName = logicalName.substring(logicalName.lastIndexOf(File.separator)+1, logicalName.length());
				type		= FileRepositoryManager.getFileExtension(logicalName);
				
				if (type.equalsIgnoreCase("gif") || type.equalsIgnoreCase("jpg") || type.equalsIgnoreCase("jpeg") || type.equalsIgnoreCase("png"))
				{
					physicalName 	= new Long(new Date().getTime()).toString()+"."+type;
					dir 			= new File(FileRepositoryManager.getAbsolutePath(kmelia.getComponentId())+kmelia.getPublicationSettings().getString("imagesSubDirectory")+ File.separator +physicalName);
  					mimeType 		= file.getContentType();
  					
  					result = new ArrayList();
  					result.add(physicalName);
  					result.add(mimeType);
  					
					file.write(dir);
				}
			}
		}
		return result;
	}

	private void processPublicationsToLink(KmeliaSessionController kmelia, HttpServletRequest request)
	{
		String selectedPubIds 		= request.getParameter("SelectedIds");
		String notSelectedPubIds 	= request.getParameter("NotSelectedIds");
		
		List publicationsToLink = kmelia.getPublicationsToLink();
		
		StringTokenizer tokenizer = new StringTokenizer(selectedPubIds, ",");
		String pubId = null;
		while (tokenizer.hasMoreTokens())
		{
			pubId = tokenizer.nextToken();
			if (!publicationsToLink.contains(pubId))
				publicationsToLink.add(pubId);
		}
		
		tokenizer = new StringTokenizer(notSelectedPubIds, ",");
		while (tokenizer.hasMoreTokens())
		{
			pubId = tokenizer.nextToken();
			publicationsToLink.remove(pubId);
		}
	}

	/**
	 * Process Form Upload for publications import
	 * @param kmeliaScc
	 * @param request
	 * @param routDestination
	 * @return destination
	 */
	private String processFormUpload(KmeliaSessionController kmeliaScc, HttpServletRequest request, String routeDestination, boolean isMassiveMode)
	{
		String destination = "";
		String topicId = "";
		String importMode = KmeliaSessionController.UNITARY_IMPORT_MODE;
		boolean draftMode = false;
		String logicalName = "";
		String message = "";

		String tempFolderName = "";
		String tempFolderPath = "";

		String fileType = "";
		long fileSize=0;
		long processStart = new Date().getTime();
		ResourceLocator attachmentResourceLocator = new ResourceLocator("com.stratelia.webactiv.util.attachment.multilang.attachment", kmeliaScc.getLanguage());
		//ResourceLocator multiparserResourceLocator = new ResourceLocator("com.stratelia.silverpeas.multipart.multipart","");
		FileItem fileItem = null;
		int versionType = DocumentVersion.TYPE_DEFAULT_VERSION;
        //long maxFileSize = new Integer(multiparserResourceLocator.getString("MultipartParserMaxSize")).longValue();
		try {
			List items 	= FileUploadUtil.parseRequest(request);
			topicId 	= FileUploadUtil.getParameter(items, "topicId");
			importMode 	= FileUploadUtil.getParameter(items, "opt_importmode");
			
			String sVersionType = FileUploadUtil.getParameter(items, "opt_versiontype");
			if (StringUtil.isDefined(sVersionType))
				versionType = Integer.parseInt(sVersionType);
			
			String sDraftMode = FileUploadUtil.getParameter(items, "chk_draft");
			if (StringUtil.isDefined(sDraftMode))
				draftMode = new Boolean(sDraftMode).booleanValue();
			
			fileItem = FileUploadUtil.getFile(items, "file_name");
			
			if (fileItem != null)
	        {
	          logicalName = fileItem.getName();
	          if (logicalName != null)
	          {
	        	  ResourceLocator settings = new ResourceLocator("com.stratelia.webactiv.util.attachment.Attachment", "");
			      boolean runOnUnix = SilverpeasSettings.readBoolean(settings, "runOnSolaris", false);
	        	  if (runOnUnix)
	        	  {
	        		  logicalName = logicalName.replace('\\', File.separatorChar);
	        		  SilverTrace.info("kmelia", "KmeliaRequestRouter.processFormUpload", "root.MSG_GEN_PARAM_VALUE", "fileName on Unix = "+logicalName);
	        	  }
	        	  
	        	  logicalName = logicalName.substring(logicalName.lastIndexOf(File.separator)+1, logicalName.length());
	        	  
	              //Name of temp folder: timestamp and userId
	              tempFolderName = new Long(new Date().getTime()).toString() + "_" + kmeliaScc.getUserId();

	              //Mime type of the file
	              fileType = fileItem.getContentType();
	              
	              //Zip contentType not detected under Firefox !
	              if (request.getHeader("User-Agent") != null && request.getHeader("User-Agent").indexOf("MSIE") == -1)
	            	  fileType = KmeliaSessionController.FILETYPE_ZIP1;
	              
	              fileSize = fileItem.getSize();

	              //Directory Temp for the uploaded file
	              tempFolderPath = FileRepositoryManager.getAbsolutePath(kmeliaScc.getComponentId()) + GeneralPropertiesManager.getGeneralResourceLocator().getString("RepositoryTypeTemp") + File.separator + tempFolderName;
	              if (!new File(tempFolderPath).exists())
		              FileRepositoryManager.createAbsolutePath(kmeliaScc.getComponentId(),GeneralPropertiesManager.getGeneralResourceLocator().getString("RepositoryTypeTemp") + File.separator + tempFolderName);

	              //Creation of the file in the temp folder
	              File fileUploaded = new File(FileRepositoryManager.getAbsolutePath(kmeliaScc.getComponentId())+GeneralPropertiesManager.getGeneralResourceLocator().getString("RepositoryTypeTemp")+ File.separator + tempFolderName + File.separator +logicalName);
	              fileItem.write(fileUploaded);

	              //Is a real file ?
	              if (fileSize > 0)
	              {
		              SilverTrace.debug("kmelia","KmeliaRequestRouter.processFormUpload()","root.MSG_GEN_PARAM_VALUE","fileUploaded = "+fileUploaded+" fileSize="+fileSize+" fileType="+fileType+" importMode="+importMode+" draftMode="+draftMode);
		              int nbFiles = 1;
		              //Compute nbFiles only in unitary Import mode
		              if (!importMode.equals(KmeliaSessionController.UNITARY_IMPORT_MODE) && fileUploaded.getName().toLowerCase().endsWith(".zip"))
		            	  nbFiles = ZipManager.getNbFiles(fileUploaded);

		              //Import !!
		              ArrayList publicationDetails = kmeliaScc.importFile(fileUploaded, fileType, topicId, importMode, draftMode, versionType);
		              long processDuration = new Date().getTime() - processStart;

		              //Title for popup report
		              String importModeTitle = "";
		              if (importMode.equals(KmeliaSessionController.UNITARY_IMPORT_MODE))
		            	importModeTitle = kmeliaScc.getString("kmelia.ImportModeUnitaireTitre");
		              else
		            	importModeTitle = kmeliaScc.getString("kmelia.ImportModeMassifTitre");

		              SilverTrace.debug("kmelia","KmeliaRequestRouter.processFormUpload()","root.MSG_GEN_PARAM_VALUE","nbFiles = "+nbFiles+" publicationDetails="+publicationDetails+" ProcessDuration="+processDuration+" ImportMode="+importMode+" Draftmode="+draftMode+" Title="+importModeTitle);

		              request.setAttribute("PublicationsDetails", publicationDetails);
		              request.setAttribute("NbFiles", new Integer(nbFiles));
		              request.setAttribute("ProcessDuration", FileRepositoryManager.formatFileUploadTime(processDuration));
		              request.setAttribute("ImportMode", importMode);
		              request.setAttribute("DraftMode", new Boolean(draftMode));
		              request.setAttribute("Title", importModeTitle);
		              destination = routeDestination + "reportImportFiles.jsp";
	              }
	              else
	              {
	            	  //File access failed
	            	  message = attachmentResourceLocator.getString("liaisonInaccessible");
		        	  request.setAttribute("Message",message);
		        	  request.setAttribute("TopicId",topicId);
	        		  destination = routeDestination + "importOneFile.jsp";
		        	  if (isMassiveMode)
		        		  destination = routeDestination + "importMultiFiles.jsp";
	              }
	              FileFolderManager.deleteFolder(tempFolderPath);
	          }
	          else
	          {
                  // the field did not contain a file
	        	  request.setAttribute("Message", attachmentResourceLocator.getString("liaisonInaccessible"));
	        	  request.setAttribute("TopicId", topicId);
        		  destination = routeDestination + "importOneFile.jsp";
	        	  if (isMassiveMode)
	        		  destination = routeDestination + "importMultiFiles.jsp";
	          }
	        }
		}
		/*catch (IOException e)
		{
			//File size exceeds Maximum file size
			message = attachmentResourceLocator.getString("fichierTropGrand")+ " (" + FileRepositoryManager.formatFileSize(maxFileSize) + "&nbsp;" + attachmentResourceLocator.getString("maximum") +") !!";
			request.setAttribute("Message",message);
			request.setAttribute("TopicId",topicId);
			destination = routeDestination + "importOneFile.jsp";
			if (isMassiveMode)
				destination = routeDestination + "importMultiFiles.jsp";
		}*/
		catch (Exception e)
		{
      	  //Other exception
			request.setAttribute("Message", e.getMessage());
			request.setAttribute("TopicId", topicId);
			destination = routeDestination + "importOneFile.jsp";
			if (isMassiveMode)
				destination = routeDestination + "importMultiFiles.jsp";
			
			SilverTrace.warn("kmelia","KmeliaRequestRouter.processFormUpload()","root.EX_LOAD_ATTACHMENT_FAILED", e);
		}
		return destination;
	}
	
	private void processPath(KmeliaSessionController kmeliaSC, String id) throws RemoteException
	{
		TopicDetail currentTopic = null;
		if (!StringUtil.isDefined(id))
			currentTopic = kmeliaSC.getSessionTopic();
		else
			currentTopic = kmeliaSC.getPublicationTopic(id); //Calcul du chemin de la publication
      	
      	Collection pathColl = currentTopic.getPath();
      	String linkedPathString = displayPath(pathColl, true, 3, kmeliaSC.getCurrentLanguage());
	  	String pathString = displayPath(pathColl, false, 3, kmeliaSC.getCurrentLanguage());
      	kmeliaSC.setSessionPath(linkedPathString);
  	  	kmeliaSC.setSessionPathString(pathString);
	}

	private String displayPath(Collection path, boolean linked, int beforeAfter, String translation) {
	      StringBuffer	linkedPathString	= new StringBuffer();
	      StringBuffer	pathString			= new StringBuffer();
	      int			nbItemInPath		= path.size();
	      Iterator		iterator			= path.iterator();
	      boolean		alreadyCut			= false;
	      int			i					= 0;
		  NodeDetail	nodeInPath			= null;
	      while (iterator.hasNext()) {
	            nodeInPath = (NodeDetail) iterator.next();
	            if ((i <= beforeAfter) || (i + beforeAfter >= nbItemInPath - 1)){
					if (!nodeInPath.getNodePK().getId().equals("0")) {
						String nodeName;
						if (translation != null)
							nodeName = nodeInPath.getName(translation);
						else
							nodeName = nodeInPath.getName();
						linkedPathString.append("<a href=\"javascript:onClick=topicGoTo('").append(nodeInPath.getNodePK().getId()).append("')\">").append(EncodeHelper.javaStringToHtmlString(nodeName)).append("</a>");
						pathString.append(EncodeHelper.javaStringToHtmlString(nodeName));
						if (iterator.hasNext()) {
							  linkedPathString.append(" > ");
							  pathString.append(" > ");
						}
					}
	           } else {
	                if (!alreadyCut) {
	                      linkedPathString.append(" ... > ");
	                      pathString.append(" ... > ");
	                      alreadyCut = true;
	                }
	           }
	           i++;
	      }
		  nodeInPath = null;
	      if (linked)
	          return linkedPathString.toString();
	      else
	          return pathString.toString();
	}
	
	private void putXMLDisplayerIntoRequest(PublicationDetail pubDetail, KmeliaSessionController kmelia, HttpServletRequest request) throws PublicationTemplateException, FormException
	{
		String infoId = pubDetail.getInfoId();
		String pubId = pubDetail.getPK().getId();
		if (!isInteger(infoId))
		{
			PublicationTemplateImpl pubTemplate = (PublicationTemplateImpl) PublicationTemplateManager.getPublicationTemplate(pubDetail.getPK().getInstanceId()+":"+infoId);
			
			//RecordTemplate recordTemplate = pubTemplate.getRecordTemplate();
			Form formView = pubTemplate.getViewForm();
			
			//get displayed language
			String language = checkLanguage(kmelia, pubDetail);
	
			RecordSet recordSet = pubTemplate.getRecordSet();
			DataRecord data = recordSet.getRecord(pubId, language);
			if (data == null) {
				data = recordSet.getEmptyRecord();
				data.setId(pubId);
			}
			
			request.setAttribute("XMLForm", formView);
			request.setAttribute("XMLData", data);
		}
	}
	
	private String processWizard(String function, KmeliaSessionController kmeliaSC, HttpServletRequest request, String rootDestination) throws RemoteException, PublicationTemplateException, FormException
	{
		String destination = "";
		if (function.equals("WizardStart"))
		{
			// r�cup�ration de l'id du th�me dans lequel on veux mettre la publication si on ne viens pas d'un theme-tracker
			String topicId = request.getParameter("TopicId");
			if (StringUtil.isDefined(topicId))
			{
				TopicDetail topic = kmeliaSC.getTopic(topicId);
				kmeliaSC.setSessionTopic(topic);
			}
			// recherche du dernier onglet
			String wizardLast = "1";
			ArrayList invisibleTabs = kmeliaSC.getInvisibleTabs();
			if ((kmeliaSC.isPDCClassifyingMandatory() && invisibleTabs.indexOf(KmeliaSessionController.TAB_PDC) == -1) || kmeliaSC.isKmaxMode)
				wizardLast = "4";
			else if (invisibleTabs.indexOf(KmeliaSessionController.TAB_ATTACHMENTS) == -1)
				wizardLast = "3";
			else if (invisibleTabs.indexOf(KmeliaSessionController.TAB_CONTENT) == -1)
				wizardLast = "2";
			kmeliaSC.setWizardLast(wizardLast);
			request.setAttribute("WizardLast",wizardLast);
			kmeliaSC.setWizard("progress");
			request.setAttribute("Action","Wizard");
			request.setAttribute("Profile",kmeliaSC.getProfile());
			destination = rootDestination + "wizardPublicationManager.jsp";
		}
		else if (function.equals("WizardHeader"))
		{
			// passage des param�tres
			String id = request.getParameter("PubId");
			if (!StringUtil.isDefined(id))
			{
				id = (String) request.getAttribute("PubId");
			}
			request.setAttribute("WizardRow",kmeliaSC.getWizardRow());
			request.setAttribute("WizardLast",kmeliaSC.getWizardLast());
			request.setAttribute("Action","UpdateWizard");
			request.setAttribute("Profile",kmeliaSC.getProfile());
			request.setAttribute("PubId",id);
			
			destination = rootDestination + "wizardPublicationManager.jsp";
		}
		else if (function.equals("WizardNext"))
		{
			// redirige vers l'onglet suivant de l'assistant de publication
			String position = request.getParameter("Position");
			if (!StringUtil.isDefined(position))
				position = (String) request.getAttribute("Position");
			
			String next = "End";
			
			String wizardRow = kmeliaSC.getWizardRow();
			request.setAttribute("WizardRow",wizardRow);
			int numRow = 0;
			if (StringUtil.isDefined(wizardRow))
				numRow = Integer.parseInt(wizardRow);
			
			ArrayList invisibleTabs = kmeliaSC.getInvisibleTabs();
			
			if (position.equals("View"))
			{
				if (invisibleTabs.indexOf(KmeliaSessionController.TAB_CONTENT) == -1)
				{
					// on passe � la page du contenu
					next = "Content";
					if (numRow <= 2)
						wizardRow = "2";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
 						
				}
				else if (invisibleTabs.indexOf(KmeliaSessionController.TAB_ATTACHMENTS) == -1)
				{
					next = "Attachment";
					if (numRow <= 3)
						wizardRow = "3";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
				}
				else if (kmeliaSC.isPDCClassifyingMandatory() && invisibleTabs.indexOf(KmeliaSessionController.TAB_PDC) == -1)
				{
					if (numRow <= 4)
						wizardRow = "4";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
					next = "Pdc";
				}
				else if (kmeliaSC.isKmaxMode)
				{
					if (numRow <= 4)
						wizardRow = "4";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
					next = "KmaxClassification";
				}
			}
			else if (position.equals("Content"))
			{
				if (invisibleTabs.indexOf(KmeliaSessionController.TAB_ATTACHMENTS) == -1)
				{
					next = "Attachment";
					if (numRow <= 3)
						wizardRow = "3";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
				}
				else if (kmeliaSC.isPDCClassifyingMandatory() && invisibleTabs.indexOf(KmeliaSessionController.TAB_PDC) == -1)
				{
					if (numRow <= 4)
						wizardRow = "4";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
					next = "Pdc";
				}
				else if (kmeliaSC.isKmaxMode)
				{
					if (numRow <= 4)
						wizardRow = "4";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
					next = "KmaxClassification";
				}
			}
			else if (position.equals("Attachment"))
			{
				if (kmeliaSC.isPDCClassifyingMandatory() && invisibleTabs.indexOf(KmeliaSessionController.TAB_PDC) == -1)
					next = "Pdc";
				else if (kmeliaSC.isKmaxMode)
					next = "KmaxClassification";
				else
					next = "End";
				
				if (!next.equals("End"))
				{
					if (numRow <= 4)
						wizardRow = "4";
					else if (numRow < Integer.parseInt(wizardRow))
						wizardRow = Integer.toString(numRow);
				}
			}
			else if (position.equals("Pdc") || position.equals("KmaxClassification"))
			{
				if (numRow <= 4)
					wizardRow = "4";
				else if (numRow < Integer.parseInt(wizardRow))
					wizardRow = Integer.toString(numRow);
				next = "End";
			}
			
			// mise � jour du rang en cours
			kmeliaSC.setWizardRow(wizardRow);
			
			// passage des param�tres 
			setWizardParams(request, kmeliaSC);
			
			if (next.equals("View"))
				destination = getDestination("WizardStart", kmeliaSC, request);
			else if (next.equals("Content"))
				destination = getDestination("ToPubliContent", kmeliaSC, request);
			else if (next.equals("Attachment"))
				destination = getDestination("ViewAttachments", kmeliaSC, request);
			else if (next.equals("Pdc"))
				destination = getDestination("ViewPdcPositions", kmeliaSC, request);
			else if (next.equals("KmaxClassification"))
				destination = getDestination("KmaxViewCombination", kmeliaSC, request);
			else if (next.equals("End"))
			{
				// terminer la publication : la sortir du mode brouillon 
				kmeliaSC.setWizard("finish");
				kmeliaSC.draftOutPublication();
				destination = getDestination("ViewPublication", kmeliaSC, request);
			}
		}
		
		return destination;
	}
	
	private void setWizardParams(HttpServletRequest request, KmeliaSessionController kmelia)
	{
		//Param�tres du wizard
		request.setAttribute("Wizard", kmelia.getWizard());
		request.setAttribute("WizardRow", kmelia.getWizardRow());
		request.setAttribute("WizardLast",kmelia.getWizardLast());
	}
	
	private void resetWizard(KmeliaSessionController kmelia)
	{
		kmelia.setWizard("none");
		kmelia.setWizardLast("0");
		kmelia.setWizardRow("0");
	}
	
	private void setXMLForm(HttpServletRequest request, KmeliaSessionController kmelia, String xmlFormName) throws PublicationTemplateException, FormException
	{
		PublicationDetail pubDetail = kmelia.getSessionPubliOrClone().getPublication().getPublicationDetail();
		String pubId = pubDetail.getPK().getId();
		
		String xmlFormShortName = null;
		if (!StringUtil.isDefined(xmlFormName))
		{
			xmlFormShortName 	= pubDetail.getInfoId();
			xmlFormName 		= null;
		}
		else
		{
			xmlFormShortName = xmlFormName.substring(xmlFormName.indexOf("/")+1, xmlFormName.indexOf("."));
			SilverTrace.info("kmelia","KmeliaRequestRouter.setXMLForm()", "root.MSG_GEN_PARAM_VALUE","xmlFormShortName = " + xmlFormShortName);
			
			//register xmlForm to publication
			PublicationTemplateManager.addDynamicPublicationTemplate(kmelia.getComponentId()+":"+xmlFormShortName, xmlFormName);
		}
						
		PublicationTemplateImpl pubTemplate = (PublicationTemplateImpl) PublicationTemplateManager.getPublicationTemplate(kmelia.getComponentId()+":"+xmlFormShortName, xmlFormName);
		Form formUpdate = pubTemplate.getUpdateForm();
		RecordSet recordSet = pubTemplate.getRecordSet();
		
		//get displayed language
		String language = checkLanguage(kmelia, pubDetail);
		
		DataRecord data= recordSet.getRecord(pubId, language);
		if (data == null) {
			data = recordSet.getEmptyRecord();
			data.setId(pubId);
		}
		
		request.setAttribute("Form", formUpdate);
		request.setAttribute("Data", data);
		request.setAttribute("XMLFormName", xmlFormName);
	}
	
	private void setLanguage(HttpServletRequest request, KmeliaSessionController kmelia)
	{
		String language = request.getParameter("SwitchLanguage");
		if (StringUtil.isDefined(language))
			kmelia.setCurrentLanguage(language);
		
		request.setAttribute("Language", kmelia.getCurrentLanguage());
	}
	
	private String checkLanguage(KmeliaSessionController kmelia)
	{
		return checkLanguage(kmelia, kmelia.getSessionPublication().getPublication().getPublicationDetail());
	}
	
	private String checkLanguage(KmeliaSessionController kmelia, PublicationDetail pubDetail)
	{
		return pubDetail.getLanguageToDisplay(kmelia.getCurrentLanguage());
	}
	
	private void checkAlias(KmeliaSessionController kmelia, UserCompletePublication publication)
	{
		if (!kmelia.getComponentId().equals(publication.getPublication().getPublicationDetail().getPK().getInstanceId()))
			publication.setAlias(true);
	}
	
}