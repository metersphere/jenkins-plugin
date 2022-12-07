package io.metersphere.commons.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import hudson.model.Run;
import io.metersphere.client.MeterSphereClient;
import io.metersphere.commons.constants.Results;
import io.metersphere.commons.model.MsExecResponseDTO;
import io.metersphere.commons.model.RunModeConfig;
import io.metersphere.commons.model.TestCaseDTO;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeterSphereUtils {
    public static PrintStream logger;
    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";

    private static void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }

    public static int runUiTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String openMode) {
        String url = meterSphereClient.getBaseInfo();
        int num = 1;
        String result = "";
        String id = c.getId();
        try {
            result = meterSphereClient.runUiTest(id, c.getProjectId());
        } catch (Exception e) {
            num = 0;
        }
        try {
            List<MsExecResponseDTO> dto = JSON.parseObject(result, new TypeReference<List<MsExecResponseDTO>>() {
            });
            String reportId = dto.get(0).getReportId();
            String reportView = "/#/ui/report/view/" + reportId.replace("\"", "");
            boolean state = true;
            String apiTestState = "";
            while (state) {
                apiTestState = meterSphereClient.getUiTestState(reportId);
                if (apiTestState.equalsIgnoreCase(Results.SUCCESS)) {
                    state = false;
                    meterSphereClient.changeState(id, Results.PASS);
                } else if (apiTestState.equalsIgnoreCase(Results.ERROR)) {
                    state = false;
                    num = 0;
                    meterSphereClient.changeState(id, Results.FAILURE);
                }
                Thread.sleep(5000);
            }
            if (StringUtils.equals(openMode, "anon")) {
                Map<String, String> params = new HashMap<>();
                params.put("customData", reportId);
                params.put("shareType", "UI_REPORT");
                String shareUrl = meterSphereClient.getShareInfo(params);
                reportView = "/ui/shareUiReport" + shareUrl;
            }
            log("点击链接进入" + c.getName() + "测试报告页面:" + url + reportView);
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;

        }
        return num;
    }

    public static int runPerformTest(MeterSphereClient meterSphereClient, TestCaseDTO c, String testPlanId, String openMode) {
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
            String reportView = "/#/performance/report/view/" + reportId.replace("\"", "");
            boolean state = true;
            String pfmTestState = "";
            while (state) {
                pfmTestState = meterSphereClient.getPerformanceTestState(id);
                log("性能测试[" + c.getName() + "]执行状态：" + pfmTestState);
                if (pfmTestState.equalsIgnoreCase(Results.COMPLETED)) {
                    //更新测试计划下性能测试状态
                    meterSphereClient.updateStateLoad(testPlanId, id, "success");
                    state = false;
                    meterSphereClient.changeState(id, Results.PASS);
                } else if (pfmTestState.equalsIgnoreCase(Results.ERROR)) {
                    //更新测试计划下性能测试状态
                    meterSphereClient.updateStateLoad(testPlanId, id, "Error");
                    state = false;
                    num = 0;
                    meterSphereClient.changeState(id, Results.FAILURE);
                }
                Thread.sleep(5000);
            }
            if (StringUtils.equals(openMode, "anon")) {
                Map<String, String> params = new HashMap<>();
                params.put("customData", reportId);
                params.put("shareType", "PERFORMANCE_REPORT");
                String shareUrl = meterSphereClient.getShareInfo(params);
                reportView = "/performance/share-report" + shareUrl;
            }
            log("点击链接进入" + c.getName() + "测试报告页面: " + url + reportView);
        } catch (InterruptedException e) {
            log(c.getName() + "发生异常：" + e.getMessage());
            num = 0;
        }
        return num;
    }

    public static int runScenario(MeterSphereClient meterSphereClient, TestCaseDTO c, String projectId, String runMode, String resourcePoolId, String openMode) {
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
            String reportView = "/#/api/automation/report/view/" + reportId.replace("\"", "");
            boolean state = true;
            String apiTestState = "";
            while (state) {
                apiTestState = meterSphereClient.getApiScenario(reportId);
                log("场景测试[" + c.getName() + "]执行状态：" + apiTestState);
                if (apiTestState.equalsIgnoreCase(Results.SUCCESS)) {
                    state = false;
                } else if (apiTestState.equalsIgnoreCase(Results.ERROR)) {
                    state = false;
                    num = 0;
                } else if (apiTestState.equalsIgnoreCase(Results.FAKE_ERROR)) {
                    state = false;
                    num = 0;
                }
                Thread.sleep(5000);
            }
            if (StringUtils.equals(openMode, "anon")) {
                Map<String, String> params = new HashMap<>();
                params.put("customData", reportId);
                params.put("shareType", "API_REPORT");
                String shareUrl = meterSphereClient.getShareInfo(params);
                reportView = "/api/share-api-report" + shareUrl;
            }
            log("点击链接进入" + c.getName() + "测试报告页面: " + url + reportView);
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
                } else if (status.replace("\"", "").equalsIgnoreCase(Results.FAKE_ERROR) || status.replace("\"", "").equalsIgnoreCase("errorReportResult")) {
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

    public static void runTestPlan(Run<?, ?> run, MeterSphereClient meterSphereClient, String projectId,
                                   String mode, String testPlanId, String resourcePoolId, String openMode) throws InterruptedException {
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
            } else if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.FAILED)) {
                flag = false;
                log("该测试计划失败");
            } else if (status.replace('"', ' ').trim().equalsIgnoreCase(Results.COMPLETED)) {
                flag = false;
                log("该测试计划已完成");
            }
            Thread.sleep(5000);
        }
        String reportView = "/#/track/testPlan/reportList?resourceId=" + id;
        if (StringUtils.equals(openMode, "anon")) {
            Map<String, String> params = new HashMap<>();
            params.put("customData", id);
            params.put("shareType", "PLAN_DB_REPORT");
            String shareUrl = meterSphereClient.getShareInfo(params);
            reportView = "/track/share-plan-report" + shareUrl;
        }
        log("点击链接进入测试计划报告页面:" + url + reportView);
    }

    public static void getTestStepsBySingle(MeterSphereClient meterSphereClient, String projectId, TestCaseDTO testCase,
                                            String testPlanId, String resourcePoolId, String openMode) {
        log("测试ID: " + testCase.getId());
        log("测试名称: " + testCase.getName() + " [" + testCase.getType() + "]" + " [" + testCase.getVersionName() + "]");
        boolean flag = true;
        int num = 1;
        switch (testCase.getType()) {
            case Results.PERFORMANCE:
            case Results.LOAD_TEST:
                num = MeterSphereUtils.runPerformTest(meterSphereClient, testCase, "", openMode);
                if (num == 0) {
                    flag = false;
                }
                break;
            case Results.SCENARIO:
            case Results.API_SCENARIO:
                num = MeterSphereUtils.runScenario(meterSphereClient, testCase, projectId, "scenario", resourcePoolId, openMode);
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
            case Results.UI:
                num = MeterSphereUtils.runUiTest(meterSphereClient, testCase, openMode);
                if (num == 0) {
                    flag = false;
                }
        }

        if (flag) {
            log("该测试用例请求通过，登陆MeterSphere网站查看该报告结果");
        } else {
            log("该测试用例请求未能通过，登陆MeterSphere网站查看该报告结果");
        }

    }


}
