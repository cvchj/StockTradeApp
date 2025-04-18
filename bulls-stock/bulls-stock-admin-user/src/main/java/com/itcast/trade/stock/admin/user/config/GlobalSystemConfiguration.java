package com.itcast.trade.stock.admin.user.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;

import com.itcast.bulls.stock.common.utils.GlobalConstants;
import com.itcast.bulls.stock.entity.user.AuthorityNoLimitUri;
import com.itcast.trade.stock.admin.user.repository.AuthorityNoLimitUriRepository;

import lombok.extern.log4j.Log4j2;

/**
 * <p>Description: </p>
 * @date  
 * @author   
 * @version 1.0
 * @name   
 * <p>Copyright:Copyright(c)2020</p>
 */
@Configuration
@Log4j2
public class GlobalSystemConfiguration implements ApplicationRunner {

    /**
     * 缓存的接口管理器
     */
    @Autowired
    private CacheManager cacheManager;

    /**
     * 公开菜单的数据层接口
     */
    @Autowired
    private AuthorityNoLimitUriRepository authorityNoLimitUriRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Process in GlobalSystemConfiguration.run method.");
        // 加载公开的菜单数据信息至缓存当中
        Cache cache = cacheManager.getCache(GlobalConstants.AUTHORITY_MENU_KEY);
        List<AuthorityNoLimitUri> nolimitMenuList = authorityNoLimitUriRepository.findAll();
        Set<String> noLimitSet = nolimitMenuList.stream().map(noLimit -> noLimit.getNoLimitUri()).collect(Collectors.toSet());
        cache.put(GlobalConstants.AUTHORITY_MENU_NO_LIMIT_KEY, noLimitSet);

    }
}
