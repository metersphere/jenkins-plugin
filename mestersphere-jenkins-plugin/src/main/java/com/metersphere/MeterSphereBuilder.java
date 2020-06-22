package com.metersphere;

import hudson.Extension;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;


import javax.servlet.ServletException;
import java.io.IOException;


public class MeterSphereBuilder extends Builder implements SimpleBuildStep {
    private static final String LOG_PREFIX = "[MeterSphere测试]";
    private final String serverUrl;
    private final String credentialsId;
    private final String workspaceId;
    private final String projectId;
    private final String testPlan;
    private final String name;

    @DataBoundConstructor
    public MeterSphereBuilder(String serverUrl, String credentialsId, String workspaceId, String applicationId, String projectId, String testPlan, String name) {
        this.serverUrl = serverUrl;
        this.credentialsId = credentialsId;
        this.workspaceId = workspaceId;
        this.projectId = projectId;
        this.testPlan = testPlan;
        this.name = name;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private boolean useFrench;

        private String content;

        public String getContent() {
            return content;
        }
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "MeterSphere";
        }
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
        public boolean getUseFrench() {
            return useFrench;
        }
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
        if (getDescriptor().getUseFrench())
            listener.getLogger().println("Bonjour, " + name + "!");
        else {
            listener.getLogger().println("Hello, " + name + "!");
        }
        listener.getLogger().println("workspace=" + workspace);
        listener.getLogger().println("number=" + build.getNumber());
        listener.getLogger().println("url=" + build.getUrl());




    }
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    public String getCredentialsId() {
        return credentialsId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }


    public String getProjectId() {
        return projectId;
    }

    public String getTestPlan() {
        return testPlan;
    }

    public String getName() {
        return name;
    }


}
