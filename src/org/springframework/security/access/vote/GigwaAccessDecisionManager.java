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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.web.FilterInvocation;

import fr.cirad.tools.mongo.MongoTemplateManager;

/**
 * The Class GigwaAccessDecisionManager.
 */
@SuppressWarnings("deprecation")
public class GigwaAccessDecisionManager extends AffirmativeBased
{

	/** The role admin. */
	static public String ROLE_ADMIN = "ROLE_ADMIN";

	/** The user role prefix. */
	static public String USER_ROLE_PREFIX = "ROLE_USER_";

    /* (non-Javadoc)
     * @see org.springframework.security.access.vote.AffirmativeBased#decide(org.springframework.security.core.Authentication, java.lang.Object, java.util.Collection)
     */
    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException
    {
    	if (object instanceof FilterInvocation)
    	{
    		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

    		FilterInvocation fi = (FilterInvocation) object;
    		String sModule = fi.getRequest().getParameter("module");
    		if (sModule != null && MongoTemplateManager.get(sModule) != null && !MongoTemplateManager.isModulePublic(sModule))
    		{
    			boolean fIsAnonymous = authorities != null && authorities.contains(new GrantedAuthorityImpl("ROLE_ANONYMOUS"));
    			boolean fIsAdmin = authorities != null && authorities.contains(new GrantedAuthorityImpl(ROLE_ADMIN));
    			boolean fHasRequiredRole = authorities != null && authorities.contains(new GrantedAuthorityImpl(USER_ROLE_PREFIX + sModule.toUpperCase().replaceAll(" ", "_")));
    			if (fIsAnonymous || (!fIsAdmin && !fHasRequiredRole))
	    			throw new AccessDeniedException("You are not allowed to access module '" + sModule + "'");
    		}
    	}
    	super.decide(authentication, object, configAttributes);
    }

}
