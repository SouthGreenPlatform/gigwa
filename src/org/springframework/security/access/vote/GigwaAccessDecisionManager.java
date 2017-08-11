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
package org.springframework.security.access.vote;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.FilterInvocation;

import fr.cirad.mgdb.model.mongo.maintypes.GenotypingProject;
import fr.cirad.tools.mongo.MongoTemplateManager;
import fr.cirad.tools.security.TokenManager;
import fr.cirad.web.controller.gigwa.GigwaController;

/**
 * The Class GigwaAccessDecisionManager.
 */
@SuppressWarnings("deprecation")
public class GigwaAccessDecisionManager extends AffirmativeBased
{
    @Autowired private TokenManager tokenManager;
	
    /* (non-Javadoc)
     * @see org.springframework.security.access.vote.AffirmativeBased#decide(org.springframework.security.core.Authentication, java.lang.Object, java.util.Collection)
     */
    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException
    {
    	if (object instanceof FilterInvocation)
    	{
    		FilterInvocation fi = (FilterInvocation) object;
    		String sModule = fi.getRequest().getParameter("module");
    		String username = authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal()) ? "(" + ((User) authentication.getPrincipal()).getUsername() + ") " : "";
    		if (sModule != null && MongoTemplateManager.get(sModule) != null && !MongoTemplateManager.isModulePublic(sModule))
    		{		
    			int projId = -1;
    			try
    			{
    				projId = Integer.parseInt(fi.getRequest().getParameter("project"));
    				if (!tokenManager.canUserReadProject(authentication, sModule, projId))
    					throw new AccessDeniedException("You " + username + "are not allowed to access project " + projId + " in database " + sModule);
    			}
    			catch (NumberFormatException ignored)
    			{	// no project involved in request
    				if (fi.getRequest().getRequestURI().equals(fi.getRequest().getContextPath() + GigwaController.genotypingDataImportSubmissionURL))
    				{	// attempt to import genotyping data
    					MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
    					Query q = new Query(Criteria.where(GenotypingProject.FIELDNAME_NAME).is(fi.getRequest().getParameter("project")));
    					q.fields().include("_id");
    					GenotypingProject project = mongoTemplate.findOne(q, GenotypingProject.class);
    					if (project == null)
    					{	// project does not exist yet
	    					if (!tokenManager.canUserWriteToDB(authentication, sModule))
	    						throw new AccessDeniedException("You " + username + "are not allowed to write to database " + sModule);
    					}
    					else if (!tokenManager.canUserWriteToProject(authentication, sModule, project.getId()))
    						throw new AccessDeniedException("You " + username + "are not allowed to write to project " + fi.getRequest().getParameter("project") + " of database " + sModule);
    				}
    				else if (!tokenManager.canUserReadDB(authentication, sModule))
        				throw new AccessDeniedException("You " + username + "are not allowed to access database " + sModule);
    			}
    		}
    	}
    	super.decide(authentication, object, configAttributes);
    }

}
