package org.opencds.cqf.qdm.fivepoint4;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class QdmContext implements ApplicationContextAware {
    private static ApplicationContext context;

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext context) throws BeansException {

        // store ApplicationContext reference to access required beans later on
        QdmContext.context = context;
    }
}
