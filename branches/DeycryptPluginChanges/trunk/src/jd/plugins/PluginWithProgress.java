package jd.plugins;

import java.net.URL;
import java.util.Vector;

import jd.controlling.ProgressController;
import jd.utils.JDUtilities;


public abstract class PluginWithProgress extends Plugin {
	protected ProgressController progress;
	
	private int seed = 0; 
	
	protected void initProgress( String statusText ){
		if (progress != null && !progress.isFinished()) {
			progress.finalize();
			logger.warning(" Progress ist besetzt von " + progress);
		}

		progress = new ProgressController("Decrypter: " + this.getLinkName());
		progress.setStatusText("decrypt-" + getPluginName() + ": " + statusText);
	}
	
	
	protected DownloadLink createDownloadlink(String link){
		DownloadLink dl= new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(link), true);
		return dl;
	}
	
	
	/**
	 * 
	 * @param url
	 * @return
	 */
	protected String generateJDFileName( URL url){
		return url.toString()+" ["+ (++seed) +"]";
	}
	
	
	/**
	 * Sucht in data nach allen passenden links und gibt diese als vektor zurück
	 * 
	 * @param data
	 * @return
	 */
	public Vector<String> getDecryptableLinks(String data) {
		Vector<String> hits = getMatches(data, getSupportedLinks());
		if (hits != null && hits.size() > 0) {

			for (int i = 0; i < hits.size(); i++) {
				String file = hits.get(i);
				while (file.charAt(0) == '"')
					file = file.substring(1);
				while (file.charAt(file.length() - 1) == '"')
					file = file.substring(0, file.length() - 1);
				hits.setElementAt(file, i);
			}
		}
		return hits;
	}
	
}
