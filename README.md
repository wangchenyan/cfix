# cfix

cfix [![cfix](https://api.bintray.com/packages/chanwong21/maven/cfix/images/download.svg)](https://bintray.com/chanwong21/maven/cfix/_latestVersion) cfix-gradle [![cfix-gradle](https://api.bintray.com/packages/chanwong21/maven/cfix-gradle/images/download.svg)](https://bintray.com/chanwong21/maven/cfix-gradle/_latestVersion)

[手把手带你打造一个 Android 热修复框架](https://www.jianshu.com/p/a7f11e0f3a2e)

cfix 是一个基于 [QQ 空间热修复方案](https://mp.weixin.qq.com/s/xuvHomyTzTA90IEWDrdwgw)打造的 Android 热修复框架。

大量参考了 [Nuwa](https://github.com/jasonross/Nuwa)，可以说是 Nuwa 的衍生版，感谢 Nuwa 作者。

## Features

- 全面支持 Gradle 1.5.0-3.x 版本
- 支持除 Application 之外的所有代码增删改
- 不支持 AndroidManifest，因此不支持新增四大组件
- 不支持资源修复
- 支持补丁签名验证

## Change Log

`v1.1`
- 新增支持 Gradle 3.x 版本

`v1.0`
- 第一个版本

## Dependency

1. Add gradle dependency

```
// root project build.gradle
buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.1'
        classpath 'me.wcy:cfix-gradle:1.1'
    }
}
```

2. Add library dependency & apply plugin

```
// module build.gradle
apply plugin: 'com.android.application'
apply plugin: 'me.wcy.cfix'

...

dependencies {
    ...
    compile 'me.wcy:cfix:1.0'
}
```

3. Config

```
// module build.gradle
...
apply plugin: 'me.wcy.cfix'

cfix {
    includePackage = ['me/wcy/cfix/sample']
    excludeClass = ['me/wcy/cfix/sample/Exclude']
    debugOn = true

    sign = true
    storeFile = file("release.jks")
    storePassword = 'android'
    keyAlias = 'cfix'
    keyPassword = 'android'
}
...
```

参数说明

|参数名|作用|是否必须|备注|
|:-- |:-- |:-- |:-- |
| includePackage | 热修复包含的包名 | 否 | 如果不设置，默认将包含所有类，包括依赖的第三方库。建议设置为应用包名，如 `['me/wcy/cfix/sample', ...]` |
| excludeClass | 热修复需要排除的类名 | 否 | 如果不设置将不排除任何类。示例 `['me/wcy/cfix/sample/Exclude', ...]` |
| debugOn | Debug 模式是否开启热修复 | 否 | 默认打开 |
| sign | 是否对 Patch 签名 | 否 | 默认关闭。如果开启签名，则需要配置以下4项，且需要和 APK 签名一致 |
| storeFile | 签名文件 | 否 | 参考 signingConfigs |
| storePassword | 密钥库密码 | 否 |  |
| keyAlias | 证书别名 | 否 |  |
| keyPassword | 证书密钥 | 否 |  |

## Usage

1. Init CFix in you Application

```
@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    CFix.init(this);
}
```

2. Load the patch file

```
CFix.loadPatch(patchFile, verify);
```

## ProGuard

Keep cfix library

```
-keep class me.wcy.cfix.lib.** { *; }
```

## Generate Patch

1. 打包后保存 CFix 输出文件，作为 Patch 的基准，位于

```
module/build/outputs/cfix
```

这里我们将 cfix 文件夹复制到 module 根目录

2. 制作 Patch

Run in terminal

```
gradlew clean cfix${variant.name.capitalize()}Patch -P cfixDir=CFIX_OUTPUT_DIR

例如
gradlew clean cfixXiaomiDebugPatch -P cfixDir=D:\Android\AndroidStudioProjects\CFix\sample\cfix
```

生成的 Patch 文件位于

```
module/build/outputs/cfix${variant.dirName}/patch.jar
```

## Sample

更多用法请参考 [Sample](https://github.com/wangchenyan/cfix/tree/master/sample)

## About me

简书：http://www.jianshu.com/users/3231579893ac

微博：http://weibo.com/wangchenyan1993

## License

    Copyright 2017 wangchenyan

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
