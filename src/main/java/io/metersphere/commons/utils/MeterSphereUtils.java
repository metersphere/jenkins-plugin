package io.metersphere.commons.utils;

import hudson.model.Result;
import hudson.model.Run;
import io.metersphere.client.MeterSphereClient;
import io.metersphere.commons.constants.Results;
import io.metersphere.commons.model.RunModeConfig;
import io.metersphere.commons.model.TestCaseDTO;

import java.io.PrintStream;

public class MeterSphereUtils {
    public static PrintStream logger;
    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";

    private static void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }

    public static int runApiTest(MeterSphereClient meterSphereClient, TestCaseDTO c) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        String reportId = "";
        String id = c.getId();
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
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }

    public static int runPerformTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String testPlanId) {
        String url = meterSphereClient.getBaseInfo();
        String reportId = "";
        int num = 1;
        String id = c.getId();
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
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;
        }
        return num;
    }

    public static int runScenario(MeterSphereClient meterSphereClient, TestCaseDTO c, String projectId, String runMode, String resourcePoolId) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        String reportId = null;
        try {
            RunModeConfig config = new RunModeConfig();
            config.setResourcePoolId(resourcePoolId);
            config.setMode(runMode);
            config.setReportName("");
            config.setReportType("iddReport");
            config.setOnSampleError(true);
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
                } else if (apiTestState.equalsIgnoreCase(Results.FAKE_ERROR)) {
                    state = false;
                    num = 0;
                    log("点击链接进入" + c.getName() + "测试报告页面: " + url + "/#/api/automation/report/view/" + reportId.replace("\"", ""));
                }
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }

    public static int runDefinition(MeterSphereClient meterSphereClient, TestCaseDTO c, String testPlanId, String runMode) {
        int num = 1;
        String id = c.getId();
        try {
            meterSphereClient.runDefinition(c, runMode, testPlanId, id);
        } catch (Exception e) {
            num = 0;
            log(c.getName() + "测试用例发生异常:" + e.getMessage());
        }
        try {
            boolean state = true;
            String status = "";
            while (state) {
                status = meterSphereClient.getApiTestCaseReport(c.getId());
                log("测试用例[" + c.getName() + "]执行状态：" + status);
                if (status.replace("\"", "").equalsIgnoreCase("success")) {
                    state = false;
                } else if (status.replace("\"", "").equalsIgnoreCase("error")) {
                    state = false;
                    num = 0;
                } else if (status.replace("\"", "").equalsIgnoreCase(Results.FAKE_ERROR)) {
                    state = false;
                    num = 0;
                }
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }

    public static void execTestPlan(Run<?, ?> run, MeterSphereClient meterSphereClient, String projectId, String mode, String testPlanId, String resourcePoolId) throws InterruptedException {
        log("测试计划开始执行");
        String id = meterSphereClient.exeTestPlan(projectId, testPlanId, mode, resourcePoolId);
        log("生成测试报告id:" + id);
        String url = meterSphereClient.getBaseInfo();
        boolean flag = true;
        while (flag) {
            String status = meterSphereClient.getStatus(id);
            if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.SUCCESS)) {
                flag = false;
                log("该测试计划已完成");
                log("点击链接进入测试计划报告页面:" + url + "/#/track/testPlan/reportList?resourceId=" + id);
            } else if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.FAILED)) {
                flag = false;
                log("该测试计划失败");
                log("点击链接进入测试计划报告页面:" + url + "/#/track/testPlan/reportList?resourceId=" + id);
            } else if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.COMPLETED)) {
                flag = false;
                log("该测试计划已完成");
                log("点击链接进入测试计划报告页面:" + url + "/#/track/testPlan/reportList?resourceId=" + id);
            }
            Thread.sleep(5000);
        }
    }

    public static void getTestStepsBySingle(MeterSphereClient meterSphereClient, String projectId, TestCaseDTO testCase, String testPlanId, String resourcePoolId) {
        log("测试ID: " + testCase.getId());
        log("测试名称: " + testCase.getName() + " [" + testCase.getType() + "]" + " [" + testCase.getVersionName() + "]");
        boolean flag = true;
        int num = 1;
        switch (testCase.getType()) {
            case Results.API:
                num = num * MeterSphereUtils.runApiTest(meterSphereClient, testCase);
                if (num == 0) {
                    flag = false;
                }
                break;
            case Results.PERFORMANCE:
            case Results.LOAD_TEST:
                num = MeterSphereUtils.runPerformTest(meterSphereClient, testCase, "");
                if (num == 0) {
                    flag = false;
                }
                break;
            case Results.SCENARIO:
            case Results.API_SCENARIO:
                num = MeterSphereUtils.runScenario(meterSphereClient, testCase, projectId, "scenario", resourcePoolId);
                if (num == 0) {
                    flag = false;
                }
                break;
            case Results.DEFINITION:
            case Results.API_CASE:
                num = MeterSphereUtils.runDefinition(meterSphereClient, testCase, testPlanId, "JENKINS");
                if (num == 0) {
                    flag = false;
                }
                break;
        }

        if (flag) {
            log("该测试用例请求通过，登陆MeterSphere网站查看该报告结果");
        } else {
            log("该测试用例请求未能通过，登陆MeterSphere网站查看该报告结果");
        }

    }


}
