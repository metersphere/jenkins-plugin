package io.metersphere.v3.commons.utils;

import hudson.model.Run;
import io.metersphere.v3.client.MeterSphereClient;
import io.metersphere.v3.commons.constants.Results;
import io.metersphere.v3.commons.model.TestPlanDTO;
import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class MeterSphereUtils {
    public PrintStream logger;
    private static final String LOG_PREFIX = "[MeterSphere，代码测试]";

    public MeterSphereUtils(PrintStream logger) {
        this.logger = logger;
    }

    public void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }


    public boolean runTestPlan(Run<?, ?> run, MeterSphereClient meterSphereClient, TestPlanDTO testPlan, String organizationId, String projectId, String endpoint) throws InterruptedException {
        log("测试计划开始执行");
        String id = meterSphereClient.exeTestPlan(testPlan.getId());
        log("生成测试报告id: " + id + "，测试计划: " + testPlan.getName() + "，类型: " + testPlan.getType());
        boolean flag = true;
        boolean state = true;
        while (state) {
            // 避免报告还没入库
            Thread.sleep(5000);

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
        }

        String openMode = "anon";
        if (!meterSphereClient.checkLicense()) {
            openMode = "auth";
        }
        String reportView = String.format("/#/test-plan/testPlanReportDetail?id=%s&type=%s&pId=%s&orgId=%s", id, testPlan.getType(), projectId, organizationId);
        if (StringUtils.equals(openMode, "anon")) {
            Map<String, String> params = new HashMap<>();
            params.put("projectId", projectId);
            params.put("reportId", id);
            String shareUrl = meterSphereClient.getShareInfo(params);
            reportView = String.format("/#/share/shareReportTestPlan%s&type=%s&pId=%s&orgId=%s", shareUrl, testPlan.getType(), projectId, organizationId);
        }
        log("点击链接进入测试计划报告页面:" + StringUtils.stripEnd(endpoint, "/") + reportView);
        return flag;
    }


    public String handleTestPlanName(String name, String num) {
        return "[" + num + "] " + name;
    }

}
