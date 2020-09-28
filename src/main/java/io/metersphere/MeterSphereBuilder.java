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
import io.metersphere.commons.exception.MeterSphereException;
import io.metersphere.commons.model.ProjectDTO;
import io.metersphere.commons.model.TestCaseDTO;
import io.metersphere.commons.model.TestPlanDTO;
import io.metersphere.commons.model.WorkspaceDTO;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
    private final String nodePaths;
    private PrintStream logger;
    private final String testPlanId;
    private final String testCaseNodeId;
    private final String testId;
    private final String testCaseId;
    private final String method;
    private final String result;

    @DataBoundConstructor
    public MeterSphereBuilder(String msEndpoint, String msAccessKey, String msSecretKey, String workspaceId, String projectId, String nodePaths, PrintStream logger, String testPlanId, String testCaseNodeId, String testId, String testCaseId, String method, String result) {
        this.msEndpoint = msEndpoint;
        this.msAccessKey = msAccessKey;
        this.msSecretKey = msSecretKey;
        this.workspaceId = workspaceId;
        this.projectId = projectId;
        this.nodePaths = nodePaths;
        this.logger = logger;
        this.testPlanId = testPlanId;
        this.testCaseNodeId = testCaseNodeId;
        this.testId = testId;
        this.testCaseId = testCaseId;
        this.method = StringUtils.isBlank(method) ? Method.modular : method;
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
                case Method.modular:
                    String nodePath = "";
                    final List<TestCaseDTO> modelList;
                    if (StringUtils.contains(nodePaths, "/*")) {
                        nodePath = nodePaths.replace("*", "").replace("/", "f");
                        modelList = meterSphereClient.getTestCaseIdsByNodePath(testPlanId, nodePath);//模块下全部
                        if (modelList.size() <= 0) {
                            log("您所选模块下没有相应的接口和性能测试，请检查所填模块是否正确");
                        }

                    } else {
                        nodePath = nodePaths.replace("/", "f");
                        modelList = meterSphereClient.getTestCaseIdsByNodePaths(testPlanId, nodePath);//模块下
                        if (modelList.size() <= 0) {
                            log("您所选模块下没有相应的接口和性能测试，请检查所填模块是否正确");
                        }

                    }
                    final ExecutorService testThreadPool = Executors.newFixedThreadPool(modelList.size());
                    final CountDownLatch countDownLatch = new CountDownLatch(modelList.size());
                    final AtomicBoolean success = new AtomicBoolean(false);
                    if (modelList.size() > 0) {
                        for (final TestCaseDTO c : modelList) {
                            if (c.getType().equals("api")) {
                                testThreadPool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        String reportId = "";
                                        try {
                                            log("开始执行接口测试:  " + c.getName());
                                            reportId = meterSphereClient.runApiTest(c.getTestId());
                                            log("更新测试用例结果：" + c.getName());
                                            meterSphereClient.changeState(c.getId(), "Failure");
                                        } catch (Exception e) {
                                            success.set(true);
                                            log(c.getName() + "发生异常：" + e.getMessage());
                                        }
                                        try {
                                            int count = 10;
                                            String apiTestState = "";
                                            while (count > 0) {
                                                log("开始请求api状态：" + c.getName());
                                                apiTestState = meterSphereClient.getApiTestState(reportId);
                                                log(c.getName() + "api执行状态：" + apiTestState);
                                                if (apiTestState.equalsIgnoreCase("Success")) {
                                                    count = 1;
                                                    log("更新测试用例结果：" + c.getName());
                                                    meterSphereClient.changeState(c.getId(), "Pass");
                                                } else if (apiTestState.equalsIgnoreCase("error")) {
                                                    count = 1;
                                                    success.set(true);
                                                    log("更新测试用例结果：" + c.getName());
                                                    meterSphereClient.changeState(c.getId(), "Failure");
                                                } else {
                                                    log("更新测试用例结果：" + c.getName());
                                                    meterSphereClient.changeState(c.getId(), "Failure");
                                                }
                                                count--;
                                                Thread.sleep(1000 * 2L);
                                            }
                                            if (count == 0) {
                                                if (!apiTestState.equalsIgnoreCase("Success")) {
                                                    log(c.getName() + "：api请求状态" + apiTestState);
                                                    success.set(true);


                                                }
                                            }
                                        } catch (InterruptedException e) {
                                            log(c.getName() + "发生异常：" + e.getMessage());
                                            success.set(true);
                                        } finally {
                                            countDownLatch.countDown();
                                        }

                                    }
                                });
                            }

                            if (c.getType().equals("performance")) {
                                testThreadPool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            log("开始执行性能测试:" + c.getName());
                                            meterSphereClient.runPerformanceTest(c.getTestId());
                                        } catch (Exception e) {
                                            success.set(true);
                                            log(c.getName() + "发生异常：" + e.getMessage());
                                            log("更新测试用例结果：" + c.getName());
                                            meterSphereClient.changeState(c.getId(), "Failure");
                                        }
                                        try {
                                            int count = 10;
                                            String pfmTestState = "";
                                            while (count > 0) {
                                                log("开始请求性能测试状态：" + c.getName());
                                                pfmTestState = meterSphereClient.getPerformanceTestState(c.getTestId());
                                                log(c.getName() + "性能执行状态" + pfmTestState);
                                                if (pfmTestState.equalsIgnoreCase("Running")) {
                                                    count = 1;
                                                    log("更新测试用例结果：" + c.getName());
                                                    meterSphereClient.changeState(c.getId(), "Pass");
                                                } else if (pfmTestState.equalsIgnoreCase("error")) {
                                                    count = 1;
                                                    success.set(true);
                                                    log("更新测试用例结果：" + c.getName());
                                                    meterSphereClient.changeState(c.getId(), "Failure");

                                                }
                                                count--;
                                                Thread.sleep(1000 * 4L);
                                            }
                                            if (count == 0) {
                                                if (!pfmTestState.equalsIgnoreCase("Running")) {
                                                    success.set(true);
                                                    log("更新测试用例结果：" + c.getName());
                                                    meterSphereClient.changeState(c.getId(), "Failure");
                                                }
                                            }
                                        } catch (InterruptedException e) {
                                            log(c.getName() + "发生异常：" + e.getMessage());
                                            success.set(true);
                                        } finally {
                                            countDownLatch.countDown();
                                        }
                                    }
                                });
                            }

                            if (c.getType().equals("functional")) {
                                countDownLatch.countDown();
                            }
                        }
                        try {
                            countDownLatch.await();
                            if (success.compareAndSet(false, true)) {
                                log("测试用例请求全部通过，登陆MeterSphere网站查看该报告结果");
                            } else {
                                if (result.equals("metersphere")) {
                                    throw new MeterSphereException("测试用例未能全部完成");
                                } else {
                                    log("测试用例请求未能全部通过，登陆MeterSphere网站查看该报告结果");
                                }
                            }
                        } catch (InterruptedException e) {
                            log(e.getMessage());
                        } finally {

                        }
                    }
                    break;
                case Method.single:
                    boolean flag = true;
                    List<TestCaseDTO> testCaseIds = meterSphereClient.getTestCaseIds(projectId);//项目下
                    if (CollectionUtils.isNotEmpty(testCaseIds)) {
                        for (TestCaseDTO c : testCaseIds) {
                            if (StringUtils.equals(testCaseId, c.getId())) {
                                if (StringUtils.equals("api", c.getType())) {
                                    String reportId = "";
                                    try {
                                        log("开始执行接口测试:" + c.getName());
                                        reportId = meterSphereClient.runApiTest(testCaseId);
                                    } catch (Exception e) {
                                        flag = false;
                                        log(c.getName() + "发生异常：" + e.getMessage());
                                    }
                                    String apiTestState = "";
                                    try {
                                        int count = 10;
                                        while (count > 0) {
                                            log("开始请求api状态：" + c.getName());
                                            apiTestState = meterSphereClient.getApiTestState(reportId);
                                            log(c.getName() + "api执行状态：" + apiTestState);
                                            if (apiTestState.equalsIgnoreCase("Success")) {
                                                count = 1;
                                            } else if (apiTestState.equalsIgnoreCase("error")) {
                                                count = 1;
                                                flag = false;

                                            }
                                            count--;
                                            Thread.sleep(1000 * 2L);
                                        }
                                        if (count == 0) {
                                            if (!apiTestState.equalsIgnoreCase("Success")) {
                                                flag = false;
                                            }
                                        }
                                    } catch (Exception e) {
                                        flag = false;
                                        log(c.getName() + "发生异常：" + e.getMessage());
                                    }
                                }
                                if (StringUtils.equals("perform", c.getType())) {
                                    try {
                                        log("开始执行性能测试:" + c.getName());
                                        meterSphereClient.runPerformanceTest(testCaseId);
                                    } catch (Exception e) {
                                        flag = false;
                                        log(c.getName() + "发生异常：" + e.getMessage());
                                    }
                                    String pfmTestState = "";
                                    try {
                                        int count = 20;
                                        while (count-- > 0) {
                                            log("开始请求性能测试状态：" + c.getName());
                                            pfmTestState = meterSphereClient.getPerformanceTestState(testCaseId);
                                            log(c.getName() + "性能执行状态" + pfmTestState);
                                            if (pfmTestState.equalsIgnoreCase("Running")) {
                                                count = 1;
                                            } else if (pfmTestState.equalsIgnoreCase("error")) {
                                                count = 1;
                                                flag = false;

                                            }
                                            count--;
                                            Thread.sleep(1000 * 4L);
                                        }
                                        if (count == 0) {
                                            if (!pfmTestState.equalsIgnoreCase("Running")) {
                                                flag = false;
                                            }
                                        }

                                    } catch (Exception e) {
                                        flag = false;
                                        log(c.getName() + "发生异常：" + e.getMessage());
                                    }

                                }
                            }
                        }
                        if (flag) {
                            log("该测试用例请求通过，登陆MeterSphere网站查看该报告结果");
                        } else {
                            if (result.equals("metersphere")) {
                                throw new MeterSphereException("该测试用例未能完成");
                            } else {
                                log("该测试用例请求未能通过，登陆MeterSphere网站查看该报告结果");
                            }
                        }
                    }
                    break;
                default:
                    log("测试用例不存在");
            }

        } catch (Exception e) {
            if (result.equals("metersphere")) {
                run.setResult(Result.FAILURE);
            } else {
                log("该测试用例请求未能通过，登陆MeterSphere网站查看该报告结果");
            }

        }

    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }


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
                return FormValidation.error("验证MeterSphere帐号失败！");
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
//            		e.printStackTrace();
//                return FormValidation.error(e.getMessage());
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

    public String getNodePaths() {
        return nodePaths;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public String getResult() {
        return result;
    }
}