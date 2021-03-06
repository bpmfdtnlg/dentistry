
package com.youruan.dentistry.core.backstage.service.impl;

import com.youruan.dentistry.core.backstage.domain.Report;
import com.youruan.dentistry.core.backstage.mapper.ReportMapper;
import com.youruan.dentistry.core.backstage.query.ReportQuery;
import com.youruan.dentistry.core.backstage.service.ProductService;
import com.youruan.dentistry.core.backstage.service.ReportService;
import com.youruan.dentistry.core.backstage.vo.AppointRecordVo;
import com.youruan.dentistry.core.backstage.vo.ExtendedReport;
import com.youruan.dentistry.core.backstage.vo.ReportRecordVo;
import com.youruan.dentistry.core.base.exception.OptimismLockingException;
import com.youruan.dentistry.core.base.query.Pagination;
import com.youruan.dentistry.core.base.storage.DiskFileStorage;
import com.youruan.dentistry.core.base.storage.UploadFile;
import com.youruan.dentistry.core.base.utils.SnowflakeIdWorker;
import com.youruan.dentistry.core.frontdesk.domain.Appointment;
import com.youruan.dentistry.core.frontdesk.service.AppointmentService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasicReportService
        implements ReportService {

    private final ReportMapper reportMapper;
    private final DiskFileStorage diskFileStorage;
    private final AppointmentService appointmentService;
    private final ProductService productService;

    public BasicReportService(ReportMapper reportMapper, DiskFileStorage diskFileStorage, @Lazy AppointmentService appointmentService, ProductService productService) {
        this.reportMapper = reportMapper;
        this.diskFileStorage = diskFileStorage;
        this.appointmentService = appointmentService;
        this.productService = productService;
    }

    @Override
    public Report get(Long id) {
        return reportMapper.get(id);
    }

    protected void update(Report report) {
        int affected = reportMapper.update(report);
        if (affected == 0) {
            throw new OptimismLockingException("version!!");
        }
        report.setVersion((report.getVersion() + 1));
    }

    protected Report add(Report report) {
        report.setCreatedDate(new Date());
        reportMapper.add(report);
        return report;
    }

    @Override
    public List<ExtendedReport> listAll(ReportQuery qo) {
        qo.setMaxPageSize();
        return reportMapper.query(qo);
    }

    @Override
    public ExtendedReport queryOne(ReportQuery qo) {
        qo.setPageSize(1);
        List<ExtendedReport> datas = reportMapper.query(qo);
        return (((datas == null) || datas.isEmpty()) ? null : datas.get(0));
    }

    @Override
    public Pagination<ExtendedReport> query(ReportQuery qo) {
        int rows = reportMapper.count(qo);
        List<ExtendedReport> datas = ((rows == 0) ? new ArrayList<>() : reportMapper.query(qo));
        return new Pagination<>(rows, datas);
    }

    @Override
    public int count(ReportQuery qo) {
        return reportMapper.count(qo);
    }

    @Override
    @Transactional
    public void create(Integer peopleNum, Long userId, Long appointId, Long productId, List<String> pathList) {
        Integer afterCount = this.checkAdd(peopleNum, userId, appointId, productId, pathList);
        List<Report> reportList = this.createData(userId,appointId,productId,pathList);
        this.batchAdd(reportList);
        if(afterCount < peopleNum) return;
        // ??????????????????????????????==?????????????????????????????????
        Appointment appointment = appointmentService.get(appointId);
        appointmentService.reportCompleted(appointment);
    }

    /**
     * ??????????????????
     */
    private void batchAdd(List<Report> reportList) {
        reportMapper.batchAdd(reportList);
    }

    /**
     * ??????????????????
     */
    private List<Report> createData(Long userId, Long appointId, Long productId, List<String> pathList) {
        return pathList.stream().map(item -> {
            Report report = new Report();
            this.assign(report,userId,appointId,productId,item);
            return report;
        }).collect(Collectors.toList());
    }

    /**
     * ????????????
     */
    private void assign(Report report, Long userId, Long appointId, Long productId, String path) {
        report.setCreatedDate(new Date());
        report.setUserId(userId);
        report.setAppointId(appointId);
        report.setProductId(productId);
        report.setPath(path);
        report.setSync(false);
        report.setReportNo(SnowflakeIdWorker.getIdWorker());
    }

    /**
     * ?????????????????? ?????????????????????????????????
     */
    private Integer checkAdd(Integer peopleNum, Long userId, Long appointId, Long productId, List<String> pathList) {
        Assert.notNull(peopleNum, "????????????????????????");
        Assert.notNull(userId, "??????????????????id");
        Assert.notNull(appointId, "??????????????????id");
        Assert.notNull(productId, "??????????????????id");
        Assert.notNull(pathList, "????????????????????????");
        Integer appointState = appointmentService.get(appointId).getAppointState();
        Assert.isTrue(Appointment.APPOINT_STATE_APPOINTED.equals(appointState),"???????????????????????????????????????");
        ReportQuery qo = new ReportQuery();
        qo.setAppointId(appointId);
        int count = this.count(qo);
        Assert.isTrue(pathList.size() <= peopleNum-count, "?????????????????????????????????");
        return count + pathList.size();
    }

    @Override
    public void update(Report report, Boolean sync) {

    }

    @Override
    public List<? extends Report> listAll(Long[] reportIds) {
        ReportQuery qo = new ReportQuery();
        qo.setIds(reportIds);
        return listAll(qo);
    }

    @Override
    public List<ExtendedReport> listAll() {
        ReportQuery qo = new ReportQuery();
        qo.setMaxPageSize();
        return listAll(qo);
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        try {
            UploadFile uploadFile = new UploadFile();
            uploadFile.setInputStream(file.getInputStream());
            uploadFile.setOriginalFilename(file.getOriginalFilename());
            return diskFileStorage.store(uploadFile, directory);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Pagination<ReportRecordVo> record(ReportQuery qo) {
        int rows = reportMapper.countRecord(qo);
        List<ReportRecordVo> data = (rows == 0) ? new ArrayList<>() : reportMapper.record(qo);
        return new Pagination<>(rows, data);
    }

    @Override
    public AppointRecordVo getAppoint(Report report) {
        Assert.notNull(report,"??????????????????");
        return appointmentService.getInfo(report.getAppointId());
    }

    @Override
    public void reset(Report report, String path) {
        this.checkReset(report, path);
        report.setPath(path);
        this.update(report);
    }

    @Override
    @Transactional
    public void sync(Report report) {
        this.checkSync(report);
        report.setSync(true);
        this.update(report);
        this.autoAppointCompleted(report);
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????
     */
    private void autoAppointCompleted(Report report) {
        ReportQuery qo = new ReportQuery();
        qo.setAppointId(report.getAppointId());
        qo.setSync(true);
        int count = reportMapper.count(qo);
        Integer peopleNum = productService.get(report.getProductId()).getPeopleNum();
        if(count < peopleNum) return;
        Appointment appointment = appointmentService.get(report.getAppointId());
        appointmentService.appointCompleted(appointment);
    }

    /**
     * ?????????????????????
     */
    private void checkSync(Report report) {
        Assert.notNull(report, "??????????????????");
        Assert.isTrue(!report.getSync(),"?????????????????????????????????");
    }

    /**
     * ??????????????????
     */
    private void checkReset(Report report, String path) {
        Assert.notNull(report,"??????????????????");
        Assert.isTrue(!report.getSync(),"?????????????????????????????????");
        Assert.notNull(path,"??????????????????????????????");
    }
}
