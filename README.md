MeterSphere Jenkins 插件
=============================
[MeterSphere](https://github.com/metersphere/metersphere) 是一站式开源持续测试平台，涵盖测试跟踪、接口测试、性能测试、团队协作等功能，兼容JMeter 等开源标准，有效助力开发和测试团队充分利用云弹性进行高度可扩展的自动化测试，加速高质量软件的交付。

该项目为 MeterSphere 配套的 Jenkins 插件，在 Jenkins 中安装该插件后可将 Jenkins 任务中添加 MeterSphere 构建环节，用户在该构建环节中配置 MeterSphere 平台的认证信息后，可选择指定项目下的接口/性能测试进行触发执行。

## 安装使用

### 下载插件
  1. 在该项目的 [release](https://github.com/metersphere/jenkins-plugin/releases) 页面下载最新版本的 hpi 包
  2. 在 Jenkins 的插件管理页面，上传并安装下载好的 hpi 插件包

### 使用指导

插件安装后，在指定的 Jenkins 构建任务中，添加「MeterSphere」类型的构建步骤

![](https://metersphere.oss-cn-hangzhou.aliyuncs.com/img/jenkins-plugin.png)

根据图示配置，填写认证信息并选择需要触发执行的用例

![](https://metersphere.oss-cn-hangzhou.aliyuncs.com/img/Jenkins-config.png)

## 问题反馈

如果您在使用过程中遇到什么问题，或有进一步的需求需要反馈，请提交 GitHub Issue 到 [MeterSphere 项目的主仓库](https://github.com/metersphere/metersphere/issues)
  
## 微信群

![wechat-group](https://metersphere.oss-cn-hangzhou.aliyuncs.com/img/wechat-group.png)

## License & Copyright

Copyright (c) 2014-2022 飞致云 FIT2CLOUD, All rights reserved.

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

https://www.gnu.org/licenses/gpl-3.0.html

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
