package cn.enjoy.sys.controller;

import cn.enjoy.core.utils.GridModel;
import cn.enjoy.core.utils.response.HttpResponseBody;
import cn.enjoy.sys.model.Department;
import cn.enjoy.sys.model.SysRole;
import cn.enjoy.sys.model.SysUser;
import cn.enjoy.sys.service.IDepartmentService;
import cn.enjoy.sys.service.IRoleService;
import cn.enjoy.sys.service.IUserService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 用户管理
 *
 * @author Jack
 */
@RestController
@RequestMapping("/api/user/")
public class UserController extends BaseController {


    @Autowired
    private IRoleService iRoleService;

    @Autowired
    private IUserService iUserService;

    @Autowired
    private IDepartmentService iDepartmentService;

    /**
     * 新增用户
     *
     * @param user
     * @param departIds
     * @param roleIds
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @PostMapping("addUser")
    public HttpResponseBody addUser(SysUser user, @RequestParam(name = "departIds", required = false, defaultValue = "") String[] departIds, @RequestParam(name = "roleIds", required = false, defaultValue = "") String[] roleIds) {
        iUserService.addUser(user, getSessionUserId(), JSONObject.toJSONString(departIds), JSONObject.toJSONString(roleIds));
        return HttpResponseBody.successResponse("新增成功");
    }

    /**
     * 查询用户列表
     *
     * @param param
     * @param page
     * @param pageSize
     * @param sidx
     * @param sord
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @GetMapping("queryUserPage")
    public HttpResponseBody<GridModel<SysUser>> searchUsers(@RequestParam(required = false, defaultValue = "") String param, @RequestParam(required = false, defaultValue = "0") int page,
                                                            @RequestParam(required = false, defaultValue = "10") int pageSize, @RequestParam(required = false, defaultValue = "") String sidx, @RequestParam(required = false, defaultValue = "") String sord) {
        GridModel<SysUser> sysUserGridModel = iUserService.queryByPage(param, "", page, pageSize, sidx, sord);
        //return sysUserGridModel;
        return HttpResponseBody.successResponse("查询成功", sysUserGridModel);
    }

    /**
     * 全量查询用户
     *
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @GetMapping("queryUserList")
    public HttpResponseBody<List<SysUser>> searchUserList() {
        List<SysUser> sysUsers = iUserService.queryList();
        return HttpResponseBody.successResponse("查询成功", sysUsers);
    }

    /**
     * 查询用户详情
     *
     * @param id
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @GetMapping("detailUser")
    public HttpResponseBody<Map<String, Object>> detailUser(String id) {
        SysUser sysUser = iUserService.selectByPrimaryKey(id);
        List<Department> departments = iDepartmentService.selectByUserId(id);
        List<SysRole> list = iRoleService.queryUserRoleByUserId(id);
        if (sysUser != null) {
            sysUser.setPassword(null);
            sysUser.setPasswordRand(null);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userInfo", sysUser);
        result.put("departments", departments);
        result.put("roleNames", list);
        return HttpResponseBody.successResponse("查询成功", result);
    }


    /**
     * 删除用户
     *
     * @param id
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @PostMapping("deleteUser")
    public HttpResponseBody delete(String id) {
        iUserService.freezeUser(id, "0", getSessionUserId());
        return HttpResponseBody.successResponse("删除成功");
    }

    /**
     * 修改用户、部门、角色
     *
     * @param user
     * @param departIds
     * @param roleIds
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @PostMapping("updateUser")
    public HttpResponseBody update(SysUser user, @RequestParam(name = "departIds", required = false, defaultValue = "") String[] departIds,
                                   @RequestParam(name = "roleIds", required = false, defaultValue = "") String[] roleIds) {
        user.setUpdateUser(this.getSessionUserId());
        boolean result = iUserService.update(user, JSONObject.toJSONString(departIds), JSONObject.toJSONString(roleIds));
        if (result) {
            return HttpResponseBody.successResponse("修改成功");
        } else {
            return HttpResponseBody.failResponse("修改失败,用户邮箱不能与其它用户重复");
        }
    }


    /**
     * 批量删除用户
     *
     * @param ids
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @PostMapping("batchDeleteUser")
    public HttpResponseBody batchDelete(@RequestParam(name = "ids") String[] ids) {
        iUserService.deleteByIds(Arrays.asList(ids));
        return HttpResponseBody.successResponse("删除成功");
    }


    /**
     * 根据用户编码查询用户
     *
     * @param identityCode
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @RequestMapping(value = "getUserByIdentityCode", method = {RequestMethod.GET, RequestMethod.POST})
    public HttpResponseBody<SysUser> getUserByIdentityCode(String identityCode) {
        SysUser user = iUserService.getUserByIdentityCode(identityCode);
        return HttpResponseBody.successResponse("查询成功", user);
    }


    /**
     * 工时补录查询
     *
     * @param param
     * @param page
     * @param pageSize
     * @param sidx
     * @param sord
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @GetMapping("queryTimeExtraPage")
    public HttpResponseBody<GridModel<Map<String, Object>>> queryTimeExtralPage(@RequestParam(required = false, defaultValue = "") String param, @RequestParam(required = false, defaultValue = "0") int page,
                                                                                @RequestParam(required = false, defaultValue = "10") int pageSize, @RequestParam(required = false, defaultValue = "") String sidx, @RequestParam(required = false, defaultValue = "") String sord) {
        GridModel<Map<String, Object>> sysUserGridModel = iUserService.queryTimeExtraPage(param, page, pageSize, sidx, sord);
        return HttpResponseBody.successResponse("查询成功", sysUserGridModel);
    }


    /**
     * 重置密码
     *
     * @param userId
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @PostMapping("resetPassword")
    public HttpResponseBody resetPassword(String userId) {
        iUserService.resetPassword(userId, getSessionUserId());
        return HttpResponseBody.successResponse("密码重置成功");
    }

    /**
     * 密码修改
     *
     * @param originalPwd
     * @param newPwd
     * @param conPwd
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @PostMapping("updatePassword")
    public HttpResponseBody updatePassword(String originalPwd, String newPwd, String conPwd) {
        iUserService.updatePassword(getSessionUserId(), originalPwd, newPwd, conPwd, getSessionUserId());
        return HttpResponseBody.successResponse("密码修改成功");
    }

    /**
     * 通讯录查询
     *
     * @param param
     * @param page
     * @param pageSize
     * @param sidx
     * @param sord
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @GetMapping("queryContactsPage")
    public HttpResponseBody<GridModel<SysUser>> queryContactsPage(@RequestParam(required = false, defaultValue = "") String param, @RequestParam(required = false, defaultValue = "0") int page,
                                                                  @RequestParam(required = false, defaultValue = "10") int pageSize, @RequestParam(required = false, defaultValue = "") String sidx, @RequestParam(required = false, defaultValue = "") String sord) {
        GridModel<SysUser> list = iUserService.queryContacts(param, page, pageSize, sidx, sord);
        return HttpResponseBody.successResponse("查询成功", list);
    }

    /**
     * 冻结和解冻用户
     *
     * @param userId
     * @param status
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @RequestMapping(value = "freezeUser", method = {RequestMethod.GET, RequestMethod.POST})
    public HttpResponseBody freezeUser(String userId, String status) {

        iUserService.freezeUser(userId, status, getSessionUserId());
        if ("2".equals(status)) {
            return HttpResponseBody.successResponse("冻结成功");
        } else {
            return HttpResponseBody.successResponse("解冻成功");
        }

    }

    /**
     * 注册用户
     *
     * @param sysUser
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @PostMapping("register")
    public HttpResponseBody register(SysUser sysUser) {
        iUserService.register(sysUser);
        return HttpResponseBody.successResponse("注册成功");
    }

    /**
     * 获取用户信息
     *
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/9
     * @version
     */
    @GetMapping("getUserInfo")
    public HttpResponseBody getUserInfo() {
        logger.info("----------getUserInfo:----------");
        String sessionUserId = getSessionUserId();
        logger.info("--------sessionUserId:" + sessionUserId);
        SysUser sysUser = iUserService.selectByPrimaryKey(sessionUserId);
        logger.info("----------sysUser:" + JSONObject.toJSONString(sysUser));
        if (sysUser == null) {
            return HttpResponseBody.failResponse("请重新登录");
        }
        sysUser.setPassword(null);
        sysUser.setPasswordRand(null);
        Map<String, Object> result = new HashMap<>();
        result.put("userInfo", sysUser);
        return HttpResponseBody.successResponse("查询成功", result);
    }

}
