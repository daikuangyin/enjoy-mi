package cn.enjoy.config;


import cn.enjoy.util.ShiroCacheUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.shiro.web.filter.authc.PassThruAuthenticationFilter;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;


public class ShiroAuthFilter extends PassThruAuthenticationFilter {

    @Autowired
    private ShiroCacheUtil shiroCacheUtil;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 这是没权限的路径，不是没登录的
     */
    private String unauthorizedUrl  = "/api/system/accessDenied";
    private Set<String> ignoreSaveRequestUrl;//在xml文件中加载不需要保存的url链接

    public Set<String> getIgnoreSaveRequestUrl() {
        return ignoreSaveRequestUrl;
    }

    public void setIgnoreSaveRequestUrl(Set<String> ignoreSaveRequestUrl) {
        this.ignoreSaveRequestUrl = ignoreSaveRequestUrl;
    }

    public String getUnauthorizedUrl() {
        return unauthorizedUrl;
    }

    public void setUnauthorizedUrl(String unauthorizedUrl) {
        this.unauthorizedUrl = unauthorizedUrl;
    }

    @Override
    public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest req = (HttpServletRequest) request;
        String authorization = req.getHeader("Authorization");
        if (authorization == null || "".equals(authorization)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("respCode", 9);
            jsonObject.put("respMsg", "Authorization not exsit");
            returnJson((HttpServletResponse) response, jsonObject.toJSONString());
            return false;
        }
        //token过期了
        if (!shiroCacheUtil.checkAccessToken(authorization)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("respCode", 8);
            jsonObject.put("respMsg", "token is expire");
            returnJson((HttpServletResponse) response, jsonObject.toJSONString());
            return false;
        }
        return true;
    }

    private void returnJson(HttpServletResponse response, String json) {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
//        response.setContentType("application/json; charset=utf-8");
//        response.setHeader("Access-Control-Allow-Origin", "*");
//        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
//        response.setHeader("Access-Control-Max-Age", "3600");
//        response.setHeader("Access-Control-Allow-Headers", "x-requested-with");
        try {
            writer = response.getWriter();
            writer.print(json);
        } catch (IOException e) {

        } finally {
            if (writer != null)
                writer.close();
        }
    }

    //处理被阻止的请求
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        return false;
    }

    //重写saveRequest,在配置文件中配置的地址不需要保存。实现登录后直接跳转到之前输入的地址。
    @Override
    protected void saveRequest(ServletRequest request) {
        String reqUrl =((ShiroHttpServletRequest)request).getRequestURI();
        if(reqUrl.indexOf("/") > 1) {
            String url = reqUrl.substring(reqUrl.indexOf("/", 1));
            if (!ignoreSaveRequestUrl.contains(url)) {
                WebUtils.saveRequest(request);
            }
        }
    }

   
}

