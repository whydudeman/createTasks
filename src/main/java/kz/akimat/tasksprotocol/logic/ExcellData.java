package kz.akimat.tasksprotocol.logic;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcellData {
    public String protocolName;
    public Date protocolDate;
    public String protocolNumber;
    public String protocolPoint;
    public String taskText;
    public List<String> userControllers;
    public List<String> departments;
    public Date deadline;
    public String deadlineRepeat;
    public String result;
    public String sphere;
    public String status;
//    public String doneStatus;
    public String protocolType;

    public ExcellData(Row row) {
        this.protocolName = getStringFromRowByIndex(row.getCell(0));
        this.protocolDate = getDateFromRowByIndex(row.getCell(1));
        this.protocolNumber = getStringFromRowByIndex(row.getCell(2));
        this.protocolPoint = getStringFromRowByIndex(row.getCell(3));
        if (protocolPoint == null || protocolPoint.isEmpty())
            throw new RuntimeException("ERROR: protocolPoint is null or empty");
        this.taskText = getStringFromRowByIndex(row.getCell(4));
        String userControllersAsText = getStringFromRowByIndex(row.getCell(5));
        this.userControllers = new ArrayList<>(Arrays.asList(userControllersAsText.trim().split(",")));
        String departmentsAsText = getStringFromRowByIndex(row.getCell(6));
        this.departments = new ArrayList<>(Arrays.asList(departmentsAsText.trim().split(",")));
        this.deadline = getEndOfDate(getDateFromRowByIndex(row.getCell(7)));
        String deadlineRepeatText = getStringFromRowByIndex(row.getCell(8));

        if (deadlineRepeatText.equalsIgnoreCase("ежемесячно"))
            this.deadlineRepeat = "MONTH";
        else if (deadlineRepeatText.equalsIgnoreCase("разово"))
            this.deadlineRepeat = null;
        else throw new RuntimeException("SOMETHING WRONG WITH DEADLINE REPEAT");

        this.result = getStringFromRowByIndex(row.getCell(9));

//        String sphereWithCommas = getStringFromRowByIndex(row.getCell(9));
//        List<String> sphereSplitByComma = new ArrayList<>(Arrays.asList(sphereWithCommas.trim().split(",")));
//        this.sphere = sphereSplitByComma.get(0).toLowerCase().trim();
        this.sphere = null;
        String statusText = getStringFromRowByIndex(row.getCell(10)).trim();

        if (statusText.equalsIgnoreCase("В работе"))
            this.status = "IN_PROGRESS";
        else if (statusText.equalsIgnoreCase("Исполнено"))
            this.status = "DONE";
        else if (statusText.equalsIgnoreCase("Не исполнено")) {
            if (this.result != null && !this.result.trim().isEmpty()) {
                this.status = "ON_APPROVAL";
            } else {
                this.status = "NOT_DONE";
            }
        }
        else throw new RuntimeException("SOMETHING WRONG WITH STATUS");

//        if (status.equalsIgnoreCase("DONE")) {
//            String doneStatusText = getStringFromRowByIndex(row.getCell(10));
//            if (doneStatusText.equalsIgnoreCase("Полное исполнение"))
//                this.doneStatus = "FULL";
//            else if (doneStatusText.equalsIgnoreCase("РКЗ")) {
//                this.doneStatus = "VICE_CONTROL";
//            } else if (doneStatusText.equalsIgnoreCase("Дублирование")) {
//                this.doneStatus = "DUPLICATE";
//            } else throw new RuntimeException("SOMETHING WRONG WITH DONE_STATUS");
//        }
        this.protocolType = getStringFromRowByIndex(row.getCell(11));
    }

    public static String getStringFromRowByIndex(Cell cell) {
        if (cell != null && cell.getCellType() != CellType.BLANK) {
            if (cell.getCellType().equals(CellType.STRING))
                return cell.getStringCellValue().trim();
            if (cell.getCellType().equals(CellType.NUMERIC))
                return String.valueOf((int) cell.getNumericCellValue()).trim();
            throw new RuntimeException("CANNOT GET CELL VALUE");
        } else
            return null;
    }

    public static Date getDateFromRowByIndex(Cell cell) {
        if (cell != null) {
            if (cell.getCellType().equals(CellType.STRING))
                if (!cell.getStringCellValue().isEmpty() || getDateFromString(cell.getStringCellValue()) != null)
                    return getDateFromString(cell.getStringCellValue());
            if (cell.getCellType().equals(CellType.NUMERIC))
                return cell.getDateCellValue();
        }
        System.out.println("cannot get date");
        return null;
    }

    public static Integer getIntegerFromRowByIndex(Cell cell) {
        if (cell != null) {
            if (cell.getCellType().equals(CellType.STRING))
                return Integer.valueOf(cell.getStringCellValue());
            if (cell.getCellType().equals(CellType.NUMERIC))
                return (int) cell.getNumericCellValue();
        }
        return 0;
    }

    public static Date getDateFromString(String date) {
        List<String> formatStrings = Arrays.asList("dd/MM/yyyy", "dd.MM.yyyy");
        for (String formatString : formatStrings) {
            try {
                return new SimpleDateFormat(formatString).parse(date.trim());
            } catch (ParseException e) {
            }
        }
        System.out.println(date + " IT IS DATE");
        return null;

    }

    private static Date getEndOfDate(Date date) {
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        }
        return null;
    }

    private String getProtocolDate(String taskText) {
        String result = "empty";
        Matcher m = Pattern.compile("(\\d{1,2}.\\d{1,2}.\\d{4}|\\d{1,2}.\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(taskText);
        while (m.find()) {
            result = m.group(1);
        }
        return result;
    }

    @Override
    public String toString() {
        return "ExcellData{" +
                "protocolName='" + protocolName + '\'' +
                ", protocolDate=" + protocolDate +
                ", protocolNumber='" + protocolNumber + '\'' +
                ", protocolPoint=" + protocolPoint +
                ", taskText='" + taskText + '\'' +
                ", userControllers=" + userControllers +
                ", departments=" + departments +
                ", deadline=" + deadline +
                ", sphere='" + sphere + '\'' +
                ", result='" + result + '\'' +
                ", status='" + status + '\'' +
                ", protocolType='" + protocolType + '\'' +
                '}';
    }
}
