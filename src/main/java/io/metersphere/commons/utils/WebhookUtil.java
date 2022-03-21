package io.metersphere.commons.utils;

import com.alibaba.fastjson.JSONObject;
import io.metersphere.ResultHolder;
import io.metersphere.client.MeterSphereClient;
import java.io.PrintStream;

/**
 * 执行webhook回调
 *
 * @author zhuwenping
 */
public class WebhookUtil {
    private static final String LOG_PREFIX = "[MeterSphere-Webhook]";

    private static final String WEBHOOK_URL = "http://192.168.10.47/devops/api/metersphere/notify";

    private static void log(String msg) {
        MeterSphereUtils.logger.println(LOG_PREFIX + msg);
    }

    /**
     * 执行回调.
     *
     * @param meterSphereClient
     * @param reportId
]     * @param workspacePath
     */
    public static void sendReportDetail(MeterSphereClient meterSphereClient, String reportId, String workspacePath) {
        log("查询报告详情，开始=====");
        ResultHolder resultHolder = meterSphereClient.getTestPlanReportDetail(reportId);
        log("查询报告详情，结束=====");

        if (!resultHolder.isSuccess()) {
            log("查询报告详情，结果返回失败");
            return;
        }

        JSONObject jsonObject = (JSONObject) JSONObject.toJSON(resultHolder.getData());
        jsonObject.put("reportUrl", meterSphereClient.getBaseInfo() + "/#/track/testPlan/reportList");
        jsonObject.put("reportId", reportId);

        // 解析jenkins的jobName，获得工程名以及环境名
        parseWorkspace(workspacePath, jsonObject);

        log("回调的报文内容是：" + jsonObject.toJSONString());
        HttpClientUtil.post(WEBHOOK_URL, jsonObject.toJSONString());
        log("回调完成");
    }

    /**
     * 解析JobName中的工程名和环境.
     *
     * @param workspacePath
     * @param jsonObject
     */
    public static void parseWorkspace(String workspacePath, JSONObject jsonObject) {
        String jobName = workspacePath.substring(workspacePath.indexOf("/AUTO_") + 1, workspacePath.length());

        String[] jobArray = jobName.split("_");

        jsonObject.put("projectName", jobArray[2]);

        jsonObject.put("env", jobArray[3]);
    }
}
