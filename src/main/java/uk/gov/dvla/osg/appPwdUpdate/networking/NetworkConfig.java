package uk.gov.dvla.osg.appPwdUpdate.networking;

public class NetworkConfig {

	private String protocol, host, port, loginUrl, logoutUrl, updateUrl;

	public String getLoginUrl() {
		return protocol + host + ":" + port + loginUrl;
	}

	public String getLogoutUrl() {
		return protocol + host + ":" + port + logoutUrl;
	}
	
	public String getUpdateUrl() {
		return protocol + host + ":" + port + updateUrl;
	}
	
}
