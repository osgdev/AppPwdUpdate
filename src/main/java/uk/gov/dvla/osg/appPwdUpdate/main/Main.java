package uk.gov.dvla.osg.appPwdUpdate.main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import uk.gov.dvla.osg.appPwdUpdate.email.DevNotifyEmail;
import uk.gov.dvla.osg.appPwdUpdate.networking.NetworkConfig;
import uk.gov.dvla.osg.appPwdUpdate.networking.RestClient;
import uk.gov.dvla.osg.appPwdUpdate.utils.RandomPasswordGenerator;


/**
 * Update passwords for all applications that login with RPD.
 * Each application will be given its own password and stored in a common
 * password file on the RPD server.
 * Application to be run on a scheduled basis from Crontab.
 * ******************** REVISION HISTORY *****************************************
 * 18/01/2018 - Initial version -  Pete Broomhall
 * *******************************************************************************
 * @version 1.0.0
 * @author Pete Broomhall - OSG Dev Team
 */
public class Main {
	
	private static Properties appPasswords;
	private static NetworkConfig networkConfig;
	private static String passwordsFile, configFile, token, appName, appPwd, loggedInApp;
	private static boolean loggedIn;
	static final Logger LOG = LogManager.getLogger();
	
	public static void main(String[] args) {

		LOG.info("----- Application Started -----");
		// Process command line args
		processArgs(args);
		// Load passwords file
		setProperties();
		// Loop through the applications with new random passwords
		updatePasswords();
		// Logout of session
		logout();
		// Save properties to disk
		saveProperties();
		LOG.info("----- Application Ended -----");
		
	}

	/**
	 * Loads properties file and network configuration file
	 * Usage: AppPwdUpdate.jar {config_file} {data_file}
	 * @param args command line arguments
	 */
	private static void processArgs(String[] args) {
		
		LOG.trace("Processing command line args");
		
		if (args.length == 2) {
			
			configFile = args[0];
			LOG.debug("configFile = {}", configFile);
			passwordsFile = args[1];
			LOG.debug("passwordsFile = {}", passwordsFile);
			
			if (!(new File(passwordsFile).exists())) {
				LOG.fatal("Data file '{}' doesn't exist!", passwordsFile);
				System.exit(1);
			}
			if (!(new File(configFile).exists())) {
				LOG.fatal("Config file '{}' doesn't exist!", configFile);
				System.exit(1);
			}
		} else {
			LOG.fatal("Incorrect number of args. Usage: appPwdUpdate.jar {config_file} {data_file}");
			System.exit(1);
		}
	}
	
	/**
	 * Load in the properties from the two config files
	 */
	private static void setProperties() {
		LOG.trace("Loading Properties file");
		try {
			// load application properties
			byte[] fileContents = Files.readAllBytes(Paths.get(passwordsFile));
			InputStream reader = new ByteArrayInputStream(fileContents);
			appPasswords = new Properties();
			appPasswords.load(reader);
		} catch (Exception ex) {
			error("Unable to load application properties.", ex);
			System.exit(1);
		}
		
		// load network properties from JSON file
		LOG.trace("Loading JSON file");
		try {
			networkConfig = new Gson().fromJson(new FileReader(configFile), NetworkConfig.class);
		} catch (Exception ex) {
			error("Unable to read JSON file", ex);
			System.exit(1);
		}
	}
	
	/**
	 * Log an application in with with RPD. The app used to login is stored in the
	 * LoggedInApp variable to be used for logging out once complete.
	 * Create a new password according to RPDs complexity rules and send the
	 * old and new passwords to RPD.
	 */
	private static void updatePasswords() {
		// Get a list of all usernames
		Enumeration em = appPasswords.keys();
		// Log in once and use this session token to update all applications
		loggedIn = false;
		// Loop through all applications
        while (em.hasMoreElements()) {
            // Get key value pairs from props
            appName = (String) em.nextElement();
            LOG.debug("appName = {}", appName);
            appPwd = appPasswords.getProperty(appName);
            LOG.debug("appPwd = {}", appPwd);
            
            // Use the first application to login and retrieve token 
            if (!loggedIn) {
            	LOG.info("Logging in with application {}", appName);
            	token = RestClient.rpdLogin(networkConfig, appName, appPwd);
            	loggedInApp = appName;
            	// Log in was successful if token isn't empty
            	if (!token.equals("")) {
					loggedIn = true;
					LOG.trace("Log in successful.");
				} else {
					error("Unable to log in with application " + appName);
				} 
            }
            
            // once token retrieved, construct JSON for HTML body and transmit to RPD
            if (loggedIn) {
            	
            	boolean success = false;
				int retry = 0;
				String newPassword;
				
				/* Send new pasword to RPD - update may fail RPD deems the new password
				 * to be too similar to previous one. As this is unlikely due to the randomization
				 * algorithms being used to build the new password, we allow RPD to make this check.
				 * Two attempts are made as RPD will lock the account after the third attempt and 
				 * this would prevent the application from working.
				 */
				do {
	            	// Generate random password
	                newPassword = RandomPasswordGenerator.generatePswd();
	                LOG.debug("newPassword = {}", newPassword);
					// Manually Construct JSON
					String json = "{\"User.password\":\"" + appPwd + "\"," 
								+ "\"User.passwordNew\":\"" + newPassword + "\","
								+ "\"User.passwordConfirm\":\"" + newPassword + "\"}";
					LOG.trace("json = {}", json);
					success = RestClient.rpdUpdatePwd(networkConfig, appName, token, json);
					retry++;
					LOG.debug("Transmitted to RPD {}, Attempt {}, Success {}", appName, retry, success);
				} while (!success && retry < 2);
				
				// Update password in properties object if accepted by RPD, error email sent in RestClient
				if (success) {
					appPasswords.setProperty(appName, newPassword);
					LOG.info("Password updated for {}", appName);
				} else {
					error("Unable to set password for " + appName);
				}
			}
        }
	}
	
	/**
	 * Write the changed passwords back to the password file
	 */
	private static void saveProperties() {
		
		LOG.trace("Saving application properties file : {}", passwordsFile);
		
		try (FileOutputStream fileOut = new FileOutputStream(new File(passwordsFile))) {
			appPasswords.store(fileOut, null);
		} catch (Exception e) {
			error("Unable to save password file", e);
		} 
	}
	
	/**
	 * Send data to Rest Client to log session app out of RPD.
	 */
	private static void logout() {
		if (loggedIn) {
			RestClient.rpdLogOut(networkConfig, loggedInApp, token);
		}
	}
	
	/**
	 * Log the error to the rolling file and send a notification email to dev team
	 * @param msg Message to add to log file
	 */
	private static void error(String msg) {
		LOG.error(msg);
		DevNotifyEmail.send();
	}
	
	/**
	 * Log the error to the rolling file and send a notification email to dev team
	 * @param msg Message to add to log file
	 * @param ex caught Exception
	 */
	private static void error(String msg, Exception ex) {
		LOG.error(msg, ex);
		DevNotifyEmail.send();
	}
}
