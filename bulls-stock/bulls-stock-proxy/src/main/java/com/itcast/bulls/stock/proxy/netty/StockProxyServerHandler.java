package com.itcast.bulls.stock.proxy.netty;

import com.itcast.bulls.stock.common.exception.ComponentException;
import com.itcast.bulls.stock.common.exception.constants.ApplicationErrorCodeEnum;
import com.itcast.bulls.stock.common.utils.GlobalConstants;
import com.itcast.bulls.stock.proxy.processor.StockProcessorFactory;
import com.itcast.bulls.stock.struct.netty.trade.ResponseMessage;
import com.itcast.bulls.stock.struct.netty.trade.StockHeadProto;
import com.itcast.bulls.stock.struct.netty.trade.StockMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * <p>Description: </p>
 * @date 
 * @author 
 * @version 1.0
 * <p>Copyright:Copyright(c)2020</p>
 */
@Component
@ChannelHandler.Sharable
@Log4j2
public class StockProxyServerHandler extends SimpleChannelInboundHandler<StockMessage> {

    /**
     * 缓存管理器
     */
    @Autowired
    private CacheManager cacheManager;
    /**
     * Spring 的线程池
     */
    @Autowired
    private TaskExecutor taskExecutor;

    /**
     * 业务处理器的工厂
     */
    @Autowired
    private StockProcessorFactory stockProcessorFactory;

    /**
     *  负责客户端的channel连接通道的管理(线程安全)
     */
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    /**
     * 接收处理客户端所发送的数据
     * @param channelHandlerContext
     * @param stockMessage
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, StockMessage stockMessage) throws Exception {
        // 异步方式处理
        taskExecutor.execute(() -> {
            try {
                // 校验用户登录鉴权处理
                checkLogin(channelHandlerContext, stockMessage);
                // 获取指定的业务处理器, 处理业务请求数据
                stockProcessorFactory.generateStockProcessor(stockMessage.getStockHead().getRequestType())
                        .processRequest(channelHandlerContext, stockMessage);

            }catch(ComponentException e) {
                // 业务异常
                log.error(e.getMessage(), e);
                StockHeadProto.StockHead stockHead = generateStockHead(stockMessage.getStockHead(), GlobalConstants.STATUS_ERROR);
                ResponseMessage responseMessage = ResponseMessage.newBuilder().setStockHead(stockHead)
                        .setMessage(e.getErrorCodeEnum().getMessage()).build();
                channelHandlerContext.writeAndFlush(responseMessage);
            }catch(Exception e) {
                // 系统异常
                log.error(e.getMessage(), e);
                StockHeadProto.StockHead stockHead = generateStockHead(stockMessage.getStockHead(), GlobalConstants.STATUS_ERROR);
                ResponseMessage responseMessage = ResponseMessage.newBuilder().setStockHead(stockHead)
                        .setMessage(e.getMessage()).build();
                channelHandlerContext.writeAndFlush(responseMessage);
            }
        });

    }

    /**
     * 检查用户是否已登录
     * @param channelHandlerContext
     * @param stockMessage
     */
    private void checkLogin(ChannelHandlerContext channelHandlerContext, StockMessage stockMessage) throws ComponentException {
        // 排除用户登录接口
        if(stockMessage.getStockHead().getRequestType() != StockHeadProto.RequestType.USER_LOGIN) {
            // 获取channelId
            String channelId = channelHandlerContext.channel().id().asLongText();
            // 查询缓存
            Cache cache = cacheManager.getCache(GlobalConstants.STOCK_PROXY_USER_KEY);
            if(null == cache || null == cache.get(channelId)){
                throw new ComponentException(ApplicationErrorCodeEnum.NOT_VALID_LOGIN);
            }
        }
    }

    /**
     * 生成返回的头部信息
     * @param requestType
     * @param seqId
     * @param status
     * @return
     */
    protected StockHeadProto.StockHead generateStockHead(StockHeadProto.StockHead stockHead, String status) {

        StockHeadProto.StockHead stockHeadResult = StockHeadProto.StockHead.newBuilder()
                .setRequestType(stockHead.getRequestType()).setSeqId(stockHead.getSeqId())
                .setStatus(status).build();

        return stockHeadResult;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught on socket: ", cause);
        ctx.close();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // channelGroup 会自动调用处理, 可以不用写
        channels.remove(ctx.channel());
        // 主动关闭, 释放资源
        ctx.close();

    }
}
