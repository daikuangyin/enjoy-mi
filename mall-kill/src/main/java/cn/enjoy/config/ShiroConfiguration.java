package cn.enjoy.config;

import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author cl
 */
//@Configuration
public class ShiroConfiguration {

    @Value("${shiro.maxAge.day}")
    private Integer maxAgeDay =  10;
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.password}")
    private String password;


    /**
     * 配置shiro redisManager
     * <p>
     * 使用的是shiro-redis开源插件
     *
     * @return
     */
    @Bean
    public RedisManager redisManager() {
        RedisManager redisManager = new RedisManager();
        redisManager.setHost(host);
        redisManager.setPort(port);
        redisManager.setExpire(7200);// 配置缓存过期时间
        redisManager.setPassword(password);
        return redisManager;
    }

    /**
     * cacheManager 缓存 redis实现
     * <p>
     * 使用的是shiro-redis开源插件
     *
     * @return
     */
    @Bean
    public RedisCacheManager cacheManager() {
        RedisCacheManager redisCacheManager = new RedisCacheManager();
        redisCacheManager.setRedisManager(redisManager());
        return redisCacheManager;
    }


    @Bean(name = "securityManager")
    public WebSecurityManager webSecurityManager() {
        DefaultWebSecurityManager dwsm = new DefaultWebSecurityManager();
        dwsm.setCacheManager(cacheManager());
        return dwsm;
    }

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean getShiroFilterFactoryBean() {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(webSecurityManager());
//        shiroFilterFactoryBean.setLoginUrl("/api/system/unLogin");
//        shiroFilterFactoryBean.setSuccessUrl("/api/system/logined");
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();
        filterChainDefinitionMap.put("/static/**", "anon");
        filterChainDefinitionMap.put("/api/system/getFileServerUrl", "anon");
        filterChainDefinitionMap.put("/api/system/login", "anon");
        filterChainDefinitionMap.put("/api/system/unLogin", "anon");
        filterChainDefinitionMap.put("/api/system/accessDenied", "anon");
        filterChainDefinitionMap.put("/api/sys/**", "anon");
        filterChainDefinitionMap.put("/api/user/register", "anon");
        filterChainDefinitionMap.put("/api/wx/bind", "anon");
        filterChainDefinitionMap.put("/api/home/**", "anon");
        filterChainDefinitionMap.put("/api/authorize", "anon");
        filterChainDefinitionMap.put("/api/accessToken", "anon");
        filterChainDefinitionMap.put("/api/userInfo", "anon");
        filterChainDefinitionMap.put("/api/user/register", "anon");

        filterChainDefinitionMap.put("/wx/login", "anon");
        filterChainDefinitionMap.put("/api/system/logout", "anon");
        filterChainDefinitionMap.put("/api/logout", "anon");
        //配置记住我过滤器或认证通过可以访问的地址(当上次登录时，记住我以后，在下次访问/或/index时，可以直接访问，不需要登陆)
        filterChainDefinitionMap.put("/", "anon");
        filterChainDefinitionMap.put("/**.js", "anon");
        filterChainDefinitionMap.put("/**.css", "anon");
        filterChainDefinitionMap.put("/**.html", "anon");
      //  filterChainDefinitionMap.put("/**", "user");
        filterChainDefinitionMap.put("/kill/**", "accessPerms");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        Map<String,Filter> filterMap = new HashMap<>();
        AuthenticationFilter shiroAuthFilter2 = getShiroAuthFilter();
        filterMap.put("accessPerms", shiroAuthFilter2);
        shiroFilterFactoryBean.setFilters(filterMap);
        //shiroFilterFactoryBean.setUnauthorizedUrl("/api/system/unLogin");
        return shiroFilterFactoryBean;
    }

    @Bean
    @Scope("prototype")
    public AuthenticationFilter getShiroAuthFilter() {
        return new ShiroAuthFilter();
    }
}