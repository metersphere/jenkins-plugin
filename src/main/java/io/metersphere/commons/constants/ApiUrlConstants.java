package io.metersphere.commons.constants;

public class ApiUrlConstants {
    public static final String USER_INFO = "/user/key/validate";//身份验证
    public static final String LIST_USER_WORKSPACE = "/workspace/list/userworkspace";//所属工作空间
    public static final String PROJECT_LIST_ALL = "/project/list/related";//项目列表
    public static final String TEST_CASE_LIST_METHOD = "/test/case/list/method";//项目下所有接口和性能测试
    public static final String PLAN_LIST_ALL = "/test/plan/list/all";//测试计划项目下
    public static final String PERFORMANCE_RUN = "/performance/run";//性能测试单个
    public static final String PERFORMANCE_RUN_TEST_PLAN_STATE = "/test/plan/load/case/update/api";//性能测试测试计划下状态修改
    public static final String API_RUN = "/api/run";//api测试
    public static final String API_GET = "/api/report/get";//API测试报告（特定）
    public static final String PERFORMANCE_GET = "/performance/state/get";//性能测试测试报告（特定）
    public static final String CHANGE_STATE = "/test/plan/case/edit";//更新测试计划用例的结果
    public static final String BASE_INFO = "/system/base/info";//查询站点
    public static final String API_AUTOMATION_RUN = "/test/plan/scenario/case/jenkins/run";//测试计划下场景运行
    public static final String API_AUTOMATION_RUN_SINGLE = "/api/automation/run/jenkins";//单个场景运行
    public static final String API_AUTOMATION_GETAPISCENARIO = "/api/scenario/report/get";//报告详情
    public static final String API_DEFINITION_RUN = "/api/testcase/jenkins/run";//接口测试用例运行
    public static final String API_DEFINITION = "/api/definition/get";//接口测详情
    public static final String API_TES_RESULT = "/api/testcase/findById";//查询单接口用例执行结果
    public static final String API_TES_RESULT_TEST = "/api/testcase/getStateByTestPlan";//计划下接口用例执行结果
    public static final String ENVIRONMEN_LIST = "/api/environment/list";//环境列表
    public static final String TEST_POOL = "/testresourcepool/list/quota/valid";
    public static final String TEST_PLAN = "/test/plan/run";//测试计划执行
    public static final String TEST_PLAN_STATUS = "/test/plan/report/status";//测试计划执行

}
