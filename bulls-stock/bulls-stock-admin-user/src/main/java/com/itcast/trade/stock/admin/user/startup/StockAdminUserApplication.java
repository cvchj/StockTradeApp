package com.itcast.trade.stock.admin.user.startup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration;

/**
 * <p>Description: </p>
 * @date 
 * @author 
 * @version 1.0
 * <p>Copyright:Copyright(c)2020</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.itcast"})
@Import({SpringDataRestConfiguration.class})
@EntityScan(basePackages = {"com.itcast"})
@EnableJpaRepositories(basePackages =  {"com.itcast"})
@EnableTransactionManagement
@EnableRedisHttpSession
@EnableCaching
@ServletComponentScan(basePackages = {"com.itcast"})
public class StockAdminUserApplication {

    public static void main(String[] args) {

        SpringApplication.run(StockAdminUserApplication.class, args);
    }
}
