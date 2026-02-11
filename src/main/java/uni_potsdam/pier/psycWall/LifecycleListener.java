package uni_potsdam.pier.psycWall;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LifecycleListener {
	
	private String startPass;
	private final RedisConnect rc;
	
	public LifecycleListener(RedisConnect rc,
			@Value("${STARTPASS:}") String startPass) {
		this.rc = rc;
		this.startPass = startPass;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady(ApplicationReadyEvent event) {
		
		if(rc.getKey("conf_WelcomeText") == null || rc.getKey("conf_WelcomeText").equals("")) {
			rc.storeKey("conf_WelcomeText", "<h1>PsycWall</h1>Please enter tan to proceed:");
			System.out.println("Welcome text set to default!");
		}
		if(rc.getKey("conf_confirmationText") == null || rc.getKey("conf_confirmationText").equals("")) {
			rc.storeKey("conf_confirmationText", "<h1>PsycWall</h1>login success");
			System.out.println("Confirmation text set to default!");
		}
		if(rc.getKey("conf_pass") == null || rc.getKey("conf_pass").equals("")) {
			if(startPass == null || startPass.equals("") || startPass.equals("secret")) {
				String message = "STARTPASS in .env not set. ";
		        System.err.println(message);
		        throw new IllegalStateException(message);
			} else {
				rc.storeKey("conf_pass", PsycWallHelper.hashString(startPass));
			}
			System.out.println("Admin password set!");
		}
	}

}
