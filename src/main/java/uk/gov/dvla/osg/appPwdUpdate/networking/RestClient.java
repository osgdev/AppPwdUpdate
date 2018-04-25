package uk.gov.dvla.osg.appPwdUpdate.networking;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.GsonBuilder;

import uk.gov.dvla.osg.appPwdUpdate.email.DevNotifyEmail;
import uk.gov.dvla.osg.appPwdUpdate.utils.JsonUtils;

/**
 * Utility methods to transmit messages to the RPD REST service. 
 * These are set by the RPD REST api and shouldn't be amended.
 */
public class RestClient {

	static final Logger LOG = LogManager.getLogger();

	/**
	 * Log an application into RPD and obtain a session token.
	 * @param config Contains the URL to locate the LogIn service
	 * @param appName Name of the application
	 * @param appPwd Current password for the application
	 * @return active token for the session
	 */
	public static String rpdLogin(NetworkConfig config, String appName, String appPwd) {
        
        try {
        	Response response = ClientBuilder.newClient()
        									 .target(config.getLoginUrl())
        									 .queryParam("name", appName)
        									 .queryParam("pwd", appPwd)
        									 .request(MediaType.APPLICATION_JSON)
        									 .get();
        	String data = response.readEntity(String.class); 
            if (response.getStatus() == 200) {
            	LOG.trace("Login succeeded");
            	String token = JsonUtils.getTokenFromJson(data);
            	return token;
            } else {
            	// RPD provides clear error information, and so is mapped to model
                LoginBadResponseModel br = new GsonBuilder().create().fromJson(data, LoginBadResponseModel.class);
                LOG.error("{} {} {}",br.getMessage(), br.getAction(), br.getCode());
            }
        } catch (ProcessingException e) {
        	LOG.error("Failed to connect to RPD", e);
        } catch (Exception e) {
        	LOG.error("Failed to log into RPD", e);
        }
        return "";
	}

	/**
	 * Log user out of RPD
	 * @param config Contains the URL to locate the LogOut service
	 * @param appName name of the application used to log in
	 * @param token RPD token for the active session
	 */
	public static void rpdLogOut(NetworkConfig config, String appName, String token) {

		try {
			Response response = ClientBuilder.newClient()
									.target(config.getLogoutUrl())
									.path(appName)
									.request(MediaType.APPLICATION_JSON)
									.header("token", token)
									.post(null);
			if (response.getStatus() != 200) {
				LOG.error("Logout failed - Unable to log application out of RPD web service.");
				DevNotifyEmail.send();
			}
		} catch (ProcessingException e) {
			LOG.error("Connection timed out - Unable to log application " + appName + " out of RPD web service.");
			DevNotifyEmail.send();
		} catch (Exception e) {
			LOG.error("Unable to log application " + appName + " out of RPD", e);
			DevNotifyEmail.send();
		}
	}
	
	/**
	 * Request RPD updaes the password for the applicaiton. This may fail due to the provided password
	 * being too similar to the previous one.
	 * @param config Network configuration data required to build the URL
	 * @param appName Application whose credentials are being updated
	 * @param token Session token required by RPD
	 * @param json RPD reqires the HTML body to be in JSON format
	 * @return true if password succesfully updated in RPD
	 */
	public static boolean rpdUpdatePwd(NetworkConfig config, String appName, String token, String json) {
        
        try {
        	// Create an Apache HttpClient as Jersey client has no PATCH method
        	HttpClient httpclient  = HttpClientBuilder.create().build();
        	String patchUrl = config.getUpdateUrl() + appName;
        	HttpPatch httpPatch  = new HttpPatch(patchUrl);
        	// Add message headers
        	httpPatch.addHeader("token", token);
        	// Set content type and message body
            StringEntity params = new StringEntity(json);
            params.setContentType("application/json");
            httpPatch.setEntity(params);
            // Send the request to RPD
            HttpResponse response = httpclient.execute(httpPatch);
            int statusCode = response.getStatusLine().getStatusCode();
            LOG.trace("Response Code for" + appName + ": " + response.getStatusLine().getStatusCode());
            // Check the status of the response
            if (statusCode == 200) {
            	LOG.info(appName + " password updated");
            	return true;
            } else {
                LOG.error("Unable to update password for {}, Error code = {}", appName, statusCode);
            }
        } catch (HttpHostConnectException ex) {
        	LOG.error("Unable to connect to RPD!", ex);
        } catch (Exception e) {
			LOG.error("An error occured while updating the password.", e);
        }
        return false;
	}
}
