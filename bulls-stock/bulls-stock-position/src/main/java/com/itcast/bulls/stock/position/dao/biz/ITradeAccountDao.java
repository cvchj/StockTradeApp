package com.itcast.bulls.stock.position.dao.biz;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.itcast.bulls.stock.common.utils.GlobalConstants;
import com.itcast.bulls.stock.entity.user.TradeAccount;
import com.itcast.bulls.stock.entity.user.TradeGroup;


/**
 * <p>Description: </p>
 * @date 2019/7/24
 * @author 贺锟 
 * @version 1.0
 * @name  Mirson
 * <p>Copyright:Copyright(c)2019</p>
 */
@Repository
public interface ITradeAccountDao {

    /**
     * 新增用户账号
     * @param record
     * @return
     */
    int insert(TradeAccount record);


    /**
     * 根据用户ID获取账号(多个只取第一个)
     * @param accountNo
     * @return
     */
    TradeAccount getByUserId(Long userId);

    /**
     * 根据ID获取交易账户组
     * @param tradeGroupId
     * @return
     */
    @Cacheable(value= GlobalConstants.STOCK_TRADE_GROUP_ACCOUNT_KEY)
    TradeGroup getTradeGroupByAccountId(Long accountId);

}
