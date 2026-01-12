package com.zmbdp.mstemplate.service.test;

import com.alibaba.excel.annotation.ExcelProperty;
import com.zmbdp.common.core.annotation.excel.CellMerge;
import com.zmbdp.common.core.excel.DefaultExcelListener;
import com.zmbdp.common.core.excel.ExcelResult;
import com.zmbdp.common.core.utils.ExcelUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 功能测试控制器
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/excel")
public class TestExcelController {

    /**
     * Excel 测试 DTO（用于导入导出测试）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExcelTestDTO {
        @ExcelProperty("姓名")
        @NotBlank(message = "姓名不能为空")
        private String name;

        @ExcelProperty("年龄")
        @NotNull(message = "年龄不能为空")
        private Integer age;

        @ExcelProperty("部门")
        @CellMerge  // 测试单元格合并
        private String department;

        @ExcelProperty("大数值ID")
        private Long bigNumber;  // 测试大数值处理
    }

    /**
     * 测试1：导出Excel（正常情况 - 不合并单元格）
     */
    @GetMapping("/export/normal")
    public Result<String> exportNormal(HttpServletResponse response) {
        log.info("========== 开始测试：导出Excel（正常情况 - 不合并单元格） ==========");
        try {
            // 准备测试数据
            List<ExcelTestDTO> list = new ArrayList<>();
            list.add(new ExcelTestDTO("张三", 25, "技术部", 123456789012345L));
            list.add(new ExcelTestDTO("李四", 30, "产品部", 123456789012346L));
            list.add(new ExcelTestDTO("王五", 28, "技术部", 123456789012347L));
            log.info("准备导出数据，共{}条", list.size());

            // 导出Excel
            ExcelUtil.exportExcel(list, "用户列表", ExcelTestDTO.class, false, response);
            log.info("✅ 导出成功，文件名：用户列表.xlsx");
            log.info("========== 测试完成：导出Excel（正常情况） ==========");
            return Result.success("导出成功，请查看下载的文件");
        } catch (Exception e) {
            log.error("❌ 导出失败", e);
            log.info("========== 测试失败：导出Excel（正常情况） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 测试2：导出Excel（合并单元格）
     */
    @GetMapping("/export/merge")
    public Result<String> exportMerge(HttpServletResponse response) {
        log.info("========== 开始测试：导出Excel（合并单元格） ==========");
        try {
            // 准备测试数据（部门有重复值，用于测试合并）
            List<ExcelTestDTO> list = new ArrayList<>();
            list.add(new ExcelTestDTO("张三", 25, "技术部", 123456789012345L));
            list.add(new ExcelTestDTO("李四", 30, "技术部", 123456789012346L));
            list.add(new ExcelTestDTO("王五", 28, "产品部", 123456789012347L));
            list.add(new ExcelTestDTO("赵六", 32, "产品部", 123456789012348L));
            log.info("准备导出数据，共{}条，部门字段有重复值用于测试合并", list.size());

            // 导出Excel（启用合并）
            ExcelUtil.exportExcel(list, "用户列表（合并）", ExcelTestDTO.class, true, response);
            log.info("✅ 导出成功（启用合并），文件名：用户列表（合并）.xlsx");
            log.info("========== 测试完成：导出Excel（合并单元格） ==========");
            return Result.success("导出成功（启用合并），请查看下载的文件");
        } catch (Exception e) {
            log.error("❌ 导出失败", e);
            log.info("========== 测试失败：导出Excel（合并单元格） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 测试3：导出Excel（大数值测试 - 超过15位）
     */
    @GetMapping("/export/bigNumber")
    public Result<String> exportBigNumber(HttpServletResponse response) {
        log.info("========== 开始测试：导出Excel（大数值测试） ==========");
        try {
            // 准备测试数据（包含超过15位的大数值）
            List<ExcelTestDTO> list = new ArrayList<>();
            list.add(new ExcelTestDTO("用户1", 25, "技术部", 1234567890123456L));  // 16位
            list.add(new ExcelTestDTO("用户2", 30, "产品部", 12345678901234567L)); // 17位
            list.add(new ExcelTestDTO("用户3", 28, "运营部", 123456789012345678L)); // 18位
            log.info("准备导出数据，包含超过15位的大数值，测试精度处理");

            // 导出Excel
            ExcelUtil.exportExcel(list, "大数值测试", ExcelTestDTO.class, false, response);
            log.info("✅ 导出成功（大数值），文件名：大数值测试.xlsx");
            log.info("========== 测试完成：导出Excel（大数值测试） ==========");
            return Result.success("导出成功（大数值），请查看下载的文件，大数值应自动转换为字符串");
        } catch (Exception e) {
            log.error("❌ 导出失败", e);
            log.info("========== 测试失败：导出Excel（大数值测试） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 测试4：导出Excel（空列表）
     */
    @GetMapping("/export/empty")
    public Result<String> exportEmpty(HttpServletResponse response) {
        log.info("========== 开始测试：导出Excel（空列表） ==========");
        try {
            // 准备空数据
            List<ExcelTestDTO> list = new ArrayList<>();
            log.info("准备导出空列表数据");

            // 导出Excel
            ExcelUtil.exportExcel(list, "空列表", ExcelTestDTO.class, false, response);
            log.info("✅ 导出成功（空列表），应只有表头");
            log.info("========== 测试完成：导出Excel（空列表） ==========");
            return Result.success("导出成功（空列表），应只有表头");
        } catch (Exception e) {
            log.error("❌ 导出失败", e);
            log.info("========== 测试失败：导出Excel（空列表） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 测试5：导出Excel到输出流
     */
    @GetMapping("/export/stream")
    public Result<String> exportToStream() {
        log.info("========== 开始测试：导出Excel到输出流 ==========");
        try {
            // 准备测试数据
            List<ExcelTestDTO> list = new ArrayList<>();
            list.add(new ExcelTestDTO("张三", 25, "技术部", 123456789012345L));
            list.add(new ExcelTestDTO("李四", 30, "产品部", 123456789012346L));
            log.info("准备导出数据，共{}条", list.size());

            // 导出到输出流
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ExcelUtil.exportExcel(list, "用户列表", ExcelTestDTO.class, false, os);
            byte[] bytes = os.toByteArray();
            log.info("✅ 导出到输出流成功，数据大小：{} 字节", bytes.length);
            log.info("========== 测试完成：导出Excel到输出流 ==========");
            return Result.success("导出到输出流成功，数据大小：" + bytes.length + " 字节");
        } catch (Exception e) {
            log.error("❌ 导出失败", e);
            log.info("========== 测试失败：导出Excel到输出流 ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 测试6：导入Excel（正常情况 - 不校验）
     */
    @PostMapping("/import/normal")
    public Result<String> importNormal(@RequestParam("file") MultipartFile file) {
        log.info("========== 开始测试：导入Excel（正常情况 - 不校验） ==========");
        try {
            log.info("接收文件：{}，大小：{} 字节", file.getOriginalFilename(), file.getSize());

            // 导入Excel（不校验）
            ExcelResult<ExcelTestDTO> result = ExcelUtil.importExcel(
                    file.getInputStream(), ExcelTestDTO.class, false);

            List<ExcelTestDTO> list = result.getList();
            List<String> errorList = result.getErrorList();

            log.info("✅ 导入成功，成功数据：{} 条，错误信息：{} 条", list.size(), errorList.size());
            log.info("成功数据：{}", list);
            if (!errorList.isEmpty()) {
                log.info("错误信息：{}", errorList);
            }
            log.info("导入回执：{}", result.getAnalysis());
            log.info("========== 测试完成：导入Excel（正常情况） ==========");
            return Result.success("导入成功，成功：" + list.size() + " 条，错误：" + errorList.size() + " 条");
        } catch (Exception e) {
            log.error("❌ 导入失败", e);
            log.info("========== 测试失败：导入Excel（正常情况） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导入失败：" + e.getMessage());
        }
    }

    /**
     * 测试7：导入Excel（启用校验）
     */
    @PostMapping("/import/validate")
    public Result<String> importValidate(@RequestParam("file") MultipartFile file) {
        log.info("========== 开始测试：导入Excel（启用校验） ==========");
        try {
            log.info("接收文件：{}，大小：{} 字节", file.getOriginalFilename(), file.getSize());

            // 导入Excel（启用校验）
            ExcelResult<ExcelTestDTO> result = ExcelUtil.importExcel(
                    file.getInputStream(), ExcelTestDTO.class, true);

            List<ExcelTestDTO> list = result.getList();
            List<String> errorList = result.getErrorList();

            log.info("✅ 导入完成（启用校验），成功数据：{} 条，错误信息：{} 条", list.size(), errorList.size());
            log.info("成功数据：{}", list);
            if (!errorList.isEmpty()) {
                log.info("错误信息：{}", errorList);
            }
            log.info("导入回执：{}", result.getAnalysis());
            log.info("========== 测试完成：导入Excel（启用校验） ==========");
            return Result.success("导入完成（启用校验），成功：" + list.size() + " 条，错误：" + errorList.size() + " 条");
        } catch (Exception e) {
            log.error("❌ 导入失败", e);
            log.info("========== 测试失败：导入Excel（启用校验） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导入失败：" + e.getMessage());
        }
    }

    /**
     * 测试8：导入Excel（使用自定义监听器）
     */
    @PostMapping("/import/custom")
    public Result<String> importCustom(@RequestParam("file") MultipartFile file) {
        log.info("========== 开始测试：导入Excel（使用自定义监听器） ==========");
        try {
            log.info("接收文件：{}，大小：{} 字节", file.getOriginalFilename(), file.getSize());

            // 创建自定义监听器
            DefaultExcelListener<ExcelTestDTO> listener = new DefaultExcelListener<>(true);

            // 导入Excel（使用自定义监听器）
            ExcelResult<ExcelTestDTO> result = ExcelUtil.importExcel(
                    file.getInputStream(), ExcelTestDTO.class, listener);

            List<ExcelTestDTO> list = result.getList();
            List<String> errorList = result.getErrorList();

            log.info("✅ 导入完成（自定义监听器），成功数据：{} 条，错误信息：{} 条", list.size(), errorList.size());
            log.info("成功数据：{}", list);
            if (!errorList.isEmpty()) {
                log.info("错误信息：{}", errorList);
            }
            log.info("导入回执：{}", result.getAnalysis());
            log.info("========== 测试完成：导入Excel（自定义监听器） ==========");
            return Result.success("导入完成（自定义监听器），成功：" + list.size() + " 条，错误：" + errorList.size() + " 条");
        } catch (Exception e) {
            log.error("❌ 导入失败", e);
            log.info("========== 测试失败：导入Excel（自定义监听器） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导入失败：" + e.getMessage());
        }
    }

    /**
     * 测试9：导入Excel（错误情况 - 空文件）
     */
    @PostMapping("/import/empty")
    public Result<String> importEmpty(@RequestParam("file") MultipartFile file) {
        log.info("========== 开始测试：导入Excel（错误情况 - 空文件） ==========");
        try {
            log.info("接收文件：{}，大小：{} 字节", file.getOriginalFilename(), file.getSize());

            if (file.isEmpty()) {
                log.warn("⚠️ 文件为空");
                log.info("========== 测试完成：导入Excel（空文件） ==========");
                return Result.fail(ResultCode.FAILED.getCode(), "文件为空");
            }

            // 导入Excel
            ExcelResult<ExcelTestDTO> result = ExcelUtil.importExcel(
                    file.getInputStream(), ExcelTestDTO.class, false);

            List<ExcelTestDTO> list = result.getList();
            List<String> errorList = result.getErrorList();

            log.info("导入结果，成功数据：{} 条，错误信息：{} 条", list.size(), errorList.size());
            log.info("导入回执：{}", result.getAnalysis());
            log.info("========== 测试完成：导入Excel（空文件） ==========");
            return Result.success("导入完成，成功：" + list.size() + " 条，错误：" + errorList.size() + " 条");
        } catch (Exception e) {
            log.error("❌ 导入失败（预期行为）", e);
            log.info("========== 测试完成：导入Excel（空文件 - 预期失败） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导入失败（预期行为）：" + e.getMessage());
        }
    }

    /**
     * 测试10：导出Excel（null数据测试）
     */
    @GetMapping("/export/null")
    public Result<String> exportNull(HttpServletResponse response) {
        log.info("========== 开始测试：导出Excel（null数据测试） ==========");
        try {
            // 准备包含null的数据
            List<ExcelTestDTO> list = new ArrayList<>();
            list.add(new ExcelTestDTO("张三", 25, null, null));  // null值
            list.add(new ExcelTestDTO(null, null, "技术部", 123456789012345L));  // null值
            log.info("准备导出数据，包含null值");

            // 导出Excel
            ExcelUtil.exportExcel(list, "null测试", ExcelTestDTO.class, false, response);
            log.info("✅ 导出成功（包含null值），文件名：null测试.xlsx");
            log.info("========== 测试完成：导出Excel（null数据测试） ==========");
            return Result.success("导出成功（包含null值），请查看下载的文件");
        } catch (Exception e) {
            log.error("❌ 导出失败", e);
            log.info("========== 测试失败：导出Excel（null数据测试） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }
}