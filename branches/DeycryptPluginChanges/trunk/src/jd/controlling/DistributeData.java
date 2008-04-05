//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.controlling;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForRedirect;
import jd.plugins.domain.JDFile;
import jd.plugins.domain.JDFileContainer;
import jd.plugins.domain.Project;
import jd.utils.JDUtilities;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage
 * an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData extends ControlMulticaster {
    /**
     * Der Logger
     */
    private static Logger            logger = JDUtilities.getLogger();

    /**
     * Die zu verteilenden Daten
     */
    private String                   data;

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data Daten, die verteilt werden sollen
     */
    public DistributeData(String data) {
        super("JD-DistributeData");
        this.data = data;
    }

    public void run() {
        ContentTransport transporter = findLinks();
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, transporter));
    }

    /**
     * Ermittelt über die Plugins alle Passenden Links und gibt diese in einem
     * Vector zurück
     * 
     * @return link-Vector
     */
    public ContentTransport findLinks(){
        Vector<String> foundpassword = Plugin.findPasswords(data);
        reformDataString();

        //first search for projects
        Vector<Project> projects = handleDecryptPlugins();
        Vector<JDFileContainer> containers = handleRedirectPlugins();
        Vector<JDFile> files = handleHosterPlugins( );
        
        
        //
        Vector<JDFileContainer> tmp = new Vector<JDFileContainer>(projects.size() + containers.size());
        tmp.addAll(projects);
        tmp.addAll(containers);

        
        for( JDFileContainer container : tmp){
        	deepRedirect(container);
        	attachHostPlugin(container);
        }
        
        ContentTransport transporter = new ContentTransport(projects, containers, files);
        transporter.setPasswords(foundpassword);

        return transporter;
    }
    
    
    /**
     * Attaches the host plugin to the downloadlink. If no Host plugin can be found
     * for a mirror, the mirror will be dropped
     * @param container
     */
    private void attachHostPlugin(JDFileContainer container ){
    	Vector<PluginForHost> pluginsHost = JDUtilities.getPluginsForHost();
    	for( JDFile jdFile : container.getFiles()){
    		for( int i=jdFile.mirrorCount()-1; i>=0; --i){
    			DownloadLink link = jdFile.getMirror(i);
    			boolean attachSuccessfull = false;
    			for(PluginForHost pluginHost : pluginsHost){
    				if( pluginHost.canHandle(link.getDownloadURL())){
    					pluginHost.attachHostPlugin(link);
    					attachSuccessfull = true;
    					break;
    				}
    			}
    			if( !attachSuccessfull ){
    				logger.info("unable to find host plugin for: "+link.getDownloadURL());
    				jdFile.removeMirror(i);
    			}
    		}
    	}

    }

    /**
     * @return
     */
    private Vector<Project> handleDecryptPlugins() {
        Vector<Project> createdProjects = new Vector<Project>();
    	
        if ( JDUtilities.getPluginsForDecrypt() == null) return createdProjects;
        
        Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
        while (iteratorDecrypt.hasNext()) {
			PluginForDecrypt pDecrypt = (PluginForDecrypt) iteratorDecrypt.next();
            if (pDecrypt.canHandle(data)) {

                try {
                    pDecrypt = pDecrypt.getClass().newInstance();

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));
                    Vector<String> decryptableLinks = pDecrypt.getDecryptableLinks(data);
                    data = pDecrypt.cutMatches(data);

                    createdProjects.addAll(pDecrypt.decryptLinks(decryptableLinks));

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
                }
                catch (Exception e) {

                    e.printStackTrace();
                }
            }
		}

        return createdProjects;
    }
    
    private Vector<JDFileContainer>  handleRedirectPlugins(){
    	Vector<JDFileContainer> createdContainer = new Vector<JDFileContainer>();
    	
    	if( JDUtilities.getPluginsForRedirect() == null ) return createdContainer;
    	
    	for( PluginForRedirect pluginRedirect : JDUtilities.getPluginsForRedirect()){
    		if( pluginRedirect.canHandle(data)){
    			
    			try {
					pluginRedirect = pluginRedirect.getClass().newInstance();
				} catch (Exception e) {
					logger.severe("unable to greate a new Instance of redirectPlugin: "+ pluginRedirect.getHost());
					continue;
				}
				
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_REDIRECT_ACTIVE, pluginRedirect));
                Vector<String> redirectableLinks = pluginRedirect.getDecryptableLinks(data);
                data = pluginRedirect.cutMatches(data);
                
                for(String link: redirectableLinks){
					try {
	                	URL url;
						url = new URL(link);
						createdContainer.add( pluginRedirect.redirect(url) );
					} catch (MalformedURLException e) {
						logger.severe("unable to create url out of redirectPlugin supported Pattern: "+pluginRedirect.getHost()+"\n"+link);
					}
                }

                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_REDIRECT_INACTIVE, pluginRedirect));
    		}
    	}
    	
    	
    	return createdContainer;
    }


    /**
     * Parses all links from data, that can be directly downloaded from a hoster
     * and returns them in a JDFile format
     * @return
     */
    private Vector<JDFile> handleHosterPlugins( ){
    	Vector<JDFile> createdFiles = new Vector<JDFile>();
    	
        Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
        while (iteratorHost.hasNext()) {
			PluginForHost pHost = (PluginForHost) iteratorHost.next();
            if (pHost.canHandle(data)) {
                Vector<DownloadLink> dl = pHost.getDownloadLinks(data);
                for( DownloadLink link : dl){
                	JDFile jdFile = new JDFile(link.getName());
                	jdFile.addMirror(link);
                	createdFiles.add(jdFile);
                }
                data = pHost.cutMatches(data);
            }
		}
    	return createdFiles;
    }
    
    private JDFileContainer deepRedirect( JDFile jdFile ){
    	Vector <PluginForRedirect> redirectPlugins = JDUtilities.getPluginsForRedirect();
    	JDFileContainer result = new JDFileContainer();
    	
    	boolean redirected = false;
    	
    	do{
    		//reset the exit condition
    		redirected= false;
        	for( PluginForRedirect redirectPlugin : redirectPlugins){
        		int mirrorCount = jdFile.mirrorCount();
        		for( int i= mirrorCount -1; i>=0 ; --i){
        			DownloadLink mirror = jdFile.getMirror(i);
        			String urlString = mirror.getDownloadURL();
        			URL url = null;
        			
        			try {
						url = new URL(urlString);
					} catch (MalformedURLException e) {
						//this mirror can not be handled, remove it
						logger.severe("maleformed url: "+ urlString);
						jdFile.removeMirror(i);
						continue;
					}
        			
        			if( redirectPlugin.canHandle(urlString)){
        				JDFileContainer tmpContainer = redirectPlugin.redirect( url );
        				if( null == tmpContainer || tmpContainer.isEmpty()){
        					logger.warning("redirect Plugin returned invalid JDFileContainer - mirror dropped: "+urlString);
        					jdFile.removeMirror(i);
        					continue;
        				}
        				
        				
        				Collection<JDFile> files = tmpContainer.getFiles();
        				Iterator<JDFile> iterator = files.iterator();
        				
        				logger.info("RedirectPlugin returned "+ files.size() + " files");
        				//redirect JD
        				if( 1 == tmpContainer.size() ){
        					
        					//TODO signed: check if the redirect plugin gave back a file name we can use
        					JDFile redirectedFile = iterator.next();
        					jdFile.replaceMirror(i, redirectedFile.getMirrors());
        					redirected = true;
        				}else{
        					//the redirect retrieved multiple files
        					deepRedirect(tmpContainer);
        					result.addFiles(tmpContainer.getFiles());
        					jdFile.removeMirror(i);
        				}
        			}
        		}
        	}
        	
        	
    	}while( redirected );

    	//logger.info("deepRedirect returns:\n"+ result);
    	return result; 
    }
    
    /**
     * 
     * @param jdFileContainer
     * @return the provided argument jdFileContainer is just returned
     */
    private JDFileContainer deepRedirect( JDFileContainer jdFileContainer){
    	for( JDFile jdFile : jdFileContainer.getFiles()){
    		JDFileContainer redirectedJDFileContainer = deepRedirect(jdFile);
    		
    		if( 0 == jdFile.mirrorCount() && (null == redirectedJDFileContainer || redirectedJDFileContainer.isEmpty())){
    			logger.severe("Redirect for file \""+jdFile.getName()+"\" failed");
    			jdFileContainer.removeFile(jdFile);
    			continue;
    		}
    		
    		if( 1 <  redirectedJDFileContainer.size()){
    			if( jdFile.mirrorCount()>=0){
    				jdFileContainer.addFiles(redirectedJDFileContainer);
    			}else{
    				jdFileContainer.replaceFile(jdFile, redirectedJDFileContainer);
    			}
    		}
    	}
    	
    	return jdFileContainer;
    }

    /**
     * Bringt alle links in data in eine einheitliche Form
     */
    private void reformDataString() {
        if (data != null) {
            data = Plugin.getHttpLinkList(data);

            try {
                this.data = URLDecoder.decode(this.data, "UTF-8");
            }
            catch (Exception e) {
                logger.warning("text not url decodeable");
            }
        }
    }
}