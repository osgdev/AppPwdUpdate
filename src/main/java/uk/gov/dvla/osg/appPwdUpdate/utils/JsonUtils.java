package uk.gov.dvla.osg.appPwdUpdate.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Utility methods to extract information from the JSON data responses that are
 * returned from the RPD REST api.
 */
public class JsonUtils {
	
	static final Logger LOG = LogManager.getLogger();
	
	/**
	 * Extracts the user token from message body of a successful RPD login request
	 * @param jsonString RPD login request message body
	 * @return session token, or blank string if token not available
	 */
	public static String getTokenFromJson(String jsonString) {
		try {
			return new JsonParser().parse(jsonString).getAsJsonObject().get("token").getAsString();
		} catch (JsonSyntaxException e) {
			LOG.error("String is not valid JSON.", e);
		} catch (Exception e) {
			LOG.error("Unable to extract token from JSON.", e);
		}
		return "";
	}
	
	// Suppress default constructor for noninstantiability
	private JsonUtils() {
		throw new AssertionError();
	}
}
