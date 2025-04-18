package com.itcast.bulls.stock.proxy.processor;

import com.itcast.bull.stock.common.service.lock.RedisLockService;
import com.itcast.bulls.stock.common.utils.GlobalConstants;
import com.itcast.bulls.stock.proxy.netty.StockProxyServerHandler;
import com.itcast.bulls.stock.struct.grpc.trade.LoginReponse;
import com.itcast.bulls.stock.struct.grpc.trade.LoginRequest;
import com.itcast.bulls.stock.struct.grpc.trade.StockUserServiceGrpc;
import com.itcast.bulls.stock.struct.netty.trade.LoginData;
import com.itcast.bulls.stock.struct.netty.trade.ResponseMessage;
import com.itcast.bulls.stock.struct.netty.trade.StockHeadProto;
import com.itcast.bulls.stock.struct.netty.trade.StockMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description: </p>
 * @date 2020/1/22
 * @author 贺锟 
 * @version 1.0
 * @name  Mirson
 * <p>Copyright:Copyright(c)2019</p>
 */
@Service
@Log4j2
public class StockUserProcessor extends AbstractStockProcessor implements IBaseStockProcessor {


    @GrpcClient("grpc-trade-server")
    private StockUserServiceGrpc.StockUserServiceBlockingStub stockUserServiceStub;

    @Autowired
    private RedisLockService redisLockService;

    /**
     * 处理客户端请求
     * @param channelHandlerContext
     * @param stockMessage
     */
    @Override
    public void processRequest(ChannelHandlerContext channelHandlerContext, StockMessage stockMessage) {

        ResponseMessage responseMessage = null;
        StockHeadProto.StockHead stockHead = null;
        try {

            // 1. 转换组装请求参数
            LoginData loginData = stockMessage.getLoginData();
            LoginRequest loginRequest = LoginRequest.newBuilder().setUserNo(loginData.getUserNo()).setUserPwd(loginData.getUserPwd()).build();

            // 2. 调用远程gRpc登陆接口
            LoginReponse reponse = stockUserServiceStub.userLogin(loginRequest);
            if (GlobalConstants.STATUS_OK.equals(reponse.getStatus())) {
                // 3. 缓存处理
                recordCache(channelHandlerContext.channel().id(), reponse);

                // 记录Channel
                StockProxyServerHandler.channels.add(channelHandlerContext.channel());

                // 返回成功报文
                stockHead = generateStockHead(stockMessage.getStockHead(), GlobalConstants.STATUS_OK);
            } else {
                // 返回失败报文
                stockHead = generateStockHead(stockMessage.getStockHead(), GlobalConstants.STATUS_ERROR);

            }
            responseMessage = ResponseMessage.newBuilder().setStockHead(stockHead).setMessage(reponse.getMessage()).build();

        } catch (Exception e) {
            // 系统异常返回
            log.error(e.getMessage(), e);
            stockHead = generateStockHead(stockMessage.getStockHead(), GlobalConstants.STATUS_ERROR);
            responseMessage = ResponseMessage.newBuilder().setStockHead(stockHead).setMessage(e.getMessage()).build();
        }

        channelHandlerContext.writeAndFlush(responseMessage);
        log.info("Process in StockUserProcessor.userLogin write : " + responseMessage);

    }


    /**
     * 记录缓存
     */
    private void recordCache(ChannelId channelId, LoginReponse reponse) {
        // 记录通道ID与账户ID的关联缓存
        Cache cache = cacheManager.getCache(GlobalConstants.STOCK_PROXY_USER_KEY);
        cache.put(channelId.asLongText(), reponse.getAccountId());

        // 记录在线用户缓存
        Cache.ValueWrapper onlineWrapper = cache.get(GlobalConstants.STOCK_PROXY_USER_KEY_ONLINE);
        // 分布式锁， 防止用户同时登陆，MAP信息记录不一致
        try {
            Map<Long, ChannelId> accountInfos = null;
            redisLockService.tryLock(GlobalConstants.STOCK_PROXY_USER_KEY);
            if (null != onlineWrapper) {
                // 更新
                accountInfos = (Map<Long, ChannelId>) onlineWrapper.get();
            }else {
                // 新增
                accountInfos = new HashMap<Long, ChannelId>();
            }
            accountInfos.put(reponse.getAccountId(), channelId);
            // 更新缓存
            cache.put(GlobalConstants.STOCK_PROXY_USER_KEY_ONLINE, accountInfos);
        }catch(Exception e) {
            log.error(e.getMessage(), e);
        }finally {
            // 解锁处理
            redisLockService.unlock(GlobalConstants.STOCK_PROXY_USER_KEY);
        }

    }

}
