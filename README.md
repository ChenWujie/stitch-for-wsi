# WSI图像拼接软件

全切片图像拼接软件，解决低内存设备上imagej拼接大量图像内存溢出的问题。

项目目录结构：

```
root:
│  .gitignore
│  dependency-reduced-pom.xml
│  launch4j-config.xml
│  opencv_java490.dll
│  pom.xml
│  README.md
├─lib
│      opencv-490.jar
├─src...
```

本项目使用java11和opencv490，Maven打包项目，`Launch4j`构建.exe可执行文件，`launch4j-config.xml`为`Launch4j`配置，由于`opencv`依赖及java11版本问题，打包后的可执行文件目录中需要手动放入jdk以及opencv的jar包和dll文件：

```
root:
│  jre
│  lib
│  launch4j-config.xml
│  opencv_java490.dll
│  pom.xml
│  README.md
├─lib
│      opencv-490.jar
├─src...
```

Windows系统完整可直接运行软件：
通过网盘分享的文件：stitch.zip
链接: https://pan.baidu.com/s/1tk_EL2uEo7k14YmZYySkhg?pwd=yyds 提取码: yyds
--来自百度网盘超级会员v10的分享