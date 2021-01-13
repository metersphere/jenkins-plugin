package io.metersphere.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.ResultHolder;
import io.metersphere.commons.constants.ApiUrlConstants;
import io.metersphere.commons.constants.RequestMethod;
import io.metersphere.commons.exception.MeterSphereException;
import io.metersphere.commons.model.*;
import io.metersphere.commons.utils.HttpClientConfig;
import io.metersphere.commons.utils.HttpClientUtil;
import io.metersphere.commons.utils.LogUtil;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MeterSphereClient {


    private static final String ACCEPT = "application/json;charset=UTF-8";

    private final String accessKey;
    private final String secretKey;
    private final String endpoint;

    public MeterSphereClient(String accessKey, String secretKey, String endpoint) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
    }

    public String checkUser() {
        ResultHolder getUserResult = call(ApiUrlConstants.USER_INFO);
        if (!getUserResult.isSuccess()) {
            throw new MeterSphereException(getUserResult.getMessage());
        }
        return getUserResult.getData().toString();
    }

    public List<WorkspaceDTO> getWorkspace() {
        String userId = this.checkUser();
        ResultHolder result = call(ApiUrlConstants.LIST_USER_WORKSPACE + "/" + userId);
        String list = JSON.toJSONString(result.getData());
        LogUtil.info("用户所属工作空间" + list);
        return JSON.parseArray(list, WorkspaceDTO.class);
    }

    public List<ProjectDTO> getProjectIds(String workspaceId) {
        ResultHolder result = call(ApiUrlConstants.PROJECT_LIST_ALL + "/" + workspaceId);
        String listJson = JSON.toJSONString(result.getData());
        LogUtil.info("用户所属项目" + listJson);
        return JSON.parseArray(listJson, ProjectDTO.class);

    }

    /*单独测试用例*/
    public List<TestCaseDTO> getTestCaseIds(String projectId) {
        ResultHolder result = call(ApiUrlConstants.TEST_CASE_LIST_METHOD + "/" + projectId);
        String listJson = JSON.toJSONString(result.getData());
        LogUtil.info("该项目下所有的接口和性能测试" + listJson);
        return JSON.parseArray(listJson, TestCaseDTO.class);
    }

    /*环境测试列表*/
    public List<ApiTestEnvironmentDTO> getEnvironmentIds(String projectId) {
        ResultHolder result = call(ApiUrlConstants.ENVIRONMEN_LIST + "/" + projectId);
        String listJson = JSON.toJSONString(result.getData());
        LogUtil.info("该项目下的环境列表" + listJson);
        return JSON.parseArray(listJson, ApiTestEnvironmentDTO.class);
    }

    public List<TestCaseDTO> getTestCaseIdsByPlanId(String testPlanId) {
        ResultHolder result;
        result = call(ApiUrlConstants._TEST_PLAN_CASE_LIST + "/" + testPlanId);
        String listJson = JSON.toJSONString(result.getData());
        return JSON.parseArray(listJson, TestCaseDTO.class);
    }

    public List<TestCaseDTO> getTestCaseIdsByNodePath(String planId, String nodePaths) {
        ResultHolder result;
        result = call(ApiUrlConstants._TEST_PLAN_CASE_LIST_ + "/" + planId + "/" + nodePaths);
        String listJson = JSON.toJSONString(result.getData());
        return JSON.parseArray(listJson, TestCaseDTO.class);
    }

    public List<TestPlanDTO> getTestPlanIds(String projectId, String workspaceId) {
        ResultHolder result = call(ApiUrlConstants.PLAN_LIST_ALL + "/" + projectId + "/" + workspaceId);
        String listJson = JSON.toJSONString(result.getData());
        LogUtil.info("该项目下的所有的测试计划" + listJson);
        return JSON.parseArray(listJson, TestPlanDTO.class);
    }

    public String runApiTest(String testCaseId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", testCaseId);
        params.put("triggerMode", "API");
        ResultHolder result = call(ApiUrlConstants.API_RUN, RequestMethod.POST, params);
        return JSON.toJSONString(result.getData());
    }

    public String runScenario(TestCaseDTO testCaseDTO, String runMode) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", UUID.randomUUID().toString());
        params.put("projectId", testCaseDTO.getProjectId());
        params.put("scenarioIds", Arrays.asList(testCaseDTO.getId()));
        params.put("executeType", "Saved");
        params.put("runMode", runMode);
        ResultHolder result = call(ApiUrlConstants.API_AUTOMATION_RUN, RequestMethod.POST, params);
        return JSON.toJSONString(result.getData());
    }

    public String getApiScenario(String id) {
        if (id.equals("") || id == null) {
            id = UUID.randomUUID().toString();
        }
        ResultHolder result = call(ApiUrlConstants.API_AUTOMATION_GETAPISCENARIO + "/" + id.replace('"', ' ').trim());
        String listJson = JSON.toJSONString(result.getData());
        JSONObject jsonObject = JSONObject.parseObject(listJson);
        return jsonObject.getString("status");
    }

    public void runDefinition(TestCaseDTO testCaseDTO, String runMode, String environmentId, String testPlanId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("caseId", testCaseDTO.getId());
        params.put("reportId", UUID.randomUUID().toString());
        params.put("runMode", runMode);
        params.put("environmentId", environmentId);
        params.put("testPlanId", testPlanId);
        call(ApiUrlConstants.API_DEFINITION_RUN, RequestMethod.POST, params);

    }

    public String getDefinition(String id) {
        if (id.equals("") || id == null) {
            id = UUID.randomUUID().toString();
        }
        ResultHolder result = call(ApiUrlConstants.API_DEFINITION + "/" + id.replace('"', ' ').trim());
        String listJson = JSON.toJSONString(result.getData());
        JSONObject jsonObject = JSONObject.parseObject(listJson);
        return jsonObject.getString("status");
    }

    public String getApiTestCaseReport(String id) {
        if (id.equals("") || id == null) {
            id = UUID.randomUUID().toString();
        }
        ResultHolder result = call(ApiUrlConstants.API_TES_RESULT + "/" + id.replace('"', ' ').trim());
        String listJson = JSON.toJSONString(result.getData());
        return listJson.replace('"', ' ').trim();
    }

    public void runPerformanceTest(String testCaseId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", testCaseId);
        params.put("triggerMode", "API");
        call(ApiUrlConstants.PERFORMANCE_RUN, RequestMethod.POST, params);
    }

    public String getApiTestState(String reportId) {
        String newReportId = reportId.replace("\"", "");
        ResultHolder result = call(ApiUrlConstants.API_GET + "/" + newReportId);
        String listJson = JSON.toJSONString(result.getData());
        JSONObject jsonObject = JSONObject.parseObject(listJson);
        return jsonObject.getString("status");
    }

    public String getPerformanceTestState(String testCaseId) {
        ResultHolder result = call(ApiUrlConstants.PERFORMANCE_GET + "/" + testCaseId);
        String listJson = JSON.toJSONString(result.getData());
        JSONObject jsonObject = JSONObject.parseObject(listJson);
        return jsonObject.getString("status");
    }

    public void changeState(String id, String status) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("status", status);
        call(ApiUrlConstants.CHANGE_STATE, RequestMethod.POST, params);
    }

    /*查询站点*/
    public String getBaseInfo() {
        BaseSystemConfigDTO baseSystemConfigDTO = new BaseSystemConfigDTO();
        ResultHolder result = call(ApiUrlConstants.BASE_INFO);
        String listJson = JSON.toJSONString(result.getData());
        JSONObject jsonObject = JSONObject.parseObject(listJson);
        return jsonObject.getString("url");
    }

    private ResultHolder call(String url) {
        return call(url, RequestMethod.GET, null);
    }

    private ResultHolder call(String url, RequestMethod requestMethod, Object params) {
        url = this.endpoint + url;
        String responseJson;

        HttpClientConfig config = auth();
        if (requestMethod.equals(RequestMethod.GET)) {
            responseJson = HttpClientUtil.get(url, config);
        } else {
            responseJson = HttpClientUtil.post(url, JSON.toJSONString(params), config);
        }

        ResultHolder result = JSON.parseObject(responseJson, ResultHolder.class);
        if (!result.isSuccess()) {
            throw new MeterSphereException(result.getMessage());
        }
        return JSON.parseObject(responseJson, ResultHolder.class);
    }

    private HttpClientConfig auth() {
        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.addHeader("Accept", ACCEPT);
        httpClientConfig.addHeader("accessKey", accessKey);
        httpClientConfig.addHeader("Content-type", "application/json");
        String signature;
        try {
            signature = aesEncrypt(accessKey + "|" + UUID.randomUUID().toString() + "|" + System.currentTimeMillis(), secretKey, accessKey);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw new MeterSphereException("签名失败: " + e.getMessage());
        }
        httpClientConfig.addHeader("signature", signature);
        return httpClientConfig;
    }

    private static String aesEncrypt(String src, String secretKey, String iv) throws Exception {
        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv1);
        byte[] encrypted = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(encrypted);
    }

}

