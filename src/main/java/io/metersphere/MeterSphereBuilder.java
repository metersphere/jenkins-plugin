package io.metersphere;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.metersphere.client.MeterSphereClient;
import io.metersphere.commons.constants.Method;
import io.metersphere.commons.constants.Results;
import io.metersphere.commons.exception.MeterSphereException;
import io.metersphere.commons.model.*;
import io.metersphere.commons.utils.HttpClientUtil;
import io.metersphere.commons.utils.LogUtil;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MeterSphereBuilder extends Builder implements SimpleBuildStep, Serializable {

    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";
    private final String msEndpoint;
    private final String msAccessKey;
    private final String msSecretKey;
    private final String workspaceId;
    private final String orgId;
    private final String projectId;
    private PrintStream logger;
    private final String testPlanId;
    private final String testCaseNodeId;
    private final String testId;
    private final String testCaseId;
    private final String method;
    private final String result;
    private final String environmentId;
    private final String mode;//运行模式
    private final String runEnvironmentId;//运行环境

    private final String callbackUrls;

    @DataBoundConstructor
    public MeterSphereBuilder(String msEndpoint, String msAccessKey, String msSecretKey, String workspaceId, String orgId,
                              String projectId, PrintStream logger, String testPlanId, String testCaseNodeId, String testId,
                              String testCaseId, String method, String result, String environmentId, String mode,
                              String runEnvironmentId, String callbackUrls) {
        this.msEndpoint = msEndpoint;
        this.msAccessKey = msAccessKey;
        this.msSecretKey = msSecretKey;
        this.workspaceId = workspaceId;
        this.orgId = orgId;
        this.projectId = projectId;
        this.logger = logger;
        this.testPlanId = testPlanId;
        this.testCaseNodeId = testCaseNodeId;
        this.testId = testId;
        this.testCaseId = testCaseId;
        this.method = StringUtils.isBlank(method) ? Method.testPlan : method;
        this.result = result;
        this.environmentId = environmentId;
        this.mode = mode;
        this.runEnvironmentId = runEnvironmentId;

        this.callbackUrls = callbackUrls;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        this.logger = listener.getLogger();
        listener.getLogger().println("workspace=" + workspace);
        listener.getLogger().println("number=" + run.getNumber());
        listener.getLogger().println("url=" + run.getUrl());
        final MeterSphereClient meterSphereClient = new MeterSphereClient(this.msAccessKey, this.msSecretKey, this.msEndpoint);
        log("执行方式" + method);
        try {
            switch (method) {
                case Method.testPlan:
                    log("测试计划开始执行");
                    String id = meterSphereClient.exeTestPlan(projectId, testPlanId, mode, runEnvironmentId);
                    log("生成测试报告id:" + id.replace('"', ' ').trim());
                    String url = meterSphereClient.getBaseInfo();
                    log("当前站点url:" + url);
                    boolean flag = true;
                    while (flag) {
                        String status = meterSphereClient.getStatus(id);
                        if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.SUCCESS)) {
                            flag = false;
                            log("该测试计划已完成");
                            log("点击链接进入测试计划报告页面:" + url + "/#/track/testPlan/reportList");
                        } else if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.FAILED)) {
                            flag = false;
                            run.setResult(Result.FAILURE);
                            log("该测试计划失败");
                            log("点击链接进入测试计划报告页面:" + url + "/#/track/testPlan/reportList");
                        } else if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.COMPLETED)) {
                            flag = false;
                            log("该测试计划已完成");
                            log("点击链接进入测试计划报告页面:" + url + "/#/track/testPlan/reportList");
                        }
                        Thread.sleep(5000);
                    }

                    if (!flag) {
                        log("开始执行回调，地址列表是：" + callbackUrls);

                        this.call(meterSphereClient, id, callbackUrls, workspace.getRemote());
                    }

                    break;
                case Method.single:
                    List<TestCaseDTO> testCaseIds = meterSphereClient.getTestCaseIds(projectId);//项目下
                    getTestStepsBySingle(meterSphereClient, testCaseIds, environmentId, projectId);
                    break;
                default:
                    log("测试用例不存在");
            }

        } catch (Exception e) {
            log("出现异常:" + e.getMessage());
            if (result.equals(Results.METERSPHERE)) {
                run.setResult(Result.FAILURE);
            } else {
                log("该测试请求未能通过，登陆MeterSphere网站查看该报告结果");
            }

        }

    }

    /**
     * 执行回调.
     *
     * @param meterSphereClient
     * @param reportId
     * @param callbackUrls
     * @param workspacePath
     */
    private void call(MeterSphereClient meterSphereClient, String reportId, String callbackUrls, String workspacePath) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(callbackUrls)) {
            log("Metersphere回调地址为空，退出回调");
            return;
        }

        log("Metersphere查询报告详情，开始=====");
        ResultHolder resultHolder = meterSphereClient.getTestPlanReportDetail(reportId);
        log("Metersphere查询报告详情，结束=====");

        if (!resultHolder.isSuccess()) {
            log("Metersphere查询报告详情，结果返回失败");
            return;
        }

        com.alibaba.fastjson.JSONObject jsonObject = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSONObject.toJSON(resultHolder.getData());

        jsonObject.put("reportUrl", meterSphereClient.getBaseInfo() + "/#/track/testPlan/reportList");

        jsonObject.put("reportId", reportId);

        parseWorkspace(workspacePath, jsonObject);

        String[] callbackUrlArray = callbackUrls.split(",");

        for (String callbackUrl : callbackUrlArray) {
            log("Metersphere回调的报文内容是：" + jsonObject.toJSONString());

            HttpClientUtil.post(callbackUrl, jsonObject.toJSONString());

            log("Metersphere回调完成");
        }
    }

    /**
     * 解析JobName中的工程名和环境.
     *
     * @param workspacePath
     * @param jsonObject
     */
    private void parseWorkspace(String workspacePath, com.alibaba.fastjson.JSONObject jsonObject) {
        String jobName = workspacePath.substring(workspacePath.indexOf("/AUTO_") + 1, workspacePath.length());

        String[] jobArray = jobName.split("_");

        jsonObject.put("projectName", jobArray[2]);

        jsonObject.put("env", jobArray[3]);
    }

    public void getTestStepsBySingle(MeterSphereClient meterSphereClient, List<TestCaseDTO> testCaseIds, String environmentId, String projectId) {
        //log("testList=" + "[" + JSON.toJSONString(testCaseIds) + "]");
        log("testCaseId=" + "[" + testCaseId + "]");
        log("environmentId=" + "[" + environmentId + "]");
        boolean flag = true;
        if (CollectionUtils.isNotEmpty(testCaseIds)) {
            for (TestCaseDTO c : testCaseIds) {
                if (StringUtils.equals(testCaseId, c.getId())) {
                    if (StringUtils.equals(Results.API, c.getType())) {
                        int num = 1;
                        num = num * runApiTest(meterSphereClient, c, testCaseId);
                        if (num == 0) {
                            flag = false;
                        }
                        if (num == 0) {
                            flag = false;
                        }
                    }
                    if (StringUtils.equals(Results.PERFORMANCE, c.getType())) {
                        int num = runPerformTest(meterSphereClient, c, testCaseId, "");
                        if (num == 0) {
                            flag = false;
                        }
                    }
                    if (StringUtils.equals(Results.SCENARIO, c.getType())) {
                        int num = runScenario(meterSphereClient, c, testCaseId, projectId, "scenario");
                        if (num == 0) {
                            flag = false;
                        }
                    }
                    if (StringUtils.equals(Results.DEFINITION, c.getType())) {
                        int num = runDefinition(meterSphereClient, c, testCaseId, testPlanId, "JENKINS", environmentId);
                        if (num == 0) {
                            flag = false;
                        }
                    }
                }
            }
            if (flag) {
                log("该测试用例请求通过，登陆MeterSphere网站查看该报告结果");
            } else {
                if (result.equals(Results.METERSPHERE)) {
                    throw new MeterSphereException("该测试用例未能完成");
                } else {
                    log("该测试用例请求未能通过，登陆MeterSphere网站查看该报告结果");
                }
            }
        }
    }


    public int runApiTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String id) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        String reportId = "";
        try {
            reportId = meterSphereClient.runApiTest(id);
        } catch (Exception e) {
            num = 0;

        }
        try {
            boolean state = true;
            String apiTestState = "";
            while (state) {
                apiTestState = meterSphereClient.getApiTestState(reportId);
                log("接口测试[" + c.getName() + "]执行状态：" + apiTestState);
                if (apiTestState.equalsIgnoreCase(Results.SUCCESS)) {
                    state = false;
                    log("点击链接进入" + c.getName() + "测试报告页面:" + url + "/#/api/report/view/" + reportId.replace("\"", ""));
                    meterSphereClient.changeState(id, Results.PASS);
                } else if (apiTestState.equalsIgnoreCase(Results.ERROR)) {
                    state = false;
                    num = 0;
                    log("点击链接进入" + c.getName() + "测试报告页面:" + url + "/#/api/report/view/" + reportId.replace("\"", ""));
                    meterSphereClient.changeState(id, Results.FAILURE);
                }
                Thread.sleep(1000 * 60);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }

    public int runPerformTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String id, String testPlanId) {
        String url = meterSphereClient.getBaseInfo();
        String reportId = "";
        int num = 1;
        try {
            reportId = meterSphereClient.runPerformanceTest(id, testPlanId);
        } catch (Exception e) {
            num = 0;
            log(c.getName() + "发生异常：" + e.getMessage());
        }
        try {
            boolean state = true;
            String pfmTestState = "";
            while (state) {
                pfmTestState = meterSphereClient.getPerformanceTestState(id);
                log("性能测试[" + c.getName() + "]执行状态：" + pfmTestState);
                if (pfmTestState.equalsIgnoreCase(Results.COMPLETED)) {
                    //更新测试计划下性能测试状态
                    meterSphereClient.updateStateLoad(testPlanId, id, "success");
                    state = false;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/performance/report/view/" + reportId.replace("\"", ""));
                    meterSphereClient.changeState(id, Results.PASS);
                } else if (pfmTestState.equalsIgnoreCase(Results.ERROR)) {
                    //更新测试计划下性能测试状态
                    meterSphereClient.updateStateLoad(testPlanId, id, "Error");
                    state = false;
                    num = 0;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/performance/report/view/" + reportId.replace("\"", ""));
                    meterSphereClient.changeState(id, Results.FAILURE);
                }
                Thread.sleep(1000 * 60);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;
        }
        return num;
    }

    public int runScenario(MeterSphereClient meterSphereClient, TestCaseDTO c, String id, String projectId, String runMode) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        String reportId = null;
        try {
            RunModeConfig config = null;
            if (StringUtils.isNotEmpty(runEnvironmentId)) {
                config = new RunModeConfig();
                config.setResourcePoolId(runEnvironmentId);
                config.setMode(runMode);
                config.setReportName("");
                config.setReportType("iddReport");
                config.setOnSampleError(true);
            }
            reportId = meterSphereClient.runScenario(c, projectId, runMode, config);
        } catch (Exception e) {
            num = 0;
            log(c.getName() + "场景测试发生异常:" + e.getMessage());
        }
        try {
            boolean state = true;
            String apiTestState = "";
            while (state) {
                apiTestState = meterSphereClient.getApiScenario(reportId);
                log("场景测试[" + c.getName() + "]执行状态：" + apiTestState);
                if (apiTestState.equalsIgnoreCase(Results.SUCCESS)) {
                    state = false;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/api/automation/report/view/" + reportId.replace("\"", ""));
                } else if (apiTestState.equalsIgnoreCase(Results.ERROR)) {
                    state = false;
                    num = 0;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/api/automation/report/view/" + reportId.replace("\"", ""));
                }
                Thread.sleep(1000 * 60);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }

    public int runDefinition(MeterSphereClient meterSphereClient, TestCaseDTO c, String id, String testPlanId, String runMode, String environmentId) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        try {
            meterSphereClient.runDefinition(c, runMode, environmentId, testPlanId, id);
        } catch (Exception e) {
            num = 0;
            log(c.getName() + "测试用例发生异常:" + e.getMessage());
        }
        try {
            boolean state = true;
            String status = "";
            while (state) {
                status = meterSphereClient.getApiTestCaseReport(c.getId(), runMode);
                log("测试用例[" + c.getName() + "]执行状态：" + status);
                if (status.replace("\"", "").equalsIgnoreCase("success")) {
                    state = false;
                } else if (status.replace("\"", "").equalsIgnoreCase("error")) {
                    state = false;
                    num = 0;
                }
                Thread.sleep(1000 * 60);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }


    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    @Symbol("meterSphere")
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckAccount(
                @QueryParameter String msAccessKey,
                @QueryParameter String msSecretKey,
                @QueryParameter String msEndpoint) throws IOException {
            if (StringUtils.isEmpty(msAccessKey)) {
                return FormValidation.error("MeterSphere ConsumerKey不能为空！");
            }
            if (StringUtils.isEmpty(msSecretKey)) {
                return FormValidation.error("MeterSphere SecretKey不能为空！");
            }
            if (StringUtils.isEmpty(msEndpoint)) {
                return FormValidation.error("MeterSphere EndPoint不能为空！");
            }
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                MeterSphereClient.checkUser();
            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
                return FormValidation.error("验证MeterSphere帐号失败！" + e + "," + e.getMessage());
            }
            return FormValidation.ok("验证MeterSphere帐号成功！");
        }

        //用户所属组织
        public ListBoxModel doFillOrgIdItems(@QueryParameter String msAccessKey,
                                             @QueryParameter String msSecretKey,
                                             @QueryParameter String msEndpoint
        ) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择所属组织", "");
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                List<OrgDTO> list = MeterSphereClient.getOrg();
                if (list != null && list.size() > 0) {
                    for (OrgDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;

        }

        //用户所属工作空间
        public ListBoxModel doFillWorkspaceIdItems(@QueryParameter String msAccessKey,
                                                   @QueryParameter String msSecretKey,
                                                   @QueryParameter String msEndpoint,
                                                   @QueryParameter String orgId
        ) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择工作空间", "");
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                List<WorkspaceDTO> list = MeterSphereClient.getWorkspace(orgId);
                if (list != null && list.size() > 0) {
                    for (WorkspaceDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;

        }

        //在工作空间下与用户有关的项目
        public ListBoxModel doFillProjectIdItems(@QueryParameter String msAccessKey,
                                                 @QueryParameter String msSecretKey,
                                                 @QueryParameter String msEndpoint,
                                                 @QueryParameter String workspaceId) {
            ListBoxModel items = new ListBoxModel();
            try {
                List<ProjectDTO> list = new ArrayList<>();
                items.add("请选择所属项目", "");
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                if (workspaceId != null && !workspaceId.equals("")) {
                    list = MeterSphereClient.getProjectIds(workspaceId);

                }
                if (list != null && list.size() > 0) {
                    for (ProjectDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;
        }

        //该项目下的所有的测试计划
        public ListBoxModel doFillTestPlanIdItems(@QueryParameter String msAccessKey,
                                                  @QueryParameter String msSecretKey,
                                                  @QueryParameter String msEndpoint,
                                                  @QueryParameter String workspaceId,
                                                  @QueryParameter String projectId) {
            ListBoxModel items = new ListBoxModel();
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择测试计划", "");
                List<TestPlanDTO> list = new ArrayList<>();
                if (projectId != null && !projectId.equals("")) {
                    list = MeterSphereClient.getTestPlanIds(projectId, workspaceId);
                }
                if (list != null && list.size() > 0) {
                    for (TestPlanDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }

            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;
        }

        //该项目下所有的接口和性能测试
        public ListBoxModel doFillTestCaseIdItems(@QueryParameter String msAccessKey,
                                                  @QueryParameter String msSecretKey,
                                                  @QueryParameter String msEndpoint,
                                                  @QueryParameter String projectId

        ) {
            ListBoxModel items = new ListBoxModel();
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择测试名称", "");
                List<TestCaseDTO> list = new ArrayList<>();
                if (projectId != null && !projectId.equals("")) {
                    list = MeterSphereClient.getTestCaseIds(projectId);
                }
                if (list != null && list.size() > 0) {
                    for (TestCaseDTO c : list) {
                        items.add(c.getName() + "（" + c.getType() + "）", String.valueOf(c.getId()));
                    }
                }

            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;
        }

        //接口测试的运行环境的列表
        public ListBoxModel doFillEnvironmentIdItems(@QueryParameter String msAccessKey,
                                                     @QueryParameter String msSecretKey,
                                                     @QueryParameter String msEndpoint,
                                                     @QueryParameter String projectId) {
            ListBoxModel items = new ListBoxModel();
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择运行环境", "");
                List<ApiTestEnvironmentDTO> list = new ArrayList<>();
                if (projectId != null && !projectId.equals("")) {
                    list = MeterSphereClient.getEnvironmentIds(projectId);
                }
                if (list != null && list.size() > 0) {
                    for (ApiTestEnvironmentDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }

            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;
        }

        //资源池的选择
        public ListBoxModel doFillRunEnvironmentIdItems(@QueryParameter String msAccessKey,
                                                        @QueryParameter String msSecretKey,
                                                        @QueryParameter String msEndpoint
        ) {
            ListBoxModel items = new ListBoxModel();
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择资源池", "");
                List<EnvironmentPoolDTO> list = new ArrayList<>();
                list = MeterSphereClient.getPoolEnvironmentIds();

                if (list != null && list.size() > 0) {
                    for (EnvironmentPoolDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }

            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this);
            save();
            return super.configure(req, formData);
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "MeterSphere";
        }

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    public static String getLogPrefix() {
        return LOG_PREFIX;
    }

    public String getMsEndpoint() {
        return msEndpoint;
    }

    public String getMsAccessKey() {
        return msAccessKey;
    }

    public String getMsSecretKey() {
        return msSecretKey;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getProjectId() {
        return projectId;
    }


    public PrintStream getLogger() {
        return logger;
    }

    public String getTestPlanId() {
        return testPlanId;
    }

    public String getTestId() {
        return testId;
    }

    private void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }


    public String getMethod() {
        return method;
    }

    public String getTestCaseNodeId() {
        return testCaseNodeId;
    }


    public String getTestCaseId() {
        return testCaseId;
    }

    public String getResult() {
        return result;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public String getMode() {
        return mode;
    }

    public String getRunEnvironmentId() {
        return runEnvironmentId;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getCallbackUrls() {
        return callbackUrls;
    }
}