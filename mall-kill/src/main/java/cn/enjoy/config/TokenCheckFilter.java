package cn.enjoy.config;

import cn.enjoy.util.ShiroCacheUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @Classname TokenCheckFilter
 * @Description TODO
 * @Author Jack
 * Date 2020/10/28 16:00
 * Version 1.0
 */
//@Component
public class TokenCheckFilter implements Filter {

    @Autowired
    private ShiroCacheUtil shiroCacheUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String authorization = req.getHeader("Authorization");
        if (authorization == null || "".equals(authorization)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("respCode", 9);
            jsonObject.put("respMsg", "Authorization not exsit");
            returnJson((HttpServletResponse) response, jsonObject.toJSONString());
            return;
        }
        //token过期了
        if (!shiroCacheUtil.checkAccessToken(authorization)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("respCode", 8);
            jsonObject.put("respMsg", "token is expire");
            returnJson((HttpServletResponse) response, jsonObject.toJSONString());
            return;
        }
        chain.doFilter(request, response);
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
}
