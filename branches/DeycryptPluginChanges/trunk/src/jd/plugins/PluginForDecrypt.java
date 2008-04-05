//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.

//This program  is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//GNU General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import jd.plugins.domain.Project;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
@SuppressWarnings("unchecked")
public abstract class PluginForDecrypt extends PluginWithProgress implements Comparable {

	/**
	 * Diese Methode entschlüsselt Links.
	 * 
	 * @param cryptedLinks Ein Vector, mit jeweils einem verschlüsseltem Link.
	 *            Die einzelnen verschlüsselten Links werden aufgrund des
	 *            Patterns
	 *            {@link jd.plugins.Plugin#getSupportedLinks() getSupportedLinks()}
	 *            herausgefiltert
	 * @return Ein Vector mit Klartext-links
	 */

	public PluginForDecrypt() {
		super();
		this.type = Plugin.Type.DECRYPT;

	}



//	/**
//	* @deprecated
//	* @param cryptedLinks
//	* @return
//	*/
//	public Vector<DownloadLink> decryptLinks(Vector<String> cryptedLinks) {
//	Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
//	Iterator<String> iterator = cryptedLinks.iterator();

//	while (iterator.hasNext()) {
//	String link = iterator.next();


//	decryptedLinks.addAll(decryptLink(link).getDownloadLinks());
//	}

//	return decryptedLinks;
//	}

	public Vector<Project> decryptLinks(Vector<String> cryptedLinks){
		Vector<Project> projects = new Vector<Project>();
		Project project = null;

		for( String link : cryptedLinks){
			project = decryptLink(link);
			if( project.hasFiles() ){
				projects.add(project);
			}
		}

		return projects;
	}

	private String cryptedLink              = null;
	protected Vector<String> default_password=new Vector<String>();;


	/**
	 * Finds all downloadable files that are found under cryptedLinkes and
	 * returns a Project with those files. If no files are found, an empty Project is returned
	 * @param cryptedLink
	 * @return
	 */
	public Project decryptLink( String cryptedLink ){
		this.cryptedLink = cryptedLink;
		initProgress(cryptedLink);

		PluginStep step = null;
		Project project = null;

		while ((step = nextStep(step)) != null) {
			doStep(step, cryptedLink);

			if (null == nextStep(step)) {
				logger.info("decrypting finished");
				Object tmpLinks = step.getParameter();

				if( tmpLinks == null || !(tmpLinks instanceof Project)){
					logger.severe("In the last step DecryptPlugins have to return a Project");
					project =  new Project("empty");
				}else{
					project = (Project)tmpLinks;
				}
			}
		}

		progress.finalize();
		return project;
	}



	/**
	 * Diese Methode arbeitet die unterschiedlichen schritte ab. und gibt den
	 * gerade abgearbeiteten Schritt jeweisl zurück.
	 * 
	 * @param step
	 * @param parameter
	 * @return gerade abgeschlossener Schritt
	 */
	public abstract PluginStep doStep(PluginStep step, String parameter);


	/**
	 * @return the default passwords that are set for this decrypt plugin
	 */
	public Vector<String> getDefaultPassswords(){
		return default_password;
	}

	/**
	 * Deligiert den doStep Call weiter und ändert dabei nur den parametertyp.
	 */
	public PluginStep doStep(PluginStep step, Object parameter) {
		return doStep(step, (String) parameter);
	}

	/**
	 * Gibt den namen des internen CryptedLinks zurück
	 * 
	 * @return encryptedLink
	 */

	public String getLinkName() {
		if (cryptedLink == null) return "";
		try {
			return new URL(cryptedLink).getFile();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * vergleicht Decryptplugins anhand des Hostnamens
	 * wird zur Sortierung benötigt
	 */
	public int compareTo(Object o)
	{
		return getHost().toLowerCase().compareTo(((PluginForDecrypt)o).getHost().toLowerCase());
	}

}
