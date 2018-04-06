package uk.gov.dvla.osg.appPwdUpdate.email;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DevNotifyEmail {
	
	private static final Logger LOG = LogManager.getLogger();
	private static final String credentialsFile;
	private static final String contactsFile;
	private static final String OS = System.getProperty("os.name").toLowerCase();
	
	static {
		// Windows locations set for dev environment otherwise uses Unix filepath
		if (OS.indexOf("win") >= 0) {
			credentialsFile = "Z:/osg/resources/config/email.xml";
			contactsFile = "Z:/osg/resources/config/contacts.xml";
		} else {
			credentialsFile = "//aiw//osg//resources//config//email.xml";
			contactsFile = "//aiw//osg//resources//config//contacts.xml";
		}
	}
	
	/**
	 * Constructs email from settings in the email config file and sends to Dev Team members.
	 * @param subjectLine Subject line of the email
	 * @param msgText Email text body
	 * @param recipients comma separated list of email addresses
	 */
	public static void send() {
		
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		String timeStamp = dateFormat.format(new Date());
		
		String subjectLine = "AppPwdUpdate - Error";
		String msgText = timeStamp + " : An error entry has been made in the AppPwdUpdate log file.";

		// load SMTP configuration from config file
		Credentials security = null;
		try {
			security = DevNotifyEmail.loadCredentials();
		} catch (JAXBException ex) {
			LOG.error("Unable to load data from email config file: {}", credentialsFile);
			return;
		}
		
		String username = security.getUsername();
		String password = security.getPassword();
		String host = security.getHost();
		String port = security.getPort();
		String from = security.getFrom();
		
		// load dev team contact email addresses
		Address[] contacts = null;
		try {
			contacts = getContacts();
		} catch (IOException ex) {
			LOG.error("Unable to load contacts file: {}", contactsFile);
			return;
		} catch (AddressException ex) {
			LOG.error("Contacts file contains an invalid email address: {}", contactsFile);
			return;
		}
		
		// Email salutation and signature lines
		String bodyHead = "Hello,\n\n";
		String bodyFoot = "\n\nPlease investigate ASAP\n\nThanks";

		// Setup mail server
		Properties properties = new Properties();
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", port);
		properties.put("mail.smtp.auth", "false");
		properties.put("mail.smtp.starttls.ename", "false");

		// Setup authentication, get session
		Session emailSession = Session.getInstance(properties, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(emailSession);
			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));
			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, contacts);
			// Set Subject: header field
			message.setSubject(subjectLine);
			// Now set the actual message body
			message.setText(bodyHead + msgText + bodyFoot);
			// Send message
			Transport.send(message);
		} catch (MessagingException mex) {
			LOG.error("Unable to create email: ", mex);
		}
	}

	/**
	 * Converts the XML in the SMTP config file into a credetials object
	 * @return SMTP configuration information.
	 * @throws JAXBException file does not contain valid XML.
	 */
	private static Credentials loadCredentials() throws JAXBException {
		File file = new File(credentialsFile);
		JAXBContext jc = JAXBContext.newInstance(Credentials.class);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (Credentials) unmarshaller.unmarshal(file);
	}
	
	/**
	 * Gets the list of dev team members' email addresses from the config file.
	 * @return dev team email addresses.
	 * @throws IOException config file cannot be located.
	 * @throws AddressException file contains an invalid email address.
	 */
	private static Address[] getContacts() throws IOException, AddressException {
		List<String> list = Files.readAllLines(Paths.get(contactsFile));
		Address[] addresses = new Address[list.size()];
		for (int i = 0; i < list.size(); i++) {
		    addresses[i] = new InternetAddress(list.get(i));
		}
		return addresses;
	}
	
}
