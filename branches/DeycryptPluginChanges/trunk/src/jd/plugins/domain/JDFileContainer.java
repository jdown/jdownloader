package jd.plugins.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

public class JDFileContainer {
	
	protected static transient Logger logger;
	
	static {
		logger = JDUtilities.getLogger();
	}
	
	/**
	 * key: filename
	 * value: List of all DownloadLinks (Mirrors) for this file
	 */
	protected HashMap<String, JDFile> data = null;
	
	boolean  isLegacyContainer = false;
	DownloadLink[] links;
	
	

	
	/**
	 * This is a legacy Constructor to address the Problem of DownloadLink[] return Types
	 * For Plugins who do not return a JDFileContainer right now, the DownloadLinks will be wrapped
	 * into a JDFileContainer object, and this object can than be returned by methods and be unwrapped
	 * to process the DownloadLink[] using <code>getDownloadLinks()</code>
	 * @param links
	 */
	public JDFileContainer(DownloadLink[] links ){
		isLegacyContainer = true;
		this.links = links;
	}
	
	
	/**
	 * Constructs a new empty JDFileContainer
	 */
	public JDFileContainer(){
		this.data = new HashMap<String, JDFile>();
	}
	
	/**
	 * adds a new file to the Container, if the Container already contains a file with a name
	 * equal to jdFile.getName(), the file will not be added to the Container. To Add the file
	 * you  have to remove the old file First
	 * @param fileName
	 */
	public void addFile( JDFile jdFile){
		if( isLegacyContainer){
			logger.warning("this is a legacy conatiner, you can not add JDFiles to it");
			return;
		}
		
		if( null == jdFile ){
			logger.severe("The provided Argument is null");
			return;
		}
		
		String fileName = jdFile.getName();
		JDFile oldValue = data.put(fileName, jdFile);
		
		if( null != oldValue ){
			logger.info("There is already a file with name= "+fileName+" in this Project");
			data.put(fileName, oldValue);
		}
	}
	
	/**
	 * Adds all files that are in container to this container
	 * @param container
	 */
	public void addFiles( JDFileContainer container){
		this.addFiles(container.getFiles());
	}
	
	/**
	 * Adds a whole collection of files to this container
	 * @param files
	 */
	public void addFiles( Collection<JDFile> files){
		for( JDFile jdFile : files){
			this.addFile(jdFile);
		}
	}
	
	
	/**
	 * 
	 * @param jdFile
	 */
	public void removeFile( JDFile jdFile ){
		if( null == jdFile ){
			logger.warning("the passed jdFile is null");
			return;
		}
		
		JDFile removeJDFile = this.data.remove(jdFile.getName());
		if( null == removeJDFile){
			logger.warning("you tried to remove a file, that is not in the Container");
		}
	}
	
	/**
	 * replaces a given File with the files in the container
	 * @param jdFile
	 * @param container
	 */
	public void replaceFile( JDFile jdFile, JDFileContainer container){
		this.removeFile(jdFile);
		this.addFiles(container.getFiles());
	}
	
	/**
	 * Legacy Method for transition between return type DownloadLink Array and Project 
	 */
	public Vector<DownloadLink> getDownloadLinks(){
		
		if( isLegacyContainer ){
			Vector<DownloadLink> retLinks = new Vector<DownloadLink>(this.links.length);
			for( DownloadLink link: this.links){
				retLinks.add(link);
			}
			return retLinks;
		}
		
		//total number of mirrors is unknown, assume one mirror for each file -> data.size()
		Vector<DownloadLink> allDownloadLinks = new Vector<DownloadLink>(data.size());
		JDFile jdFile = null;
		for(String filename : data.keySet()){
			jdFile = data.get(filename);
			allDownloadLinks.addAll(jdFile.getMirrors());
		}
		return allDownloadLinks;
	}
	
	
	/**
	 * Gets a set of all files contained in this Container
	 * @return
	 */
	public Set<String> getFilesName(){
		return data.keySet();
	}
	
	/**
	 * Returns a maybe empty list of JDFiles associated to this container
	 * @return a collection of all files contained in this container
	 */
	public Collection<JDFile> getFiles(){
		return data.values();
	}
	
	/**
	 * are there any JDFiles Stored in this container
	 * @return
	 */
	public boolean isEmpty(){
		return data.isEmpty();
	}
	
	/**
	 * Returns the number of JDFiles that are part of this container
	 * @return #JDFiles in the container
	 */
	public int size(){
		return data.size();
	}
	
	/**
	 * Checks if the Project has a file with the same name as the on passed
	 * tho the method
	 * @return true if there is a file in the Project with the same name, false otherwise
	 */
	public boolean hasFile( JDFile file){
		if( isLegacyContainer ){
			logger.warning("this is a legacy  container, it does not contain JDFiles!");
			return false;
		}else{
			return this.hasFile(file.getName());
		}
	}
	
	/**
	 * Checks if the Project has a file with a name that equals fileName
	 * @param fileName
	 * @return true if there is a file with a name that equals fileName, false otherwise
	 */
	public boolean hasFile( String fileName ){
		return data.keySet().contains(fileName);
	}
	
	@Override
	public String toString(){
		String ret = "";
		JDFile jdFile = null;
		for( Entry<String, JDFile> entry : data.entrySet()){
			jdFile = entry.getValue();
			ret += jdFile.toString();
		}
		return ret;
	}
}
