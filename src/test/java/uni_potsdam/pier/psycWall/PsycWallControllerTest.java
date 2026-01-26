package uni_potsdam.pier.psycWall;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.http.HttpSession;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.LinkedHashSet;
import java.util.Set;

@WebMvcTest(PsycWallController.class)
class PsycWallControllerTest {
	@Autowired
    MockMvc mockMvc;
	
	  @MockitoBean
	private RedisConnect rc;
	  

	    // Hilfs-PostProcessor: sorgt dafür, dass eine Session existiert (oder vorhandene genutzt wird)
	    private static RequestPostProcessor withSessionAttr(String key, Object value) {
	        return request -> {
	            HttpSession session = request.getSession(true);
	            session.setAttribute(key, value);
	            return request;
	        };
	    }

	    @BeforeEach
	    void commonRcStubs() {
	        when(rc.getKey("conf_WelcomeText")).thenReturn("WELCOME_OLD");
	        when(rc.getKey("conf_confirmationText")).thenReturn("CONFIRM_OLD");
	    }
	  
	@Test
	void testSubmitTan_unsuccessful() throws Exception {
		given(rc.getKey(anyString())).willReturn(null);
		
		mockMvc.perform(get("/validAccess"))
		.andExpect(status().is4xxClientError());
		//.andExpectAll(content().equals("zugelassen"));
       given(rc.getKey(anyString())).willReturn("nicht zugelassen");
		
		mockMvc.perform(get("/validAccess"))
		.andExpect(status().is4xxClientError());
	}
	
	@Test
	void testSubmitTan_successful() throws Exception {
        given(rc.getKey(anyString())).willReturn("zugelassen");
		
		mockMvc.perform(get("/validAccess"))
		.andExpect(status().isOk());
	}

	@Test
    void sessionNull_returnsLogin_andStillAddsOldTexts() throws Exception {
          mockMvc.perform(post("/generate-tans")
                .param("aktion", "show"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    @Test
    void login_invalidPassword_returnsLogin_setsMessage() throws Exception {
        when(rc.getKey("conf_pass")).thenReturn("HASHED_STORED");

        try (MockedStatic<PsycWallHelper> helper = Mockito.mockStatic(PsycWallHelper.class)) {
            helper.when(() -> PsycWallHelper.hashString("wrong")).thenReturn("HASHED_WRONG");

            mockMvc.perform(post("/generate-tans")
                    // wichtig: Session muss existieren, sonst triggert dein Code vorher return "login"
                    .with(request -> { request.getSession(true); return request; })
                    .param("aktion", "login")
                    .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("message", containsString("Ungültiges Passwort")))
                .andExpect(model().attribute("message", containsString("HASHED_WRONG")));
        }
    }
	
    @Test
    void login_validPassword_returnsGenerateAdmin_setsSessionFlag() throws Exception {
        when(rc.getKey("conf_pass")).thenReturn("HASHED_STORED");

        try (MockedStatic<PsycWallHelper> helper = Mockito.mockStatic(PsycWallHelper.class)) {
            helper.when(() -> PsycWallHelper.hashString("correct")).thenReturn("HASHED_STORED");

            MvcResult res = mockMvc.perform(post("/generate-tans")
                    .with(request -> { request.getSession(true); return request; })
                    .param("aktion", "login")
                    .param("password", "correct"))
                .andExpect(status().isOk())
                .andExpect(view().name("generateAdmin"))
                .andExpect(model().attribute("oldWelcomeText", "WELCOME_OLD"))
                .andExpect(model().attribute("oldconfirmationText", "CONFIRM_OLD"))
                .andReturn();

            HttpSession session = res.getRequest().getSession(false);
            assertNotNull(session);
            assertEquals("erfolg", session.getAttribute("loginSuccessful"));
        }
    }
    
    @Test
    void nonLoginAction_notLoggedIn_returnsLogin() throws Exception {
        mockMvc.perform(post("/generate-tans")
                .with(request -> { request.getSession(true); return request; })
                .param("aktion", "show"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    @Test
    void showAction_loggedIn_returnsResult_messageHasJoinedTans() throws Exception {
        Set<String> tans = new LinkedHashSet<>();
        tans.add("tan_111");
        tans.add("tan_222");
        when(rc.getKeyList("tan_*")).thenReturn(tans);

        mockMvc.perform(post("/generate-tans")
                .with(withSessionAttr("loginSuccessful", "erfolg"))
                .param("aktion", "show"))
            .andExpect(status().isOk())
            .andExpect(view().name("generate-tans_result"))
            .andExpect(model().attribute("message", "111, 222"))
            .andExpect(model().attribute("oldWelcomeText", "WELCOME_OLD"))
            .andExpect(model().attribute("oldconfirmationText", "CONFIRM_OLD"));

        verify(rc).getKeyList("tan_*");
    }
    
    @Test
    void changeWelcomeText_loggedIn_sanitizes_stores_setsModel_andReturnsResult() throws Exception {
        PolicyFactory policy = mock(PolicyFactory.class);
        when(policy.sanitize("<b>x</b><script>bad</script>")).thenReturn("<b>x</b>");

        try (MockedStatic<PsycWallHelper> helper = Mockito.mockStatic(PsycWallHelper.class)) {
            helper.when(PsycWallHelper::genPsycWallPolicy).thenReturn(policy);

            mockMvc.perform(post("/generate-tans")
                    .with(withSessionAttr("loginSuccessful", "erfolg"))
                    .param("aktion", "changeWelcomeText")
                    .param("welcomeText", "<b>x</b><script>bad</script>"))
                .andExpect(status().isOk())
                .andExpect(view().name("generate-tans_result"))
                .andExpect(model().attribute("message", "Welcome page content updated:"))
                .andExpect(model().attribute("contentHtml", "<b>x</b>"));

            verify(rc).storeKey("conf_WelcomeText", "<b>x</b>");
        }
    }
    
    @Test
    void changeConfirmationText_loggedIn_sanitizes_stores_setsModel_andReturnsResult() throws Exception {
        PolicyFactory policy = mock(PolicyFactory.class);
        when(policy.sanitize("ok<script>bad</script>")).thenReturn("ok");

        try (MockedStatic<PsycWallHelper> helper = Mockito.mockStatic(PsycWallHelper.class)) {
            helper.when(PsycWallHelper::genPsycWallPolicy).thenReturn(policy);

            mockMvc.perform(post("/generate-tans")
                    .with(withSessionAttr("loginSuccessful", "erfolg"))
                    .param("aktion", "changeConfirmationText")
                    .param("confirmationText", "ok<script>bad</script>"))
                .andExpect(status().isOk())
                .andExpect(view().name("generate-tans_result"))
                .andExpect(model().attribute("message", "Confirmation page content updated:"))
                .andExpect(model().attribute("contentHtml", "ok"));

            verify(rc).storeKey("conf_confirmationText", "ok");
        }
    }

    @Test
    void generateAction_loggedIn_returnsResult_andSetsMessageWithCount() throws Exception {
        mockMvc.perform(post("/generate-tans")
                .with(withSessionAttr("loginSuccessful", "erfolg"))
                .param("aktion", "generate")
                .param("count", "3"))
            .andExpect(status().isOk())
            .andExpect(view().name("generate-tans_result"))
            .andExpect(model().attribute("message", containsString("Es wurden 3 TANs generiert")));
    }

    @Test
    void unknownAction_loggedIn_returnsGenerateAdmin() throws Exception {
        mockMvc.perform(post("/generate-tans")
                .with(withSessionAttr("loginSuccessful", "erfolg"))
                .param("aktion", "somethingElse"))
            .andExpect(status().isOk())
            .andExpect(view().name("generateAdmin"));
    }

    @Test
    void missingAktion_param_throwsNpe_currentImplementation() throws Exception {
           mockMvc.perform(post("/generate-tans")
                .with(withSessionAttr("loginSuccessful", "erfolg")))
        .andExpect(status().isOk())
        .andExpect(view().name("generateAdmin"));;
    }

    @Test
    void generate_countNotNumber_throws_currentImplementation() throws Exception {
        // Integer.parseInt -> NumberFormatException -> 500
        mockMvc.perform(post("/generate-tans")
                .with(withSessionAttr("loginSuccessful", "erfolg"))
                .param("aktion", "generate")
                .param("count", "abc"))
        .andExpect(status().isOk())
        .andExpect(view().name("generateAdmin"));
    }
    
}
