package com.itcast.trade.stock.admin.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.itcast.bulls.stock.entity.product.QTradeStock;
import com.itcast.bulls.stock.entity.product.TradeStock;
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
@RepositoryRestResource(path = "/authorityTradeStock")
public interface TradeStockRepository extends QuerydslPredicateExecutor<TradeStock>, QuerydslBinderCustomizer<QTradeStock>, PagingAndSortingRepository<TradeStock, Long>, JpaSpecificationExecutor<TradeStock> {

    @Override
    default public void customize(QuerydslBindings bindings, QTradeStock obj) {

        bindings.bind(obj.name).first(StringExpression::contains); // 股票名称
        bindings.bind(obj.code).first(StringExpression::contains); // 股票代码
    }

    @ApiOperation(value = "根据股票编号获取对象")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "股票编号", name = "code", dataType = "string", required = true)
    })
    TradeStock findByCode(@Param("code") String code);

    @ApiOperation(value = "根据分类编号获取股票数据")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "分类编号", name = "categoryId", dataType = "string", required = true)
    })
    List<TradeStock> findByCategoryId(@Param("categoryId") Long categoryId);
}