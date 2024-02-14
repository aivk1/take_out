package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    @Override
    public TurnoverReportVO getTurnOverStatistics(LocalDate beginTime, LocalDate endTime) {
        //获取dateList
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(beginTime);
        while(!beginTime.equals(endTime)){
            beginTime = beginTime.plusDays(1);
            dateList.add(beginTime);
        }

        //获取turnoverList
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime begin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", begin);
            map.put("end", end);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover == null?0:turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList))
                .turnoverList(StringUtils.join(turnoverList))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //获取dateList
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //获取newUserList
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer newUser = userMapper.countByMap(map);
            Integer user = userMapper.countByEnd(endTime);
            newUserList.add(newUser==null?0:newUser);
            totalUserList.add(user==null?0:user);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
//        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
//        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Integer everydayOrderCount = orderMapper.countByMap(map);
            Integer everydayValidOrderCount = orderMapper.countValidOrderByMap(map);
            orderCountList.add(everydayOrderCount);
            validOrderCountList.add(everydayValidOrderCount);
            totalOrderCount += everydayOrderCount;
            validOrderCount += everydayValidOrderCount;
        }
        Double orderCompletionRate = (double) (validOrderCount/totalOrderCount);


        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10Statistics(LocalDate begin, LocalDate end) {
        Map map = new HashMap();
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", Orders.COMPLETED);
        List<GoodsSalesDTO> goodsSalesDTOS = orderMapper.getGoodsSaleDTOS(map);
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        for(GoodsSalesDTO GS:goodsSalesDTOS){
            nameList.add(GS.getName());
            numberList.add(GS.getNumber());
        }
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    public void exportBusinessData(HttpServletResponse response) throws IOException {
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);


        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        XSSFWorkbook excel = new XSSFWorkbook(in);
        XSSFSheet sheet = excel.getSheet("Sheet1");
        sheet.getRow(1).getCell(1).setCellValue("时间" + dateBegin + "至" + dateEnd);
        sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
        sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getOrderCompletionRate());
        sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getNewUsers());
        sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
        sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getUnitPrice());

        for(int i=0;i<30;i++){
            LocalDate date = LocalDate.now().minusDays(i);
            businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
            Row row = sheet.getRow(i+7);
            row.getCell(1).setCellValue(date.toString());
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
        }

        ServletOutputStream out = response.getOutputStream();
        excel.write(out);
    }

}
