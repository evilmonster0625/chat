package com.abin.mallchat.common.chat.controller;

import com.abin.mallchat.common.chat.domain.vo.request.ChatMessageReq;
import com.abin.mallchat.common.chat.service.ChatService;
import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @description: 分布式事务一致性测试
 * @author: zh
 * @create: 2025-04-07 22:32
 **/
@RestController
@RequestMapping("/secureTest")
@Api(tags = "聊天室相关接口")
@Slf4j
public class SecureInvocatTestController {

    @Autowired
    private ChatService chatService;

    @Resource
    private Redisson redisson;


    @PostMapping(value = "/secureTest")
    public ApiResult<Void> secureTest(@RequestBody  ChatMessageReq chatMessageReq){
        RLock redLock = redisson.getLock("redLock");
        redLock.lock(5, TimeUnit.SECONDS);
        try {
            chatService.secureTest(chatMessageReq);
        }catch (Exception e){

        }finally {
            redLock.unlock();
        }





        return ApiResult.success();
    }

}
