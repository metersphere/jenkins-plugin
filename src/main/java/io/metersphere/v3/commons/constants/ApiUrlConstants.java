package io.metersphere.v3.commons.constants;

public class ApiUrlConstants {
    public static final String USER_INFO = "/user/api/key/validate";//身份验证
    public static final String LIST_USER_ORGANIZATION = "/system/organization/switch-option";//所属组织
    public static final String LIST_DEFAULT_ORGANIZATION = "/system/organization/default";//所属组织
    public static final String PROJECT_LIST_ALL = "/project/list/options";//项目列表
    public static final String PLAN_LIST_ALL = "/test-plan/page";//测试计划项目下
    public static final String CHANGE_STATE = "/track/test/plan/case/edit";//更新测试计划用例的结果
    public static final String BASE_INFO = "/setting/system/base/info";//查询站点
    public static final String LICENSE_VALIDATE = "/license/validate";//查询LICENSE
    public static final String TEST_POOL = "/setting/testresourcepool/list/quota/valid";
    public static final String TEST_PLAN = "/track/test/plan/run";//测试计划执行
    public static final String TEST_PLAN_STATUS = "/track/test/plan/report/status";//测试计划执行

}
