package io.metersphere.commons.utils;

import io.metersphere.client.MeterSphereClient;
import io.metersphere.commons.constants.Results;
import io.metersphere.commons.model.RunModeConfig;
import io.metersphere.commons.model.TestCaseDTO;
import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;

public class MeterSphereUtils {
    public static PrintStream logger;
    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";

    private static void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }

    public static int runApiTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String id) {
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

    public static int runPerformTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String id, String testPlanId) {
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

    public static int runScenario(MeterSphereClient meterSphereClient, TestCaseDTO c, String id, String projectId, String runMode, String resourcePoolId) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        String reportId = null;
        try {
            RunModeConfig config = null;
            if (StringUtils.isNotEmpty(resourcePoolId)) {
                config = new RunModeConfig();
                config.setResourcePoolId(resourcePoolId);
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

    public static int runDefinition(MeterSphereClient meterSphereClient, TestCaseDTO c, String id, String testPlanId, String runMode, String environmentId) {
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


}
