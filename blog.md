# 手把手带你打造一个 Android 热修复框架

## 前言

当前热修复和插件化可以说是 Android 领域最火热的两门技术，如果不了解这两门技术都不好意思说自己是 Android 开发工程师。<br>
目前比较流行的热修复方案有微信的Tinker，手淘的Sophix，美团的Robust，以及QQ空间热修复方案。<br>
QQ空间热修复方案使用Java实现，最容易上手。<br>
如果还不了解QQ空间方案的原理，请先学习[安卓App热补丁动态修复技术介绍](https://mp.weixin.qq.com/s?__biz=MzI1MTA1MzM2Nw==&mid=400118620&idx=1&sn=b4fdd5055731290eef12ad0d17f39d4a)<br>
今天，我们就基于QQ空间方案来深入学习热修复原理，并且手把手完成一个热修复框架。<br>
本文参考了Nuwa，在此表示感谢。本文基于 Gradle 2.3.3 版本，支持 Gradle 1.5.0-2.3.3。

## 实战

了解了热修复原理后，我们就开始打造一个热修复框架

1. 关闭dex校验

根据文章中提到的第一个问题，在 Android 5.0以上，APK安装时，为了提高dex加载速度，未引用其他dex的class将会被打上CLASS_ISPREVERIFIED标志。<br>
打上CLASS_ISPREVERIFIED标志的class，类加载器就不会去其他dex中寻找class，我们就无法使用插桩的方式替换clazz。<br>
文章给出了解决办法，即让所有class都依赖其他dex。如何实现呢？<br>
新建一个Hack类，让所有class都依赖该类，将该类打包成dex，在应用启动时优先将该dex插入到数组的最前面，即可实现。<br>

```
public class Hack {
}
```

听起来好像很简单，那么如何让所有class依赖Hack类呢，总不能一个一个class改吧，怎么才能在打包时自动添加依赖呢？<br>
接下来就要用到 Gradle Hook 和 javaassist。
还不了解Gradle构建流程的赶快去学习啦
要想修改打包后的class文件，首先要Hook打包过程，在Gradle打包出class文件到打包成apk之间植入我们的代码，对class文件进行修改，
找到打包出的class文件要依赖 Gradle Hook ，而修改class文件要依赖javaassist。
首先，我们要找到打包出的class文件
新建一个Project CFixExample，然后执行 assembleDebug 
图片01
观察 Gradle Console 输出

```
:app:preBuild UP-TO-DATE
:app:preDebugBuild UP-TO-DATE
:app:checkDebugManifest
:app:preReleaseBuild UP-TO-DATE
:app:prepareComAndroidSupportAnimatedVectorDrawable2540Library
// 省略部分Task
:app:prepareComAndroidSupportSupportVectorDrawable2540Library
:app:prepareDebugDependencies
:app:compileDebugAidl UP-TO-DATE
:app:compileDebugRenderscript UP-TO-DATE
:app:generateDebugBuildConfig UP-TO-DATE
:app:generateDebugResValues UP-TO-DATE
:app:generateDebugResources UP-TO-DATE
:app:mergeDebugResources UP-TO-DATE
:app:processDebugManifest UP-TO-DATE
:app:processDebugResources UP-TO-DATE
:app:generateDebugSources UP-TO-DATE
:app:incrementalDebugJavaCompilationSafeguard
:app:javaPreCompileDebug
:app:compileDebugJavaWithJavac
:app:compileDebugNdk NO-SOURCE
:app:compileDebugSources
:app:mergeDebugShaders
:app:compileDebugShaders
:app:generateDebugAssets
:app:mergeDebugAssets
:app:transformClassesWithDexForDebug
:app:mergeDebugJniLibFolders
:app:transformNativeLibsWithMergeJniLibsForDebug
:app:processDebugJavaRes NO-SOURCE
:app:transformResourcesWithMergeJavaResForDebug
:app:validateSigningDebug
:app:packageDebug
:app:assembleDebug

BUILD SUCCESSFUL in 10s
```

这些就是 Gradle 打包时执行的所有任务，不同版本的Gradle会有所不同。
请注意 processDebugManifest 和 transformClassesWithDexForDebug 这两个Task，根据名字我们可以猜测到
这两个任务的作用应该分别是处理Manifest和将class转换为dex。
第一个Task的作用应该是处理Manifest，这个我们等会儿会用到
第二个Task的作用应该是将class转换为dex，这不正是我们要找的Hook点吗，没错，为了验证我们的猜测，我们打印一下 transformClassesWithDexForDebug 的输出
在 app 的 build.gradle 中添加如下代码
```

```
再次打包，观察输出
```
transformClassesWithDexTask inputs
C:\Users\hzwangchenyan\.android\build-cache\97c23f4056f5ee778ec4eb674107b6b52d506af5\output\jars\classes.jar
C:\Users\hzwangchenyan\.android\build-cache\6afe39630b2c3d3c77f8edc9b1e09a2c7198cd6d\output\jars\classes.jar
C:\Users\hzwangchenyan\.android\build-cache\c30268348acf4c4c07940f031070b72c4efa6bba\output\jars\classes.jar
C:\Users\hzwangchenyan\.android\build-cache\5b09d9d421b0a6929ae76b50c69f95b4a4a44566\output\jars\classes.jar
C:\Users\hzwangchenyan\.android\build-cache\e302262273df85f0776e06e63fde3eb1bdc3e57f\output\jars\classes.jar
C:\Users\hzwangchenyan\.gradle\caches\modules-2\files-2.1\com.android.support\support-annotations\25.4.0\f6a2fc748ae3769633dea050563e1613e93c135e\support-annotations-25.4.0.jar
C:\Users\hzwangchenyan\.android\build-cache\36b7224f035cc886381f4287c806a33369f1cb1a\output\jars\classes.jar
C:\Users\hzwangchenyan\.android\build-cache\5d757d92536f0399625abbab92c2127191e0d073\output\jars\classes.jar
C:\Users\hzwangchenyan\.android\build-cache\011eb26fd0abe9f08833171835fae10cfda5e045\output\jars\classes.jar
D:\Android\sdk\extras\m2repository\com\android\support\constraint\constraint-layout-solver\1.0.2\constraint-layout-solver-1.0.2.jar
C:\Users\hzwangchenyan\.android\build-cache\36b443908e839f37d7bd7eff1ea793f138f8d0dd\output\jars\classes.jar
C:\Users\hzwangchenyan\.android\build-cache\40634d621fa35fcca70280efe0ae897a9d82ef8f\output\jars\classes.jar
D:\Android\AndroidStudioProjects\CFixExample\app\build\intermediates\classes\debug
```
build-cache就是support包，看起来是app依赖的library，但是我们自己的代码呢
看看app\build\intermediates\classes\debug目录
图片02
没错，正是我们自己的代码，看来我们的猜测是正确的。
我们刚才测试的使用debug未混淆模式，如果我们使用混淆的话Task还会和上面的完全一样吗？
我们把release的混淆打开，然后执行 assembleRelease，观察 Gradle Console 输出
```
:app:preBuild UP-TO-DATE
// 省略部分Task
:app:processReleaseJavaRes NO-SOURCE
:app:transformResourcesWithMergeJavaResForRelease
:app:transformClassesAndResourcesWithProguardForRelease
:app:transformClassesWithDexForRelease
:app:mergeReleaseJniLibFolders
:app:transformNativeLibsWithMergeJniLibsForRelease
:app:validateSigningRelease
:app:packageRelease
:app:assembleRelease
```
可以看到相比较未开启混淆多了一个 transformClassesAndResourcesWithProguardForRelease 那么这个Proguard Task有用吗？
有用！
为了保证打包apk和patch时class混淆后的名字不变，我们需要在Proguard Task前插入混淆逻辑
```

```