package io.metersphere.v3.commons.utils;

import hudson.model.Run;
import io.metersphere.v3.client.MeterSphereClient;
import io.metersphere.v3.commons.constants.Results;
import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class MeterSphereUtils {
    public static PrintStream logger;
    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";

    private static void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }


    public static boolean runTestPlan(Run<?, ?> run, MeterSphereClient meterSphereClient, String testPlanId, String projectId, String endpoint) throws InterruptedException {
        log("测试计划开始执行");
        String id = meterSphereClient.exeTestPlan(testPlanId);
        log("生成测试报告id:" + id);
        boolean flag = true;
        boolean state = true;
        while (state) {
            String status = meterSphereClient.getStatus(id);
            if (Results.STOPPED.equalsIgnoreCase(status)) {
                flag = false;
                state = false;
                log("该测试计划已停止");
            } else if (Results.COMPLETED.equalsIgnoreCase(status)) {
                state = false;
                log("该测试计划已完成");
            } else if (Results.SUCCESS.equalsIgnoreCase(status)) {
                state = false;
                log("该测试计划已完成");
            } else if (Results.ERROR.equalsIgnoreCase(status)) {
                flag = false;
                state = false;
                log("该测试计划已完成");
            }
            Thread.sleep(5000);
        }

        String openMode = "anon";
        if (!meterSphereClient.checkLicense()) {
            openMode = "auth";
        }

        String reportView = "/#/test-plan/testPlanReportDetail?id=" + id + "&type=TEST_PLAN";
        if (StringUtils.equals(openMode, "anon")) {
            Map<String, String> params = new HashMap<>();
            params.put("projectId", projectId);
            params.put("reportId", id);
            String shareUrl = meterSphereClient.getShareInfo(params);
            reportView = "/#/share/shareReportTestPlan" + shareUrl;
        }
        log("点击链接进入测试计划报告页面:" + endpoint + reportView);
        return flag;
    }


    public static String handleTestPlanName(String name, String num) {
        return "[" + num + "] " + name;
    }

}
