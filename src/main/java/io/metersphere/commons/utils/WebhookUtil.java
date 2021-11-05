package io.metersphere.commons.utils;

import com.alibaba.fastjson.JSONObject;
import io.metersphere.ResultHolder;
import io.metersphere.client.MeterSphereClient;
import org.apache.commons.lang3.StringUtils;

/**
 * @author zhuwenping
 */
public class WebhookUtil {
    /**
     * 执行回调.
     *
     * @param meterSphereClient
     * @param reportId
     * @param callbackUrls
     * @param workspacePath
     */
    public static void call(MeterSphereClient meterSphereClient, String reportId, String callbackUrls, String workspacePath) {
        if (StringUtils.isEmpty(callbackUrls)) {
            return;
        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("reportUrl", meterSphereClient.getBaseInfo() + "/#/track/testPlan/reportList");

        jsonObject.put("reportId", reportId);

        parseWorkspace(workspacePath, jsonObject);

        String[] callbackUrlArray = callbackUrls.split(",");

        for (String callbackUrl : callbackUrlArray) {
            HttpClientUtil.post(callbackUrl, jsonObject.toJSONString());
        }
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

        jsonObject.put("projectName", jobArray[1]);

        jsonObject.put("env", jobArray[2]);
    }

}

