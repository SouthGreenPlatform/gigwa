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
package fr.cirad.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.memory.InMemoryDaoImpl;
import org.springframework.stereotype.Component;

/**
 * The Class ReloadableInMemoryDaoImpl.
 */
@Component
public class ReloadableInMemoryDaoImpl extends InMemoryDaoImpl
{

	/** The Constant LOG. */
	private static final Logger LOG = Logger.getLogger(ReloadableInMemoryDaoImpl.class);

	/** The m_resource file. */
	private File m_resourceFile;

	/** The m_props. */
	private Properties m_props = null;

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.memory.InMemoryDaoImpl#loadUserByUsername(java.lang.String)
	 */
	@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
		try
		{
    		for (String aUserName : listUsers())
    			if (aUserName.equals(username))
    				continue;

	    	loadProperties();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}

	    return super.loadUserByUsername(username);
	}

    /**
     * Reload properties.
     */
    public void reloadProperties()
    {
    	m_props = null;
    }

    /**
     * Sets the resource.
     *
     * @param resource the new resource
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void setResource(Resource resource) throws IOException
    {
    	m_resourceFile = resource.getFile();
    	loadProperties();
    }

    /**
     * Load properties.
     *
     * @return the properties
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Properties loadProperties() throws IOException
    {
    	if (m_resourceFile != null && m_props == null)
    	{
	        m_props = new Properties();
	        m_props.load(new FileInputStream(m_resourceFile));
    	}
        setUserProperties(m_props);
        return m_props;
    }

    /**
     * List users.
     *
     * @return the list
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public List<String> listUsers() throws IOException
    {
		List<String> result = new ArrayList<String>();
    	for (Object key : loadProperties().keySet())
    		result.add((String) key);
    	return result;
    }
}