package fr.wseduc.security;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.METHOD)
public @interface PreAuthorize {

    String value();
    Class<?> clazz();

}
