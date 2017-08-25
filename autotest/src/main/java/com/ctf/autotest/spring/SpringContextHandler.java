package com.ctf.autotest.spring;

import com.cootf.cloudsim.oms.service.api.service.AutoTestIgniteService;
import com.cootf.cloudsim.oms.service.api.service.AutoTestMongoDBService;
import com.cootf.cloudsim.oms.service.api.service.SimCardService;
import com.cootf.cloudsim.oms.service.api.service.SimPoolService;

import org.springframework.context.ApplicationContext;

/**
 * @author Charles
 * @create 2017/7/27 17:54
 */
public class SpringContextHandler {
    private static ApplicationContext context = null;
    private static AutoTestIgniteService autoTestIgniteService;
    private static AutoTestMongoDBService autoTestMongoDBService;
    private static SimPoolService simPoolService;
    private static SimCardService simCardService;
    public static void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
        autoTestIgniteService = getBean(AutoTestIgniteService.class);
        autoTestMongoDBService = getBean(AutoTestMongoDBService.class);
        simPoolService = getBean(SimPoolService.class);
        simCardService = getBean(SimCardService.class);
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public static Object getBean(String beanName) {
        if (context == null) {
            return null;
        }
        return context.getBean(beanName);
    }

    public static <T> T getBean(Class<T> clazz) {
        if (context == null) {
            return null;
        }
        return context.getBean(clazz);
    }

    public static AutoTestIgniteService getAutoTestIgniteService() {
        return autoTestIgniteService;
    }

    public static AutoTestMongoDBService getAutoTestMongoDBService() {
        return autoTestMongoDBService;
    }

    public static SimPoolService getSimPoolService() {
        return simPoolService;
    }

    public static SimCardService getSimCardService() {
        return simCardService;
    }
}
