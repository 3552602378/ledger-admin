package com.ledger.controller;

import com.alibaba.excel.EasyExcel;
import com.ledger.dto.RecordExcelVO;
import com.ledger.dto.RecordExportDTO;
import com.ledger.entity.FinRecord;
import com.ledger.service.RecordService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RecordService recordService;

    /** 导出收支记录：ids 非空导出选中记录，exportAll=true 导出全量，否则导出当前筛选结果 */
    @PostMapping("/records")
    @PreAuthorize("@ss.hasPermi('finance:record:export')")
    public void exportRecords(@RequestBody RecordExportDTO dto, HttpServletResponse response) throws IOException {
        List<RecordExcelVO> rows = recordService.listForExport(dto).stream().map(this::toExcelRow).toList();

        String fileName = "收支记录_" + LocalDate.now().format(FILE_DATE_FMT) + ".xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename=\"" + encoded + "\";filename*=UTF-8''" + encoded);

        EasyExcel.write(response.getOutputStream(), RecordExcelVO.class)
                .sheet("收支记录")
                .doWrite(rows);
    }

    private RecordExcelVO toExcelRow(FinRecord record) {
        RecordExcelVO vo = new RecordExcelVO();
        vo.setRecordDate(record.getRecordDate() == null ? "" : record.getRecordDate().toString());
        vo.setType("income".equals(record.getType()) ? "收入" : "支出");
        vo.setCategoryName(record.getCategoryName());
        vo.setAmount(record.getAmount() == null ? null : record.getAmount().setScale(2, RoundingMode.HALF_UP));
        vo.setRemark(record.getRemark());
        return vo;
    }
}
