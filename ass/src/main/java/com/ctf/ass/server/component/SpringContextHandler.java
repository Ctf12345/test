package com.ctf.ass.server.component;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class SpringContextHandler {
    private static ApplicationContext context;

    public static void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
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
}
