package uk.gov.dvla.osg.appPwdUpdate.email;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Credentials {

	String password;
	String host;
	String port;
	String from;
	String username;
	
	@XmlElement(name="username")
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	@XmlElement(name="password")
	public String getPassword() {
		return password;
	}

	public void setPasword(String pasword) {
		this.password = pasword;
	}
	@XmlElement(name="host")
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
	
	@XmlElement(name="port")
	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}
	
	@XmlElement(name="from")
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}
}
