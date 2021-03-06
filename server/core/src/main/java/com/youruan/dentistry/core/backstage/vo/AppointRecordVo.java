package com.youruan.dentistry.core.backstage.vo;

import com.youruan.dentistry.core.base.domain.BasicDomain;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AppointRecordVo extends BasicDomain {
    private Integer timePeriod;
    private Date appointDate;
    private Date arrivedDate;
    private Integer totalNum;
    private Integer appointNum;
    private Integer peopleNum;
    private String productName;
    private String shopName;
    private String realName;
    private String phoneNumber;
    private Integer appointState;
    private Integer reportStatus;
}
