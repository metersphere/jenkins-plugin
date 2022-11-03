package io.metersphere.commons.model;

public class MsExecResponseDTO {

    private String testId;

    private String reportId;

    private String runMode;

    public MsExecResponseDTO() {

    }

    public MsExecResponseDTO(String testId, String reportId, String runMode) {
        this.testId = testId;
        this.reportId = reportId;
        this.runMode = runMode;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }
}
