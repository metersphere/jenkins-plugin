package io.metersphere.commons.model;

public class RunModeConfig {
    private String mode;
    private String reportType;
    private String reportName;
    private String reportId;
    private boolean onSampleError;
    private String resourcePoolId;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public boolean isOnSampleError() {
        return onSampleError;
    }

    public void setOnSampleError(boolean onSampleError) {
        this.onSampleError = onSampleError;
    }

    public String getResourcePoolId() {
        return resourcePoolId;
    }

    public void setResourcePoolId(String resourcePoolId) {
        this.resourcePoolId = resourcePoolId;
    }
}
