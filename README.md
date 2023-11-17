MeterSphere Jenkins 插件
=============================
[MeterSphere](https://github.com/metersphere/metersphere) 是新一代的测试管理和接口测试工具，让测试工作更简单、更高效，不再成为持续交付的瓶颈。

-   **测试管理**: 从测试用例管理，到测试计划执行、缺陷管理、测试报告生成，具有远超禅道和 TestLink 的使用体验；
-   **接口测试**: 集 Postman 的易用与 JMeter 的灵活于一体，接口定义、接口调试、接口 Mock、场景自动化、接口报告，你想要的都有；
-   **团队协作**: 摆脱单机测试工具的束缚，支持团队协作并对接 DevOps 工具链，将测试融入持续交付体系。

该项目为 MeterSphere 配套的 Jenkins 插件，在 Jenkins 中安装该插件后可将 Jenkins 任务中添加 MeterSphere 构建环节，用户在该构建环节中配置 MeterSphere 平台的认证信息后，可选择指定项目下的测试计划触发执行。

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

Copyright (c) 2014-2024 飞致云 FIT2CLOUD, All rights reserved.

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

https://www.gnu.org/licenses/gpl-3.0.html

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
