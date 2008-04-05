package jd.plugins.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;


/**
 * This class represents a single File that should be downloaded.
 * A file has a name and my have zero to infinite mirrors. A JDFile
 * either has no  Project it belongs  to, or  exactly one Project or it
 * is part of
 * @author signed
 *
 */
public class JDFile {
	
	private static transient Logger        logger;
	
	static {
		logger = JDUtilities.getLogger();
	}
	
	
	String name = null;
	Project project = null;
	
	ArrayList<DownloadLink> mirrors = null;
	
	/**
	 * Creates a new JDFile with the given name. The name can be the nome the downloaded file
	 * will have on the filesystem, or something linke "part1", "part2". Filename will be changed
	 * during the decrypting and download process to represent the files actual name.
	 * @param name
	 */
	public JDFile( String name){
		this(name, null);
	}

	/**
	 * Creates a new JDFile with the given name. The name can be the nome the downloaded file
	 * will have on the filesystem, or something linke "part1", "part2". Filename will be changed
	 * during the decrypting and download process to represent the files actual name
	 * @param name has to be a valid String, not null not empty
	 * @param project the project this JDFile belongs to, may be null until the file is assigned to a Project
	 */
	public JDFile( String name, Project project){
		if( name == null || name.equals("")){
			logger.severe("provided filename is null or is the empty string");
			this.name = "default_file_name"; //TODO signed localize
		}else{
			this.name = name;
		}
		
		this.project = project;
		this.mirrors = new ArrayList<DownloadLink>();
	}
	
	
	/**
	 * Sets the name of the file to name, but only if there exists no other file
	 * within the project this file belongs to, that has the same name you try to
	 * set
	 * @param name
	 */
	public void setName( String name ){
		if( name == null || name.equals("")){
			logger.severe("name argument is null or the empty string - fix this");
			return;
		}
		
		if( (null != this.name) && (name.equals(this.name)) ){
			//nothing to change, its the same name...
			return;
		}
		
		//if the file is part of a Project we have to check if
		//there already is a file with this name
		if( null != this.project && this.project.hasFile(name) ){
			logger.warning("the parent project already has a file with name = "+name);
			return;
		}
		
		this.name = name;
	}
	
	/**
	 * 
	 * @return the name of this JDFile, may be null
	 */
	public String getName(){
		
		return this.name;
	}
	
	/**
	 * Sets the project this package belongs to, should only be calle from within
	 * the Project class
	 * @param project
	 */
	public void setProject( Project project){
		this.project = project;
	}
	
	/**
	 * Adds a whole collection of downloadlinks/Mirrors for the file
	 * @param fileName
	 * @param mirrors
	 */
	public void addMirrors( Collection<DownloadLink> mirrors){
		if( null == mirrors){
			logger.severe("The provided collection is empty");
			return;
		}

		for( DownloadLink link: mirrors){
			addMirror(link);
		}
	}
	
	/**
	 * adds a single mirror for file, but only if there is no Mirror set
	 * that points to the same URL
	 * @param link
	 * @return true if file was added successfully, flalse otherwise
	 */
	public boolean addMirror( DownloadLink link ){
		if( null == link ){
			logger.severe("The provided Argument is null");
			return false;
		}
		
		String linkURL =  link.getDownloadURL();
		if( null == linkURL || linkURL.equals("") ){
			logger.severe("the link you try to add has no valid downloadUrl");
			return false;
		}
		
		linkURL = JDUtilities.htmlDecode(linkURL);
		link.setUrlDownload(linkURL);
		
		//check if this mirror is already set
		for( DownloadLink mirror : mirrors){
			if( mirror.getDownloadURL().equals(linkURL) ){
				logger.warning("This mirror is already set for the file");
				return false;
			}
		}
		
		mirrors.add(link);
		return true;
	}
	
	
	/**
	 * This would be used, after a redirect like lix.in, where the
	 * in-out ratio is 1:1 on a URL level. The in URL is replaced with the
	 * out URL of the redirecter e.g. lix.in
	 * @param i
	 * @param link
	 */
	public void replaceMirror(int i, DownloadLink link){
		this.mirrors.remove(i);
		this.mirrors.add(link);
	}
	
	/**
	 * This would be used after a redirect, where you put one link in,
	 * and get multiple URLs for the same file back (mirrors).
	 * @param i
	 * @param links
	 */
	public void replaceMirror(int i, Collection<DownloadLink> links){
		this.mirrors.remove(i);
		this.mirrors.addAll(links);
	}
	
	
	/**
	 * returns the i-th mirror for this file
	 * first mirror starts at 0 to <code>mirrorCount()-1</code> as the
	 * last mirror
	 * 
	 * @param i
	 * @return
	 */
	public DownloadLink getMirror( int i ){
		return this.mirrors.get(i);
	}
	
	/**
	 * removes the i-th mirror from the file
	 * @param i
	 * @return
	 */
	public DownloadLink removeMirror( int i){
		return this.mirrors.remove(i);
	}
	
	/**
	 * 
	 * @return list with all mirrors for this file, may be empty
	 */
	public final List<DownloadLink> getMirrors( ){
		return mirrors;
	}
	
	/**
	 * returns the number of mirrors available for this file
	 * @return
	 */
	public int mirrorCount(){
		return this.mirrors.size();
	}
	
	public String toString(){
		String ret = "";
		ret += this.name + " ("+this.mirrors.size()+" mirrors)\n";
		
		for( DownloadLink link : mirrors ){
			ret += "\t"+link.getDownloadURL() + "\n";
		}
		
		return ret;
	}
}
