package com.itcast.trade.stock.admin.user.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.itcast.bulls.stock.common.encrypt.EncryptUtil;
import com.itcast.bulls.stock.common.exception.ComponentException;
import com.itcast.bulls.stock.common.exception.constants.ApplicationErrorCodeEnum;
import com.itcast.bulls.stock.common.utils.GlobalConstants;
import com.itcast.bulls.stock.entity.user.AuthorityMenuOperation;
import com.itcast.bulls.stock.entity.user.AuthorityUser;
import com.itcast.bulls.stock.entity.user.AuthorityUserRole;
import com.itcast.trade.stock.admin.user.repository.AuthorityMenuOperationRepository;
import com.itcast.trade.stock.admin.user.repository.AuthorityMenuRepository;
import com.itcast.trade.stock.admin.user.repository.AuthorityUserRepository;
import com.itcast.trade.stock.admin.user.repository.AuthorityUserRoleRepository;
import com.itcast.trade.stock.admin.user.service.IAuthorityUserService;

/**
 * <p>Description: </p>
 * @date  
 * @author   
 * @version 1.0
 * @name   
 * <p>Copyright:Copyright(c)2020</p>
 */
@Service
public class AuthorityUserServiceImpl implements IAuthorityUserService {

    /**
     * 后台管理用户数据层接口
     */
    @Autowired
    private AuthorityUserRepository authorityUserRepository;

    /**
     * 管理菜单的数据层接口
     */
    @Autowired
    private AuthorityMenuRepository authorityMenuRepository;

    /**
     * 管理菜单操作的数据层接口
     */
    @Autowired
    private AuthorityMenuOperationRepository authorityMenuOperationRepository;

    /**
     * 用户与角色关联的数据层接口
     */
    @Autowired
    private AuthorityUserRoleRepository authorityUserRoleRepository;

    /**
     * Spring Cache缓存
     */
    @Autowired
    private CacheManager cacheManager;

    /**
     * 新增用户接口
     * @param user
     * @param loginUser
     * @throws ComponentException
     */
    @Override
    public void addUser(AuthorityUser user, AuthorityUser loginUser) throws ComponentException {

        //1. 根据用户账号查找对应的用户信息， 校验用户账号是否已存在
        Optional<AuthorityUser> authorityUserOptional = authorityUserRepository.findOne(eqUserName(user.getUserAccount()));
        if(authorityUserOptional.isPresent()) {
            throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_ACCOUNT_EXISTS);
        }
        // 2. 对新增的后台管理用户，密码进行加密处理
        user.setUserPassword(EncryptUtil.encryptSigned(user.getUserPassword()));
        // 3. 组装和冗余后台管理用户信息
        Date curDate = new Date();
        user.setInstitutionTypeId(loginUser.getInstitutionTypeId());
        user.setInstitutionId(loginUser.getInstitutionId());
        user.setCreatorUserAccount(loginUser.getUserAccount());
        user.setCreateTime(curDate);
        user.setLastLoginTime(curDate);
        user.setStatus(GlobalConstants.INT_YES);
        user.setAuthorityUserRoleVos(null);
        // 4. 保存后台管理用户的数据
        authorityUserRepository.save(user);

    }

    /**
     * 根据账号获取用户对象
     * @param userAccount
     * @return
     */
    private Specification eqUserName(String userAccount) {
        return new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.equal(root.get("userAccount"), userAccount);
            }
        };
    }


    /**
     * 用户登陆接口处理逻辑
     * @param username
     * @param password
     * @param clientIp
     * @throws ComponentException
     */
    @Override
    public AuthorityUser userLogin(String userAccount, String password, String clientIp) throws ComponentException{

        // 1. 查找用户, 判断是否存在,  如果不存在, 则抛出异常
        Optional<AuthorityUser> authorityUserOpt = authorityUserRepository.findOne(eqUserName(userAccount));
        if(!authorityUserOpt.isPresent()) {
            throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_ACCOUNT_NOT_FOUND);
        }

        // 2. 对用户登录密码做校验判断
        AuthorityUser authorityUser = authorityUserOpt.get();
        String encryptPassword = EncryptUtil.encryptSigned(password);
        if(!authorityUser.getUserPassword().equals(encryptPassword)) {
            throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_ACCOUNT_PWD_ERROR);
        }

        // 3. 记录用户的登录信息
        authorityUser.setLastLoginTime(new Date());
        authorityUser.setLastLoginIp(clientIp);
        authorityUserRepository.save(authorityUser);

        // 4. 获取用户的角色权限, 并且拿取最大的角色权限信息
        List<AuthorityUserRole> roleList = authorityUserRoleRepository.findAllByUserId(authorityUser.getId());
        if(null == roleList || roleList.size()< 1) {
            throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_NOT_ASSIGN_ROLES);
        }
        Long roleId = getPriorityAuthorityUserRole(roleList).getRoleId();
        authorityUser.setRoleId(roleId);

        // 5. 获取用户的角色菜单操作权限信息, 存入缓存, 用于权限控制处理
        List<AuthorityMenuOperation> optList = authorityMenuOperationRepository.findByRoleId(roleId);
        if(null != optList) {
            // 根据接口的请求类型, 做分组汇总处理, 转换为MAP结构, 格式: POST-{/add, /update}
            Map<String, Set<String>> optUrlSets = optList.stream().collect(Collectors.groupingBy(AuthorityMenuOperation::getOperationIco,
                    Collectors.mapping(AuthorityMenuOperation::getOperationUri, Collectors.toSet())));
            if(optUrlSets.size() > 0) {
                // 将MAP的菜单操作信息存入至Redis缓存当中, 格式: key=userId + | + optType, value=Set<uri>...
                Cache cache = cacheManager.getCache(GlobalConstants.AUTHORITY_MENU_KEY);
                optUrlSets.forEach((opt, urls) -> {
                    // 对uri集合做遍历处理, 将权限菜单信息按指定格式存入至Redis当中
                    // 注意: set里面的数据, 是可以支持多个uri记录, 需拆分处理转换至Set集合当中
                    Set<String> levelUrls = urls.stream().flatMap(str ->
                            Arrays.stream(str.split(",")).map(String::trim)).collect(Collectors.toSet());
                    cache.put(authorityUser.getId() + GlobalConstants.SPLIT + opt, levelUrls);
                });
            }
        }
        return authorityUser;
    }

    /**
     * 获取最大的权限角色信息
     * @param roleList
     * @return
     */
    private AuthorityUserRole getPriorityAuthorityUserRole(List<AuthorityUserRole> roleList) {
        // 1. 将平台角色权限按优先级排列, 转换成为集合, 最大权限的角色放在前面
        List<String> institutionTypes = Arrays.asList(GlobalConstants.INSTITUTION_TYPE_GROUPS, GlobalConstants.INSTITUTION_TYPE_SERVICE_OPERATE, GlobalConstants.INSTITUTION_TYPE_COMPANY);

        // 2. 将用户的角色权限信息做遍历匹配处理
        for(String institutionType : institutionTypes) {
            Optional<AuthorityUserRole> userRoleOptional = roleList.stream().filter(
                    item -> item.getInstitutionTypeId().equals(institutionType)).findFirst();
            if(userRoleOptional.isPresent()) {
                return userRoleOptional.get();
            }
        }
        return roleList.get(0);
    }


    /**
     * 重置用户密码接口, 新密码为"账号+123"
     * @param newPwd
     * @param userAccount
     * @param loginUser
     * @throws ComponentException
     */
    @Override
    public void resetPwd(String userAccount, AuthorityUser loginUser) throws ComponentException {

        // 查找用户
        Optional<AuthorityUser> authorityUserOpt = authorityUserRepository.findOne(eqUserName(userAccount));
        if(!authorityUserOpt.isPresent()){
            throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_ACCOUNT_NOT_FOUND);
        }

        // 非管理员用户， 不能进行此操作
        if(!GlobalConstants.STR_YES.equals(String.valueOf(loginUser.getIsAdmin()))) {
            throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_NOT_ADMIN);
        }

        // 生成新的加密密码
        String newPwd = EncryptUtil.encryptSigned(userAccount + "123");
        // 更新用户密码
        authorityUserRepository.updateUserPwdByAccount(newPwd, userAccount);

    }
}
