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
import io.metersphere.commons.exception.MeterSphereException;
import io.metersphere.commons.model.*;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
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

public class MeterSphereBuilder extends Builder implements SimpleBuildStep, Serializable {


    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";
    private final String msEndpoint;
    private final String msAccessKey;
    private final String msSecretKey;
    private final String workspaceId;
    private final String projectId;
    private final List<String> nodePaths;
    private PrintStream logger;
    private final String testPlanId;
    private final String testCaseNodeId;
    private final String testId;
    private final String testCaseId;
    private final String method;
    private boolean isTestNode;
    private boolean isTestOnly;

    @DataBoundConstructor
    public MeterSphereBuilder(String msEndpoint, String msAccessKey, String msSecretKey, String workspaceId, String projectId, List<String> nodePaths, PrintStream logger, String testPlanId, String testCaseNodeId, String testId, String testCaseId, String method) {
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
        this.method = StringUtils.isBlank(method) ? Method.node : method;
        this.isTestNode = this.method.equals(Method.node);
        this.isTestOnly = this.method.equals(Method.only);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        this.logger = listener.getLogger();
        listener.getLogger().println("workspace=" + workspace);
        listener.getLogger().println("number=" + run.getNumber());
        listener.getLogger().println("url=" + run.getUrl());
        final MeterSphereClient MeterSphereClient = new MeterSphereClient(this.msAccessKey, this.msSecretKey, this.msEndpoint);
        List<TestCaseDTO> list = new ArrayList<>();
        log("模块路径"+nodePaths);
        list=MeterSphereClient.getTestCaseIdsByNodePaths(testPlanId,nodePaths);
        if(list.size()<=0){
            log("该模块下没有测试用例");
        }
        log("该模块下的测试用例"+testCaseId);
        try {
            boolean findTestCaseId = false;
            if (nodePaths.size() >0||testCaseId.isEmpty()) {
                findTestCaseId = true;
            }
            if (!findTestCaseId) {
                throw new MeterSphereException("测试用例不存在！");
            }
        } catch (Exception e) {
            log(e.getMessage());
            run.setResult(Result.FAILURE);
        }
        log("执行方式" + method);
        try {
            switch (method) {
                case Method.node:
                    if (list != null && list.size() > 0) {
                        for (TestCaseDTO c : list) {
                            if (c.getType().equals("api")) {
                                log(c.getName()+"接口测试开始执行");
                                try {
                                    MeterSphereClient.getApiTest(c.getTestId());
                                } catch (Exception e) {
                                    log(e.getMessage());
                                }
                                String status = MeterSphereClient.getApiTestState(c.getTestId());
                                if (status.equalsIgnoreCase("Completed")) {
                                    log(c.getName()+"接口测试通过");
                                } else {
                                    throw new MeterSphereException(c.getName() + "测试用例失败，构建失败");
                                }
                            }
                            if (c.getType().equals("performance")) {
                                log(c.getName()+"性能测试开始执行");
                                try {
                                    MeterSphereClient.getPerformanceTest(c.getTestId());
                                } catch (Exception e) {
                                    log(e.getMessage());
                                }
                                MeterSphereClient.getApiTestState(c.getTestId());
                                String status = "";
                                if (status.equalsIgnoreCase("Completed")) {
                                    log(c.getName()+"性能测试通过");
                                } else {
                                    throw new MeterSphereException(c.getName()  + "测试用例失败，构建失败");
                                }
                            }
                        }
                    }
                    break;
                case Method.only:
                    list = MeterSphereClient.getTestCaseIds(projectId);
                    if (list != null && list.size() > 0) {
                        for (TestCaseDTO c : list) {
                            if (c.getTestId().equals(testCaseId)) {
                                if (c.getType().equals("api")) {
                                    try {
                                        log(c.getName()+"接口测试");
                                        MeterSphereClient.getApiTest(testCaseId);
                                    } catch (Exception e) {
                                        log(e.getMessage());
                                    }
                                    String status = MeterSphereClient.getApiTestState(c.getTestId());
                                    if (status.equalsIgnoreCase("Completed")) {
                                        log(c.getName()+"测试用例通过");
                                    } else {
                                        throw new MeterSphereException(c.getName() + "接口测试用例失败，构建失败");
                                    }
                                }
                                if (c.getType().equals("performance")) {
                                    try {
                                        log(c.getName()+"性能测试执行");
                                        MeterSphereClient.getPerformanceTest(c.getTestId());

                                    } catch (Exception e) {
                                        log(e.getMessage());
                                    }
                                    String status = MeterSphereClient.getPerformanceTestState(c.getTestId());
                                    if (status.equalsIgnoreCase("Completed")) {
                                        log(c.getName()+"测试用例通过");
                                    } else {
                                        throw new MeterSphereException(c.getName() + "性能测试用例失败，构建失败");
                                    }
                                }
                            } /*else {
                                throw new CodeDeployException("传值有误");
                            }*/
                        }
                    } else {
                        throw new MeterSphereException("测试用例不存在");
                    }
                    break;
                default:
                    log("测试用例不存在");
            }

        } catch (Exception e) {
            log(e.getMessage());
            run.setResult(Result.FAILURE);
        }

    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        String userId = "";

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
                return FormValidation.error(e.getMessage());
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

        //该项目下所有的测试用例TestCaseId
        public ListBoxModel doFillTestCaseIdItems(@QueryParameter String msAccessKey,
                                                  @QueryParameter String msSecretKey,
                                                  @QueryParameter String msEndpoint,
                                                  @QueryParameter String projectId

        ) {
            ListBoxModel items = new ListBoxModel();
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择测试用例", "");
                List<TestCaseDTO> list = new ArrayList<>();
                if (projectId != null && !projectId.equals("")) {
                    list = MeterSphereClient.getTestCaseIds(projectId);
                }
                if (list != null && list.size() > 0) {
                    for (TestCaseDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getTestId()));
                    }
                }

            } catch (Exception e) {

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

        //该测试计划下的所有模块
        public ListBoxModel doFillTestCaseNodeIdItems(@QueryParameter String msAccessKey,
                                                      @QueryParameter String msSecretKey,
                                                      @QueryParameter String msEndpoint,
                                                      @QueryParameter String testPlanId) {
            ListBoxModel items = new ListBoxModel();
            try {
                MeterSphereClient MeterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                items.add("请选择测试模块", "");
                List<TestCaseNodeDTO> list = new ArrayList<>();
                if (testPlanId != null && !testPlanId.equals("")) {
                    list = MeterSphereClient.getTestCaseNodeIds(testPlanId);

                }
                if (list != null && list.size() > 0) {
                    for (TestCaseNodeDTO c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
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

    public List<String> getNodePaths() {
        return nodePaths;
    }
    public PrintStream getLogger() {
        return logger;
    }

    public String getTestPlanId() {
        return testPlanId;
    }

    public String getTestCaseNodeId() {
        return testCaseNodeId;
    }

    public String getTestId() {
        return testId;
    }

    private void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public String getMethod() {
        return method;
    }

    public boolean isTestNode() {
        return isTestNode;
    }

    public boolean isTestOnly() {
        return isTestOnly;
    }
}

