package me.wcy.cfix.simple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by hzwangchenyan on 2017/12/15.
 */
@Target(ElementType.TYPE)
public @interface FixAnnotation {
    int anno();
}
