package cn.enjoy.config;

import cn.enjoy.sys.security.ShiroAuthFilter;
import cn.enjoy.sys.security.ShiroRealm;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author cl
 */
@Configuration
public class ShiroConfiguration {

    private static Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();


    @Value("${shiro.maxAge.day}")
    private Integer maxAgeDay =  10;

    @Bean(name = "ShiroRealmImpl")
    public ShiroRealm getShiroRealm() {
        return new ShiroRealm();
    }



    @Bean(name = "securityManager")
    public DefaultWebSecurityManager getDefaultWebSecurityManager() {
        DefaultWebSecurityManager dwsm = new DefaultWebSecurityManager();
        dwsm.setRealm(getShiroRealm());
        dwsm.setRememberMeManager(rememberMeManager());
        return dwsm;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor getAuthorizationAttributeSourceAdvisor() {
        AuthorizationAttributeSourceAdvisor aasa = new AuthorizationAttributeSourceAdvisor();
        aasa.setSecurityManager(getDefaultWebSecurityManager());
        return new AuthorizationAttributeSourceAdvisor();
    }

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean getShiroFilterFactoryBean() {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(getDefaultWebSecurityManager());
        shiroFilterFactoryBean.setLoginUrl("/api/system/unLogin");
        shiroFilterFactoryBean.setSuccessUrl("/api/system/logined");

        filterChainDefinitionMap.put("/static/**", "anon");
        filterChainDefinitionMap.put("/api/system/getFileServerUrl", "anon");
        filterChainDefinitionMap.put("/api/system/login", "anon");
        filterChainDefinitionMap.put("/api/system/unLogin", "anon");
        filterChainDefinitionMap.put("/api/system/accessDenied", "anon");
        filterChainDefinitionMap.put("/api/sys/config", "anon");
        filterChainDefinitionMap.put("/api/user/register", "anon");
        filterChainDefinitionMap.put("/api/wx/bind", "anon");
        filterChainDefinitionMap.put("/api/home/**", "anon");
        filterChainDefinitionMap.put("/wx/login", "anon");
        filterChainDefinitionMap.put("/api/system/logout", "anon");
        filterChainDefinitionMap.put("/api/logout", "logout");
        //????????????????????????????????????????????????????????????(??????????????????????????????????????????????????????/???/index??????????????????????????????????????????)
        filterChainDefinitionMap.put("/", "anon");
        filterChainDefinitionMap.put("/**.js", "anon");
        filterChainDefinitionMap.put("/**.css", "anon");
        filterChainDefinitionMap.put("/**.html", "anon");
      //  filterChainDefinitionMap.put("/**", "user");
        filterChainDefinitionMap.put("/api/**", "accessPerms");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        Map<String,Filter> filterMap = new HashMap<>();
        ShiroAuthFilter shiroAuthFilter2 = getShiroAuthFilter();
        filterMap.put("accessPerms", shiroAuthFilter2);
        shiroFilterFactoryBean.setFilters(filterMap);
        //shiroFilterFactoryBean.setUnauthorizedUrl("/api/system/unLogin");
        return shiroFilterFactoryBean;
    }

    @Bean
    @Scope("prototype")
    public ShiroAuthFilter getShiroAuthFilter() {
        return  new ShiroAuthFilter();
    }


    @Bean
    public CookieRememberMeManager rememberMeManager(){
       // logger.info("??????Shiro????????????(CookieRememberMeManager)?????????-->rememberMeManager", CookieRememberMeManager.class);
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        //rememberme cookie??????????????? ?????????????????????????????? ??????AES?????? ???????????????128 256 512 ???????????????????????????????????????
        //KeyGenerator keygen = KeyGenerator.getInstance("AES");
        //SecretKey deskey = keygen.generateKey();
        //System.out.println(Base64.encodeToString(deskey.getEncoded()));
        byte[] cipherKey = Base64.decode("wGiHplamyXlVB11UXWol8g==");
        cookieRememberMeManager.setCipherKey(cipherKey);
        cookieRememberMeManager.setCookie(rememberMeCookie());
        return cookieRememberMeManager;
    }
    @Bean
    public SimpleCookie rememberMeCookie(){
        //???????????????cookie???????????????????????????checkbox???name = rememberMe
        SimpleCookie simpleCookie = new SimpleCookie("rememberMe");
        //??????httyOnly?????????true????????????????????????????????????????????????????????????HttpOnly cookie??????????????????????????????????????????????????????
        simpleCookie.setHttpOnly(true);
        //?????????cookie????????????,??????30??? ,????????????60 * 60 * 24 * 30
        simpleCookie.setMaxAge(60 * 60 * 24 * maxAgeDay);
        //simpleCookie.setMaxAge(60*1);

        return simpleCookie;
    }

//    @Bean
//    public SecurityManager securityManager() {
//        //logger.info("??????Shiro???Web?????????-->securityManager", ShiroFilterFactoryBean.class);
//        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
//        //??????Realm???????????????????????????
//        securityManager.setRealm(getShiroRealm());
//        //?????????????????????
//        securityManager.setCacheManager(getEhCacheManager());
//        //??????Cookie(?????????)?????????(remenberMeManager)
//        securityManager.setRememberMeManager(rememberMeManager());
//
//        return securityManager;
//    }

}