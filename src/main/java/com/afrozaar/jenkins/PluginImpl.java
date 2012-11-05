package com.afrozaar.jenkins;

import hudson.Plugin;

import org.apache.log4j.Logger;

public class PluginImpl extends Plugin {
	private final static Logger LOG = Logger.getLogger(PluginImpl.class
			.getName());

	public void start() throws Exception {
		LOG.info("starting jenkins azure");
	}
}