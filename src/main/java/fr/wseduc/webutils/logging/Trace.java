package fr.wseduc.webutils.logging;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.METHOD)
public @interface Trace {
    String value();
    boolean body() default true;
}
