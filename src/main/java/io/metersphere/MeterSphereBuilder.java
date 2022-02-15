package io.metersphere;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.*;
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
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MeterSphereBuilder extends Builder implements SimpleBuildStep, Serializable {

    private static final String LOG_PREFIX = "[MeterSphere] ";

    private final String msEndpoint;
    private final String msAccessKey;
    private final String msSecretKey;

    private String workspaceId;
    private String projectId;
    private String testPlanId;
    private String testPlanName;
    private String testCaseId;
    private String testCaseName;
    private String method;
    private String result;
    private String mode;//运行模式
    private String resourcePoolId;//运行环境


    @DataBoundConstructor
    public MeterSphereBuilder(String msEndpoint, String msAccessKey, String msSecretKey) {
        this.msEndpoint = msEndpoint;
        this.msAccessKey = msAccessKey;
        this.msSecretKey = msSecretKey;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher,
                        @NonNull TaskListener listener) throws InterruptedException, IOException {
        MeterSphereUtils.logger = listener.getLogger();
        listener.getLogger().println("workspace=" + workspace);
        listener.getLogger().println("number=" + run.getNumber());
        listener.getLogger().println("url=" + run.getUrl());
        final MeterSphereClient client = new MeterSphereClient(this.msAccessKey, this.msSecretKey, this.msEndpoint);
        log("执行方式: " + method);
        try {
            List<TestCaseDTO> testCases;
            Optional<TestCaseDTO> firstCase;
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
                    testCases = client.getTestCases(projectId);//项目下
                    firstCase = testCases.stream()
                            .filter(testCase -> StringUtils.equals(testCaseId, testCase.getId()))
                            .findFirst();
                    if (!firstCase.isPresent()) {
                        log("测试不存在");
                        return;
                    }
                    MeterSphereUtils.getTestStepsBySingle(client, projectId, firstCase.get(), testPlanId, resourcePoolId);
                    break;
                case Method.SINGLE_NAME:
                    String testCaseName = Util.replaceMacro(this.testCaseName, env);
                    testCases = client.getTestCases(projectId);//项目下
                    firstCase = testCases.stream()
                            .filter(testCase -> StringUtils.equals(testCaseName, testCase.getId()) ||
                                    StringUtils.equals(testCaseName, testCase.getName() + " [" + testCase.getType() + "]" + " [" + testCase.getVersionName() + "]"))
                            .findFirst();

                    if (!firstCase.isPresent()) {
                        log("测试不存在");
                        return;
                    }
                    MeterSphereUtils.getTestStepsBySingle(client, projectId, firstCase.get(), testPlanId, resourcePoolId);
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
        private List<TestPlanDTO> testPlanList = new ArrayList<>();
        private List<TestCaseDTO> testList = new ArrayList<>();

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

//                testList = meterSphereClient.getTestCases(projectId);
//                testPlanList = meterSphereClient.getTestPlanIds(projectId, workspaceId);

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

                if (projectId != null && !projectId.equals("")) {
                    testPlanList = meterSphereClient.getTestPlanIds(projectId, workspaceId);
                }
                if (testPlanList != null && testPlanList.size() > 0) {
                    for (TestPlanDTO c : testPlanList) {
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
                if (projectId != null && !projectId.equals("")) {
                    testList = meterSphereClient.getTestCases(projectId);
                }
                if (testList != null && testList.size() > 0) {
                    for (TestCaseDTO c : testList) {
                        items.add(c.getName() + " [" + c.getType() + "]" + " [" + c.getVersionName() + "]", c.getId());
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

        public AutoCompletionCandidates doAutoCompleteTestPlanName(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            if (StringUtils.isBlank(value)) {
                testPlanList.stream().map(TestPlanDTO::getName).forEach(c::add);
            } else {
                testPlanList.stream().map(TestPlanDTO::getName).forEach(v -> {
                    if (v.toLowerCase().contains(value.toLowerCase())) {
                        c.add(v);
                    }
                });
            }

            return c;
        }

        public AutoCompletionCandidates doAutoCompleteTestCaseName(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            List<TestCaseDTO> list = new ArrayList<>();
            testList.forEach(test -> {
                try {
                    TestCaseDTO clone = (TestCaseDTO) BeanUtils.cloneBean(test);
                    clone.setName(clone.getName() + " [" + clone.getType() + "]" + " [" + clone.getVersionName() + "]");
                    list.add(clone);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if (StringUtils.isBlank(value)) {
                list.stream().map(TestCaseDTO::getName).forEach(c::add);
            } else {
                list.stream().map(TestCaseDTO::getName).forEach(v -> {
                    if (v.toLowerCase().contains(value.toLowerCase())) {
                        c.add(v);
                    }
                });
            }

            return c;
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


    private void log(String msg) {
        MeterSphereUtils.logger.println(LOG_PREFIX + msg);
    }

    @DataBoundSetter
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @DataBoundSetter
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @DataBoundSetter
    public void setTestPlanId(String testPlanId) {
        this.testPlanId = testPlanId;
    }

    @DataBoundSetter
    public void setTestPlanName(String testPlanName) {
        this.testPlanName = testPlanName;
    }

    @DataBoundSetter
    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    @DataBoundSetter
    public void setResult(String result) {
        this.result = result;
    }

    @DataBoundSetter
    public void setResourcePoolId(String resourcePoolId) {
        this.resourcePoolId = resourcePoolId;
    }

    @DataBoundSetter
    public void setMethod(String method) {
        this.method = StringUtils.isBlank(method) ? Method.TEST_PLAN : method;
    }

    @DataBoundSetter
    public void setMode(String mode) {
        this.mode = StringUtils.isBlank(mode) ? "serial" : mode;
    }

    @DataBoundSetter
    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
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

    public String getTestPlanId() {
        return testPlanId;
    }

    public String getTestPlanName() {
        return testPlanName;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public String getMethod() {
        return method;
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

    public String getTestCaseName() {
        return testCaseName;
    }
}