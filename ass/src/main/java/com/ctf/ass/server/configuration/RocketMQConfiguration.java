package com.ctf.ass.server.configuration;

import com.ctf.ass.server.utils.LogWrapper;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PreDestroy;

/**
 * @author Charles
 * @create 2017/7/21 14:49
 */
@Configuration
@PropertySource(value = "classpath:/rocketmq.properties")
public class RocketMQConfiguration {
    private static final LogWrapper logger = LogWrapper.getLogger(RocketMQConfiguration.class);
    @Value("${rocketmq.namesrvAddr}")
    private String nameSrvAddr;
    @Value("${rocketmq.producerGroup}")
    private String producerGroup;
    @Value("${rocketmq.instanceName}")
    private String instanceName;
    private DefaultMQProducer defaultMQProducer;

    @Bean
    public DefaultMQProducer defaultMQProducer() {
/*        try {
            DefaultMQProducer defaultMQProducer = new DefaultMQProducer(producerGroup);
            defaultMQProducer.setNamesrvAddr(nameSrvAddr);
            defaultMQProducer.setInstanceName(instanceName);
            defaultMQProducer.setVipChannelEnabled(false);

            defaultMQProducer.start();
            this.defaultMQProducer = defaultMQProducer;
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        logger.debug("Producer Started.");*/
        return defaultMQProducer;
    }

    @PreDestroy
    public void destroy() {
        if (this.defaultMQProducer != null) {
            defaultMQProducer.shutdown();
        }
    }
}
