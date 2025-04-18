package com.itcast.trade.stock.admin.user.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import com.itcast.bulls.stock.common.exception.constants.ApplicationErrorCodeEnum;
import com.itcast.bulls.stock.common.utils.GlobalConstants;
import com.itcast.bulls.stock.entity.user.AuthorityUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.alibaba.fastjson.JSONObject;
import com.itcast.stock.common.web.vo.ApiRespResult;

import lombok.extern.log4j.Log4j2;

/**
 * <p>Description: </p>
 * @date  
 * @author   
 * @version 1.0
 * @name   
 * <p>Copyright:Copyright(c)2020</p>
 */
@WebFilter(urlPatterns = "/*")
@Log4j2
public class GlobalSecurityFilter implements Filter {

    private static final Set<String> AUTH_PATHS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("^/authority.*", "/auth/.*")));

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化
    }

    /**
     * 权限过滤控制处理
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        // 1. 获取请求的相对路径, 同时去除contextPath
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String uri = getActRequestUri(request);

        // 2. 针对需要权限判断的数据做拦截处理, 提升处理性能
        if(!isNeedAuth(uri)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 3. 权限控制判断, 如果是系统忽略的菜单(白名单), 则放行处理
        Cache cache = cacheManager.getCache(GlobalConstants.AUTHORITY_MENU_KEY);
        if(null != cache){
            // 公开放行的菜单权限处理(支持正则表达式)
            Set<String> noLimitUrls = (Set<String>)cache.get(GlobalConstants.AUTHORITY_MENU_NO_LIMIT_KEY).get();
            if(noLimitUrls.contains(uri)) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }else {
                // 优先进入全路径匹配, 匹配不成功, 则进入正则匹配
                for(String noLimitUrl : noLimitUrls) {
                    if(Pattern.compile(noLimitUrl).matcher(uri).matches()) {
                        filterChain.doFilter(servletRequest, servletResponse);
                        return;
                    }
                }
            }
        }

        // 4. 校验用户所分配的权限菜单, 首先判断用户是否登录
        AuthorityUser authorityUser = (AuthorityUser)request.getSession().getAttribute(request.getSession().getId());
        if(null == authorityUser) {
            // 判断用户是否已登录, 如果没有登录, 则抛出异常提示信息
            ApiRespResult respResult = ApiRespResult.validError(ApplicationErrorCodeEnum.ADMIN_USER_NEED_LOGIN.getMessage());
            returnJson(servletResponse, respResult);
            return;
        }

        // 5. 获取用户登录的操作菜单权限缓存数据
        String userOptKey = authorityUser.getId() + GlobalConstants.SPLIT + request.getMethod().toUpperCase();
        Cache.ValueWrapper userUriWrapper = cache.get(userOptKey);
        if(null == userUriWrapper) {
            ApiRespResult respResult = ApiRespResult.validError(ApplicationErrorCodeEnum.SYS_NOT_VALID_REQUEST.getMessage());
            returnJson(servletResponse, respResult);
            return ;
        }

        // 6. 匹配校验用户的菜单操作权限信息
        Set<String> userUris = (Set<String>)cache.get(userOptKey).get();
        if(userOptKey.contains(uri)) {
            // 首先, 进行完全匹配
            filterChain.doFilter(servletRequest, servletResponse);
            return ;
        }else {
            // 如果完全匹配不成功, 则进入模糊匹配处理
            for(String userUri: userUris) {
                if(Pattern.compile(userUri).matcher(uri).matches()){
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }
        }
        // 7. 如果经过以上逻辑的处理, 权限都不符合校验规则, 则提示异常
        ApiRespResult respResult = ApiRespResult.validError(ApplicationErrorCodeEnum.SYS_NOT_VALID_REQUEST.getMessage());
        returnJson(servletResponse, respResult);
        return;

    }

    /**
     * 获取去除contextPath的请求路径URI
     *
     * @param request
     * @return
     */
    private String getActRequestUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isEmpty(contextPath) || contextPath.equals("/")) {
            return uri;
        }
        return uri.substring(contextPath.length());
    }

    /**
     * 需要权限认证的URI路径判断
     * @param uri
     * @return
     */
    private boolean isNeedAuth(String uri) {

        for(String authPath : AUTH_PATHS) {
            if(Pattern.compile(authPath).matcher(uri).matches()) {
                return true;
            };
        }
        return false;
    }


    @Override
    public void destroy() {
        // 销毁处理
    }

    /**
     * 返回统一封装的JSON数据
     * @param response
     * @param apiRespResult
     * @throws Exception
     */
    private void returnJson(ServletResponse response, ApiRespResult apiRespResult) {
        PrintWriter writer = null;
        try {
            String json = JSONObject.toJSONString(apiRespResult);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            writer = response.getWriter();
            writer.print(json);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (writer != null){
                try {
                    writer.close();
                }catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

        }
    }

}

