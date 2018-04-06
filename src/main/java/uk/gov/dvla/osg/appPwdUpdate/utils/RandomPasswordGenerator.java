package uk.gov.dvla.osg.appPwdUpdate.utils;

import java.util.Random;

/**
 * @author http://theopentutorials.com/tutorials/java/util/generating-a-random-password-with-restriction-in-java/
 *
 */
public class RandomPasswordGenerator {
    private static final String ALPHA_CAPS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA   = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUM     = "0123456789";
    private static final String SPL_CHARS   = "!@#$%^&*";
 
    private static final int NO_OF_CAPS_ALPHA = 1;
    private static final int NO_OF_DGITS = 1;
    private static final int NO_OF_SPECIAL_CHARS = 1;
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 15;
	
    public static String generatePswd() {
        Random rnd = new Random();
        int len = rnd.nextInt(MAX_LENGTH - MIN_LENGTH + 1) + MIN_LENGTH;
        char[] pswd = new char[len];
        int index = 0;
        for (int i = 0; i < NO_OF_CAPS_ALPHA; i++) {
            index = getNextIndex(rnd, len, pswd);
            pswd[index] = ALPHA_CAPS.charAt(rnd.nextInt(ALPHA_CAPS.length()));
        }
        for (int i = 0; i < NO_OF_DGITS; i++) {
            index = getNextIndex(rnd, len, pswd);
            pswd[index] = NUM.charAt(rnd.nextInt(NUM.length()));
        }
        for (int i = 0; i < NO_OF_SPECIAL_CHARS; i++) {
            index = getNextIndex(rnd, len, pswd);
            pswd[index] = SPL_CHARS.charAt(rnd.nextInt(SPL_CHARS.length()));
        }
        for(int i = 0; i < len; i++) {
            if(pswd[i] == 0) {
                pswd[i] = ALPHA.charAt(rnd.nextInt(ALPHA.length()));
            }
        }
        return new String(pswd);
    }
     
    private static int getNextIndex(Random rnd, int len, char[] pswd) {
        int index = rnd.nextInt(len);
        while(pswd[index = rnd.nextInt(len)] != 0);
        return index;
    }
    
	// Suppress default constructor for noninstantiability
	private RandomPasswordGenerator() {
		throw new AssertionError();
	}
}