package io.metersphere.v3;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.metersphere.v3.client.MeterSphereClient;
import io.metersphere.v3.commons.model.OrganizationDTO;
import io.metersphere.v3.commons.model.ProjectDTO;
import io.metersphere.v3.commons.model.TestPlanDTO;
import io.metersphere.v3.commons.utils.LogUtil;
import io.metersphere.v3.commons.utils.MeterSphereUtils;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MeterSphereBuilder extends Builder implements SimpleBuildStep, Serializable {

    private static final String LOG_PREFIX = "[MeterSphere] ";
    private MeterSphereUtils meterSphereUtils;

    private final String msEndpoint;
    private final String msAccessKey;
    private final String msSecretKey;

    private String organizationId;
    private String projectName;
    private String testPlanName;
    private String result;

    @DataBoundConstructor
    public MeterSphereBuilder(String msEndpoint, String msAccessKey, String msSecretKey) {
        this.msEndpoint = msEndpoint;
        this.msAccessKey = msAccessKey;
        this.msSecretKey = msSecretKey;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        this.meterSphereUtils = new MeterSphereUtils(listener.getLogger());
        listener.getLogger().println("workspace=" + workspace);
        listener.getLogger().println("number=" + run.getNumber());
        listener.getLogger().println("url=" + run.getUrl());
        final MeterSphereClient client = new MeterSphereClient(this.msAccessKey, this.msSecretKey, this.msEndpoint);

        try {
            EnvVars environment = run.getEnvironment(listener);

            // 找到实际的project
            String realProjectId = "";
            realProjectId = Util.replaceMacro(this.projectName, environment);
            if (StringUtils.isNotBlank(realProjectId)) {
                List<ProjectDTO> projectIds = client.getProjectIds(organizationId);
                String finalRealProjectId = realProjectId;
                Optional<ProjectDTO> project = projectIds.stream()
                        .filter(projectDTO -> projectDTO.getName().equals(finalRealProjectId) || projectDTO.getId().equals(finalRealProjectId))
                        .findFirst();
                if (project.isPresent()) {
                    realProjectId = project.get().getId();
                }
            }

            boolean result = false;
            String testPlanName = Util.replaceMacro(this.testPlanName, environment);

            List<TestPlanDTO> testPlans = client.getTestPlanIds(realProjectId);
            Optional<TestPlanDTO> first = testPlans.stream()
                    .filter(plan -> StringUtils.equals(testPlanName, meterSphereUtils.handleTestPlanName(plan.getId(), plan.getNum()))
                            || StringUtils.equals(testPlanName, meterSphereUtils.handleTestPlanName(plan.getName(), plan.getNum())))
                    .findFirst();

            if (!first.isPresent()) {
                log("测试计划不存在");
                run.setResult(Result.FAILURE);
                return;
            }
            result = meterSphereUtils.runTestPlan(run, client, first.get(), organizationId, realProjectId, msEndpoint);
            // 使用case的结果
            run.setResult(result ? Result.SUCCESS : Result.FAILURE);
        } catch (Exception e) {
            run.setResult(Result.FAILURE);
            log("该测试请求未能通过，登录MeterSphere网站查看该报告结果");
        }

    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    @Symbol("MeterSphereV3")
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private String msAccessKey;
        private String msSecretKey;
        private String msEndpoint;

        private List<TestPlanDTO> testPlanList = new ArrayList<>();
        private List<ProjectDTO> projectList = new ArrayList<>();


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
            this.msAccessKey = msAccessKey;
            this.msSecretKey = msSecretKey;
            this.msEndpoint = msEndpoint;
            try {
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                meterSphereClient.checkUser();
            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
                return FormValidation.error("验证MeterSphere帐号失败！" + e + "," + e.getMessage());
            }
            return FormValidation.ok("验证MeterSphere帐号成功！");
        }

        //用户所属组织
        public ListBoxModel doFillOrganizationIdItems(@QueryParameter String msAccessKey,
                                                      @QueryParameter String msSecretKey,
                                                      @QueryParameter String msEndpoint
        ) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择组织", "");
            if (StringUtils.isEmpty(msAccessKey) || StringUtils.isEmpty(msSecretKey) || StringUtils.isEmpty(msEndpoint)) {
                return items;
            }
            this.msAccessKey = msAccessKey;
            this.msSecretKey = msSecretKey;
            this.msEndpoint = msEndpoint;

            try {
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                List<OrganizationDTO> list;
                list = meterSphereClient.getOrganization();
                if (list != null && list.size() > 0) {
                    for (OrganizationDTO c : list) {
                        items.add(c.getName(), c.getId());
                    }
                }
            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
            return items;

        }

        public AutoCompletionCandidates doAutoCompleteProjectName(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            if (StringUtils.isBlank(value)) {
                projectList.stream().map(ProjectDTO::getName).forEach(c::add);
            } else {
                projectList.stream().map(ProjectDTO::getName).forEach(v -> {
                    if (v.toLowerCase().contains(value.toLowerCase())) {
                        c.add(v);
                    }
                });
            }
            return c;
        }


        public AutoCompletionCandidates doAutoCompleteTestPlanName(@QueryParameter String value) {

            AutoCompletionCandidates c = new AutoCompletionCandidates();

            if (StringUtils.isBlank(value)) {
                testPlanList.stream().map(model -> "[" + model.getNum() + "] " + model.getName()).forEach(c::add);
            } else {
                testPlanList.stream().map(model -> "[" + model.getNum() + "] " + model.getName()).forEach(v -> {
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
            return "MeterSphere V3";
        }

        public void doCheckSelectOrganizationOption(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return;
            }
            MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
            projectList = meterSphereClient.getProjectIds(value);
        }

        public void doCheckSelectProjectOption(@QueryParameter String value) {
            Optional<ProjectDTO> first = projectList.stream().filter(project -> StringUtils.equals(project.getName(), value)).findFirst();
            if (first.isPresent()) {
                ProjectDTO projectDTO = first.get();
                MeterSphereClient meterSphereClient = new MeterSphereClient(msAccessKey, msSecretKey, msEndpoint);
                testPlanList = meterSphereClient.getTestPlanIds(projectDTO.getId());
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    private void log(String msg) {
        meterSphereUtils.log(LOG_PREFIX + msg);
    }

    @DataBoundSetter
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }


    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


    @DataBoundSetter
    public void setTestPlanName(String testPlanName) {
        this.testPlanName = testPlanName;
    }


    @DataBoundSetter
    public void setResult(String result) {
        this.result = result;
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

    public String getOrganizationId() {
        return organizationId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getTestPlanName() {
        return testPlanName;
    }


    public String getResult() {
        return result;
    }

}
