package kz.akimat.tasksprotocol.logic;

import kz.akimat.tasksprotocol.util.DbConstants;
import kz.akimat.tasksprotocol.util.User;
import kz.akimat.tasksprotocol.util.UserUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.*;
import java.util.*;

public class Main {
    public static void main(String... strings) throws IOException, SQLException {
        Main objExcelFile = new Main();
        String fileName = "tasks_2.xlsx";
        String path = "/home/nurbol/akimat/ekibastuz/";
        Workbook workbook = getExcelDocument(fileName, path);
        objExcelFile.processExcelObject(workbook);
    }

    private static Workbook getExcelDocument(String fileName, String path) throws IOException {

        File file = new File(path + fileName);

        FileInputStream inputStream = new FileInputStream(file);
        Workbook workbook = null;
        String fileExtensionName = fileName.substring(fileName.indexOf("."));
        if (fileExtensionName.equals(".xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else if (fileExtensionName.equals(".xls")) {
            workbook = new HSSFWorkbook(inputStream);
        }

        return workbook;
    }

    private static void createTaskHistoryInDB(Long taskId, java.util.Date protocolDate) {
        String SQL_INSERT = "INSERT INTO task_history (created_at, updated_at, date, deadline,status, author_id,task_id,type) \n" +
                "SELECT NOW(),NOW(),?,deadline,status,author_id,id,? FROM task where id=?";

        Date deadline = null;
        if (protocolDate != null) {
            deadline = new java.sql.Date(protocolDate.getTime());
        } else throw new RuntimeException("ProtocolDate ERROR ");
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT);
        ) {
            statement.setDate(1, deadline);
            statement.setString(2, "TASK_CREATED");
            statement.setLong(3, taskId);
            statement.executeUpdate();
            // ...
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void processExcelObject(Workbook workbook) throws SQLException {
        for (int i = 0; i < Objects.requireNonNull(workbook).getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
//            int rowCount = sheet.getLastRowNum() - sheet.getFirstRowNum();
            for (int j = 1; j <= 1600; j++) {
                Row row = sheet.getRow(j);
                System.out.println(j);
                insertAndUpdateTask(row);
            }
        }
    }

    private void insertAndUpdateTask(Row row) {
        List<User> users = UserUtils.getUsers();
        ExcellData excellData = new ExcellData(row);
//        System.out.println(excellData.toString());
        if (excellData.protocolNumber != null && !excellData.protocolNumber.trim().isEmpty()
                && excellData.protocolPoint != null && !excellData.protocolPoint.isEmpty()
                && excellData.protocolDate != null && excellData.deadline != null) {
            List<Long> userId = UserUtils.getUsersId(excellData.userControllers, users);
            List<Long> executionGroupUsers = new ArrayList<>();
            boolean isExecutionGroupFirst = false;
            String firstDepartment = excellData.departments.get(0);
            Long executionGroupId = getExecutionGroup(firstDepartment.trim());
            if (executionGroupId != null && executionGroupId != 0) {
                executionGroupUsers = getExecutionGroupUsersByExecutionGroupId(executionGroupId);
                isExecutionGroupFirst = true;
            }

            if (isExecutionGroupFirst) {
                excellData.departments.remove(firstDepartment);
                for (Long firstUserId : executionGroupUsers) {
                    List<Long> executorIds = new ArrayList<>();
                    executorIds.add(firstUserId);
                    executorIds.addAll(UserUtils.getUserIdByDepartment(excellData.departments, users, row.getRowNum()));
                    Set<Long> departmentIds = new LinkedHashSet<>(executorIds);
                    executorIds.addAll(departmentIds);
                    executorIds = new ArrayList<>(departmentIds);
                    createTaskRelatedObjects(excellData, userId, executorIds);
                }
            } else {
                List<String> executionGroups = new ArrayList<>();
                for (String department : excellData.departments) {
                    Long possiblyExecutionGroup = getExecutionGroup(department.trim());
                    if (possiblyExecutionGroup != null && possiblyExecutionGroup != 0) {
                        executionGroups.add(department);
                    }
                }
                excellData.departments.removeAll(executionGroups);

                List<Long> executionGroupIds = new ArrayList<>();
                for (String executionGroupName : executionGroups) {
                    Long executionGroupIdTemp = getExecutionGroup(executionGroupName.trim());
                    executionGroupIds.addAll(Objects.requireNonNull(getExecutionGroupUsersByExecutionGroupId(executionGroupIdTemp)));
                }
                List<Long> executorIds = new ArrayList<>();
                executorIds = UserUtils.getUserIdByDepartment(excellData.departments, users, row.getRowNum());
                executorIds.addAll(executionGroupIds);
                executorIds = new ArrayList<>(new LinkedHashSet<>(executorIds));
                createTaskRelatedObjects(excellData, userId, executorIds);
            }

        } else
            throw new RuntimeException("INVALID_ROW");
    }

    private void createTaskRelatedObjects(ExcellData excellData, List<Long> userId, List<Long> executorIds) {
        if (userId == null || userId.isEmpty() || executorIds == null || executorIds.isEmpty())
            System.out.println("SKIPPING_TASK");
        else {
            Long protocolTypeId = getProtocolTypeIfExists(excellData.protocolType);
            if (protocolTypeId == null) {
                protocolTypeId = insertProtocolType(excellData.protocolType);
            }

            Long protocolId = getProtocolIfExists(excellData.protocolNumber);
            if (protocolId == null) {
                protocolId = insertProtocol(excellData.protocolName, excellData.protocolNumber, excellData.protocolDate, protocolTypeId, "APPROVED");
            }

            Long taskId = createTask(
                    excellData.deadline,
                    excellData.protocolPoint,
                    excellData.result,
                    excellData.status,
                    excellData.taskText,
                    protocolId,
                    excellData.protocolDate,
                    excellData.taskDeadlineRepeat);
            if (taskId == null) {
                System.out.println("ERROR: Task with name " + excellData.taskText);
                throw new RuntimeException("TASK_NOT_CREATED");
            }


            createTaskHistoryInDB(taskId, excellData.protocolDate);


            createTaskUserInDb(taskId, executorIds);
            int counter = 1;
            boolean isMain = true;
            for (Long userLongId : userId) {

                if (counter != 1)
                    isMain = false;
                if (getTaskUser(taskId, userLongId, "CONTROL") == null)
                    createTaskUserInDb(taskId, userLongId, isMain);
                counter++;
            }

        }
    }

    private Long getProtocolTypeIfExists(String protocolType) {
        String SQL_SELECT = "SELECT id from protocol_type where name=?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, protocolType);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long insertProtocolType(String protocolType) {
        String SQL_INSERT = "INSERT INTO `protocol_type` (`created_at`,`updated_at`,`name`) VALUES (NOW(),NOW(),?);";
        if (protocolType != null && !protocolType.isEmpty()) {
            try (
                    Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                    PreparedStatement statement = connection.prepareStatement(SQL_INSERT,
                            Statement.RETURN_GENERATED_KEYS);
            ) {
                statement.setString(1, protocolType);

                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("ERROR: Creating ProtocolType failed, no rows affected.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("ERROR: Creating ProtocolType failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("ERROR: Cannot create protocolType");
    }

    private Long getProtocolIfExists(String protocolNumber) {
        String SQL_SELECT = "SELECT id from protocol where protocol_number like ?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, "%"+protocolNumber+"%");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long insertProtocol(String protocolName, String protocolNumber, java.util.Date date, Long protocolTypeId, String status) {
        String SQL_INSERT = "INSERT INTO `protocol` (`title`, `protocol_number`,`date`,`protocol_type_id`,`status`) VALUES (?, ?,?,?,?);";
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT,
                        Statement.RETURN_GENERATED_KEYS);
        ) {
            statement.setString(1, protocolName);
            statement.setString(2, protocolNumber);
            statement.setDate(3, new java.sql.Date(date.getTime()));
            statement.setLong(4, protocolTypeId);
            statement.setString(5, status);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("ERROR: Creating Protocol failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("ERROR: Creating Protocol failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;

    }

    private Long createTask(
            java.util.Date deadlineDate,
            String protocolPoint,
            String result,
            String status,
            String taskText,
            Long protocolId,
            java.util.Date protocolDate,
            String taskDeadlineRepeat) {

        String SQL_INSERT = "INSERT INTO `task`(`created_at`, `updated_at`,`deadline`,`protocol_point`, `result`, " +
                "`status`, `task_text`, `protocol_id`, `initial_deadline`,`inspector_result_date`," +
                "task_deadline_repeat) " +
                "VALUES (NOW(), NOW(),?,?,?,?,?,?,?,?,?)";
        Date deadline = null;
        if (deadlineDate != null) {
            deadline = new java.sql.Date(deadlineDate.getTime());
        }
        Date inspectorResultDate = null;
        if (protocolDate != null) {
            inspectorResultDate = new java.sql.Date(protocolDate.getTime());
        }
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT,
                        Statement.RETURN_GENERATED_KEYS);
        ) {
            statement.setDate(1, deadline);
            statement.setString(2, protocolPoint);
            statement.setString(3, result);
            statement.setString(4, status);
            statement.setString(5, taskText);
            statement.setLong(6, protocolId);
            statement.setDate(7, deadline);
            statement.setDate(8, inspectorResultDate);
            statement.setString(9, taskDeadlineRepeat);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("ERROR: Creating Task failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("ERROR: Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void createTaskUserInDb(Long taskId, Long userId, boolean isMain) {
        String SQL_INSERT = "INSERT INTO `task_user`( `created_at`, `updated_at`, `is_main`, " +
                "`type`, `task_id`, `user_id`) VALUES (NOW(),NOW(),?,?,?,?)";
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT);
        ) {
            statement.setBoolean(1, isMain);
            statement.setString(2, "CONTROL");
            statement.setLong(3, taskId);
            statement.setLong(4, userId);
            statement.executeUpdate();
            // ...
        } catch (SQLException e) {
            System.out.println("ERROR: CONTROL: TASK AND USER ALREADY EXISTS");
        }


    }

    private void createTaskUserInDb(Long taskId, List<Long> executersId) {
        String SQL_INSERT = "INSERT INTO `task_user`( `created_at`, `updated_at`, `is_main`, " +
                "`type`, `task_id`, `user_id`) VALUES (NOW(),NOW(),?,?,?,?)";
        int count = 1;
        boolean isMain = true;
        for (Long id : executersId) {
            if (count != 1)
                isMain = false;
            try (
                    Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                    PreparedStatement statement = connection.prepareStatement(SQL_INSERT);
            ) {
                statement.setBoolean(1, isMain);
                statement.setString(2, "EXECUTION");
                statement.setLong(3, taskId);
                statement.setLong(4, id);
                statement.executeUpdate();
                // ...
            } catch (SQLException e) {
                System.out.println("ERROR: EXECUTION: TASK AND USER ALREADY EXISTS");
            }
            count++;
        }

    }

    private Long getTaskUser(Long taskId, Long userId, String type) {
        String SQL_SELECT = "SELECT id from task_user where task_id=? and user_id=? and type=? ";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setLong(1, taskId);
            preparedStatement.setLong(2, userId);
            preparedStatement.setString(3, type);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long getExecutionGroup(String name) {
        String SQL_SELECT = "SELECT eg.id from execution_group eg where eg.name like ?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, "%" + name + "%");
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Long> getExecutionGroupUsersByExecutionGroupId(Long executionGroupId) {
        String SQL_SELECT = "SELECT egu.positions_id from execution_group_positions egu where egu.execution_group_id = ?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setLong(1, executionGroupId);
            ResultSet rs = preparedStatement.executeQuery();
            List<Long> userIds = new ArrayList<>();
            while (rs.next()) {
                userIds.add(rs.getLong("positions_id"));
            }
            return userIds;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
