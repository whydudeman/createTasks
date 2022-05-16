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
    public Integer id;
    public String protocolName;//0
    public Date protocolDate;//1
    public String protocolNumber;//2
    public String protocolPoint;//3
    public String taskText;//4
    public List<String> userControllers;//5
    public List<String> departments;//6
    public Date deadline;//7
    public String result;//8
    public String status;//9
    public String protocolType;//10


    public String taskDeadlineRepeat;//8
    public String sphere;//11

    public ExcellData(Row row) {
        this.id = getIntegerFromRowByIndex(row.getCell(0));
        this.protocolName = getStringFromRowByIndex(row.getCell(1));
        this.protocolDate = getDateFromRowByIndex(row.getCell(2));
        this.protocolNumber = getStringFromRowByIndex(row.getCell(3));
        this.protocolPoint = getStringFromRowByIndex(row.getCell(4));

        this.taskText = getStringFromRowByIndex(row.getCell(5));
        String userControllersAsText = getStringFromRowByIndex(row.getCell(6));
        this.userControllers = getArrayFromText(userControllersAsText);

        String departmentsAsText = getStringFromRowByIndex(row.getCell(7));
        this.departments = getArrayFromText(departmentsAsText);
        this.deadline = getDateFromRowByIndex(row.getCell(8));
        this.taskDeadlineRepeat = getTaskDeadlineRepeat(getStringFromRowByIndex(row.getCell(9)));
        this.result = getStringFromRowByIndex(row.getCell(10));




        String statusText = getStringFromRowByIndex(row.getCell(11)).trim();
        if (statusText.equalsIgnoreCase("В работе"))
            this.status = "IN_PROGRESS";
        else if (statusText.equalsIgnoreCase("Исполнено"))
            this.status = "DONE";
        else if (statusText.equalsIgnoreCase("Не исполнено"))
            this.status = "NOT_DONE";
        else throw new RuntimeException("SOMETHING WRONG WITH STATUS");



        this.protocolType = getStringFromRowByIndex(row.getCell(12));

        if (protocolPoint == null || protocolPoint.isEmpty())
            throw new RuntimeException("ERROR: protocolPoint is null or empty");
    }

    private String getTaskDeadlineRepeat(String stringFromRowByIndex) {
        if(stringFromRowByIndex!=null
                && !stringFromRowByIndex.isEmpty()
                && stringFromRowByIndex.trim().toLowerCase().contains("ежемесячно")) {
            return "MONTH";
        }
        return null;
    }

    public static String getStringFromRowByIndex(Cell cell) {
        if (cell != null && cell.getCellType() != CellType.BLANK) {
            if (cell.getCellType().equals(CellType.STRING))
                return cell.getStringCellValue().trim();
            if (cell.getCellType().equals(CellType.NUMERIC)) {
                return String.valueOf((int) cell.getNumericCellValue()).trim();
            }
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
        return null;
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


    public static Integer getIntegerFromRowByIndex(Cell cell) {
        if (cell != null) {
            if (cell.getCellType().equals(CellType.STRING))
                return Integer.valueOf(cell.getStringCellValue());
            if (cell.getCellType().equals(CellType.NUMERIC))
                return (int) cell.getNumericCellValue();
        }
        return 0;
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

    private ArrayList<String> getArrayFromText(String userControllersAsText) {
        if (userControllersAsText == null)
            throw new RuntimeException("NULL EXCEPTION");
        return new ArrayList<>(Arrays.asList(userControllersAsText.trim().split(",")));
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
                ",\n protocolNumber='" + protocolNumber + '\'' +
                ",\n protocolDate=" + protocolDate +
                ", \nprotocolName='" + protocolName + '\'' +
                ", \nprotocolPoint='" + protocolPoint + '\'' +
                ", \ntaskText='" + taskText + '\'' +
                ", \ntaskDeadlineRepeat='" + taskDeadlineRepeat + '\'' +
                ", \ndeadline=" + deadline +
                ", \nsphere='" + sphere + '\'' +
                ", \nuserControllers=" + userControllers +
                ", \ndepartments=" + departments +
                ", \nresult='" + result + '\'' +
                ", \nstatus='" + status + '\'' +
                ", \nprotocolType='" + protocolType + '\'' +
                '}';
    }
}
