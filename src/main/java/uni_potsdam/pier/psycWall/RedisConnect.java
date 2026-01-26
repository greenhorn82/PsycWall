package uni_potsdam.pier.psycWall;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisConnect {

    private StringRedisTemplate redisTemplate;
    
    public RedisConnect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String storeKey(String keyname, String keyvalue, int ablauf) {
    	if(ablauf < 0) {
    		redisTemplate.opsForValue().set(keyname, keyvalue);
    	} else {
    		redisTemplate.opsForValue().set(keyname, keyvalue, ablauf, TimeUnit.SECONDS);
    	}
    	
        return "Gespeichert!";
    }
    
    public String storeKey(String keyname, int ablauf) {
    	return storeKey(keyname, "zugelassen", ablauf);
    }
    
    public String storeKey(String keyname, String keyvalue) {
    	return storeKey(keyname, keyvalue, -1);
    }
    
    public String getKey(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Set<String> getKeyList(String key) {
    	return redisTemplate.keys(key);      
    }
    
    public boolean removeKey(String key) {
    	return redisTemplate.delete(key);
    }
}
	