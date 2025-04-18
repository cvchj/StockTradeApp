package com.itcast.trade.stock.admin.user.service.impl;

import com.itcast.bulls.stock.common.exception.BusinessException;
import com.itcast.bulls.stock.common.exception.ComponentException;
import com.itcast.bulls.stock.common.exception.constants.ApplicationErrorCodeEnum;
import com.itcast.bulls.stock.entity.user.*;
import com.itcast.trade.stock.admin.user.repository.AuthorityRoleMenuRepository;
import com.itcast.trade.stock.admin.user.repository.AuthorityRoleOperationRepository;
import com.itcast.trade.stock.admin.user.repository.AuthorityRoleRepository;
import com.itcast.trade.stock.admin.user.repository.AuthorityUserRoleRepository;
import com.itcast.trade.stock.admin.user.service.IAuthorityLogicService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Description: </p>
 * @date 
 * @author 
 * @version 1.0
 * <p>Copyright:Copyright(c)2020</p>
 */
@Log4j2
@Service
public class AuthorityLogicServiceImpl implements IAuthorityLogicService {

    /**
     * 权限分配-菜单的操作前缀标识
     */
    public final static String MENU_OPT_FLAG = "O_";
    /**
     * 权限分配-菜单标识
     */
    public final static String MENU_FLAG = "M_";
    /**
     * 用户角色的数据层接口
     */
    @Autowired
    private AuthorityRoleRepository authorityRoleRepository;

    /**
     * 角色菜单关联数据层接口
     */
    @Autowired
    private AuthorityRoleMenuRepository authorityRoleMenuRepository;

    /**
     * 角色和菜单操作的关联数据层接口
     */
    @Autowired
    private AuthorityRoleOperationRepository authorityRoleOperationRepository;

    /**
     * 用户与角色的关联数据层接口
     */
    private AuthorityUserRoleRepository authorityUserRoleRepository;


    /**
     * 保存角色指定的权限信息(支持批量方式, 多个角色可以同时保存)
     * @param roleAuth
     * @param roleIds
     * @throws BusinessException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProvileges(String roleAuth, String... roleIds) throws BusinessException {
        log.info("Process in AuthorityLogicServiceImpl.saveProvileges method. params, roleAuth: " + roleAuth
        + ", roleIds: " + roleIds);
        // 1. 保存多个角色的权限信息, 支持批量方式处理(参数由前端校验)
        for(String roleId :  roleIds) {
            // 2. 将roleId信息转换, 校验角色信息是否存在
            Long roleIdLong = Long.valueOf(roleId);
            Optional<AuthorityRole> authorityRole = authorityRoleRepository.findById(roleIdLong);
            if(!authorityRole.isPresent()) {
                throw new BusinessException(ApplicationErrorCodeEnum.USER_ROLE_NOT_FOUND);
            }
            // 3. 保存角色与权限的关联关系
            saveRolePrivileges(roleIdLong, roleAuth);
        }


    }

    /**
     * 保存角色和权限的关联关系
     * @param roleId
     * @param roleAuth
     */
    private void saveRolePrivileges(Long roleId, String roleAuth) {
        log.info("Process in AuthorityLogicServiceImpl.saveRolePrivileges method. roleId: " + roleId);
        // 1. 过滤roleAuth参数的数据, 规范: "M_ID, O_ID" , 以M开头的代表的是菜单信息, 以O开头代表的是操作信息, 通过逗号进行分割
        String[] roleAuthArr = roleAuth.replace("-1", "").trim().split(",");
        // 2. 对权限参数信息做去重处理
        List<String> roleAuthList = Stream.of(roleAuthArr).distinct().collect(Collectors.toList());
        // 3. 先删除角色原有的菜单和操作权限信息
        authorityRoleOperationRepository.deleteAllByRoleId(roleId);
        authorityRoleMenuRepository.deleteAllByRoleId(roleId);

        // 4. 重新进行角色和权限信息的保存, 遍历权限信息, 逐个进行保存处理
        AuthorityRoleOperation authorityRoleOperation;
        AuthorityRoleMenu authorityRoleMenu;
        for(String id: roleAuthList) {
            authorityRoleOperation = new AuthorityRoleOperation();
            authorityRoleMenu  = new AuthorityRoleMenu();
            if(id.contains(MENU_OPT_FLAG)) {
                // 对菜单操作进行保存
                authorityRoleOperation.setOperationId(Long.parseLong(id.replace(MENU_OPT_FLAG, "").trim()));
                authorityRoleOperation.setRoleId(roleId);
                authorityRoleOperationRepository.save(authorityRoleOperation);
            }else if(id.contains(MENU_FLAG)) {
                // 对菜单进行操作保存
                authorityRoleMenu.setMenuId(Long.parseLong(id.replace(MENU_FLAG, "").trim()));
                authorityRoleMenu.setRoleId(roleId);
                authorityRoleMenuRepository.save(authorityRoleMenu);
            }else {
                log.warn("unknown menu or opt ! id: " + id);
            }
        }

    }


    /**
     * 分配用户与角色的关联关系接口
     * @param authorityUserRole
     * @param authorityUser
     * @throws ComponentException
     */
    @Override
    public void userAssignRoles(AuthorityUserRole authorityUserRole, AuthorityUser authorityUser) throws ComponentException{
        log.info("Process in AuthorityLogicServiceImpl.userAssignRoles method. authorityUserRole: " + authorityUserRole);
        // 1. 查找用户已分配的角色信息
        List<AuthorityUserRole> authorityUserRoleList = authorityUserRoleRepository.findAllByUserId(authorityUserRole.getUserId());

        // 2. 获取需要分配的角色详情
        AuthorityRole authorityRole = authorityRoleRepository.findById(authorityUserRole.getRoleId()).get();

        // 3. 校验用户是否存在多个相同机构类型的角色
        for(AuthorityUserRole userRole: authorityUserRoleList) {
            if(userRole.getInstitutionTypeId().equals(authorityRole.getInstitutionTypeId())) {
                // 用户存在相同机构的不同角色, 不能进行保存, 要抛出异常
                throw new ComponentException(ApplicationErrorCodeEnum.ADMIN_USER_INSTITUTION_UNIQUE);
            }
        }

        // 4. 完善用户与角色的关联数据
        authorityUserRole.setRoleName(authorityRole.getRoleName());
        authorityUserRole.setInstitutionTypeId(authorityRole.getInstitutionTypeId());
        authorityUserRole.setInstitutionId(authorityRole.getInstitutionId());
        if(null != authorityRole.getInstitutionVo()) {
            authorityUserRole.setInstitutionName(authorityRole.getInstitutionVo().getDetailInstitutionName());
        }
        authorityUserRole.setCreateBy(authorityUser.getId());
        authorityUserRole.setCreateTime(new Date());
        authorityUserRole.setCreateUser(authorityUser.getUserName());
        authorityUserRole.setLastUpdateBy(authorityUser.getId());
        authorityUserRole.setLastUpdateUser(authorityUser.getUserName());
        authorityUserRole.setLastUpdateTime(new Date());
        // 5. 保存用户角色关联数据
        authorityUserRoleRepository.save(authorityUserRole);


    }


}
