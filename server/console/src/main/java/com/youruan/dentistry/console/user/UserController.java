package com.youruan.dentistry.console.user;

import com.google.common.collect.ImmutableMap;
import com.youruan.dentistry.console.base.interceptor.RequiresPermission;
import com.youruan.dentistry.console.user.form.BoughtListForm;
import com.youruan.dentistry.console.user.form.UserAddForm;
import com.youruan.dentistry.console.user.form.UserEditForm;
import com.youruan.dentistry.console.user.form.UserListForm;
import com.youruan.dentistry.core.backstage.vo.AppointRecordVo;
import com.youruan.dentistry.core.base.query.Pagination;
import com.youruan.dentistry.core.base.utils.BeanMapUtils;
import com.youruan.dentistry.core.frontdesk.domain.Appointment;
import com.youruan.dentistry.core.frontdesk.query.AppointmentQuery;
import com.youruan.dentistry.core.frontdesk.query.OrdersQuery;
import com.youruan.dentistry.core.frontdesk.service.AppointmentService;
import com.youruan.dentistry.core.frontdesk.service.OrdersService;
import com.youruan.dentistry.core.user.domain.RegisteredUser;
import com.youruan.dentistry.core.user.query.RegisteredUserQuery;
import com.youruan.dentistry.core.user.service.RegisteredUserService;
import com.youruan.dentistry.core.user.vo.ExtendedRegisteredUser;
import com.youruan.dentistry.core.user.vo.UserBoughtVo;
import com.youruan.dentistry.core.user.vo.UserRecordVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final RegisteredUserService registeredUserService;
    private final OrdersService ordersService;
    private final AppointmentService appointmentService;

    public UserController(RegisteredUserService registeredUserService, OrdersService ordersService, AppointmentService appointmentService) {
        this.registeredUserService = registeredUserService;
        this.ordersService = ordersService;
        this.appointmentService = appointmentService;
    }

    @PostMapping("/add")
    @RequiresPermission(value = "user.user.add", description = "??????-??????")
    public ResponseEntity<?> add(UserAddForm form) {
//        RegisteredUser user = registeredUserService.create(
//                form.getPhoneNumber(),
//                form.getLocked());
//        return ResponseEntity.ok(ImmutableMap.builder()
//                .put("id", user.getId())
//                .build());
        return null;
    }

    @PostMapping("/edit")
    @RequiresPermission(value = "user.user.edit", description = "??????-??????")
    public ResponseEntity<?> edit(UserEditForm form) {
        RegisteredUser user = registeredUserService.get(form.getId());
        registeredUserService.update(
                user,
                form.getPhoneNumber(),
                form.getLocked());
        return ResponseEntity.ok(ImmutableMap.builder()
                .put("id", user.getId())
                .build());
    }

    @GetMapping("/list")
    @RequiresPermission(value = "user.user.list", description = "??????-??????")
    public ResponseEntity<?> list(UserListForm form) {
        RegisteredUserQuery qo = form.buildQuery();
        Pagination<ExtendedRegisteredUser> pagination = registeredUserService.query(qo);
        List<UserRecordVo> voList = registeredUserService.handleData(pagination.getData());
        return ResponseEntity.ok(ImmutableMap.builder()
                .put("data", voList)
                .put("rows", pagination.getRows())
                .build());
    }

    @PostMapping("/get")
    @RequiresPermission(value = "user.user.get", description = "??????-??????")
    public ResponseEntity<?> get(@RequestParam("id") Long id) {
        RegisteredUser user = registeredUserService.get(id);
        return ResponseEntity.ok(BeanMapUtils.pick(user, "id", "no", "createdDate", "phoneNumber",  "locked"));
    }

    /**
     * ??????????????????
     */
    @GetMapping("/bought")
    @RequiresPermission(value = "user.user.bought", description = "??????-??????????????????")
    public ResponseEntity<?> bought(BoughtListForm form) {
        OrdersQuery qo = form.buildQuery();
        Pagination<UserBoughtVo> pagination = ordersService.bought(qo);
        return ResponseEntity.ok(ImmutableMap.builder()
                .put("data",pagination.getData())
                .put("rows",pagination.getRows())
                .build());
    }

    /**
     * ????????????????????????????????????
     */
    @GetMapping("/reportable")
    @RequiresPermission(value = "user.user.reportable", description = "??????-??????????????????")
    public ResponseEntity<?> reportable(Long id) {
        AppointmentQuery qo = new AppointmentQuery();
        qo.setUserId(id);
        qo.setReportStatus(Appointment.REPORT_STATUS_NOT);
        Pagination<AppointRecordVo> pagination = appointmentService.record(qo);
        return ResponseEntity.ok(ImmutableMap.builder()
                .put("data", pagination.getData())
                .build());
    }

}
