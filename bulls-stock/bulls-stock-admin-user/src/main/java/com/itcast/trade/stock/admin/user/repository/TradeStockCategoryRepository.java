package com.itcast.trade.stock.admin.user.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.itcast.bulls.stock.entity.product.QTradeStockCategory;
import com.itcast.bulls.stock.entity.product.TradeStockCategory;
import com.querydsl.core.types.dsl.StringExpression;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * <p>Description: </p>
 * @date  
 * @author   
 * @version 1.0
 * @name   
 * <p>Copyright:Copyright(c)2020</p>
 */
@RepositoryRestResource(path = "/authorityTradeStockCat")
public interface TradeStockCategoryRepository extends QuerydslPredicateExecutor<TradeStockCategory>, QuerydslBinderCustomizer<QTradeStockCategory>,
        PagingAndSortingRepository<TradeStockCategory, Long>, JpaSpecificationExecutor<TradeStockCategory> {

    @Override
    default public void customize(QuerydslBindings bindings, QTradeStockCategory obj) {

        bindings.bind(obj.name).first(StringExpression::contains); // 模糊查询, 分类名称
        bindings.bind(obj.code).first(StringExpression::contains); // 模糊查询, 分类编号
    }

    @ApiOperation(value = "根据分类编号获取对象")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "分类编号", name = "code", dataType = "string", required = true)
    })
    TradeStockCategory findByCode(@Param("code") String code);
}
