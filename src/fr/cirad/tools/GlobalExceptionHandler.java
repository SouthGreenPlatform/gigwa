/**
 * ***********************************************************************
 * GIGWA - Genotype Investigator for Genome Wide Analyses
 * Copyright (C) 2016 <CIRAD>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License, version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * See <http://www.gnu.org/licenses/agpl.html> for details about GNU Affero
 * General Public License V3.
 * ***********************************************************************
 */
package fr.cirad.tools;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.UnmodifiableMap;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

@ControllerAdvice
public class GlobalExceptionHandler {

	protected static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);
	
	@Autowired private SimpleMappingExceptionResolver exceptionResolver;
	
//	@ExceptionHandler(AccessDeniedException.class)
//	public String handleAccessDeniedException(HttpServletRequest request, Exception ex){
//		LOG.info("AccessDeniedException Occured:: URL="+request.getRequestURL());
//		return "database_error";
//	}
//	
//	@ExceptionHandler(Exception.class)
//	public String handleException(HttpServletRequest request, Exception ex){
//		LOG.info("Exception Occured:: URL="+request.getRequestURL());
//		return "database_error";
//	}

//	
//	@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="IOException occured")
//	@ExceptionHandler(IOException.class)
//	public void handleIOException(){
//		logger.error("IOException handler executed");
//		//returning 404 error code
//	}
	
  @ExceptionHandler(Exception.class)
  @ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
  public ModelAndView handleAllExceptions(HttpServletRequest request, HttpServletResponse response, Exception ex) {
  	LOG.error("Error at URL " + request.getRequestURI() + "?" + request.getQueryString(), ex);
  	if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")))
  	{
  		HashMap<String, String> map = new HashMap<String, String>();
  		map.put("errorMsg", ExceptionUtils.getStackTrace(ex));
  		return new ModelAndView(new MappingJackson2JsonView(), UnmodifiableMap.decorate(map));
  	}
  	else
  		return exceptionResolver.resolveException(request, response, null, ex);
  }
//  
//  @ExceptionHandler(AccessDeniedException.class)
//  @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
//  public ModelAndView handleAccessDeniedExceptions(HttpServletRequest request, HttpServletResponse response, Exception ex) {
//  	LOG.error("Error at URL " + request.getRequestURI() + "?" + request.getQueryString(), ex);
//  	if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")))
//  	{
//  		HashMap<String, String> map = new HashMap<String, String>();
//  		map.put("errorMsg", ExceptionUtils.getStackTrace(ex));
//  		return new ModelAndView(new MappingJackson2JsonView(), UnmodifiableMap.decorate(map));
//  	}
//  	else
//  		return exceptionResolver.resolveException(request, response, null, ex);
//  }
	
//	@RequestMapping("gigwa/totoruru")
//	public ModelAndView dd(HttpServletRequest request, HttpServletResponse response, Exception ex)
//	{
//		return new ModelAndView("aie");
//	}
}