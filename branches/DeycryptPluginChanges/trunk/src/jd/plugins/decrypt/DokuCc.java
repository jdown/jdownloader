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


package jd.plugins.decrypt; import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.plugins.domain.JDFile;
import jd.plugins.domain.Project;

public class DokuCc extends PluginForDecrypt {
	final static String host             = "doku.cc";

	private String      version          = "1.0.0.0";

	private static Pattern patternSupported = Pattern.compile("http://doku\\.cc/[\\d]{4}/[\\d]{2}/[\\d]{2}/([^/]+)/", Pattern.CASE_INSENSITIVE);
	private static Pattern patternHttpLink = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");

	public DokuCc() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
		default_password.add("doku.cc");
	}

	@Override
	public PluginStep doStep(PluginStep step, String parameter) {
		if (step.getStep() == PluginStep.STEP_DECRYPT) {
			
			Project project = null;
			
			try {
				//get the name
				String name = "Default Name";
				Matcher nameMatcher = patternSupported.matcher(parameter);
				if( nameMatcher.find() ){				
					name = nameMatcher.group(1);
				}
				
				logger.info("create project with name: "+ name );
				project = new Project( name );
				project.setComment(parameter);
				
				//load the page that contains the links
				URL url = new URL(parameter);
				RequestInfo reqinfo = getRequest(url);
				
				String[] htmlSourceAreas = new Regexp(reqinfo.getHtmlCode(), "<p><strong>[^<]+(</strong><a href.*?)</p>").getMatches(1);
				if( 1 < htmlSourceAreas.length){
					logger.warning("found more than one potential link zone in html source - adjust regexp");
				}
				
				
				JDFile jdFile = null;
				for (int i = 0; i < htmlSourceAreas.length; ++i) {
					//System.out.println(htmlSourceAreas[i]);

					//TODO signed: this has to be improved to handle mirrors correctly, especially if the mirror consists only of one link
					Matcher links = patternHttpLink.matcher(htmlSourceAreas[i]);
					while( links.find()){
						DownloadLink link = createDownloadlink(links.group(1));
						String linkName = links.group(2);
						
						jdFile = new JDFile( linkName );
						jdFile.addMirror(link);
						project.addFile(jdFile);
					}
				}

				step.setParameter(project);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean doBotCheck(File file) {
		return false;
	}

	@Override
	public String getCoder() {
		return "signed";
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public String getPluginID() {
		return host + "-" + version;
	}

	@Override
	public String getPluginName() {
		return host;
	}

	@Override
	public Pattern getSupportedLinks() {
		return patternSupported;
	}

	@Override
	public String getVersion() {
		return version;
	}

}