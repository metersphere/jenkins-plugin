package io.metersphere;

import com.alibaba.fastjson.JSON;
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
import io.metersphere.commons.model.ProjectDTO;
import io.metersphere.commons.model.TestCaseDTO;
import io.metersphere.commons.model.TestPlanDTO;
import io.metersphere.commons.model.WorkspaceDTO;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MeterSphereBuilder extends Builder implements SimpleBuildStep, Serializable {


    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";
    private final String msEndpoint;
    private final String msAccessKey;
    private final String msSecretKey;
    private final String workspaceId;
    private final String projectId;
    private PrintStream logger;
    private final String testPlanId;
    private final String testCaseNodeId;
    private final String testId;
    private final String testCaseId;
    private final String method;
    private final String result;

    @DataBoundConstructor
    public MeterSphereBuilder(String msEndpoint, String msAccessKey, String msSecretKey, String workspaceId, String projectId, PrintStream logger, String testPlanId, String testCaseNodeId, String testId, String testCaseId, String method, String result) {
        this.msEndpoint = msEndpoint;
        this.msAccessKey = msAccessKey;
        this.msSecretKey = msSecretKey;
        this.workspaceId = workspaceId;
        this.projectId = projectId;
        this.logger = logger;
        this.testPlanId = testPlanId;
        this.testCaseNodeId = testCaseNodeId;
        this.testId = testId;
        this.testCaseId = testCaseId;
        this.method = StringUtils.isBlank(method) ? Method.testPlan : method;
        this.result = result;
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
                    final List<TestCaseDTO> modelList;
                    modelList = meterSphereClient.getTestCaseIdsByPlanId(testPlanId);//测试计划下全部
                    getTestStepsByModular(meterSphereClient, modelList);
                    break;
                case Method.single:
                    List<TestCaseDTO> testCaseIds = meterSphereClient.getTestCaseIds(projectId);//项目下
                    getTestStepsBySingle(meterSphereClient, testCaseIds);
                    break;
                default:
                    log("测试用例不存在");
            }

        } catch (Exception e) {
            if (result.equals(Results.METERSPHERE)) {
                run.setResult(Result.FAILURE);
            } else {
                log("该测试用例请求未能通过，登陆MeterSphere网站查看该报告结果");
            }

        }

    }

    public void getTestStepsByModular(MeterSphereClient meterSphereClient, List<TestCaseDTO> modelList) {
        final AtomicBoolean success = new AtomicBoolean(false);
        JSON.toJSONString(modelList);
        log("testList=" + "[" + JSON.toJSONString(modelList) + "]");
        final ExecutorService testThreadPool = Executors.newFixedThreadPool(modelList.size());
        final CountDownLatch countDownLatch = new CountDownLatch(modelList.size());
        if (modelList.size() > 0) {
            for (final TestCaseDTO c : modelList) {
                if (StringUtils.equals(Results.API, c.getType())) {
                    log("接口测试[" + c.getName() + "]开始执行");
                    testThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int num = 1;
                                num = num * runApiTest(meterSphereClient, c, c.getId());
                                if (num == 0) {
                                    success.set(true);
                                }

                            } catch (Exception e) {
                                log(e.getMessage());
                            } finally {
                                countDownLatch.countDown();
                            }
                        }
                    });
                }

                if (StringUtils.equals("performance", c.getType())) {
                    log("性能测试[" + c.getName() + "]开始执行");
                    testThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int num = 1;
                                num = num * runPerformTest(meterSphereClient, c, c.getId());
                                if (num == 0) {
                                    success.set(true);
                                }
                            } catch (Exception e) {
                                log(e.getMessage());
                            } finally {
                                countDownLatch.countDown();
                            }
                        }
                    });
                }

                if (StringUtils.equals("definition", c.getType())) {
                    log("接口测试用例[" + c.getName() + "]开始执行");
                    countDownLatch.countDown();
                    testThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int num = 1;
                                num = num * runDefinition(meterSphereClient, c, c.getTestId());
                                if (num == 0) {
                                    success.set(true);
                                }
                                countDownLatch.countDown();
                            } catch (Exception e) {
                                log(e.getMessage());
                            } finally {
                                countDownLatch.countDown();
                            }
                        }
                    });
                }

                if (StringUtils.equals("scenario", c.getType())) {
                    log("接口场景测试[" + c.getName() + "]开始执行");
                    testThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int num = 1;
                                num = num * runScenario(meterSphereClient, c, c.getTestId());
                                if (num == 0) {
                                    success.set(true);
                                }
                                countDownLatch.countDown();
                            } catch (Exception e) {
                                log(e.getMessage());
                            } finally {
                                countDownLatch.countDown();
                            }
                        }
                    });
                }

                if (c.getType().equals(Results.FUNCTIONAL)) {
                    countDownLatch.countDown();
                }
            }
            try {
                countDownLatch.await();
                if (success.compareAndSet(false, true)) {
                    log("测试用例请求全部通过，可以登陆MeterSphere网站查看全部报告结果");
                } else {
                    if (result.equals(Results.METERSPHERE)) {
                        throw new MeterSphereException("测试用例未能全部完成");
                    } else {
                        log("测试用例请求未能全部通过，可以登陆MeterSphere网站查看全部报告结果");
                    }
                }
            } catch (InterruptedException e) {
                log(e.getMessage());
            } finally {

            }
        }
    }

    public void getTestStepsBySingle(MeterSphereClient meterSphereClient, List<TestCaseDTO> testCaseIds) {
        log("testList=" + "[" + JSON.toJSONString(testCaseIds) + "]");
        log("testCaseId=" + "[" + testCaseId + "]");
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
                        int num = runPerformTest(meterSphereClient, c, testCaseId);
                        if (num == 0) {
                            flag = false;
                        }
                    }
                    if (StringUtils.equals(Results.SCENARIO, c.getType())) {
                        int num = runScenario(meterSphereClient, c, testCaseId);
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
            log(c.getName() + "发生异常:" + e.getMessage());
        }
        try {
            boolean state = true;
            String apiTestState = "";
            while (state) {
                apiTestState = meterSphereClient.getApiTestState(reportId);
                log("接口测试【" + c.getName() + "】执行状态：" + apiTestState);
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

    public int runPerformTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String id) {
        String url = meterSphereClient.getBaseInfo();

        int num = 1;
        try {
            meterSphereClient.runPerformanceTest(id);
        } catch (Exception e) {
            num = 0;
            log(c.getName() + "发生异常：" + e.getMessage());
        }
        try {
            boolean state = true;
            String pfmTestState = "";
            while (state) {
                pfmTestState = meterSphereClient.getPerformanceTestState(id);
                log("性能测试【" + c.getName() + "】执行状态：" + pfmTestState);
                if (pfmTestState.equalsIgnoreCase(Results.COMPLETED)) {
                    state = false;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/performance/report/view/" + id);
                    meterSphereClient.changeState(id, Results.PASS);
                } else if (pfmTestState.equalsIgnoreCase(Results.ERROR)) {
                    state = false;
                    num = 0;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/performance/report/view/" + id);
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

    public int runScenario(MeterSphereClient meterSphereClient, TestCaseDTO c, String id) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        String reportId = null;
        try {
            reportId = meterSphereClient.runScenario(c);
        } catch (Exception e) {
            num = 0;
            log(c.getName() + "场景测试发生异常:" + e.getMessage());
        }
        try {
            boolean state = true;
            String apiTestState = "";
            while (state) {
                apiTestState = meterSphereClient.getApiScenario(reportId);
                log("场景测试【" + c.getName() + "】执行状态：" + apiTestState);
                if (apiTestState.equalsIgnoreCase(Results.SUCCESS)) {
                    state = false;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/api/automation/report");
                } else if (apiTestState.equalsIgnoreCase(Results.ERROR)) {
                    state = false;
                    num = 0;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/api/automation/report");
                }
                Thread.sleep(1000 * 60);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }

    public int runDefinition(MeterSphereClient meterSphereClient, TestCaseDTO c, String id) {
        int num = 1;
        try {
            meterSphereClient.runDefinition(c);
        } catch (Exception e) {
            num = 0;
            log(c.getName() + "测试用例发生异常:" + e.getMessage());
        }
        try {
            boolean state = true;
            String apiDefinition = "";
            String apiTestState = "";

            while (state) {
                apiDefinition = meterSphereClient.getApiTestCase(c.getId());
                apiTestState = meterSphereClient.getDefinition(apiDefinition);
                log("测试用例【" + c.getName() + "】执行状态：" + apiTestState);
                if (apiTestState.equalsIgnoreCase(Results.SUCCESS)) {
                    state = false;
                } else if (apiTestState.equalsIgnoreCase(Results.ERROR)) {
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
                return FormValidation.error("验证MeterSphere帐号失败！" + e.getMessage());
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
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;

        }

        //所属项目
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
}