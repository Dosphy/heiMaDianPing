package com.hmdp.listener;

import com.hmdp.entity.Score;
import com.hmdp.mapper.TbScoreMapper;
import com.hmdp.mapper.UserMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;


/**
 * @Author Dosphy
 * @Date 2025/9/22 11:51
 */
@Component
public class RabbitMqListener {
    private static final String QUEUE = "queue";

    @Autowired
    TbScoreMapper scoreMapper;

    //监听消息
    @RabbitListener(queues = QUEUE)
    public void listen(Score score) {
        scoreMapper.insert(score);
    }
}
