package jd.plugins;

import java.net.URL;

import jd.plugins.domain.JDFileContainer;

public abstract class PluginForRedirect extends PluginWithProgress{
	
	protected URL redirectInputUrl;
	
	public PluginForRedirect(){
		super();
		this.type = Plugin.Type.REDIRECT;
	}
	
	@Override
	public String getLinkName(){
		return redirectInputUrl.toString();
	}
	
	
	
	/**
	 * Deligiert den doStep Call weiter und ändert dabei nur den parametertyp.
	 */
	public PluginStep doStep(PluginStep step, Object parameter) {
		return doStep(step, (URL) parameter);
	} 
	
	/**
	 * In the last plugin step the <code>step.setParameter()</code> has to contain
	 * the links  url redirects to
	 * @param step
	 * @param url
	 * @return the next step to perform or null if the current step was the last one
	 */
	public abstract PluginStep doStep(PluginStep step, URL url);
	
	public JDFileContainer redirect( URL url ){
		this.redirectInputUrl = url;
		
		initProgress(url.toString());
		
		PluginStep step = null;
		JDFileContainer jdFileContainer = null;

		while ((step = nextStep(step)) != null) {
			doStep(step, url);

			if (null == nextStep(step)) {
				logger.info("redirecting finished");
				Object tmpContainer = step.getParameter();

				if( tmpContainer == null || !(tmpContainer instanceof JDFileContainer)){
					logger.severe("In the last step DecryptPlugins have to return a Project");
					jdFileContainer =  new JDFileContainer();
				}else{
					jdFileContainer = (JDFileContainer)tmpContainer;
				}
			}
		}
		
		
		progress.finalize();
		
		
		return jdFileContainer;
	}

}
