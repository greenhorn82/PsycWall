package uni_potsdam.pier.psycWall;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.owasp.html.PolicyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


@Controller
public class PsycWallController {
	private final static int MAXSESSIONTIME = 14400;
    private final RedisConnect rc;

   

    public PsycWallController(RedisConnect rc) {
        this.rc = rc;
    }
    
    private String genSessionId(HttpServletRequest request){ 	
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getRemoteAddr();
    } else {
        // Falls mehrere IPs im Header stehen (bei Proxies), nimm die erste
        ip = ip.split(",")[0];
    }

    return "IP:" + PsycWallHelper.hashString(ip);
    }
    
  

   
    
    
    @GetMapping("/")
    public String downloadIndex(@RequestParam(value = "vpCode", required = false) String vpCode,
    		Model model) {
        
        if(vpCode == null || vpCode.equals("")) {
        	vpCode="no_code";
        }
        model.addAttribute("vpCode", vpCode);
        model.addAttribute("contentHtml", rc.getKey("conf_WelcomeText"));
        //model.addAttribute("contentHtml","test");
        return "tan";
    }
    
    @RequestMapping("/submit-tan")
    public String submitTan(@RequestParam("tan") String tan, 
    		@RequestParam(value = "vpCode", required = false) String vpCode, 
    		HttpServletRequest request, Model model) {
    	String aktStatus = rc.getKey("tan_" +  tan);
    	if (aktStatus == null || aktStatus.compareTo("zugelassen") != 0) {
    		aktStatus = "nicht zugelassen";
    		
    	} else {
           	rc.storeKey(genSessionId(request) , MAXSESSIONTIME);
           	rc.removeKey("tan_" +  tan);
        }
    	model.addAttribute("contentHtml", rc.getKey("conf_confirmationText"));
    	model.addAttribute("message", "TAN : " + tan + " ist " + aktStatus);
        return "afterTan";
        		
    }
    
    @RequestMapping("/generate-tans")
    public String generateTans(@RequestParam Map<String, String> params,
    						   HttpServletRequest request,
                               Model model) throws IOException {
    	HttpSession session = request.getSession(false);
    	String aktion = "";
    	if(!(params.get("aktion") == null)) {
    		 aktion =params.get("aktion");
    	}
    	 
    	model.addAttribute("oldWelcomeText", rc.getKey("conf_WelcomeText"));
    	model.addAttribute("oldconfirmationText", rc.getKey("conf_confirmationText"));
      	
    	if(session == null) {
    		session = request.getSession();
    		return "login";
    	}
    	if(aktion.equals("login")) {
    		  String aktPass = rc.getKey("conf_pass");    	
    	        if (aktPass == null || 
    	        		!aktPass.equals(PsycWallHelper.hashString(params.get("password"))) ) {
    	        	model.addAttribute("message", "Ungültiges Passwort: " + 
    	        PsycWallHelper.hashString(params.get("password")));
    	        	return "login";
    	        }else {
    	        	session.setAttribute("loginSuccessful", "erfolg");
    	        	return "generateAdmin";
    	        }
    	}
    	boolean passValid = false;
    	if(session.getAttribute("loginSuccessful") == null || 
    			!session.getAttribute("loginSuccessful").equals("erfolg")) {
    		return "login";
    	}else {
    		passValid = true;
    	}
    	
      
        if(aktion.equals("generate") && passValid) {
        	
        	int count = 0;
        	String message = "";
        	try {
        		 count = Integer.parseInt(params.get("count"));
        	}catch(NumberFormatException  e) {
        		count = 0;
        		message = "Anzahl der Strings nicht erkannt!<br>";
        	}
        	
        	String[] tan_list = new String[count];
        	for(int i = 0;  i < count; i++) {
        	
        			tan_list[i] = PsycWallHelper.generateRandomString(6);
        			rc.storeKey("tan_" + tan_list[i], -1);
        }
        	model.addAttribute("message", 
        			message + "Es wurden " + count + " TANs generiert..." + String.join("\r\n", tan_list));
            return "generate-tans_result";

        }
        if(aktion.equals("changeWelcomeText") && passValid) {
        	PolicyFactory policy = PsycWallHelper.genPsycWallPolicy();
        	String safeHTML = policy.sanitize(params.get("welcomeText"));
        	rc.storeKey("conf_WelcomeText", safeHTML);
        	model.addAttribute("message", "Welcome page content updated:");
        	model.addAttribute("contentHtml", safeHTML);
            return "generate-tans_result";

        }
        if(aktion.equals("changeConfirmationText") && passValid) {
        	PolicyFactory policy = PsycWallHelper.genPsycWallPolicy();
        	String safeHTML = policy.sanitize(params.get("confirmationText"));
        	rc.storeKey("conf_confirmationText", safeHTML);
        	model.addAttribute("message", "Confirmation page content updated:");
        	model.addAttribute("contentHtml", safeHTML);
            return "generate-tans_result";

        }

        
        if(aktion.equals("show") && passValid) {
        	Set<String> tans = rc.getKeyList("tan_*");
        	String tanstring = String.join(", ", tans).replaceAll("tan_", "");
        	model.addAttribute("message", tanstring);
        	//return tanstring;
            return "generate-tans_result";

        }
        	
        return "generateAdmin";
    }
    

    @RequestMapping("/validAccess")
    public ResponseEntity<?> checkKey(HttpServletRequest request) { 
    	
    	
    	String aktStatus = rc.getKey(genSessionId(request));
    	if (aktStatus == null || aktStatus.compareTo("zugelassen") != 0) {
    		
    		String errorMess = rc.getKey("conf_noValidTan");
    		rc.storeKey(request.toString() + request.getHeader("X-Forwarded-For"), -1);
    		if(errorMess == null) {
    			errorMess = "<html><body><h1>404 - Tan fehlt</h1></body></html>" ;
    		}
    		return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorMess);
        }

        return ResponseEntity.ok(aktStatus);
    }
    
}