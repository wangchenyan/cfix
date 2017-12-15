package me.wcy.cfix.simple;

/**
 * Created by hzwangchenyan on 2017/11/16.
 */
public class Hello {
    private static Hello hello = new Hello();

    public static Hello get() {
        return hello;
    }

    private Hello() {
    }

    public String hello() {
        return "hello";
    }
}
