package com.itcast.bulls.stock.starter.swagger;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description: </p>
 * @date 
 * @author 
 * @version 1.0
 * <p>Copyright:Copyright(c)2020</p>
 */
@Data
public class SwaggerProperties {
    /**
     * 是否开启swagger
     */
    @Value("${swagger.enabled}")
    private Boolean enabled;
    /**
     * swagger会解析的包路径
     **/
    private String basePackage = "";
    /**
     * swagger会解析的url规则
     **/
    private List<String> basePath = new ArrayList<>();
    /**
     * 在basePath基础上需要排除的url规则
     **/
    private List<String> excludePath = new ArrayList<>();
    /**
     * 标题
     **/
    @Value("${swagger.title}")
    private String title = "";
    /**
     * 描述
     **/
    @Value("${swagger.description}")
    private String description = "";
    /**
     * 版本
     **/
    @Value("${swagger.version}")
    private String version = "";
    /**
     * 许可证
     **/
    private String license = "";
    /**
     * 许可证URL
     **/
    private String licenseUrl = "";

    /**
     * 联系人
     **/
    @Value("${swagger.contact.name}")
    private String contactName = "";
    /**
     * 联系人url
     **/
    @Value("${swagger.contact.url}")
    private String contactUrl = "";
    /**
     * 联系人email
     **/
    @Value("${swagger.contact.email}")
    private String contactEmail = "";

    /**
     * 服务条款URL
     **/
    @Value("${swagger.termsOfServiceUrl}")
    private String termsOfServiceUrl = "";

    /**
     * host信息
     **/
    private String host = "";

    /**
     * 全局统一鉴权配置
     **/
    private Authorization authorization = new Authorization();


    @Data
    @NoArgsConstructor
    public static class Authorization {

        /**
         * 鉴权策略ID，需要和SecurityReferences ID保持一致
         */
        private String name = "";

        /**
         * 需要开启鉴权URL的正则
         */
        private String authRegex = "^.*$";

        /**
         * 鉴权作用域列表
         */
        private List<AuthorizationScope> authorizationScopeList = new ArrayList<>();

        private List<String> tokenUrlList = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class AuthorizationScope {

        /**
         * 作用域名称
         */
        private String scope = "";

        /**
         * 作用域描述
         */
        private String description = "";

    }
}