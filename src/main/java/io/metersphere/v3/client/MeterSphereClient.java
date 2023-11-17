package io.metersphere.v3.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.v3.ResultHolder;
import io.metersphere.v3.commons.constants.ApiUrlConstants;
import io.metersphere.v3.commons.constants.RequestMethod;
import io.metersphere.v3.commons.exception.MeterSphereException;
import io.metersphere.v3.commons.model.EnvironmentPoolDTO;
import io.metersphere.v3.commons.model.OrganizationDTO;
import io.metersphere.v3.commons.model.ProjectDTO;
import io.metersphere.v3.commons.model.TestPlanDTO;
import io.metersphere.v3.commons.utils.HttpClientConfig;
import io.metersphere.v3.commons.utils.HttpClientUtil;
import io.metersphere.v3.commons.utils.LogUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MeterSphereClient {


    private static final String ACCEPT = "application/json;charset=UTF-8";

    private final String accessKey;
    private final String secretKey;
    private final String endpoint;

    private final static ExecutorService executorService = Executors.newFixedThreadPool(5);

    public MeterSphereClient(String accessKey, String secretKey, String endpoint) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
    }

    /*校验账号*/
    public String checkUser() {
        ResultHolder getUserResult = call(ApiUrlConstants.USER_INFO);
        if (!getUserResult.isSuccess()) {
            throw new MeterSphereException(getUserResult.getMessage());
        }
        return getUserResult.getData().toString();
    }

    /*获取组织下组织*/
    public List<OrganizationDTO> getOrganization() {
        if (this.checkLicense()) {
            String url = ApiUrlConstants.LIST_USER_ORGANIZATION;
            ResultHolder result = call(url);
            String list = JSON.toJSONString(result.getData());
            LogUtil.info("用户所属组织: " + list);
            return JSON.parseArray(list, OrganizationDTO.class);
        } else {
            String url = ApiUrlConstants.LIST_DEFAULT_ORGANIZATION;
            ResultHolder result = call(url);
            String list = JSON.toJSONString(result.getData());
            LogUtil.info("用户所属组织: " + list);
            return JSON.parseArray("[" + list + "]", OrganizationDTO.class);
        }
    }

    /*获取组织下项目列表*/
    public List<ProjectDTO> getProjectIds(String organizationId) {
        ResultHolder result = call(ApiUrlConstants.PROJECT_LIST_ALL + "/" + organizationId);
        String listJson = JSON.toJSONString(result.getData());
        LogUtil.info("用户所属项目: " + listJson);
        return JSON.parseArray(listJson, ProjectDTO.class);

    }

    /*查询该项目下所有测试计划*/
    public List<TestPlanDTO> getTestPlanIds(String projectId) {
        Map<String, String> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("pageSize", "500");
        params.put("current", "1");
        params.put("type", "ALL");
        ResultHolder result = call(ApiUrlConstants.PLAN_LIST_ALL, RequestMethod.POST, params);
        String listJson = JSON.toJSONString(result.getData());
        LogUtil.info("该项目下的所有的测试计划: " + listJson);
        return JSON.parseArray(JSON.parseObject(listJson).getJSONArray("list").toJSONString(), TestPlanDTO.class);
    }

    /*资源池列表*/
    public List<EnvironmentPoolDTO> getPoolEnvironmentIds() {
        ResultHolder result = call(ApiUrlConstants.TEST_POOL);
        String listJson = JSON.toJSONString(result.getData());
        LogUtil.debug("该项目下的资源池列表" + listJson);
        return JSON.parseArray(listJson, EnvironmentPoolDTO.class);
    }

    /*执行测试计划*/
    public String exeTestPlan(String testPlanId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("executeId", testPlanId);
        params.put("executionSource", "API");

        ResultHolder result = call(ApiUrlConstants.TEST_PLAN_EXECUTE, RequestMethod.POST, params);
        if (result.getData() instanceof String) {
            return (String) result.getData();
        }
        return JSON.toJSONString(result.getData());
    }

    /*查询测试计划报告状态*/
    public String getStatus(String reportId) {
        ResultHolder result = call(ApiUrlConstants.TEST_PLAN_STATUS + "/" + reportId);
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(result.getData()));

        return jsonObject.getString("resultStatus");
    }


    public void changeState(String id, String status) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("status", status);
        call(ApiUrlConstants.CHANGE_STATE, RequestMethod.POST, params);
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
            signature = aesEncrypt(accessKey + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(), secretKey, accessKey);
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


    public boolean checkLicense() {
        ResultHolder result = call(ApiUrlConstants.LICENSE_VALIDATE);
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(result.getData()));
        return StringUtils.equals("valid", jsonObject.getString("status"));
    }

    public String getShareInfo(Map<String, String> params) {
        ResultHolder result = call(ApiUrlConstants.TEST_PLAN_REPORT_SHARE, RequestMethod.POST, params);
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(result.getData()));
        return jsonObject.getString("shareUrl");
    }
}

