package kz.akimat.tasksprotocol.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserUtils {

    public static List<User> getUsers() {
        try {
            Connection con = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);

            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("select u.id, u.name, d.name , GROUP_CONCAT(role.name), p.name from user u\n" +
                    "                    inner join position p ON p.id = u.position_id\n" +
                    "                    left join department d ON d.id = p.department_id\n" +
                    "                    left join role_user r on u.id=r.user_id\n" +
                    "                    LEFT JOIN role role on role.id=r.role_id\n" +
                    "                    GROUP BY u.id");
            List<User> users = new ArrayList();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong(1));
                user.setName(rs.getString(2));
                user.setNameWithSpace(rs.getString(2));
                user.setDepartment(rs.getString(3));
                user.setNameNoChange(rs.getString(2));
                user.setRoles(rs.getString(4));
                user.setPosition(rs.getString(5));
                users.add(user);
            }
            con.close();
            return users;
        } catch (Exception e) {
            System.out.println(e);
        }
        return new ArrayList<>();
    }

    public static Long getUserId(String name, List<User> users) {
        if (name != null && !name.isEmpty()) {
            Long id = users.stream().filter(user -> user.getNameNoChange().equals(name)).map(User::getId).findAny().orElse(null);
            if (id == null)
                id = users.stream().filter(user ->
                        levenstain(user.getName(), name) < 4 || levenstain(user.getNameWithSpace(), name) < 4)
                        .map(User::getId).findFirst().orElse(null);
            return id;
        }
        return null;
    }

    public static List<Long> getUsersId(List<String> names, List<User> users) {
        List<Long> ids = new ArrayList<>();
        for (String name : names) {
            for (User user : users) {
                if ((levenstain(user.getName(), name.trim()) < 4 || levenstain(user.getNameWithSpace(), name.trim()) < 4) && (user.getRoles().contains("VICE AKIM")))
                    ids.add(user.getId());
            }

        }
        if (names.size() != ids.size()) {
            System.out.println("USER_ERROR: Some User was not found");
            for (String name : names) {
                System.out.println("Excell UserName: " + name);
                for (User user : users) {
                    if ((levenstain(user.getName(), name) < 4 || levenstain(user.getNameWithSpace(), name) < 4) && (user.getRoles().contains("VICE AKIM")))
                        System.out.println("User Name: " + user.getName() + " User Id: " + user.getId() + "User Name: " + user.getNameNoChange() + " ROLE: " + user.getRoles().toString());
                }

            }
        }
        return ids;
    }

    public static List<Long> getUserIdByDepartment(List<String> departments, List<User> users, int rowNum) {
        List<Long> ids = new ArrayList<>();
        for (String s : departments) {
            int count = 0;
            for (User user : users) {
                if (count > 1) {
                    System.out.println(rowNum + " Many users for one department " + s);
                }

                if ((user.getDepartment() != null && (user.getDepartment().trim().equalsIgnoreCase(s.trim()) ||
                        levenstain(user.getDepartment().trim(), s.trim()) < 2)) && (user.getRoles().contains("EXECUTOR"))) {
                    count++;
                    ids.add(user.getId());
//                    System.out.println(user.getNameNoChange() + " Department:" + s);
                }

            }
            if (count == 0)
                System.out.println(rowNum + " " + s);

        }
        return ids;
    }

    public static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int levenstain(CharSequence lhs, CharSequence rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

        for (int i = 0; i <= lhs.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= lhs.length(); i++)
            for (int j = 1; j <= rhs.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));

        return distance[lhs.length()][rhs.length()];
    }
}
