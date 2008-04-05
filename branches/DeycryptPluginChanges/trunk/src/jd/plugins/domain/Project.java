package jd.plugins.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

/**
 * A project is a collection of files. For each file, there can be a number of downloadlinks (Mirrors)
 * 
 * @author signed
 *
 */
//TODO signed: should listen on default download directory changed
public class Project extends JDFileContainer implements Comparable<Project>{
	
	String name;
	String downloadDirectory = null;
	String comment = "";
	Set<String> passwords;
	
	boolean useDefaultDownloadDirectory = false;
	
	
	
	
	/**
	 * creates a project object with the given name
	 * The files will be stored in the default downoadDirectory, unless
	 * you set it with <code>setDownloadDirectory()</code>
	 * @param name
	 */
	public Project( String name){
		this(name, null );
	}

	/**
	 * create a project object with the given name an downloaddirectory
	 * @param name
	 * @param downloadDirectory if null, the default download directory will be taken
	 */
	public Project( String name, String downloadDirectory ){
		super();
		this.name = name;
		this.downloadDirectory = downloadDirectory;
		this.passwords = new HashSet<String>();
		
		if( null == name || name.equals("")){
			logger.warning("The name provided was null or the empty String - use default name");
			this.name = "defaul ProjectName";
		}
		
		if( null == downloadDirectory || downloadDirectory.equals("")){
			logger.info("the provided downloadDirectory is null or the empty string - use default download directory");
			this.useDefaultDownloadDirectory = true;
			this.downloadDirectory = null;
		}
	}
	
	/**
	 * sets the name of the project
	 * @param name (null or empty strings are not allowed and will be skipped if provided as argument
	 */
	public void setName( String name ){
		if( null == name || name.equals("")){
			logger.severe("The name you provided is null or an empty String");
			return;
		}
		this.name = name;
	}
	
	/**
	 * @return the name of the project
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * sets the comment for this project. If the provided argument is null, the comment
	 * will be reset to the empty string
	 * @param comment
	 */
	public void setComment( String comment){
		if( null == comment ){
			this.comment = "";
			return;
		}
		
		this.comment = comment; 
		
	}
	
	/**
	 * Gets the comment for this Project as specified by the user
	 * @return the comment String
	 */
	public String getComment( ){
		return this.comment;
	}
	
	/**
	 * adds password to the set of passwords that are set for this Project
	 * @param password  not null, not empty
	 */
	public void addPassword(String password){
		if(null == password || password.length()==0 ){
			logger.warning("The provided password is null or the empty string");
			return;
		}
		passwords.add(password);
	}
	
	/**
	 * Sets the Base directory, where the files of this project will be
	 * stored after downloading them
	 * @param path
	 */
	public void setDownloadDirectory( String path){
		if( null == path || path.equals("")){
			logger.severe("The provided argument is null or an empty string");
			return;
		}
		
		//TODO signed: if some files have already been downloaded, they should be moved to the new Directory
		//TODO signed: check if this folder gan be created and used as a download directory
		
		this.useDefaultDownloadDirectory = false;
		
		this.downloadDirectory = path;
		
	}
	
	/**
	 * returns the base directory path where the files of the project will
	 * be stored after downloading them
	 * @return
	 */
	public String getDownloadDirectory(){
		
		if( useDefaultDownloadDirectory){
			return downloadDirectory = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
		}
		
		return downloadDirectory;
	}

	/**
	 * returns a string representation of a project object
	 */
	public String toString(){
		String ret = "";
		ret += "name: " + this.name +"\n";
		ret += "downDir: " + ((useDefaultDownloadDirectory)?"default" : this.downloadDirectory) + "\n";
		ret += "files:\n";
		ret += super.toString();
		
		return ret;
	}
	
	/**
	 * 
	 * @return true if project has files, false otherwise
	 */
	public boolean hasFiles(){
		return !data.isEmpty();
	}

	
	@Override
	public int compareTo(Project o) {
		return this.name.compareTo(o.getName());
	}
}
