package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.NhatKyDieuDuong;
import com.hospital.attendance.Entity.NhatKyDieuDuong.LoaiMauNhatKy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NhatKyDieuDuongExportService {

    private static final Logger logger = LoggerFactory.getLogger(NhatKyDieuDuongExportService.class);

    @Autowired
    private NhatKyDieuDuongService nhatKyDieuDuongService;

    /**
     * Export báo cáo tháng ra Excel
     */
    public byte[] exportMonthlyReport(String tenDangNhap, Long khoaPhongId, LoaiMauNhatKy loaiMau,
                                      Integer thang, Integer nam) throws IOException {

        logger.info("Exporting monthly report for khoaPhongId: {}, loaiMau: {}, thang: {}, nam: {}",
                khoaPhongId, loaiMau, thang, nam);

        List<NhatKyDieuDuong> nhatKyList = nhatKyDieuDuongService.getNhatKyForMonthlyReport(
                tenDangNhap, khoaPhongId, loaiMau, thang, nam);

        // Tạo map để lookup dữ liệu theo ngày
        Map<Integer, NhatKyDieuDuong> dataMap = nhatKyList.stream()
                .collect(Collectors.toMap(nk -> nk.getNgay().getDayOfMonth(), nk -> nk));

        try (Workbook workbook = new XSSFWorkbook()) {
            if (loaiMau == LoaiMauNhatKy.MAU_1) {
                createImprovedMau1Sheet(workbook, dataMap, thang, nam, khoaPhongId);
            } else if (loaiMau == LoaiMauNhatKy.MAU_2) {
                createImprovedMau2Sheet(workbook, dataMap, thang, nam, khoaPhongId);
            } else if (loaiMau == LoaiMauNhatKy.MAU_3) {
                createImprovedMau3Sheet(workbook, dataMap, thang, nam, khoaPhongId);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * ✅ TẠO SHEET CẢI TIẾN CHO MẪU 1 VỚI DIAGONAL HEADER VÀ FONT TIMES NEW ROMAN
     */
    private void createImprovedMau1Sheet(Workbook workbook, Map<Integer, NhatKyDieuDuong> dataMap,
                                         Integer thang, Integer nam, Long khoaPhongId) {
        // ✅ SỬA TIÊU ĐỀ SHEET CHO MẪU 1
        XSSFSheet sheet = (XSSFSheet) workbook.createSheet("Nhật ký quản lý khoa khối lâm sàng");

        // ✅ TẠO CÁC STYLE VỚI FONT TIMES NEW ROMAN
        CellStyle headerStyle = createVerticalHeaderStyle(workbook);
        CellStyle groupHeaderStyle = createGroupHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle diagonalHeaderStyle = createDiagonalHeaderStyle(workbook);

        int rowNum = 0;

        // Header thông tin
        String khoaPhongName = dataMap.values().stream().findFirst()
                .map(nk -> nk.getKhoaPhong().getTenKhoaPhong())
                .orElse("Khoa " + khoaPhongId);

        // ✅ TRUYỀN TIÊU ĐỀ ĐÚNG CHO MẪU 1
        createImprovedTitleSection(sheet, rowNum, thang, nam, khoaPhongName,
                "NHẬT KÝ QUẢN LÝ KHOA KHỐI LÂM SÀNG", titleStyle);
        rowNum += 8; // Skip title rows

        // ✅ TẠO DIAGONAL HEADER CHO NGÀY
        createDiagonalDateHeader(sheet, rowNum, diagonalHeaderStyle);

        // Tạo headers với merge cells và text dọc
        createMau1VerticalHeaders(sheet, rowNum, headerStyle, groupHeaderStyle);
        rowNum += 2; // Headers take 2 rows

        // Tạo data cho tất cả ngày trong tháng
        YearMonth yearMonth = YearMonth.of(nam, thang);
        int daysInMonth = yearMonth.lengthOfMonth();

        // ✅ TÍNH TOÁN TỔNG CỘNG CHO MẪU 1
        Mau1Totals totals = new Mau1Totals();

        for (int day = 1; day <= daysInMonth; day++) {
            NhatKyDieuDuong nhatKy = dataMap.get(day);
            createMau1DataRowImproved(sheet, rowNum++, day, nhatKy, dataStyle);

            // ✅ CỘNG DỒN VÀO TỔNG
            if (nhatKy != null) {
                totals.add(nhatKy);
            }
        }

        // ✅ THÊM DÒNG TỔNG CỘNG
        CellStyle totalStyle = createTotalStyle(workbook);
        createMau1TotalRow(sheet, rowNum, totals, totalStyle);
        rowNum++; // Tăng rowNum sau khi thêm total row

        // ✅ THÊM PHẦN CHỮ KÝ
        createSignatureSection(sheet, rowNum + 2, titleStyle); // Cách 2 dòng từ total row

        // Set column widths cho text dọc
        setMau1ColumnWidths(sheet);
    }

    /**
     * ✅ TẠO SHEET CẢI TIẾN CHO MẪU 2 VỚI DIAGONAL HEADER VÀ FONT TIMES NEW ROMAN
     */
    private void createImprovedMau2Sheet(Workbook workbook, Map<Integer, NhatKyDieuDuong> dataMap,
                                         Integer thang, Integer nam, Long khoaPhongId) {
        // ✅ SỬA TIÊU ĐỀ SHEET CHO MẪU 2
        XSSFSheet sheet = (XSSFSheet) workbook.createSheet("Tình hình nhân sự khối lâm sàng");

        // ✅ TẠO CÁC STYLE VỚI FONT TIMES NEW ROMAN
        CellStyle headerStyle = createVerticalHeaderStyle(workbook);
        CellStyle groupHeaderStyle = createGroupHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle diagonalHeaderStyle = createDiagonalHeaderStyle(workbook);

        int rowNum = 0;

        // Header thông tin
        String khoaPhongName = dataMap.values().stream().findFirst()
                .map(nk -> nk.getKhoaPhong().getTenKhoaPhong())
                .orElse("Khoa " + khoaPhongId);

        // ✅ TRUYỀN TIÊU ĐỀ ĐÚNG CHO MẪU 2
        createImprovedTitleSection(sheet, rowNum, thang, nam, khoaPhongName,
                "TÌNH HÌNH NHÂN SỰ KHỐI LÂM SÀNG", titleStyle);
        rowNum += 8; // Skip title rows

        // ✅ TẠO DIAGONAL HEADER CHO NGÀY
        createDiagonalDateHeader(sheet, rowNum, diagonalHeaderStyle);

        // Tạo headers với merge cells và text dọc
        createMau2VerticalHeaders(sheet, rowNum, headerStyle, groupHeaderStyle);
        rowNum += 2; // Headers take 2 rows

        // Tạo data cho tất cả ngày trong tháng
        YearMonth yearMonth = YearMonth.of(nam, thang);
        int daysInMonth = yearMonth.lengthOfMonth();

        // ✅ TÍNH TOÁN TỔNG CỘNG CHO MẪU 2
        Mau2Totals totals = new Mau2Totals();

        for (int day = 1; day <= daysInMonth; day++) {
            NhatKyDieuDuong nhatKy = dataMap.get(day);
            createMau2DataRowImproved(sheet, rowNum++, day, nhatKy, dataStyle);

            // ✅ CỘNG DỒN VÀO TỔNG
            if (nhatKy != null) {
                totals.add(nhatKy);
            }
        }

        // ✅ THÊM DÒNG TỔNG CỘNG
        CellStyle totalStyle = createTotalStyle(workbook);
        createMau2TotalRow(sheet, rowNum, totals, totalStyle);
        rowNum++; // Tăng rowNum sau khi thêm total row

        // ✅ THÊM PHẦN CHỮ KÝ
        createSignatureSection(sheet, rowNum + 2, titleStyle); // Cách 2 dòng từ total row

        // Set column widths cho text dọc
        setMau2ColumnWidths(sheet);
    }

    /**
     * ✅ TẁO DIAGONAL HEADER ĐÚNG VỚI 2 CELLS RIÊNG BIỆT
     */
    private void createDiagonalDateHeader(XSSFSheet sheet, int rowNum, CellStyle diagonalStyle) {
        // ✅ TẠO 2 STYLES KHÁC NHAU
        CellStyle topLeftStyle = createTopLeftTextStyle(sheet.getWorkbook());
        CellStyle bottomRightStyle = createBottomRightTextStyle(sheet.getWorkbook());

        Row groupRow = sheet.getRow(rowNum);
        if (groupRow == null) {
            groupRow = sheet.createRow(rowNum);
        }

        Row detailRow = sheet.getRow(rowNum + 1);
        if (detailRow == null) {
            detailRow = sheet.createRow(rowNum + 1);
        }

        // ✅ SET CHIỀU CAO ROW ĐỂ CÓ CHỖ CHO TEXT
        groupRow.setHeightInPoints(40);
        detailRow.setHeightInPoints(40);

        // ✅ TẠO CELL CHO "Nội dung" VÀ "Ngày" CÙNG POSITION NHƯNG KHÁC STYLE
        Cell dateCell = groupRow.createCell(0);

        // ✅ SỬ DỤNG RICH TEXT ĐỂ CÓ 2 DÒNG VỚI ALIGNMENT KHÁC NHAU
        XSSFWorkbook workbook = (XSSFWorkbook) sheet.getWorkbook();
        XSSFRichTextString richText = new XSSFRichTextString();

        // Font bold cho cả 2
        XSSFFont font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);

        // ✅ TẠO TEXT VỚI KHOẢNG CÁCH ĐỂ TÁCH 2 GÓC
        richText.append("    Nội dung", font);
        richText.append("\n\n\n\n\n\n"); // Nhiều line breaks để đẩy xuống
        richText.append("                              Ngày", font); // Spaces để đẩy sang phải

        dateCell.setCellValue(richText);
        dateCell.setCellStyle(topLeftStyle); // Dùng style có wrap text

        // ✅ MERGE CELL
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, 0, 0));

        // ✅ VẼ ĐƯỜNG CHÉO ĐƠN GIẢN
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(
                100, 100,     // dx1, dy1 - bắt đầu từ gần góc trên trái
                900, 700,     // dx2, dy2 - kết thúc ở gần góc dưới phải
                0, rowNum,    // col1, row1
                1, rowNum + 2 // col2, row2
        );

        XSSFSimpleShape line = drawing.createSimpleShape(anchor);
        line.setShapeType(org.apache.poi.ss.usermodel.ShapeTypes.LINE);
        line.setLineStyleColor(0, 0, 0); // Màu đen
        line.setLineWidth(2.0);
    }

    /**
     * ✅ Style cho diagonal cell với wrap text
     */
    private CellStyle createTopLeftTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);

        // ✅ ALIGNMENT VÀ WRAP TEXT
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true); // QUAN TRỌNG: Cho phép wrap text

        // Borders đầy đủ
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THICK);
        style.setBorderRight(BorderStyle.THICK);

        // Background
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    /**
     * ✅ Style cho bottom right (không dùng nhưng để sẵn)
     */
    private CellStyle createBottomRightTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.BOTTOM);
        style.setWrapText(true);

        // Borders
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THICK);
        style.setBorderRight(BorderStyle.THICK);

        // Background
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    /**
     * ✅ TẠO TITLE SECTION CẢI TIẾN VỚI TIÊU ĐỀ DYNAMIC CHO TỪNG MẪU
     */
    private void createImprovedTitleSection(Sheet sheet, int startRow, Integer thang, Integer nam,
                                            String khoaPhongName, String mainTitle, CellStyle titleStyle) {

        // ✅ TẠO CÁC STYLE KHÁC NHAU
        CellStyle leftTitleStyle = createLeftTitleStyle(sheet.getWorkbook());
        CellStyle rightTitleStyle = createRightTitleStyle(sheet.getWorkbook());
        CellStyle centerTitleStyle = createCenterTitleStyle(sheet.getWorkbook());
        CellStyle mainTitleStyle = createMainTitleStyle(sheet.getWorkbook());

        // Row 0: SỞ Y TẾ | CỘNG HOÀ XÃ HỘI CHỦ NGHĨA VIỆT NAM
        Row row0 = sheet.createRow(startRow);
        Cell cell0_0 = row0.createCell(0);
        cell0_0.setCellValue("SỞ Y TẾ");
        cell0_0.setCellStyle(leftTitleStyle);

        Cell cell0_1 = row0.createCell(25); // Cột xa hơn
        cell0_1.setCellValue("CỘNG HOÀ XÃ HỘI CHỦ NGHĨA VIỆT NAM");
        cell0_1.setCellStyle(rightTitleStyle);

        // Row 1: BỆNH VIỆN QUẬN TÂN PHÚ | Độc lập - Tự do - Hạnh phúc
        Row row1 = sheet.createRow(startRow + 1);
        Cell cell1_0 = row1.createCell(0);
        cell1_0.setCellValue("BỆNH VIỆN ĐA KHOA TÂN PHÚ");
        cell1_0.setCellStyle(leftTitleStyle);

        Cell cell1_1 = row1.createCell(25);
        cell1_1.setCellValue("Độc lập - Tự do - Hạnh phúc");
        cell1_1.setCellStyle(rightTitleStyle);

        // Row 2: =============== (gạch ngang)
        Row row2 = sheet.createRow(startRow + 2);
        Cell cell2_1 = row2.createCell(25);
        cell2_1.setCellValue("════════════════════");
        cell2_1.setCellStyle(rightTitleStyle);

        // Row 3: Trống
        sheet.createRow(startRow + 3);

        // Row 4: ✅ TIÊU ĐỀ CHÍNH DYNAMIC (căn giữa, font lớn) - MỞ RỘNG HỚN
        Row row4 = sheet.createRow(startRow + 4);
        Cell cell4_0 = row4.createCell(10); // Giữa bảng
        cell4_0.setCellValue(mainTitle); // ✅ SỬ DỤNG THAM SỐ THAY VÌ HARD-CODE
        cell4_0.setCellStyle(mainTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 4, startRow + 4, 10, 22)); // ✅ RỘNG HỚN: 10-22 thay vì 10-18

        // Row 5: Khoa phòng (căn giữa)
        Row row5 = sheet.createRow(startRow + 5);
        Cell cell5_0 = row5.createCell(10);
        cell5_0.setCellValue("(" + khoaPhongName + ")");
        cell5_0.setCellStyle(centerTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 5, startRow + 5, 10, 22)); // ✅ RỘNG HỚN

        // Row 6: Tháng/Năm (căn giữa)
        Row row6 = sheet.createRow(startRow + 6);
        Cell cell6_0 = row6.createCell(10);
        cell6_0.setCellValue("Tháng " + String.format("%02d/%d", thang, nam));
        cell6_0.setCellStyle(centerTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 10, 22)); // ✅ RỘNG HỚN

        // Row 7: Trống
        sheet.createRow(startRow + 7);
    }

    /**
     * Tạo headers cho mẫu 1 với text dọc và borders đầy đủ
     */
    private void createMau1VerticalHeaders(Sheet sheet, int rowNum, CellStyle headerStyle, CellStyle groupHeaderStyle) {
        // Row 1: Group headers
        Row groupRow = sheet.getRow(rowNum);
        if (groupRow == null) {
            groupRow = sheet.createRow(rowNum);
        }
        int colNum = 1; // Bỏ qua cột ngày đã có diagonal

        // Tình hình NB nội trú
        Cell nbtCell = groupRow.createCell(colNum);
        nbtCell.setCellValue("Tình hình NB nội trú");
        nbtCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 10)); // 11 columns

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 10; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 11;

        // Tình hình sản phụ
        Cell spCell = groupRow.createCell(colNum);
        spCell.setCellValue("Tình hình sản phụ");
        spCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 2)); // 3 columns

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 2; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 3;

        // Tình hình phẫu thuật - thủ thuật
        Cell ptCell = groupRow.createCell(colNum);
        ptCell.setCellValue("Tình hình phẫu thuật - thủ thuật");
        ptCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 7)); // 8 columns

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 7; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 8;

        // Chăm sóc điều dưỡng - THÊM 1 CỘT CHO TRUYỀN MÁU
        Cell csCell = groupRow.createCell(colNum);
        csCell.setCellValue("Chăm sóc điều dưỡng");
        csCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 11)); // 12 columns (tăng từ 11)

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 11; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 12;

        // Phân cấp chăm sóc - THÊM 1 CỘT CHO CS CẤP I MỚI
        Cell pcCell = groupRow.createCell(colNum);
        pcCell.setCellValue("Phân cấp chăm sóc");
        pcCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 3)); // 4 columns (tăng từ 3)

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 3; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 4;

        // Tình hình KCB
        Cell kcbCell = groupRow.createCell(colNum);
        kcbCell.setCellValue("Tình hình KCB");
        kcbCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 10)); // 11 columns

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 10; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }

        // Row 2: Detail headers với text dọc
        Row detailRow = sheet.createRow(rowNum + 1);
        colNum = 1; // Skip merged date cell

        String[] detailHeaders = {
                // Tình hình NB nội trú (11)
                "Giường thực kê", "Giường chỉ tiêu", "Tổng bệnh cũ", "BN vào viện", "Tổng xuất viện",
                "Chuyển viện", "Chuyển khoa", "Trốn viện", "Xin về", "Tử vong", "Bệnh hiện có",
                // Tình hình sản phụ (3)
                "Sanh thường", "Sanh mổ", "Mổ Phụ khoa",
                // Tình hình phẫu thuật - thủ thuật (8)
                "Cấp cứu", "Chương trình", "Thủ thuật", "Tiểu phẫu", "Phẫu thuật", "PT loại I", "PT loại II", "PT loại III",
                // Chăm sóc điều dưỡng (12) - THÊM TRUYỀN MÁU
                "Thở CPAP", "Thở máy", "Thở Oxy", "Bóp Bóng", "Monitor", "CVP", "Nội Khí Quản", "Nội soi",
                "Sonde dạ dày", "Sonde tiểu", "Hút đàm nhớt", "Truyền máu",
                // Phân cấp chăm sóc (4) - CHIA CS CẤP I
                "Tổng CS cấp I", "CS cấp I mới", "CS Cấp II", "CS cấp III",
                // Tình hình KCB (11)
                "TS NB KCB", "TS NB cấp cứu", "Ngoại viện", "Chuyển Nội trú", "Chuyển Cấp cứu",
                "Chuyển viện", "Chuyển PK K.Ngoại", "Tử vong", "Tổng NB đo điện tim",
                "Tổng NB đo điện cơ", "Tổng NB đo chức năng hô hấp"
        };

        for (String header : detailHeaders) {
            Cell cell = detailRow.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Tạo headers cho mẫu 2 với text dọc và borders đầy đủ
     */
    private void createMau2VerticalHeaders(Sheet sheet, int rowNum, CellStyle headerStyle, CellStyle groupHeaderStyle) {
        // Row 1: Group headers
        Row groupRow = sheet.getRow(rowNum);
        if (groupRow == null) {
            groupRow = sheet.createRow(rowNum);
        }
        int colNum = 1; // Bỏ qua cột ngày đã có diagonal

        // Tình hình nhân sự - GIẢM 1 CỘT (BỎ Y SĨ)
        Cell nsCell = groupRow.createCell(colNum);
        nsCell.setCellValue("Tình hình nhân sự");
        nsCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 5)); // 6 columns (giảm từ 7)

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 5; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 6;

        // Hiện diện
        Cell hdCell = groupRow.createCell(colNum);
        hdCell.setCellValue("Hiện diện");
        hdCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 8)); // 9 columns

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 8; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 9;

        // Vắng
        Cell vCell = groupRow.createCell(colNum);
        vCell.setCellValue("Vắng");
        vCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 9)); // 10 columns

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 9; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 10;

        // Đào tạo - GIẢM 2 CỘT (BỎ SV Y SĨ, SV DƯỢC)
        Cell dtCell = groupRow.createCell(colNum);
        dtCell.setCellValue("Đào tạo");
        dtCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 4)); // 5 columns (giảm từ 7)

        // Fill empty cells với border
        for (int i = colNum + 1; i <= colNum + 4; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }

        // Row 2: Detail headers với text dọc
        Row detailRow = sheet.createRow(rowNum + 1);
        colNum = 1; // Skip merged date cell

        String[] detailHeaders = {
                // Tình hình nhân sự (6) - BỎ Y SĨ, ĐỔI KỸ THUẬT VIÊN
                "Điều dưỡng", "Hộ sinh", "Kỹ thuật y", "Khác", "Hộ lý", "Tổng",
                // Hiện diện (9)
                "ĐDT khoa", "ĐDHC", "Phòng khám", "Tour sáng", "Tour chiều", "Tour đêm", "Trực 24/24", "Hộ lý", "Tổng",
                // Vắng (10)
                "Ra trực", "Bù trực", "Nghỉ phép", "Nghỉ ốm", "Nghỉ hậu sản", "Nghỉ khác", "Đi học", "Công tác", "Hộ lý", "Tổng",
                // Đào tạo (5) - BỎ SV Y SĨ, SV DƯỢC, ĐỔI SV KTV
                "Nhân viên thử việc", "Thực hành k lương", "Nhân sự tăng cường", "SV ĐD - HS", "SV KTY"
        };

        for (String header : detailHeaders) {
            Cell cell = detailRow.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * ✅ Tạo style cho các dòng trống (không có dữ liệu) với màu nền khác
     */
    private CellStyle createEmptyRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders như bình thường
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // ✅ MÀU NỀN NHẸ NHÀNG ĐỂ PHÂN BIỆT DÒNG TRỐNG (CHỌN 1 TRONG CÁC OPTION SAU)

        // OPTION 1: Màu xám nhẹ (khuyên dùng - dễ nhìn nhất)
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

        // OPTION 2: Màu kem/vàng nhạt (ấm áp, thân thiện)
        // style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());

        // OPTION 3: Màu xanh lá nhạt (tươi mát nhưng không chói)
        // style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());

        // OPTION 4: Màu hồng nhạt (dịu nhẹ)
        // style.setFillForegroundColor(IndexedColors.ROSE.getIndex());

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    /**
     * Tạo data row cải tiến cho mẫu 1
     */
    private void createMau1DataRowImproved(Sheet sheet, int rowNum, int day, NhatKyDieuDuong nhatKy, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;

        // TẠO STYLE CHO DÒNG TRỐNG NỂU KHÔNG CÓ DỮ LIỆU
        CellStyle styleToUse = dataStyle;
        if (nhatKy == null) {
            styleToUse = createEmptyRowStyle(sheet.getWorkbook());
        }

        // Ngày - luôn dùng style bình thường
        Cell dateCell = row.createCell(colNum++);
        dateCell.setCellValue(day);
        dateCell.setCellStyle(dataStyle);

        if (nhatKy != null) {
            // Có dữ liệu - fill như cũ
            setCellValue(row, colNum++, nhatKy.getGiuongThucKe(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getGiuongChiTieu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongBenhCu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getBnVaoVien(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongXuatVien(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getChuyenVien(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getChuyenKhoa(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTronVien(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXinVe(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTuVong(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getBenhHienCo(), dataStyle);

            // Tình hình sản phụ
            setCellValue(row, colNum++, nhatKy.getSanhThuong(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getSanhMo(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getMoPhuKhoa(), dataStyle);

            // Tình hình phẫu thuật
            setCellValue(row, colNum++, nhatKy.getCapCuu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getChuongTrinh(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getThuThuat(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTieuPhau(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getPhauThuat(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getPtLoaiI(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getPtLoaiII(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getPtLoaiIII(), dataStyle);

            // Chăm sóc điều dưỡng - THÊM TRUYỀN MÁU
            setCellValue(row, colNum++, nhatKy.getThoCpap(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getThoMay(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getThoOxy(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getBopBong(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getMonitor(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getCvp(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNoiKhiQuan(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNoiSoi(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getSondeDaDay(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getSondeTieu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getHutDamNhot(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTruyenMau(), dataStyle); // THÊM MỚI

            // Phân cấp chăm sóc - CHIA CS CẤP I
            setCellValue(row, colNum++, nhatKy.getTongCsCapI(), dataStyle); // THAY ĐỔI
            setCellValue(row, colNum++, nhatKy.getCsCapIMoi(), dataStyle);   // THÊM MỚI
            setCellValue(row, colNum++, nhatKy.getCsCapII(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getCsCapIII(), dataStyle);

            // Tình hình KCB
            setCellValue(row, colNum++, nhatKy.getTsNbKcb(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTsNbCapCuu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNgoaiVien(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getChuyenNoiTru(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getChuyenCapCuu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getChuyenVienKcb(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getChuyenPkKNgoai(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTuVongKcb(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongNbDoDienTim(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongNbDoDienCo(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongNbDoChucNangHoHap(), dataStyle);
        } else {
            // KHÔNG CÓ DỮ LIỆU - FILL CÁC CELL VỚI GIÁ TRỊ 0 VÀ STYLE MÀU KHÁC
            for (int i = 1; i < 50; i++) { // 49 columns + date column (tăng từ 48)
                Cell cell = row.createCell(colNum++);
                cell.setCellValue(0);
                cell.setCellStyle(styleToUse);
            }
        }
    }

    /**
     * Tạo data row cải tiến cho mẫu 2
     */
    private void createMau2DataRowImproved(Sheet sheet, int rowNum, int day, NhatKyDieuDuong nhatKy, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;

        // TẠO STYLE CHO DÒNG TRỐNG NỂU KHÔNG CÓ DỮ LIỆU
        CellStyle styleToUse = dataStyle;
        if (nhatKy == null) {
            styleToUse = createEmptyRowStyle(sheet.getWorkbook());
        }

        // Ngày - luôn dùng style bình thường
        Cell dateCell = row.createCell(colNum++);
        dateCell.setCellValue(day);
        dateCell.setCellStyle(dataStyle);

        if (nhatKy != null) {
            // Có dữ liệu - SỬA THEO TRƯỜNG MỚI
            setCellValue(row, colNum++, nhatKy.getDieuDuong(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getHoSinh(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getKyThuatY(), dataStyle);     // ĐỔI TỪ kyThuatVien
            setCellValue(row, colNum++, nhatKy.getNhanSuKhac(), dataStyle);   // BỎ ySi
            setCellValue(row, colNum++, nhatKy.getHoLyNhanSu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongNhanSu(), dataStyle);

            // Hiện diện
            setCellValue(row, colNum++, nhatKy.getDdtKhoa(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getDdhc(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getPhongKham(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTourSang(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTourChieu(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTourDem(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTruc2424(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getHoLyHienDien(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongHienDien(), dataStyle);

            // Vắng
            setCellValue(row, colNum++, nhatKy.getRaTruc(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getBuTruc(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNghiPhep(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNghiOm(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNghiHauSan(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNghiKhac(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getDiHoc(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getCongTac(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getHoLyVang(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getTongVang(), dataStyle);

            // Đào tạo - BỎ svYSi, svDuoc, ĐỔI svKtv
            setCellValue(row, colNum++, nhatKy.getNhanVienThuViec(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getThucHanhKLuong(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getNhanSuTangCuong(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getSvDdHs(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getSvKty(), dataStyle);        // ĐỔI TỪ svKtv
        } else {
            // KHÔNG CÓ DỮ LIỆU - FILL CÁC CELL VỚI GIÁ TRỊ 0 VÀ STYLE MÀU KHÁC
            for (int i = 1; i < 31; i++) { // 30 columns + date column (giảm từ 34)
                Cell cell = row.createCell(colNum++);
                cell.setCellValue(0);
                cell.setCellStyle(styleToUse);
            }
        }
    }

    /**
     * Set column widths cho mẫu 1 với text dọc
     */
    private void setMau1ColumnWidths(Sheet sheet) {
        // Cột ngày rộng hơn
        sheet.setColumnWidth(0, 2300);

        // Các cột dữ liệu nhỏ hơn vì text đã dọc
        for (int i = 1; i < 50; i++) { // TĂNG TỪ 48 LÊN 50
            sheet.setColumnWidth(i, 1200);
        }
    }

    /**
     * Set column widths cho mẫu 2 với text dọc
     */
    private void setMau2ColumnWidths(Sheet sheet) {
        // Cột ngày rộng hơn
        sheet.setColumnWidth(0, 2300);

        // Các cột dữ liệu nhỏ hơn vì text đã dọc
        for (int i = 1; i < 31; i++) { // GIẢM TỪ 34 XUỐNG 31
            sheet.setColumnWidth(i, 1200);
        }
    }

    // Helper methods
    private void setCellValue(Row row, int colNum, Integer value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    // ✅ CÁC STYLE MỚI VỚI FONT TIMES NEW ROMAN

    /**
     * ✅ Tạo style cho diagonal header với border và text wrap
     */
    private CellStyle createDiagonalHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // ✅ BORDERS ĐẦY ĐỦ
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THICK);
        style.setBorderRight(BorderStyle.THICK);

        // Background
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ✅ WRAP TEXT ĐỂ HIỂN THỊ NHIỀU DÒNG
        style.setWrapText(true);

        return style;
    }

    /**
     * ✅ TẠO PHẦN CHỮ KÝ 3 VỊ TRÍ Ở CUỐI FILE EXCEL - CẢI TIẾN CHO TẤT CẢ MẪU
     */
    private void createSignatureSection(Sheet sheet, int startRow, CellStyle baseStyle) {
        // Tạo style cho chữ ký
        CellStyle signatureStyle = createSignatureStyle(sheet.getWorkbook());
        CellStyle signatureDateStyle = createSignatureDateStyle(sheet.getWorkbook());

        // ✅ TÍNH TOÁN VỊ TRÍ 3 CỘT DỰA TRÊN TỔNG SỐ CỘT CỦA BẢNG
        int totalColumns = getTotalColumnsForSheet(sheet);

        // ✅ CHIA ĐỀU 3 PHẦN: TRÁI - GIỮA - PHẢI (TÍNH TOÁN THÔNG MINH)
        int leftCol, centerCol, rightCol;

        if (totalColumns <= 20) { // Mẫu 3 (19 cột)
            leftCol = 2;
            centerCol = 8;
            rightCol = 14;
        } else if (totalColumns <= 35) { // Mẫu 2 (34 cột)
            leftCol = 4;
            centerCol = 15;
            rightCol = 26;
        } else { // Mẫu 1 (48 cột)
            leftCol = 6;
            centerCol = 22;
            rightCol = 38;
        }

        // Row 1: Ngày tháng năm (bên phải)
        Row dateRow = sheet.createRow(startRow);
        Cell dateCell = dateRow.createCell(rightCol + 2); // Lệch ra một chút
        dateCell.setCellValue("Tân Phú, ngày ... tháng ... năm 2025");
        dateCell.setCellStyle(signatureDateStyle);

        // Row 2: Trống
        sheet.createRow(startRow + 1);

        // Row 3: Tiêu đề chữ ký - 3 VỊ TRÍ
        Row titleRow = sheet.createRow(startRow + 2);

        // ✅ VỊ TRÍ 1: TRƯỞNG PHÒNG ĐIỀU DƯỠNG (TRÁI)
        Cell tpddCell = titleRow.createCell(leftCol);
        tpddCell.setCellValue("Tp. PHÒNG ĐIỀU DƯỠNG");
        tpddCell.setCellStyle(signatureStyle);

        // ✅ VỊ TRÍ 2: TRƯỞNG KHOA (GIỮA)
        Cell truongKhoaCell = titleRow.createCell(centerCol);
        truongKhoaCell.setCellValue("TRƯỞNG KHOA");
        truongKhoaCell.setCellStyle(signatureStyle);

        // ✅ VỊ TRÍ 3: NGƯỜI LẬP (PHẢI)
        Cell nguoiLapCell = titleRow.createCell(rightCol);
        nguoiLapCell.setCellValue("NGƯỜI LẬP");
        nguoiLapCell.setCellStyle(signatureStyle);

        // Row 4: Ghi chú (ký, họ tên)
        Row noteRow = sheet.createRow(startRow + 3);

        Cell tpddNoteCell = noteRow.createCell(leftCol);
        tpddNoteCell.setCellValue("(Ký, họ tên)");
        tpddNoteCell.setCellStyle(signatureDateStyle);

        Cell truongKhoaNoteCell = noteRow.createCell(centerCol);
        truongKhoaNoteCell.setCellValue("(Ký, họ tên)");
        truongKhoaNoteCell.setCellStyle(signatureDateStyle);

        Cell nguoiLapNoteCell = noteRow.createCell(rightCol);
        nguoiLapNoteCell.setCellValue("(Ký, họ tên)");
        nguoiLapNoteCell.setCellStyle(signatureDateStyle);

        // ✅ THÊM 5 DÒNG TRỐNG CHO CHỮ KÝ TAY
        for (int i = 0; i < 5; i++) {
            sheet.createRow(startRow + 4 + i);
        }
    }

    /**
     * ✅ HÀM HELPER: TÍNH TỔNG SỐ CỘT CỦA BẢNG DỰA TRÊN LOẠI MẪU
     */
    private int getTotalColumnsForSheet(Sheet sheet) {
        String sheetName = sheet.getSheetName().toLowerCase();

        if (sheetName.contains("cận lâm sàng") || sheetName.contains("mau3")) {
            return 20; // ✅ SỬA: Mẫu 3: 19 cột dữ liệu + 1 cột ngày = 20 cột total
        } else if (sheetName.contains("nhân sự") || sheetName.contains("mau2")) {
            return 31; // Mẫu 2: 30 cột dữ liệu + 1 cột ngày
        } else { // Default cho mẫu 1 hoặc tên khác
            return 50; // Mẫu 1: 49 cột dữ liệu + 1 cột ngày
        }
    }

    /**
     * ✅ CẢI TIẾN SIGNATURE STYLES - FONT LỚN HỚN, ĐẸP HỚN
     */
    private CellStyle createSignatureStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 13); // ✅ TĂNG TỪ 12 LÊN 13
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // ✅ THÊM BORDER NHẸ ĐỂ NỔI BẬT
        style.setBorderBottom(BorderStyle.THIN);

        return style;
    }

    /**
     * ✅ Style cho ngày tháng và ghi chú chữ ký - CẢI TIẾN
     */
    private CellStyle createSignatureDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setItalic(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * ✅ Tạo style cho header với text dọc và Times New Roman
     */
    private CellStyle createVerticalHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // QUAN TRỌNG: Set text dọc (90 độ)
        style.setRotation((short) 90);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createGroupHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * ✅ Style cho title bên trái
     */
    private CellStyle createLeftTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * ✅ Style cho title bên phải
     */
    private CellStyle createRightTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * ✅ Style cho title ở giữa
     */
    private CellStyle createCenterTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * ✅ Style cho title chính (NHẬT KÝ ĐIỀU DƯỠNG)
     */
    private CellStyle createMainTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 16); // Font lớn hơn
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    // ✅ CÁC CLASS VÀ METHOD CHO TỔNG CỘNG

    /**
     * Class để lưu tổng cộng cho mẫu 1
     */
    private static class Mau1Totals {
        int giuongThucKe = 0, giuongChiTieu = 0, tongBenhCu = 0, bnVaoVien = 0, tongXuatVien = 0;
        int chuyenVien = 0, chuyenKhoa = 0, tronVien = 0, xinVe = 0, tuVong = 0, benhHienCo = 0;
        int sanhThuong = 0, sanhMo = 0, moPhuKhoa = 0;
        int capCuu = 0, chuongTrinh = 0, thuThuat = 0, tieuPhau = 0, phauThuat = 0;
        int ptLoaiI = 0, ptLoaiII = 0, ptLoaiIII = 0;
        int thoCpap = 0, thoMay = 0, thoOxy = 0, bopBong = 0, monitor = 0, cvp = 0;
        int noiKhiQuan = 0, noiSoi = 0, sondeDaDay = 0, sondeTieu = 0, hutDamNhot = 0, truyenMau = 0; // THÊM truyenMau
        int tongCsCapI = 0, csCapIMoi = 0, csCapII = 0, csCapIII = 0; // SỬA csCapI thành tongCsCapI và thêm csCapIMoi
        int tsNbKcb = 0, tsNbCapCuu = 0, ngoaiVien = 0, chuyenNoiTru = 0, chuyenCapCuu = 0;
        int chuyenVienKcb = 0, chuyenPkKNgoai = 0, tuVongKcb = 0;
        int tongNbDoDienTim = 0, tongNbDoDienCo = 0, tongNbDoChucNangHoHap = 0;

        void add(NhatKyDieuDuong nk) {
            giuongThucKe += nvl(nk.getGiuongThucKe());
            giuongChiTieu += nvl(nk.getGiuongChiTieu());
            tongBenhCu += nvl(nk.getTongBenhCu());
            bnVaoVien += nvl(nk.getBnVaoVien());
            tongXuatVien += nvl(nk.getTongXuatVien());
            chuyenVien += nvl(nk.getChuyenVien());
            chuyenKhoa += nvl(nk.getChuyenKhoa());
            tronVien += nvl(nk.getTronVien());
            xinVe += nvl(nk.getXinVe());
            tuVong += nvl(nk.getTuVong());
            benhHienCo += nvl(nk.getBenhHienCo());

            sanhThuong += nvl(nk.getSanhThuong());
            sanhMo += nvl(nk.getSanhMo());
            moPhuKhoa += nvl(nk.getMoPhuKhoa());

            capCuu += nvl(nk.getCapCuu());
            chuongTrinh += nvl(nk.getChuongTrinh());
            thuThuat += nvl(nk.getThuThuat());
            tieuPhau += nvl(nk.getTieuPhau());
            phauThuat += nvl(nk.getPhauThuat());
            ptLoaiI += nvl(nk.getPtLoaiI());
            ptLoaiII += nvl(nk.getPtLoaiII());
            ptLoaiIII += nvl(nk.getPtLoaiIII());

            thoCpap += nvl(nk.getThoCpap());
            thoMay += nvl(nk.getThoMay());
            thoOxy += nvl(nk.getThoOxy());
            bopBong += nvl(nk.getBopBong());
            monitor += nvl(nk.getMonitor());
            cvp += nvl(nk.getCvp());
            noiKhiQuan += nvl(nk.getNoiKhiQuan());
            noiSoi += nvl(nk.getNoiSoi());
            sondeDaDay += nvl(nk.getSondeDaDay());
            sondeTieu += nvl(nk.getSondeTieu());
            hutDamNhot += nvl(nk.getHutDamNhot());
            truyenMau += nvl(nk.getTruyenMau()); // THÊM MỚI

            tongCsCapI += nvl(nk.getTongCsCapI()); // SỬA TỪ getCsCapI
            csCapIMoi += nvl(nk.getCsCapIMoi());   // THÊM MỚI
            csCapII += nvl(nk.getCsCapII());
            csCapIII += nvl(nk.getCsCapIII());

            tsNbKcb += nvl(nk.getTsNbKcb());
            tsNbCapCuu += nvl(nk.getTsNbCapCuu());
            ngoaiVien += nvl(nk.getNgoaiVien());
            chuyenNoiTru += nvl(nk.getChuyenNoiTru());
            chuyenCapCuu += nvl(nk.getChuyenCapCuu());
            chuyenVienKcb += nvl(nk.getChuyenVienKcb());
            chuyenPkKNgoai += nvl(nk.getChuyenPkKNgoai());
            tuVongKcb += nvl(nk.getTuVongKcb());
            tongNbDoDienTim += nvl(nk.getTongNbDoDienTim());
            tongNbDoDienCo += nvl(nk.getTongNbDoDienCo());
            tongNbDoChucNangHoHap += nvl(nk.getTongNbDoChucNangHoHap());
        }
    }

    /**
     * Class để lưu tổng cộng cho mẫu 2
     */
    private static class Mau2Totals {
        int dieuDuong = 0, hoSinh = 0, kyThuatY = 0, nhanSuKhac = 0, hoLyNhanSu = 0, tongNhanSu = 0; // BỎ ySi, SỬA kyThuatVien
        int ddtKhoa = 0, ddhc = 0, phongKham = 0, tourSang = 0, tourChieu = 0, tourDem = 0;
        int truc2424 = 0, hoLyHienDien = 0, tongHienDien = 0;
        int raTruc = 0, buTruc = 0, nghiPhep = 0, nghiOm = 0, nghiHauSan = 0, nghiKhac = 0;
        int diHoc = 0, congTac = 0, hoLyVang = 0, tongVang = 0;
        int nhanVienThuViec = 0, thucHanhKLuong = 0, nhanSuTangCuong = 0;
        int svDdHs = 0, svKty = 0; // BỎ svYSi, svDuoc, SỬA svKtv

        void add(NhatKyDieuDuong nk) {
            dieuDuong += nvl(nk.getDieuDuong());
            hoSinh += nvl(nk.getHoSinh());
            kyThuatY += nvl(nk.getKyThuatY());     // SỬA TỪ getKyThuatVien
            nhanSuKhac += nvl(nk.getNhanSuKhac()); // BỎ ySi
            hoLyNhanSu += nvl(nk.getHoLyNhanSu());
            tongNhanSu += nvl(nk.getTongNhanSu());

            ddtKhoa += nvl(nk.getDdtKhoa());
            ddhc += nvl(nk.getDdhc());
            phongKham += nvl(nk.getPhongKham());
            tourSang += nvl(nk.getTourSang());
            tourChieu += nvl(nk.getTourChieu());
            tourDem += nvl(nk.getTourDem());
            truc2424 += nvl(nk.getTruc2424());
            hoLyHienDien += nvl(nk.getHoLyHienDien());
            tongHienDien += nvl(nk.getTongHienDien());

            raTruc += nvl(nk.getRaTruc());
            buTruc += nvl(nk.getBuTruc());
            nghiPhep += nvl(nk.getNghiPhep());
            nghiOm += nvl(nk.getNghiOm());
            nghiHauSan += nvl(nk.getNghiHauSan());
            nghiKhac += nvl(nk.getNghiKhac());
            diHoc += nvl(nk.getDiHoc());
            congTac += nvl(nk.getCongTac());
            hoLyVang += nvl(nk.getHoLyVang());
            tongVang += nvl(nk.getTongVang());

            nhanVienThuViec += nvl(nk.getNhanVienThuViec());
            thucHanhKLuong += nvl(nk.getThucHanhKLuong());
            nhanSuTangCuong += nvl(nk.getNhanSuTangCuong());
            svDdHs += nvl(nk.getSvDdHs());
            svKty += nvl(nk.getSvKty()); // SỬA TỪ getSvKtv
            // BỎ svYSi và svDuoc
        }
    }

    /**
     * Helper method: null value to 0
     */
    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Tạo dòng tổng cộng cho mẫu 1
     */
    private void createMau1TotalRow(Sheet sheet, int rowNum, Mau1Totals totals, CellStyle totalStyle) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;

        // Cột "TỔNG CỘNG"
        Cell totalCell = row.createCell(colNum++);
        totalCell.setCellValue("TỔNG CỘNG");
        totalCell.setCellStyle(totalStyle);

        // Tất cả các cột dữ liệu
        setCellValue(row, colNum++, totals.giuongThucKe, totalStyle);
        setCellValue(row, colNum++, totals.giuongChiTieu, totalStyle);
        setCellValue(row, colNum++, totals.tongBenhCu, totalStyle);
        setCellValue(row, colNum++, totals.bnVaoVien, totalStyle);
        setCellValue(row, colNum++, totals.tongXuatVien, totalStyle);
        setCellValue(row, colNum++, totals.chuyenVien, totalStyle);
        setCellValue(row, colNum++, totals.chuyenKhoa, totalStyle);
        setCellValue(row, colNum++, totals.tronVien, totalStyle);
        setCellValue(row, colNum++, totals.xinVe, totalStyle);
        setCellValue(row, colNum++, totals.tuVong, totalStyle);
        setCellValue(row, colNum++, totals.benhHienCo, totalStyle);

        setCellValue(row, colNum++, totals.sanhThuong, totalStyle);
        setCellValue(row, colNum++, totals.sanhMo, totalStyle);
        setCellValue(row, colNum++, totals.moPhuKhoa, totalStyle);

        setCellValue(row, colNum++, totals.capCuu, totalStyle);
        setCellValue(row, colNum++, totals.chuongTrinh, totalStyle);
        setCellValue(row, colNum++, totals.thuThuat, totalStyle);
        setCellValue(row, colNum++, totals.tieuPhau, totalStyle);
        setCellValue(row, colNum++, totals.phauThuat, totalStyle);
        setCellValue(row, colNum++, totals.ptLoaiI, totalStyle);
        setCellValue(row, colNum++, totals.ptLoaiII, totalStyle);
        setCellValue(row, colNum++, totals.ptLoaiIII, totalStyle);

        setCellValue(row, colNum++, totals.thoCpap, totalStyle);
        setCellValue(row, colNum++, totals.thoMay, totalStyle);
        setCellValue(row, colNum++, totals.thoOxy, totalStyle);
        setCellValue(row, colNum++, totals.bopBong, totalStyle);
        setCellValue(row, colNum++, totals.monitor, totalStyle);
        setCellValue(row, colNum++, totals.cvp, totalStyle);
        setCellValue(row, colNum++, totals.noiKhiQuan, totalStyle);
        setCellValue(row, colNum++, totals.noiSoi, totalStyle);
        setCellValue(row, colNum++, totals.sondeDaDay, totalStyle);
        setCellValue(row, colNum++, totals.sondeTieu, totalStyle);
        setCellValue(row, colNum++, totals.hutDamNhot, totalStyle);
        setCellValue(row, colNum++, totals.truyenMau, totalStyle); // THÊM MỚI

        setCellValue(row, colNum++, totals.tongCsCapI, totalStyle); // SỬA TỪ csCapI
        setCellValue(row, colNum++, totals.csCapIMoi, totalStyle);  // THÊM MỚI
        setCellValue(row, colNum++, totals.csCapII, totalStyle);
        setCellValue(row, colNum++, totals.csCapIII, totalStyle);

        setCellValue(row, colNum++, totals.tsNbKcb, totalStyle);
        setCellValue(row, colNum++, totals.tsNbCapCuu, totalStyle);
        setCellValue(row, colNum++, totals.ngoaiVien, totalStyle);
        setCellValue(row, colNum++, totals.chuyenNoiTru, totalStyle);
        setCellValue(row, colNum++, totals.chuyenCapCuu, totalStyle);
        setCellValue(row, colNum++, totals.chuyenVienKcb, totalStyle);
        setCellValue(row, colNum++, totals.chuyenPkKNgoai, totalStyle);
        setCellValue(row, colNum++, totals.tuVongKcb, totalStyle);
        setCellValue(row, colNum++, totals.tongNbDoDienTim, totalStyle);
        setCellValue(row, colNum++, totals.tongNbDoDienCo, totalStyle);
        setCellValue(row, colNum++, totals.tongNbDoChucNangHoHap, totalStyle);
    }

    /**
     * Tạo dòng tổng cộng cho mẫu 2
     */
    private void createMau2TotalRow(Sheet sheet, int rowNum, Mau2Totals totals, CellStyle totalStyle) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;

        // Cột "TỔNG CỘNG"
        Cell totalCell = row.createCell(colNum++);
        totalCell.setCellValue("TỔNG CỘNG");
        totalCell.setCellStyle(totalStyle);

        // Tất cả các cột dữ liệu
        setCellValue(row, colNum++, totals.dieuDuong, totalStyle);
        setCellValue(row, colNum++, totals.hoSinh, totalStyle);
        setCellValue(row, colNum++, totals.kyThuatY, totalStyle);    // SỬA TỪ kyThuatVien
        setCellValue(row, colNum++, totals.nhanSuKhac, totalStyle); // BỎ ySi
        setCellValue(row, colNum++, totals.hoLyNhanSu, totalStyle);
        setCellValue(row, colNum++, totals.tongNhanSu, totalStyle);

        setCellValue(row, colNum++, totals.ddtKhoa, totalStyle);
        setCellValue(row, colNum++, totals.ddhc, totalStyle);
        setCellValue(row, colNum++, totals.phongKham, totalStyle);
        setCellValue(row, colNum++, totals.tourSang, totalStyle);
        setCellValue(row, colNum++, totals.tourChieu, totalStyle);
        setCellValue(row, colNum++, totals.tourDem, totalStyle);
        setCellValue(row, colNum++, totals.truc2424, totalStyle);
        setCellValue(row, colNum++, totals.hoLyHienDien, totalStyle);
        setCellValue(row, colNum++, totals.tongHienDien, totalStyle);

        setCellValue(row, colNum++, totals.raTruc, totalStyle);
        setCellValue(row, colNum++, totals.buTruc, totalStyle);
        setCellValue(row, colNum++, totals.nghiPhep, totalStyle);
        setCellValue(row, colNum++, totals.nghiOm, totalStyle);
        setCellValue(row, colNum++, totals.nghiHauSan, totalStyle);
        setCellValue(row, colNum++, totals.nghiKhac, totalStyle);
        setCellValue(row, colNum++, totals.diHoc, totalStyle);
        setCellValue(row, colNum++, totals.congTac, totalStyle);
        setCellValue(row, colNum++, totals.hoLyVang, totalStyle);
        setCellValue(row, colNum++, totals.tongVang, totalStyle);

        setCellValue(row, colNum++, totals.nhanVienThuViec, totalStyle);
        setCellValue(row, colNum++, totals.thucHanhKLuong, totalStyle);
        setCellValue(row, colNum++, totals.nhanSuTangCuong, totalStyle);
        setCellValue(row, colNum++, totals.svDdHs, totalStyle);
        setCellValue(row, colNum++, totals.svKty, totalStyle); // SỬA TỪ svKtv
        // BỎ svYSi và svDuoc
    }

    /**
     * ✅ Tạo style cho dòng tổng cộng với Times New Roman
     */
    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Border dày cho dòng tổng
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Background màu vàng nổi bật
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    /**
     * ✅ TẠO SHEET CẢI TIẾN CHO MẪU 3 - KHỐI CẬN LÂM SÀNG VỚI DIAGONAL HEADER ĐÚNG
     */
    private void createImprovedMau3Sheet(Workbook workbook, Map<Integer, NhatKyDieuDuong> dataMap,
                                         Integer thang, Integer nam, Long khoaPhongId) {
        XSSFSheet sheet = (XSSFSheet) workbook.createSheet("Nhật ký quản lý khoa khối cận lâm sàng");

        // Tạo các style
        CellStyle headerStyle = createVerticalHeaderStyle(workbook);
        CellStyle groupHeaderStyle = createGroupHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle diagonalHeaderStyle = createDiagonalHeaderStyle(workbook);

        int rowNum = 0;

        // Header thông tin
        String khoaPhongName = dataMap.values().stream().findFirst()
                .map(nk -> nk.getKhoaPhong().getTenKhoaPhong())
                .orElse("Khoa " + khoaPhongId);

        // Title section
        createImprovedTitleSection(sheet, rowNum, thang, nam, khoaPhongName,
                "NHẬT KÝ QUẢN LÝ KHOA KHỐI CẬN LÂM SÀNG", titleStyle);
        rowNum += 8;

        // ✅ SỬ DỤNG DIAGONAL HEADER ĐẶC BIỆT CHO MẪU 3
        createDiagonalDateHeaderMau3(sheet, rowNum, diagonalHeaderStyle);

        // ✅ TẠO HEADERS 3 TẦNG CHO MẪU 3
        createMau3VerticalHeaders(sheet, rowNum, headerStyle, groupHeaderStyle);
        rowNum += 3; // Headers có 3 rows

        // Tạo data cho tất cả ngày trong tháng
        YearMonth yearMonth = YearMonth.of(nam, thang);
        int daysInMonth = yearMonth.lengthOfMonth();

        // Tính toán tổng cộng cho mẫu 3
        Mau3Totals totals = new Mau3Totals();

        for (int day = 1; day <= daysInMonth; day++) {
            NhatKyDieuDuong nhatKy = dataMap.get(day);
            createMau3DataRowImproved(sheet, rowNum++, day, nhatKy, dataStyle);

            if (nhatKy != null) {
                totals.add(nhatKy);
            }
        }

        // Thêm dòng tổng cộng
        CellStyle totalStyle = createTotalStyle(workbook);
        createMau3TotalRow(sheet, rowNum, totals, totalStyle);
        rowNum++;

        // ✅ THÊM PHẦN CHỮ KÝ VỚI VỊ TRÍ CHÍNH XÁC CHO MẪU 3
        createSignatureSectionMau3(sheet, rowNum + 2, titleStyle);

        // Set column widths
        setMau3ColumnWidths(sheet);
    }

    /**
     * ✅ TẠO PHẦN CHỮ KÝ RIÊNG CHO MẪU 3 VỚI VỊ TRÍ CHÍNH XÁC
     */
    private void createSignatureSectionMau3(Sheet sheet, int startRow, CellStyle baseStyle) {
        // Tạo style cho chữ ký
        CellStyle signatureStyle = createSignatureStyle(sheet.getWorkbook());
        CellStyle signatureDateStyle = createSignatureDateStyle(sheet.getWorkbook());

        // ✅ VỊ TRÍ CHÍNH XÁC CHO MẪU 3 (18 CỘT DỮ LIỆU + 1 CỘT NGÀY = 19 CỘT)
        // Chia đều thành 3 phần trên 19 cột
        int leftCol = 2;    // Cột 2 cho vị trí trái
        int centerCol = 9;  // Cột 9 cho vị trí giữa (19/2 ≈ 9-10)
        int rightCol = 16;  // Cột 16 cho vị trí phải

        // Row 1: Ngày tháng năm (bên phải)
        Row dateRow = sheet.createRow(startRow);
        Cell dateCell = dateRow.createCell(rightCol + 1); // Lệch ra một chút từ cột 16
        dateCell.setCellValue("Tân Phú, ngày ... tháng ... năm 2025");
        dateCell.setCellStyle(signatureDateStyle);

        // Row 2: Trống
        sheet.createRow(startRow + 1);

        // Row 3: Tiêu đề chữ ký - 3 VỊ TRÍ CHÍNH XÁC
        Row titleRow = sheet.createRow(startRow + 2);

        // ✅ VỊ TRÍ 1: TRƯỞNG PHÒNG ĐIỀU DƯỠNG (CỘT 2)
        Cell tpddCell = titleRow.createCell(leftCol);
        tpddCell.setCellValue("Tp. PHÒNG ĐIỀU DƯỠNG");
        tpddCell.setCellStyle(signatureStyle);

        // ✅ VỊ TRÍ 2: TRƯỞNG KHOA (CỘT 9)
        Cell truongKhoaCell = titleRow.createCell(centerCol);
        truongKhoaCell.setCellValue("TRƯỞNG KHOA");
        truongKhoaCell.setCellStyle(signatureStyle);

        // ✅ VỊ TRÍ 3: NGƯỜI LẬP (CỘT 16)
        Cell nguoiLapCell = titleRow.createCell(rightCol);
        nguoiLapCell.setCellValue("NGƯỜI LẬP");
        nguoiLapCell.setCellStyle(signatureStyle);

        // Row 4: Ghi chú (ký, họ tên)
        Row noteRow = sheet.createRow(startRow + 3);

        Cell tpddNoteCell = noteRow.createCell(leftCol);
        tpddNoteCell.setCellValue("(Ký, họ tên)");
        tpddNoteCell.setCellStyle(signatureDateStyle);

        Cell truongKhoaNoteCell = noteRow.createCell(centerCol);
        truongKhoaNoteCell.setCellValue("(Ký, họ tên)");
        truongKhoaNoteCell.setCellStyle(signatureDateStyle);

        Cell nguoiLapNoteCell = noteRow.createCell(rightCol);
        nguoiLapNoteCell.setCellValue("(Ký, họ tên)");
        nguoiLapNoteCell.setCellStyle(signatureDateStyle);

        // ✅ THÊM 5 DÒNG TRỐNG CHO CHỮ KÝ TAY
        for (int i = 0; i < 5; i++) {
            sheet.createRow(startRow + 4 + i);
        }
    }

    // ✅ THÊM CÁC METHOD HỖ TRỢ CHO MẪU 3

    /**
     * ✅ SỬA LẠI HEADERS CHO MẪU 3 - ĐỔI THỨ TỰ: NGƯỜI BỆNH TRƯỚC, MẪU XÉT NGHIỆM SAU
     */
    private void createMau3VerticalHeaders(Sheet sheet, int rowNum, CellStyle headerStyle, CellStyle groupHeaderStyle) {
        // Row 1: Group headers chính (tầng 1)
        Row groupRow = sheet.getRow(rowNum);
        if (groupRow == null) {
            groupRow = sheet.createRow(rowNum);
        }
        int colNum = 1; // Bỏ qua cột ngày đã có diagonal

        // ✅ NHÓM 1: "Tình hình Khoa Xét nghiệm" (12 cột) - KHÔNG ĐỔI
        Cell xnMainCell = groupRow.createCell(colNum);
        xnMainCell.setCellValue("Tình hình Khoa Xét nghiệm");
        xnMainCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 11)); // 12 columns

        // Fill empty cells với border cho nhóm XN
        for (int i = colNum + 1; i <= colNum + 11; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 12;

        // ✅ NHÓM 2: "Tình hình khoa CĐHA" (7 cột) - KHÔNG ĐỔI
        Cell cdhaMainCell = groupRow.createCell(colNum);
        cdhaMainCell.setCellValue("Tình hình khoa CĐHA");
        cdhaMainCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + 6)); // 7 columns

        // Fill empty cells với border cho nhóm CĐHA
        for (int i = colNum + 1; i <= colNum + 6; i++) {
            Cell emptyCell = groupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }

        // ✅ Row 2: Sub-group headers (tầng 2) - ĐỔI THỨ TỰ: NGƯỜI BỆNH TRƯỚC
        Row subGroupRow = sheet.createRow(rowNum + 1);
        colNum = 1; // Reset về cột đầu

        // ✅ SUB-GROUP 1: "Người bệnh" (4 cột) - ĐỔI LÊN ĐẦU
        Cell nguoiBenhCell = subGroupRow.createCell(colNum);
        nguoiBenhCell.setCellValue("Người bệnh");
        nguoiBenhCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, colNum, colNum + 3)); // 4 columns

        for (int i = colNum + 1; i <= colNum + 3; i++) {
            Cell emptyCell = subGroupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 4;

        // ✅ SUB-GROUP 2: "Mẫu Xét nghiệm" (4 cột) - ĐỔI XUỐNG THỨ 2
        Cell mauXnCell = subGroupRow.createCell(colNum);
        mauXnCell.setCellValue("Mẫu Xét nghiệm");
        mauXnCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, colNum, colNum + 3)); // 4 columns

        for (int i = colNum + 1; i <= colNum + 3; i++) {
            Cell emptyCell = subGroupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 4;

        // ✅ SUB-GROUP 3: "Loại Xét nghiệm" (4 cột) - KHÔNG ĐỔI
        Cell loaiXnCell = subGroupRow.createCell(colNum);
        loaiXnCell.setCellValue("Loại Xét nghiệm");
        loaiXnCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, colNum, colNum + 3)); // 4 columns

        for (int i = colNum + 1; i <= colNum + 3; i++) {
            Cell emptyCell = subGroupRow.createCell(i);
            emptyCell.setCellStyle(groupHeaderStyle);
        }
        colNum += 4;

        // ✅ SUB-GROUP 4: "X-quang" (2 cột) - KHÔNG ĐỔI
        Cell xquangCell = subGroupRow.createCell(colNum);
        xquangCell.setCellValue("X-quang");
        xquangCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, colNum, colNum + 1)); // 2 columns

        Cell emptyXQ = subGroupRow.createCell(colNum + 1);
        emptyXQ.setCellStyle(groupHeaderStyle);
        colNum += 2;

        // ✅ SUB-GROUP 5: "CT Scanner" (3 cột) - KHÔNG ĐỔI
        Cell ctCell = subGroupRow.createCell(colNum);
        ctCell.setCellValue("CT Scanner");
        ctCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, colNum, colNum + 2)); // 3 columns

        Cell emptyCT1 = subGroupRow.createCell(colNum + 1);
        emptyCT1.setCellStyle(groupHeaderStyle);
        Cell emptyCT2 = subGroupRow.createCell(colNum + 2);
        emptyCT2.setCellStyle(groupHeaderStyle);
        colNum += 3;

        // ✅ SUB-GROUP 6: "Siêu Âm" (2 cột) - KHÔNG ĐỔI
        Cell sieuAmCell = subGroupRow.createCell(colNum);
        sieuAmCell.setCellValue("Siêu Âm");
        sieuAmCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, colNum, colNum + 1)); // 2 columns

        Cell emptySA = subGroupRow.createCell(colNum + 1);
        emptySA.setCellStyle(groupHeaderStyle);

        // ✅ Row 3: Detail headers với text dọc (tầng 3) - ĐỔI THỨ TỰ
        Row detailRow = sheet.createRow(rowNum + 2);
        colNum = 1; // Skip merged date cell

        String[] detailHeaders = {
                // ✅ Người bệnh (4 cột) - ĐỔI LÊN ĐẦU
                "Tổng số NB", "Ngoại trú", "Nội trú", "Cấp cứu",

                // ✅ Mẫu Xét nghiệm (4 cột) - ĐỔI XUỐNG THỨ 2
                "Tổng số mẫu", "Ngoại trú", "Nội trú", "Cấp cứu",

                // ✅ Loại xét nghiệm (4 cột) - KHÔNG ĐỔI
                "Huyết học", "Sinh hóa", "Vi sinh", "Giải phẫu bệnh",

                // ✅ X-quang (2 cột) - KHÔNG ĐỔI
                "Tổng số NB", "Tổng số phim X-quang",

                // ✅ CT Scanner (3 cột) - KHÔNG ĐỔI
                "NB có cản quang", "NB không cản quang", "Tổng số phim CT",

                // ✅ Siêu âm (2 cột) - KHÔNG ĐỔI
                "Tổng số NB", "Tổng số siêu âm"
        };

        // ✅ ÁP DỤNG STYLE CHO TẤT CẢ DETAIL HEADERS
        for (String header : detailHeaders) {
            Cell cell = detailRow.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }

        // ✅ KIỂM TRA SỐ LƯỢNG CỘT CUỐI CÙNG
        logger.info("✅ Mau3 total detail columns created: {}", colNum - 1);
        logger.info("✅ Expected: 19 columns (4+4+4+2+3+2 = 19) - order changed");
    }

    /**
     * Tạo data row cho mẫu 3 - ĐỔI THỨ TỰ: NGƯỜI BỆNH TRƯỚC, MẪU XÉT NGHIỆM SAU
     */
    private void createMau3DataRowImproved(Sheet sheet, int rowNum, int day, NhatKyDieuDuong nhatKy, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;

        // TẠO STYLE CHO DÒNG TRỐNG NỂU KHÔNG CÓ DỮ LIỆU
        CellStyle styleToUse = dataStyle;
        if (nhatKy == null) {
            styleToUse = createEmptyRowStyle(sheet.getWorkbook());
        }

        // Ngày - luôn dùng style bình thường
        Cell dateCell = row.createCell(colNum++);
        dateCell.setCellValue(day);
        dateCell.setCellStyle(dataStyle);

        if (nhatKy != null) {
            // Có dữ liệu - 19 cột dữ liệu với thứ tự mới: NGƯỜI BỆNH TRƯỚC

            // ✅ NHÓM 1: Người bệnh XÉT NGHIỆM (4 cột) - ĐỔI LÊN ĐẦU
            setCellValue(row, colNum++, nhatKy.getXnNbTongSo(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnNbNgoaiTru(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnNbNoiTru(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnNbCapCuu(), dataStyle);

            // ✅ NHÓM 2: Mẫu xét nghiệm (4 cột) - ĐỔI XUỐNG THỨ 2
            setCellValue(row, colNum++, nhatKy.getXnTongSoMau(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnMauNgoaiTru(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnMauNoiTru(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnMauCapCuu(), dataStyle);

            // ✅ NHÓM 3: Loại xét nghiệm (4 cột) - KHÔNG ĐỔI
            setCellValue(row, colNum++, nhatKy.getXnHuyetHoc(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnSinhHoa(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnViSinh(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getXnGiaiPhauBenh(), dataStyle);

            // ✅ NHÓM 4: Khoa CĐHA - X-quang (2 cột) - KHÔNG ĐỔI
            setCellValue(row, colNum++, nhatKy.getCdhaXqTongNb(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getCdhaXqTongPhim(), dataStyle);

            // ✅ NHÓM 5: CT Scanner (3 cột) - KHÔNG ĐỔI
            setCellValue(row, colNum++, nhatKy.getCdhaCTCoCanQuangNb(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getCdhaCTKhongCanQuangNb(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getCdhaCTTongPhim(), dataStyle);

            // ✅ NHÓM 6: Siêu âm (2 cột) - KHÔNG ĐỔI
            setCellValue(row, colNum++, nhatKy.getCdhaSATongNb(), dataStyle);
            setCellValue(row, colNum++, nhatKy.getCdhaSATongSo(), dataStyle); // Cột 19
        } else {
            // ✅ KHÔNG CÓ DỮ LIỆU - ĐẢM BẢO TẤT CẢ 19 CỘT ĐỀU CÓ MÀU XÁM
            for (int i = 1; i <= 19; i++) { // ✅ SỬA: chạy từ 1 đến 19 (bao gồm cột cuối)
                Cell cell = row.createCell(colNum++);
                cell.setCellValue(0);
                cell.setCellStyle(styleToUse); // ✅ Style có màu xám
            }
        }

        // ✅ THÊM LOG ĐỂ DEBUG
        logger.debug("Mau3 row {}: created {} columns with new order (NB first)", day, colNum - 1);
    }

    /**
     * Class để lưu tổng cộng cho mẫu 3
     */
    private static class Mau3Totals {
        int xnTongSoMau = 0, xnMauNgoaiTru = 0, xnMauNoiTru = 0, xnMauCapCuu = 0;
        int xnNbTongSo = 0, xnNbNgoaiTru = 0, xnNbNoiTru = 0, xnNbCapCuu = 0;
        int xnHuyetHoc = 0, xnSinhHoa = 0, xnViSinh = 0, xnGiaiPhauBenh = 0;
        int cdhaXqTongNb = 0, cdhaXqTongPhim = 0;
        int cdhaCTCoCanQuangNb = 0, cdhaCTKhongCanQuangNb = 0, cdhaCTTongPhim = 0; // SỬA CT Scanner
        int cdhaSATongNb = 0, cdhaSATongSo = 0;

        void add(NhatKyDieuDuong nk) {
            xnTongSoMau += nvl(nk.getXnTongSoMau());
            xnMauNgoaiTru += nvl(nk.getXnMauNgoaiTru());
            xnMauNoiTru += nvl(nk.getXnMauNoiTru());
            xnMauCapCuu += nvl(nk.getXnMauCapCuu());
            xnNbTongSo += nvl(nk.getXnNbTongSo());
            xnNbNgoaiTru += nvl(nk.getXnNbNgoaiTru());
            xnNbNoiTru += nvl(nk.getXnNbNoiTru());
            xnNbCapCuu += nvl(nk.getXnNbCapCuu());
            xnHuyetHoc += nvl(nk.getXnHuyetHoc());
            xnSinhHoa += nvl(nk.getXnSinhHoa());
            xnViSinh += nvl(nk.getXnViSinh());
            xnGiaiPhauBenh += nvl(nk.getXnGiaiPhauBenh());

            cdhaXqTongNb += nvl(nk.getCdhaXqTongNb());
            cdhaXqTongPhim += nvl(nk.getCdhaXqTongPhim());
            cdhaCTCoCanQuangNb += nvl(nk.getCdhaCTCoCanQuangNb());     // SỬA TỪ getCdhaCTTongNb
            cdhaCTKhongCanQuangNb += nvl(nk.getCdhaCTKhongCanQuangNb()); // THÊM MỚI
            cdhaCTTongPhim += nvl(nk.getCdhaCTTongPhim());
            cdhaSATongNb += nvl(nk.getCdhaSATongNb());
            cdhaSATongSo += nvl(nk.getCdhaSATongSo());
        }
    }

    /**
     * Tạo dòng tổng cộng cho mẫu 3 - ĐỔI THỨ TỰ: NGƯỜI BỆNH TRƯỚC, MẪU XÉT NGHIỆM SAU
     */
    private void createMau3TotalRow(Sheet sheet, int rowNum, Mau3Totals totals, CellStyle totalStyle) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;

        // Cột "TỔNG CỘNG"
        Cell totalCell = row.createCell(colNum++);
        totalCell.setCellValue("TỔNG CỘNG");
        totalCell.setCellStyle(totalStyle);

        // ✅ NHÓM 1: Người bệnh XÉT NGHIỆM (4 cột) - ĐỔI LÊN ĐẦU
        setCellValue(row, colNum++, totals.xnNbTongSo, totalStyle);
        setCellValue(row, colNum++, totals.xnNbNgoaiTru, totalStyle);
        setCellValue(row, colNum++, totals.xnNbNoiTru, totalStyle);
        setCellValue(row, colNum++, totals.xnNbCapCuu, totalStyle);

        // ✅ NHÓM 2: Mẫu Xét nghiệm (4 cột) - ĐỔI XUỐNG THỨ 2
        setCellValue(row, colNum++, totals.xnTongSoMau, totalStyle);
        setCellValue(row, colNum++, totals.xnMauNgoaiTru, totalStyle);
        setCellValue(row, colNum++, totals.xnMauNoiTru, totalStyle);
        setCellValue(row, colNum++, totals.xnMauCapCuu, totalStyle);

        // ✅ NHÓM 3: Loại xét nghiệm (4 cột) - KHÔNG ĐỔI
        setCellValue(row, colNum++, totals.xnHuyetHoc, totalStyle);
        setCellValue(row, colNum++, totals.xnSinhHoa, totalStyle);
        setCellValue(row, colNum++, totals.xnViSinh, totalStyle);
        setCellValue(row, colNum++, totals.xnGiaiPhauBenh, totalStyle);

        // ✅ NHÓM 4: Khoa CĐHA - X-quang (2 cột) - KHÔNG ĐỔI
        setCellValue(row, colNum++, totals.cdhaXqTongNb, totalStyle);
        setCellValue(row, colNum++, totals.cdhaXqTongPhim, totalStyle);

        // ✅ NHÓM 5: CT Scanner (3 cột) - KHÔNG ĐỔI
        setCellValue(row, colNum++, totals.cdhaCTCoCanQuangNb, totalStyle);
        setCellValue(row, colNum++, totals.cdhaCTKhongCanQuangNb, totalStyle);
        setCellValue(row, colNum++, totals.cdhaCTTongPhim, totalStyle);

        // ✅ NHÓM 6: Siêu âm (2 cột) - KHÔNG ĐỔI
        setCellValue(row, colNum++, totals.cdhaSATongNb, totalStyle);
        setCellValue(row, colNum++, totals.cdhaSATongSo, totalStyle);

        // ✅ LOG KIỂM TRA
        logger.info("✅ Mau3 total row created with {} columns (new order: NB first)", colNum - 1);
    }

    /**
     * Set column widths cho mẫu 3
     */
    private void setMau3ColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 2300); // Cột ngày

        // ✅ SỬA LẠI: 1 CỘT NGÀY + 19 CỘT DỮ LIỆU = 20 CỘT TOTAL
        for (int i = 1; i <= 19; i++) { // 1-19 (19 columns data)
            sheet.setColumnWidth(i, 1200);
        }
    }


    /**
     * ✅ TẠO DIAGONAL HEADER ĐẶC BIỆT CHO MẪU 3 VỚI CHỮ "NGÀY" Ở DƯỚI
     */
    private void createDiagonalDateHeaderMau3(XSSFSheet sheet, int rowNum, CellStyle diagonalStyle) {
        // ✅ TẠO STYLE ĐẶC BIỆT CHO MẪU 3
        CellStyle mau3DiagonalStyle = createMau3DiagonalTextStyle(sheet.getWorkbook());

        Row groupRow = sheet.getRow(rowNum);
        if (groupRow == null) {
            groupRow = sheet.createRow(rowNum);
        }

        Row detailRow = sheet.getRow(rowNum + 1);
        if (detailRow == null) {
            detailRow = sheet.createRow(rowNum + 1);
        }

        Row thirdRow = sheet.getRow(rowNum + 2);
        if (thirdRow == null) {
            thirdRow = sheet.createRow(rowNum + 2);
        }

        // ✅ SET CHIỀU CAO CHO 3 ROWS
        groupRow.setHeightInPoints(40);
        detailRow.setHeightInPoints(40);
        thirdRow.setHeightInPoints(40);

        // ✅ TẠO CELL CHO "Nội dung" VÀ "Ngày"
        Cell dateCell = groupRow.createCell(0);

        // ✅ SỬ DỤNG RICH TEXT ĐỂ CÓ 2 PHẦN VỚI ALIGNMENT KHÁC NHAU
        XSSFWorkbook workbook = (XSSFWorkbook) sheet.getWorkbook();
        XSSFRichTextString richText = new XSSFRichTextString();

        // Font bold cho cả 2
        XSSFFont font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);

        // ✅ TẠO TEXT ĐÚNG VỊ TRÍ CHO MẪU 3
        richText.append("Nội dung", font);
        richText.append("\n\n\n\n\n\n\n\n"); // Nhiều line breaks để đẩy xuống dưới cùng
        richText.append("                                    Ngày", font); // Spaces để đẩy sang phải

        dateCell.setCellValue(richText);
        dateCell.setCellStyle(mau3DiagonalStyle);

        // ✅ MERGE CELL QUA 3 ROWS
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 2, 0, 0));

        // ✅ VẼ ĐƯỜNG CHÉO CHO MẪU 3
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(
                100, 100,     // dx1, dy1 - bắt đầu từ gần góc trên trái
                900, 1100,    // dx2, dy2 - kết thúc ở gần góc dưới phải (dài hơn vì 3 rows)
                0, rowNum,    // col1, row1
                1, rowNum + 3 // col2, row2 - span qua 3 rows
        );

        XSSFSimpleShape line = drawing.createSimpleShape(anchor);
        line.setShapeType(org.apache.poi.ss.usermodel.ShapeTypes.LINE);
        line.setLineStyleColor(0, 0, 0); // Màu đen
        line.setLineWidth(2.0);
    }

    /**
     * ✅ Style đặc biệt cho diagonal cell của mẫu 3
     */
    private CellStyle createMau3DiagonalTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);

        // ✅ ALIGNMENT VÀ WRAP TEXT
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true); // QUAN TRỌNG: Cho phép wrap text

        // Borders đầy đủ
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THICK);
        style.setBorderRight(BorderStyle.THICK);

        // Background
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }
}