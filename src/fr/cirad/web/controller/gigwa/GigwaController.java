/*******************************************************************************
 * GIGWA - Genotype Investigator for Genome Wide Analyses
 * Copyright (C) 2016 <CIRAD>
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * See <http://www.gnu.org/licenses/gpl-3.0.html> for details about
 * GNU Affero General Public License V3.
 *******************************************************************************/
package fr.cirad.web.controller.gigwa;

import htsjdk.samtools.util.BlockCompressedInputStream;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;

import fr.cirad.mgdb.importing.HapMapImport;
import fr.cirad.mgdb.importing.VcfImport;
import fr.cirad.mgdb.model.mongo.maintypes.GenotypingProject;
import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.tools.ProgressIndicator;
import fr.cirad.tools.mongo.MongoTemplateManager;
import fr.cirad.web.controller.gigwa.base.IGigwaViewController;

/**
 * The Class GigwaController.
 */
@Controller
public class GigwaController
{

	/** The Constant LOG. */
	private static final Logger LOG = Logger.getLogger(GigwaController.class);

	/** The user dao. */
	@Autowired private ReloadableInMemoryDaoImpl userDao;

	/** The view controllers. */
	static private TreeMap<String, String> viewControllers = null;

	/** The Constant FRONTEND_URL. */
	static final public String FRONTEND_URL = "gigwa";

	/** The Constant mainPageURL. */
	static final public String mainPageURL = "/" + FRONTEND_URL + "/index.do_";

	/** The Constant importPageURL. */
	static final public String importPageURL = "/" + FRONTEND_URL + "/import.do";

	/** The Constant genotypingDataImportSubmissionURL. */
	static final public String genotypingDataImportSubmissionURL = "/" + FRONTEND_URL + "/genotypingDataImport.json";

	/** The Constant progressIndicatorURL. */
	static final public String progressIndicatorURL = "/" + FRONTEND_URL + "/progressIndicator.json_";

	/**
	 * Setup menu.
	 *
	 * @param request the request
	 * @param fReloadDatasources whether or not to reload datasources
	 * @return the model and view
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 * @throws SecurityException the security exception
	 */
	@RequestMapping(mainPageURL)
	public ModelAndView setupMenu(HttpServletRequest request, @RequestParam(value="reloadDatasources", required=false) boolean fReloadDatasources) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		if (fReloadDatasources)
		{
			ConfigurableApplicationContext ac = (ConfigurableApplicationContext) WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession().getServletContext());
			ac.refresh();
			MongoTemplateManager.initialize(ac);
			userDao.reloadProperties();
			return new ModelAndView("redirect:" + mainPageURL);
		}

		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<? extends GrantedAuthority> authorities = authToken == null ? null : authToken.getAuthorities();

		Collection<String> modules = MongoTemplateManager.getAvailableModules(), authorizedModules = new ArrayList<String>();
		for (String module : modules)
		{
			boolean fHiddenModule = MongoTemplateManager.isModuleHidden(module);
			boolean fPublicModule = MongoTemplateManager.isModulePublic(module);
			boolean fAuthentifiedUser = authorities != null;
			boolean fAdminUser = fAuthentifiedUser && authorities.contains(new GrantedAuthorityImpl("ROLE_ADMIN"));
			boolean fAuthorizedUser = fAuthentifiedUser && authorities.contains(new GrantedAuthorityImpl("ROLE_USER_" + module));
			if (fAdminUser || (!fHiddenModule && (!fAuthentifiedUser || fAuthorizedUser || fPublicModule)))
				authorizedModules.add(module);
		}

		ModelAndView mav = new ModelAndView();
		mav.addObject("modules", authorizedModules);
		mav.addObject("views", getViewControllers());
		return mav;
	}

	/**
	 * Gets the view controllers.
	 *
	 * @return the view controllers
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 * @throws SecurityException the security exception
	 */
	public static TreeMap<String, String> getViewControllers() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		if (viewControllers == null)
		{
			viewControllers = new TreeMap<String, String>();
			ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
			provider.addIncludeFilter(new AssignableTypeFilter(IGigwaViewController.class));
			try
			{
				for (BeanDefinition component : provider.findCandidateComponents("fr.cirad"))
				{
				    Class cls = Class.forName(component.getBeanClassName());
				    if (!Modifier.isAbstract(cls.getModifiers()))
				    {
				    	IGigwaViewController viewController = (IGigwaViewController) cls.getConstructor().newInstance();
				    	viewControllers.put(viewController.getViewDescription(), viewController.getViewURL());
				    }
				}
			}
			catch (Exception e)
			{
				LOG.warn("Error scanning view controllers", e);
			}
		}
		return viewControllers;
	}

	/**
	 * Import form.
	 *
	 * @return the model and view
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 * @throws SecurityException the security exception
	 */
	@RequestMapping(importPageURL)
	public ModelAndView importForm() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		Collection<String> modules = MongoTemplateManager.getAvailableModules();
		Map<String /*module*/, Map<String /*project name*/, List<String /*run*/>>> modulesProjectsAndRuns = new LinkedHashMap<String, Map<String, List<String>>>();
		for (String module : modules)
		{
			Map<String, List<String>> projectsAndRuns = new LinkedHashMap<String, List<String>>();
			modulesProjectsAndRuns.put(module, projectsAndRuns);

			MongoTemplate mongoTemplate = MongoTemplateManager.get(module);
			Query q = new Query();
			q.fields().exclude(GenotypingProject.FIELDNAME_SEQUENCES);
			for (GenotypingProject proj : mongoTemplate.find(q, GenotypingProject.class))
				projectsAndRuns.put(proj.getName(), proj.getRuns());;
		}

		ModelAndView mav = new ModelAndView();
		mav.addObject("modulesProjectsAndRuns", modulesProjectsAndRuns);
		mav.addObject("hosts", MongoTemplateManager.getHostNames());
		return mav;
	}

	/**
	 * Gets the progress indicator.
	 *
	 * @param sProcessID the process id
	 * @return the progress indicator
	 * @throws Exception the exception
	 */
	@RequestMapping(progressIndicatorURL)
	public @ResponseBody ProgressIndicator getProgressIndicator(@RequestParam("processID") String sProcessID) throws Exception
	{
		return ProgressIndicator.get(sProcessID);
	}

	/**
	 * Import genotyping data.
	 *
	 * @param request the request
	 * @param sHost the host
	 * @param sModule the module
	 * @param sProject the project
	 * @param sRun the run
	 * @param sTechnology the technology
	 * @param fClearProjectData whether or not to clear project data
	 * @param dataFile the data file
	 * @return the string
	 * @throws Exception the exception
	 */
	@RequestMapping(genotypingDataImportSubmissionURL)
	public @ResponseBody String importGenotypingData(HttpServletRequest request, @RequestParam("host") final String sHost, @RequestParam("module") final String sModule, @RequestParam("project") final String sProject, @RequestParam("run") final String sRun, @RequestParam(value="technology", required=false) final String sTechnology, @RequestParam(value="clearProjectData", required=false) final Boolean fClearProjectData, @RequestParam("mainFile") final String dataFile) throws Exception
	{
		final String sNormalizedModule = Normalizer.normalize(sModule, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replaceAll(" ", "_");
		final String processId = "IMPORT__GENOTYPING_DATA__" + sNormalizedModule + "__" + sProject + "__" + sRun + "__" + System.currentTimeMillis();

		String remoteAddr = request.getHeader("x-forwarded-for");
		if (remoteAddr == null)
			remoteAddr = request.getRemoteAddr();
		boolean fCalledLocally = remoteAddr.equals(request.getLocalAddr());
		String sReferer = request.getHeader("referer");
		boolean fIsCalledFromWithinLocalInstance = sReferer != null && sReferer.endsWith(importPageURL);
		boolean fDatasourceExists = MongoTemplateManager.get(sNormalizedModule) != null;
		final boolean fDatasourceAlreadyExisted = fDatasourceExists;

		final ProgressIndicator progress = new ProgressIndicator(processId, new String[] {"Checking user permissions"});
		ProgressIndicator.registerProgressIndicator(progress);
		boolean fAllowedToImport = true;
		if (!fIsCalledFromWithinLocalInstance)
		{	// we're being called directly via a URL
			if (fDatasourceExists)
			{
				progress.setError("Datasource " + sNormalizedModule + " already exists!");
				fAllowedToImport = false;
			}
			else if (!fCalledLocally)
			{
				progress.setError("You are not allowed to create a datasource!");
				LOG.warn("Attempt to create database " + sNormalizedModule + " was refused. Remote address: " + remoteAddr);
				fAllowedToImport = false;
			}
		}

		if (fAllowedToImport)
		{	// allowed to continue
			final String sFinalModule = fIsCalledFromWithinLocalInstance ? sNormalizedModule : ("*" + sNormalizedModule + "*");	// non-admin users (invoking import directly via a URL) are only allowed to create public-and-hidden databases (i.e. for their own use)
			if (!fDatasourceExists)
				try
				{	// create it
					MongoTemplateManager.createDataSource(sFinalModule, sHost, fIsCalledFromWithinLocalInstance ? null : System.currentTimeMillis() + 1000*60*60*24 /* 1 day */);
					fDatasourceExists = true;
				}
				catch (Exception e)
				{
					LOG.error("Error creating datasource " + sNormalizedModule, e);
					progress.addStep("Creating datasource");
					progress.moveToNextStep();
					progress.setError(e.getMessage());
				}

			if (fDatasourceExists)
				new Thread() {
					public void run() {
						Scanner scanner = null;
						try
						{
							scanner = new Scanner(new File(dataFile));
							if (scanner.hasNext())
							{
								String sLowerCaseFirstLine = scanner.next().toLowerCase();
								if (sLowerCaseFirstLine.startsWith("rs#"))
									new HapMapImport(processId).importToMongo(sNormalizedModule, sProject, sRun, sTechnology == null ? "" : sTechnology, dataFile, Boolean.TRUE.equals(fClearProjectData) ? 1 : 0);
								else
								{
									Boolean fIsBCF = null;
									if (sLowerCaseFirstLine.startsWith("##fileformat=vcf"))
										fIsBCF = false;
									else if (dataFile.toLowerCase().endsWith(".bcf"))
										fIsBCF = true;	// we support BCF2 only
									if (fIsBCF != null)
										new VcfImport(processId).importToMongo(fIsBCF, sNormalizedModule, sProject, sRun, sTechnology == null ? "" : sTechnology, dataFile, Boolean.TRUE.equals(fClearProjectData) ? 1 : 0);
									else
										throw new Exception("Unknown file format: " + dataFile);
								}
							}
							else
							{	// looks like a compressed file
								BlockCompressedInputStream.assertNonDefectiveFile(new File(dataFile));
								new VcfImport(processId).importToMongo(dataFile.toLowerCase().endsWith(".bcf.gz"), sNormalizedModule, sProject, sRun, sTechnology == null ? "" : sTechnology, dataFile, Boolean.TRUE.equals(fClearProjectData) ? 1 : 0);
							}
						}
						catch (Exception e)
						{
							LOG.error("Error importing " + dataFile, e);
							progress.setError("Error importing " + dataFile + ": " + ExceptionUtils.getStackTrace(e));
							if (!fDatasourceAlreadyExisted)
//								try
//								{
									MongoTemplateManager.removeDataSource(sNormalizedModule, true);
									LOG.debug("Removed datasource " + sNormalizedModule + " subsequently to previous import error");
//								}
//								catch (IOException ioe)
//								{
//									LOG.error("Unable to remove datasource " + sNormalizedModule, ioe);
//								}
						}
						finally
						{
							scanner.close();
						}
					}
				}.start();
		}
		return progressIndicatorURL + "?processID=" + processId;
	}
}