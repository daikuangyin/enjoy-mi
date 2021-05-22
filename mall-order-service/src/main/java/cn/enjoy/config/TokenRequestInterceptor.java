package cn.enjoy.config;

import cn.enjoy.core.utils.CommonConstant;
import cn.enjoy.core.utils.StringUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @Classname TokenRequestInterceptor
 * @Description TODO
 * @Author Jack
 * Date 2020/10/28 15:21
 * Version 1.0
 */
@Configuration
public class TokenRequestInterceptor implements RequestInterceptor {
    public HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public String getToken() {
        HttpSession session = getRequest().getSession();
        return (String) session.getAttribute(CommonConstant.SESSION_ACCESS_TOKEN);
    }

    @Override
    public void apply(RequestTemplate template) {
        String token = getRequest().getHeader("Authorization");
        if(StringUtil.isNotEmpty(token)) {
            template.header("Authorization", token);
        }
    }
}
