package uni_potsdam.pier.psycWall;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Value;



public class PsycWallHelper {
	
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom random = new SecureRandom();
    @Value("${app.constants.saltHash}")
    private static String salt;
    
    public static String hashString(String input) {
    	
    	return hashString(input, salt);
    	
    }

	    public static String hashString(String input, String salt) {
	        try {
	            MessageDigest digest = MessageDigest.getInstance("SHA-256");
	            byte[] hashBytes = digest.digest((input + salt).getBytes(StandardCharsets.UTF_8));
	            return bytesToHex(hashBytes);
	        } catch (NoSuchAlgorithmException e) {
	            throw new RuntimeException("SHA-256 algorithm not available", e);
	        }
	    }

	    private static String bytesToHex(byte[] bytes) {
	        StringBuilder hexString = new StringBuilder();
	        for (byte b : bytes) {
	            String hex = Integer.toHexString(0xff & b);
	            if (hex.length() == 1)
	                hexString.append('0');
	            hexString.append(hex);
	        }
	        return hexString.toString();
	    }
	    
	    public static final PolicyFactory genPsycWallPolicy() {
	     return new HtmlPolicyBuilder()

  	      // --- erlaubte Block-Elemente ---
  	      .allowElements("p", "h1", "h2", "h3", "h4")

  	      // --- Links ---
  	      .allowElements("a")
  	      .allowAttributes("href").onElements("a")
  	      .allowAttributes("title").onElements("a")
  	      .allowAttributes("target").onElements("a")
  	      .allowAttributes("rel").onElements("a")
  	      .allowUrlProtocols("http", "https")

  	      // --- Bilder ---
  	      .allowElements("img")
  	      .allowAttributes("src").onElements("img")
  	      .allowAttributes("alt").onElements("img")
  	      .allowAttributes("title").onElements("img")
  	      .allowAttributes("width", "height").onElements("img")

  	      // optional: nur http/https fürs img src (greift i.d.R. über allowUrlProtocols,
  	      // aber diese Zeile ist ok als "Sicherheitsanker", falls du später erweiterst)
  	      .allowUrlProtocols("http", "https")

  	      // --- Target/Rel härten ---
  	      // Hinweis: java-html-sanitizer setzt nichts automatisch; wenn du target=_blank zulässt,
  	      // solltest du serverseitig rel="noopener noreferrer" hinzufügen (z.B. beim Rendern),
  	      // oder target ganz weglassen.
  	      .toFactory();

	    }
	   
	    public static String generateRandomString(int length) {
	        StringBuilder builder = new StringBuilder(length);
	        for (int i = 0; i < length; i++) {
	            int index = random.nextInt(LETTERS.length());
	            builder.append(LETTERS.charAt(index));
	        }
	        return builder.toString();
	    }
	    
	    
	}
	
