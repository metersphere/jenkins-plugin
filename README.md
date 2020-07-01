FIT2CLOUD2.0 CodeDeploy Jenkins Plugin
=============================

安装包
----------

请登录 FIT2CLOUD 客户支持门户，从【常用下载】栏目下载最新的Jenkins 插件安装包。


Development
----------

- Typical maven project, if you find it too slow to resolve the dependencies, try add Aliyun Nexus mirror to you ~/.m2/settings.xml

```
 <mirror>
   <id>nexus-aliyun</id>
   <mirrorOf>*</mirrorOf>
   <name>Nexus aliyun</name>
   <url>http://maven.aliyun.com/nexus/content/groups/public</url>
 </mirror> 
```

- Release .hpi

```
mvn clean package
```

- Online Debug

**Note**: comment Aliyun mirror since Aliyun Nexus doesn't have all the central repos synced.

```
mvn clean hpi:run -Djava.net.preferIPv4Stack=true
```

License
-------

This plugin is licensed under Apache 2.0. See the LICENSE file for more information.
