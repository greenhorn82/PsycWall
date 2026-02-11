package uni_potsdam.pier.psycWall;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartUpTest {


    @Test
    void setsDefaultWelcomeTextWhenMissing() {
        RedisConnect rc = mock(RedisConnect.class);
        when(rc.getKey("conf_WelcomeText")).thenReturn(null);
        when(rc.getKey("conf_confirmationText")).thenReturn("already");
        when(rc.getKey("conf_pass")).thenReturn("already");

        LifecycleListener sut = new LifecycleListener(rc, "secret");

        sut.onApplicationReady(null);

        verify(rc).storeKey("conf_WelcomeText", "<h1>PsycWall</h1>Please enter tan to proceed:");
        verify(rc, never()).storeKey(eq("conf_confirmationText"), anyString());
        verify(rc, never()).storeKey(eq("conf_pass"), anyString());
    }

    @Test
    void setsDefaultConfirmationTextWhenMissing() {
    	RedisConnect rc = mock(RedisConnect.class);
        when(rc.getKey("conf_WelcomeText")).thenReturn("already");
        when(rc.getKey("conf_confirmationText")).thenReturn("");
        when(rc.getKey("conf_pass")).thenReturn("already");

        LifecycleListener sut = new LifecycleListener(rc,  "secret");

        sut.onApplicationReady(null);

        verify(rc).storeKey("conf_confirmationText", "<h1>PsycWall</h1>login success");
        verify(rc, never()).storeKey(eq("conf_pass"), anyString());
    }

    @Test
    void throwsWhenPasswordMissingAndStartPassNotSet() {
    	RedisConnect rc = mock(RedisConnect.class);
        when(rc.getKey("conf_WelcomeText")).thenReturn("already");
        when(rc.getKey("conf_confirmationText")).thenReturn("already");
        when(rc.getKey("conf_pass")).thenReturn(null);

        LifecycleListener sut = new LifecycleListener(rc, "");

        assertThatThrownBy(() -> sut.onApplicationReady(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("STARTPASS in .env not set. ");

        verify(rc, never()).storeKey(eq("conf_pass"), anyString());
       
    }

    
    @Test
    void throwsWhenPasswordMissingAndStartPassSetSecret() {
    	RedisConnect rc = mock(RedisConnect.class);
        when(rc.getKey("conf_WelcomeText")).thenReturn("already");
        when(rc.getKey("conf_confirmationText")).thenReturn("already");
        when(rc.getKey("conf_pass")).thenReturn(null);

        LifecycleListener sut = new LifecycleListener(rc, "secret");

        assertThatThrownBy(() -> sut.onApplicationReady(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("STARTPASS in .env not set. ");

        verify(rc, never()).storeKey(eq("conf_pass"), anyString());
       
    }

    
    @Test
    void storesHashedPasswordWhenMissingAndStartPassSet() {
    	RedisConnect rc = mock(RedisConnect.class);
        when(rc.getKey("conf_WelcomeText")).thenReturn("already");
        when(rc.getKey("conf_confirmationText")).thenReturn("already");
        when(rc.getKey("conf_pass")).thenReturn("");

        LifecycleListener sut = new LifecycleListener(rc, "real_secret");

        sut.onApplicationReady(null);

        // Passwort gesetzt
        ArgumentCaptor<String> passCaptor = ArgumentCaptor.forClass(String.class);
        verify(rc).storeKey(eq("conf_pass"), passCaptor.capture());
        assertThat(passCaptor.getValue()).isEqualTo(
        		"af05bc884941536df15290c650b7b4a8065eaf5f43f2c4e906513a0c641d05ef"); 
        verify(rc, never()).storeKey(eq("conf_confirmationText"), anyString());
        verify(rc, never()).storeKey(eq("conf_WelcomeText"), anyString());
        
    }
  
  

    @Test
    void doesNotOverrideExistingValues() {
    	RedisConnect rc = mock(RedisConnect.class);
        when(rc.getKey("conf_WelcomeText")).thenReturn("custom");
        when(rc.getKey("conf_confirmationText")).thenReturn("custom");
        when(rc.getKey("conf_pass")).thenReturn("custom");

        LifecycleListener sut = new LifecycleListener(rc, "secret");

        sut.onApplicationReady(null);

        verify(rc, never()).storeKey(eq("conf_WelcomeText"), anyString());
        verify(rc, never()).storeKey(eq("conf_confirmationText"), anyString());
        verify(rc, never()).storeKey(eq("conf_pass"), anyString());
    }
}
