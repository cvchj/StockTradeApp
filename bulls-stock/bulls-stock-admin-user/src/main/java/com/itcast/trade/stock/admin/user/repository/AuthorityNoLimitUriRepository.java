package com.itcast.trade.stock.admin.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.itcast.bulls.stock.entity.user.AuthorityNoLimitUri;

/**
 * <p>Description: </p>
 * @date 
 * @author 
 * @version 1.0
 * <p>Copyright:Copyright(c)2020</p>
 */
@Repository
public interface AuthorityNoLimitUriRepository extends PagingAndSortingRepository<AuthorityNoLimitUri, Long>, JpaSpecificationExecutor {

    /**
     * 获取所有的公开菜单数据信息
     * @return
     */
    List<AuthorityNoLimitUri> findAll();

}
