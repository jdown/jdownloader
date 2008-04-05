package jd.controlling;

import java.util.Vector;

import jd.plugins.domain.JDFile;
import jd.plugins.domain.JDFileContainer;
import jd.plugins.domain.Project;

public class ContentTransport {
	
	Vector<Project> projects;
	Vector<JDFileContainer> containers;
	Vector<JDFile> files;
	Vector<String> passwords;
	
	public ContentTransport( Vector<Project> projects, Vector<JDFileContainer> containers, Vector<JDFile> files){
		this.projects = projects;
		this.containers = containers;
		this.files = files;	
	}
	
	public Vector<Project> getProjects() {
		return projects;
	}
	public void setProjects(Vector<Project> projects) {
		this.projects = projects;
	}
	public Vector<JDFileContainer> getContainers() {
		return containers;
	}
	public void setContainers(Vector<JDFileContainer> containers) {
		this.containers = containers;
	}
	public Vector<JDFile> getFiles() {
		return files;
	}
	public void setFiles(Vector<JDFile> files) {
		this.files = files;
	}

	public Vector<String> getPasswords() {
		return passwords;
	}

	public void setPasswords(Vector<String> passwords) {
		this.passwords = passwords;
	}
	
	public boolean isEmpty(){
		return projects.isEmpty() && containers.isEmpty() && files.isEmpty();
	}
	
	public String toString(){
		String ret = "";
		ret += "Projects:\n";
		for(Project project: projects){
			ret+=project.toString();
		}
		ret += "\nContainers:\n";
		for(JDFileContainer container : containers){
			ret+=container.toString();
		}
		
		ret+="\nFiles\n";
		for(JDFile file : files){
			ret+=file.toString();
		}
		
		ret+="\nPasswords\n";
		for(String pw : passwords){
			ret += "\t"+ pw;
		}
		
		return ret;
	}

}
