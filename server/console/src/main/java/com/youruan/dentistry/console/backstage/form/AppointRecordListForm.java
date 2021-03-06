package com.youruan.dentistry.console.backstage.form;

import com.youruan.dentistry.console.base.form.ListForm;
import com.youruan.dentistry.core.base.query.QueryCondition;
import com.youruan.dentistry.core.frontdesk.query.AppointmentQuery;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Setter
public class AppointRecordListForm extends ListForm<AppointmentQuery> {
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startArrivedDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endArrivedDate;
    private String realName;
    private String phoneNumber;
    private String shopName;
    private Integer appointState;

    public AppointmentQuery buildQuery() {
        AppointmentQuery qo = new AppointmentQuery();
        qo.setPage(getPage());
        qo.setStartArrivedDate(startArrivedDate);
        qo.setEndArrivedDate(endArrivedDate);
        qo.setLikeRealName(realName);
        qo.setLikePhoneNumber(phoneNumber);
        qo.setLikeShopName(shopName);
        qo.setAppointState(appointState);
        if ("createdDate".equals(getSortField())) {
            qo.setOrderByCreatedDate(getSortOrder().equalsIgnoreCase("descend")
                    ? QueryCondition.ORDER_BY_KEYWORD_DESC
                    : QueryCondition.ORDER_BY_KEYWORD_ASC);
        }
        return qo;
    }
}
