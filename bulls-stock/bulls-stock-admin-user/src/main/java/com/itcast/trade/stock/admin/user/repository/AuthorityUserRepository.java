package com.itcast.trade.stock.admin.user.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import com.itcast.bulls.stock.entity.user.AuthorityUser;

/**
 * <p>Description: </p>
 * @date 
 * @author 
 * @version 1.0
 * <p>Copyright:Copyright(c)2020</p>
 */
@RepositoryRestResource(path = "/authorityUser")
public interface AuthorityUserRepository extends PagingAndSortingRepository<AuthorityUser, Long>, JpaSpecificationExecutor {

    /**
     * 根据账号修改用户密码
     * @param account
     * @param password
     * @return
     */
    @Transactional
    @Modifying
    @Query("update AuthorityUser usr set usr.userPassword = :password where usr.userAccount = :account")
    int updateUserPwdByAccount(@Param("account")String account, @Param("password") String password);
}
