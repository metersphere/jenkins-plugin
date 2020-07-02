package io.metersphere.commons.constants;

public class ApiUrlConstants {
    public static final String USER_INFO = "http://localhost:8081/user/key/validate";//身份验证
    public static final String APPLICATION_REPOSITORY_LIST = "http://localhost:8081/workspace/list/userworkspace";//工作空间
    public static final String APPLICATION_SETTING_GET = "http://localhost:8081/project/listAll";//项目列表
    public static final String APPLICATION_SETTING_LIST = "http://localhost:8081/test/case/list/method";//项目测试用例
    public static final String USER_PERMISSION_LIST = "http://localhost:8081/case/node/list/plan";//模块列表
    public static final String REPOSITORY_LIST = "http://localhost:8081/test/plan/list/all";//测试计划项目下
    public static final String APPLICATION_LIST = "http://localhost:8081/performance/run";//性能测试
    public static final String CLUSTER_LIST = "http://localhost:8081/api/run";//api测试
    public static final String CLUSTER_ROLE_LIST = "http://localhost:8081/test/plan/case/list";//模块下测试用例
    public static final String API_REPORT = "http://localhost:8081/api/list/all";//API测试报告（特定）
    public static final String PERFORMANCE_REPORT = "http://localhost:8081/performance/list/all";//性能测试测试报告（特定）
}
