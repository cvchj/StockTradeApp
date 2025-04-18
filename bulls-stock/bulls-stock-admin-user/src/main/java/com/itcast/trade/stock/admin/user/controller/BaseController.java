package com.itcast.trade.stock.admin.user.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.itcast.bulls.stock.common.exception.ComponentException;
import com.itcast.bulls.stock.common.exception.constants.ApplicationErrorCodeEnum;
import com.itcast.bulls.stock.entity.user.AuthorityUser;

/**
 * <p>Description: </p>
 * @date 
 * @author 
 * @version 1.0
 * <p>Copyright:Copyright(c)2020</p>
 */
@Log4j2
public class BaseController {

    /**
     * 根据账号获取用户对象
     * @return
     */
    protected AuthorityUser getUser() throws ComponentException {

        HttpSession session = getSession();
        AuthorityUser authorityUser = (AuthorityUser)session.getAttribute(session.getId());
        if(null == authorityUser) {
            // 用户不存在， 抛出异常
            throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_NEED_LOGIN);
        }
        return authorityUser;
    }

    /**
     * 获取session会话
     * @return
     */
    protected HttpSession getSession() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getSession();
    }


    /**
     * 获取客户端真实IP
     * @param request  客户端request
     * @return   客户端IP
     */
    protected static String getClientIP(){

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        // 先从头部获取
        String ip = request.getHeader("x-forwarded-for");
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknow".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 直接获取IP
            ip = request.getRemoteAddr();

            if(ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")){
                //根据网卡获取本机配置的IP地址
                InetAddress inetAddress = null;
                try {
                    inetAddress = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    log.error(e.getMessage(), e);
                }
                ip = inetAddress.getHostAddress();
            }
        }
        //对于通过多个代理的情况，第一个IP为客户端真实的IP地址，多个IP按照','分割
        if(null != ip && ip.length() > 15){
            if(ip.indexOf(",") > 0){
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        return ip;
    }
}
