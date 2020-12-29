package io.metersphere.commons.constants;

public class ApiUrlConstants {
    public static final String USER_INFO = "/user/key/validate";//身份验证
    public static final String LIST_USER_WORKSPACE = "/workspace/list/userworkspace";//工作空间
    public static final String PROJECT_LIST_ALL = "/project/listAll";//项目列表
    public static final String TEST_CASE_LIST_METHOD = "/test/case/list/method";//项目下所有接口和性能测试
    public static final String PLAN_LIST_ALL = "/test/plan/list/all";//测试计划项目下
    public static final String TEST_PLAN_CASE_LIST = "/test/plan/case/list/node";//模块下测试用例
    public static final String _TEST_PLAN_CASE_LIST_ = "/test/plan/case/list/node/all";//模块下测试用例
    public static final String _TEST_PLAN_CASE_LIST = "/test/plan/case/list";//计划下测试用例
    public static final String PERFORMANCE_RUN = "/performance/run";//性能测试
    public static final String API_RUN = "/api/run";//api测试
    public static final String API_GET = "/api/report/get";//API测试报告（特定）
    public static final String PERFORMANCE_GET = "/performance/state/get";//性能测试测试报告（特定）
    public static final String CHANGE_STATE = "/test/plan/case/edit";//更新测试计划用例的结果
    public static final String BASE_INFO = "/system/base/info";//查询站点
}
