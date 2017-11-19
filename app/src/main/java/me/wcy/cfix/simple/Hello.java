package me.wcy.cfix.simple;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

/**
 * Created by hzwangchenyan on 2017/11/16.
 */
public class Hello {

    public String hello() {
        return "hello";
    }

    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass hack = classPool.makeClass("me.wcy.cfix.Hack");
        String hackPath = "D:\\Android\\AndroidStudioProjects\\CFix\\app\\cfix";
        hack.writeFile(hackPath);
        classPool.appendClassPath(hackPath);
        String path = "D:\\Android\\AndroidStudioProjects\\CFix\\app\\build\\intermediates\\classes\\xiaomi\\debug";
        classPool.appendClassPath(path);
        CtClass ctClass = classPool.get("me.wcy.cfix.simple.Hello");
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }
        CtConstructor[] constructors = ctClass.getConstructors();
        if (constructors.length > 0) {
            CtConstructor ctConstructor = constructors[0];
            ctConstructor.insertBeforeBody("Class cls = me.wcy.cfix.Hack.class;");
            ctClass.writeFile(path);
            ctClass.detach();
        }
    }
}
