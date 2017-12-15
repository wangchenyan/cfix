package me.wcy.cfix.simple;

/**
 * Created by hzwangchenyan on 2017/11/16.
 */
public class Hello {

    public static Hello newHello() {
        return new Hello();
    }

    public String hello() {
        return "hello";
    }

    private Hello() {
    }
}
