package com.metersphere.client;

import com.alibaba.fastjson.JSON;
import com.metersphere.ResultHolder;
import com.metersphere.client.model.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MeterSphereClient {


    private static final String ACCEPT = "application/json;charset=UTF-8";
    private static final Integer CONNECT_TIME_OUT = 10000;
    private static final Integer CONNECT_REQUEST_TIME_OUT = 10000;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private HttpClient httpClient;
    private String userId;

    public MeterSphereClient(String accessKey, String secretKey, String endpoint) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        /*设置http默认超时时间*/
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectTimeout(CONNECT_TIME_OUT)
                .setConnectionRequestTimeout(CONNECT_REQUEST_TIME_OUT).build();
        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig).build();
    }

    public void checkUser() {
        ResultHolder getUserResult = call(ApiUrlConstants.USER_INFO, RequestMethod.GET);
        if (!getUserResult.isSuccess()) {
            throw new MeterSphereException(getUserResult.getMessage());
        }
        this.userId = getUserResult.getData().toString();
    }

    public List<WorkspaceDTO> getWorkspace() {
        ResultHolder userPermissinResult = call(ApiUrlConstants.APPLICATION_REPOSITORY_LIST + "/" + userId, RequestMethod.GET);
        String list = JSON.toJSONString(userPermissinResult.getData());
        List<WorkspaceDTO> workspaces = JSON.parseArray(list, WorkspaceDTO.class);
        return workspaces;
    }

    public List<ProjectDTO> getProjectIds(String workspaceId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("workspaceId", workspaceId);
        ResultHolder result = call(ApiUrlConstants.APPLICATION_SETTING_GET + "/" + workspaceId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<ProjectDTO> apps = JSON.parseArray(listJson, ProjectDTO.class);
        return apps;

    }

    public List<TestCaseDTO> getTestCaseIds(String projectId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("projectId", projectId);
        ResultHolder result = call(ApiUrlConstants.APPLICATION_SETTING_LIST + "/" + projectId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestCaseDTO> apps = JSON.parseArray(listJson, TestCaseDTO.class);
        return apps;
    }

    public List<TestCaseDTO> getTestCaseIdsByNodeIds(String testPlanId, String testCaseNodeId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testPlanId", testPlanId);
        headers.put("testCaseNodeId", testCaseNodeId);
        ResultHolder result = call(ApiUrlConstants.CLUSTER_ROLE_LIST + "/" + testPlanId + "/" + testCaseNodeId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestCaseDTO> apps = JSON.parseArray(listJson, TestCaseDTO.class);
        System.out.println("模块下用例" + apps);
        return apps;
    }

    public List<TestPlanDTO> getTestPlanIds(String projectId, String workspaceId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("projectId", projectId);
        ResultHolder result = call(ApiUrlConstants.REPOSITORY_LIST + "/" + projectId + "/" + workspaceId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestPlanDTO> apps = JSON.parseArray(listJson, TestPlanDTO.class);
        return apps;
    }

    public List<TestCaseNodeDTO> getTestCaseNodeIds(String testPlanId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testPlanId", testPlanId);
        ResultHolder result = call(ApiUrlConstants.USER_PERMISSION_LIST + "/" + testPlanId, RequestMethod.GET, new HashMap<String, Object>(), headers);
        String listJson = JSON.toJSONString(result.getData());
        List<TestCaseNodeDTO> apps = JSON.parseArray(listJson, TestCaseNodeDTO.class);
        return apps;
    }

    public boolean getApiTest(String testCaseId) {
        Map<String, String> headers = new HashMap<String, String>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", testCaseId);
        params.put("triggerMode", "MANUAL");
        ResultHolder result = call(ApiUrlConstants.CLUSTER_LIST, RequestMethod.POST, params, headers);
        System.out.println(result + "api测试结果");
        boolean flag = true;
        if (!result.isSuccess()) {
            flag = false;
        }
        return flag;
    }

    public boolean getPerformanceTest(String testCaseId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testCaseId", testCaseId);
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "237e9597-45f4-48e3-8ce0-edf5712ee0cc");
        params.put("triggerMode", "MANUAL");
        ResultHolder result = call(ApiUrlConstants.APPLICATION_LIST, RequestMethod.POST, params, headers);
        boolean flag = true;
        if (!result.isSuccess()) {
            flag = false;
        }
        return flag;
    }

    public String getApiTestState(String testCaseId) {
        Map<String, String> headers = new HashMap<String, String>();
        System.out.println("状态");
        headers.put("testCaseId", testCaseId);
        ResultHolder result = call(ApiUrlConstants.API_REPORT + "/" + testCaseId, RequestMethod.GET, new HashMap<String, Object>(), headers);
/*
        String listJson = JSON.toJSONString(result.getData());
*/
     /*   System.out.println("ert");
        JSONArray jsonArray = JSONArray.parseArray(listJson);
        System.out.println("ert2");
        List<ApiTestDTO> apps = JSON.parseArray(listJson, ApiTestDTO.class);*/
        return "save";
    }

    public String getPerformanceTestState(String testCaseId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testCaseId", testCaseId);
        ResultHolder result = call(ApiUrlConstants.PERFORMANCE_REPORT, RequestMethod.GET, new HashMap<String, Object>(), headers);
        return "success";
    }

    private ResultHolder call(String url, RequestMethod requestMethod) {
        /*Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);*/
        return call(url, requestMethod, null, null);
    }

    private ResultHolder call(String url, RequestMethod requestMethod, Object params, Map<String, String> headers) {
        /*url = this.endpoint + "/" + url;*/
        String responseJson = null;
        try {
            if (requestMethod == RequestMethod.GET) {
                HttpGet httpGet = new HttpGet(url);
                auth(httpGet);
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity httpEntity = response.getEntity();
                responseJson = EntityUtils.toString(httpEntity);
            } else {
                HttpPost httpPost = new HttpPost(url);
                if (headers != null && headers.size() > 0) {
                    for (String key : headers.keySet()) {
                        httpPost.addHeader(key, headers.get(key));
                    }
                }
                if (params != null) {
                    StringEntity stringEntity = new StringEntity(JSON.toJSONString(params), "UTF-8");
                    /*设置请求参数*/
                    httpPost.setEntity(stringEntity);
                }
                auth(httpPost);
                HttpResponse response = httpClient.execute(httpPost);
                System.out.println(response + "ab");
                HttpEntity httpEntity = response.getEntity();
                System.out.println(httpEntity);
                responseJson = EntityUtils.toString(httpEntity);
                System.out.println(responseJson + "abc");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ResultHolder result = JSON.parseObject(responseJson, ResultHolder.class);
        if (!result.isSuccess()) {
            throw new MeterSphereException(result.getMessage());
        }
        return JSON.parseObject(responseJson, ResultHolder.class);
    }

    private void auth(HttpRequestBase httpRequestBase) {
        httpRequestBase.addHeader("Accept", ACCEPT);
        httpRequestBase.addHeader("accessKey", accessKey);
        String signature;
        try {
            signature = aesEncrypt(accessKey + "|" + UUID.randomUUID().toString() + "|" + System.currentTimeMillis(), secretKey, accessKey);
        } catch (Exception e) {
            throw new MeterSphereException("签名失败: " + e.getMessage());
        }
        httpRequestBase.addHeader("signature", signature);
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

class ApiUrlConstants {
    public static final String USER_INFO = "http://localhost:8081/user/key/validate";//身份验证
    public static final String APPLICATION_REPOSITORY_LIST = "http://localhost:8081/workspace/list/userworkspace";//工作空间
    public static final String APPLICATION_SETTING_GET = "http://localhost:8081/project/listAll";//项目列表
    public static final String APPLICATION_SETTING_LIST = "http://localhost:8081/test/case/list/method";//项目测试用例
    public static final String USER_PERMISSION_LIST = "http://localhost:8081/case/node/list/plan";//模块列表
    public static final String REPOSITORY_LIST = "http://localhost:8081/test/plan/list/all";//测试计划项目下
    public static final String APPLICATION_LIST = "http://localhost:8081/performance/run";//性能测试
    public static final String CLUSTER_LIST = "https://127.0.0.1:7890/api/run";//api测试
    public static final String CLUSTER_ROLE_LIST = "http://localhost:8081/test/plan/case/list";//模块下测试用例
    public static final String API_REPORT = "http://localhost:8081/api/list/all";//API测试报告（特定）
    public static final String PERFORMANCE_REPORT = "http://localhost:8081/performance/list/all";//性能测试测试报告（特定）
}

enum RequestMethod {
    GET, POST
}
