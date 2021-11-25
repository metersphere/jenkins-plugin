package io.metersphere;

import hudson.*;
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
import io.metersphere.commons.model.*;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.commons.utils.MeterSphereUtils;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
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
import java.util.Optional;

public class MeterSphereBuilder extends Builder implements SimpleBuildStep, Serializable {

    private static final String LOG_PREFIX = "[MeterSphere] ";
    private final String msEndpoint;
    private final String msAccessKey;
    private final String msSecretKey;
    private final String workspaceId;
    private final String projectId;
    private PrintStream logger;
    private final String testPlanId;
    private final String testPlanName;
    private final String testCaseNodeId;
    private final String testId;
    private final String testCaseId;
    private final String method;
    private final String result;
    private final String mode;//运行模式
    private final String resourcePoolId;//运行环境


    @DataBoundConstructor
    public MeterSphereBuilder(String msEndpoint, String msAccessKey, String msSecretKey, String workspaceId,
                              String projectId, PrintStream logger, String testPlanId, String testCaseNodeId,
                              String testId, String testCaseId, String method, String result, String testPlanName,
                              String mode, String resourcePoolId) {
        this.msEndpoint = msEndpoint;
        this.msAccessKey = msAccessKey;
        this.msSecretKey = msSecretKey;
        this.workspaceId = workspaceId;
        this.projectId = projectId;
        this.logger = logger;
        this.testPlanId = testPlanId;
        this.testPlanName = testPlanName;
        this.testCaseNodeId = testCaseNodeId;
        this.testId = testId;
        this.testCaseId = testCaseId;
        this.method = StringUtils.isBlank(method) ? Method.TEST_PLAN : method;
        this.result = result;
        this.mode = mode;
        this.resourcePoolId = resourcePoolId;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        this.logger = listener.getLogger();
        MeterSphereUtils.logger = logger;
        listener.getLogger().println("workspace=" + workspace);
        listener.getLogger().println("number=" + run.getNumber());
        listener.getLogger().println("url=" + run.getUrl());
        final MeterSphereClient client = new MeterSphereClient(this.msAccessKey, this.msSecretKey, this.msEndpoint);
        log("执行方式: " + method);
        try {
            switch (method) {
                case Method.TEST_PLAN:
                    MeterSphereUtils.execTestPlan(run, client, projectId, mode, testPlanId, resourcePoolId);
                    break;
                case Method.TEST_PLAN_NAME:
                    EnvVars environment = run.getEnvironment(listener);
                    String testPlanName = Util.replaceMacro(this.testPlanName, environment);

                    List<TestPlanDTO> testPlans = client.getTestPlanIds(projectId, workspaceId);
                    Optional<TestPlanDTO> first = testPlans.stream()
                            .filter(plan -> StringUtils.equals(testPlanName, plan.getId()) || StringUtils.equals(testPlanName, plan.getName()))
                            .findFirst();

                    if (!first.isPresent()) {
                        log("测试计划不存在");
                        return;
                    }
                    MeterSphereUtils.execTestPlan(run, client, projectId, mode, first.get().getId(), resourcePoolId);
                    break;
                case Method.SINGLE:
                    List<TestCaseDTO> testCaseIds = client.getTestCases(projectId);//项目下
                    MeterSphereUtils.getTestStepsBySingle(client, testCaseIds, projectId, testCaseId, testPlanId, resourcePoolId);
                    break;
                default:
                    log("测试用例不存在");
            }

        } catch (Exception e) {
            if (result.equals(Results.METERSPHERE)) {
                run.setResult(Result.FAILURE);
            } else {
                log("该测试请求未能通过，登陆MeterSphere网站查看该报告结果");
            }

        }

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

        //用户所属工作空间
        public ListBoxModel doFillWorkspaceIdItems(@QueryParameter String msAccessKey,
                                                   @QueryParameter String msSecretKey,
                                                   @QueryParameter String msEndpoint
        ) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择工作空间", "");
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                List<WorkspaceDTO> list = MeterSphereClient.getWorkspace();
                if (list != null && list.size() > 0) {
                    for (WorkspaceDTO c : list) {
                        items.add(c.getName(), c.getId());
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
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                if (workspaceId != null && !workspaceId.equals("")) {
                    list = meterSphereClient.getProjectIds(workspaceId);

                }
                if (list != null && list.size() > 0) {
                    for (ProjectDTO c : list) {
                        items.add(c.getName(), c.getId());
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
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择测试计划", "");
                List<TestPlanDTO> list = new ArrayList<>();
                if (projectId != null && !projectId.equals("")) {
                    list = meterSphereClient.getTestPlanIds(projectId, workspaceId);
                }
                if (list != null && list.size() > 0) {
                    for (TestPlanDTO c : list) {
                        items.add(c.getName(), c.getId());
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
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择测试名称", "");
                List<TestCaseDTO> list = new ArrayList<>();
                if (projectId != null && !projectId.equals("")) {
                    list = meterSphereClient.getTestCases(projectId);
                }
                if (list != null && list.size() > 0) {
                    for (TestCaseDTO c : list) {
                        items.add(c.getName() + "（" + c.getType() + "）", c.getId());
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
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择运行环境", "");
                List<ApiTestEnvironmentDTO> list = new ArrayList<>();
                if (projectId != null && !projectId.equals("")) {
                    list = meterSphereClient.getEnvironmentIds(projectId);
                }
                if (list != null && list.size() > 0) {
                    for (ApiTestEnvironmentDTO c : list) {
                        items.add(c.getName(), c.getId());
                    }
                }

            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;
        }

        //资源池的选择
        public ListBoxModel doFillResourcePoolIdItems(@QueryParameter String msAccessKey,
                                                      @QueryParameter String msSecretKey,
                                                      @QueryParameter String msEndpoint
        ) {
            ListBoxModel items = new ListBoxModel();
            try {
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择资源池", "");
                List<EnvironmentPoolDTO> list = meterSphereClient.getPoolEnvironmentIds();

                if (list != null && list.size() > 0) {
                    for (EnvironmentPoolDTO c : list) {
                        items.add(c.getName(), c.getId());
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

    public String getTestPlanName() {
        return testPlanName;
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

    public String getMode() {
        return mode;
    }

    public String getResourcePoolId() {
        return resourcePoolId;
    }
}