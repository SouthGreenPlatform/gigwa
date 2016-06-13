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
package fr.cirad.web.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * The Class AutoUnzipFilter.
 */
public class AutoUnzipFilter implements javax.servlet.Filter {

	/** The Constant LOG. */
	private static final Logger LOG = Logger.getLogger(AutoUnzipFilter.class);

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy()
	{
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException
	{
		if (! (request instanceof HttpServletRequest))
		{
			fc.doFilter(request, response);
			return;
		}

		HttpServletRequest hsRequest = (HttpServletRequest) request;
		String sServletPath = hsRequest.getServletPath();
		File f = new File(hsRequest.getSession().getServletContext().getRealPath(sServletPath));
		int nLastDotPos = sServletPath.lastIndexOf(".");

		if (f.exists() || nLastDotPos == -1)
		{
			fc.doFilter(request, response);
			return;
		}

		String sServletPathWithoutExtension = sServletPath.substring(0, nLastDotPos), extension = sServletPath.substring(nLastDotPos);
		f = new File(hsRequest.getSession().getServletContext().getRealPath(sServletPathWithoutExtension + ".zip"));
		if (!f.exists())
		{
			fc.doFilter(request, response);
			return;
		}

		// unzip archive
		ZipInputStream zis = new ZipInputStream(new FileInputStream(f));
		ZipEntry ze = zis.getNextEntry();

		try
		{
			if (extension.startsWith("."))
				extension = extension.substring(1);

			byte[] buffer = new byte[1024];
	    	while (ze!=null)
	    	{
	    		String fileName = ze.getName();
	    		if (fileName.endsWith("." + extension))
	    		{
	    			LOG.debug("Sending file " + fileName + " from archive " + f.getName() + " into response");
					response.setContentType("application/octet-stream;charset=ISO-8859-1");
					((HttpServletResponse) response).setHeader("Content-disposition", "inline; filename=" + fileName);
	            	int len;
		            while ((len = zis.read(buffer)) > 0)
		            	response.getOutputStream().write(buffer, 0, len);
		            response.getOutputStream().close();
		            return;
	    		}
	            ze = zis.getNextEntry();
	     	}
	    	throw new IOException("No file found with extension '" + extension + "' in " + f.getName());
		}
		finally
		{
	    	zis.closeEntry();
	     	zis.close();
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException
	{
	}

}
