package kz.akimat.tasksprotocol.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class User {
    private Long id;
    private String name;
    private String nameWithSpace;
    private String department;
    private String nameNoChange;
    private String position;
    private List<String> roles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNameNoChange(String name) {
        this.nameNoChange = name;
    }

    public String getNameNoChange() {
        return nameNoChange;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String fio[] = name.split(" ");
        if (fio.length > 2) {
            this.name = fio[0] + " " + fio[1].substring(0, 1) + "" + fio[2].substring(0, 1);
        } else if (fio.length > 1) {
            this.name = fio[0] + " " + fio[1].substring(0, 1) + "";
        } else {
            this.name = name;
        }
    }

    public String getNameWithSpace() {
        return nameWithSpace;
    }

    public void setNameWithSpace(String nameWithSpace) {
        String fio[] = nameWithSpace.split(" ");
        if (fio.length > 2) {
            this.nameWithSpace = fio[0] + " " + fio[1].substring(0, 1) + " " + fio[2].substring(0, 1);
        } else if (fio.length > 1) {
            this.nameWithSpace = fio[0] + " " + fio[1].substring(0, 1) + "";
        } else {
            this.nameWithSpace = nameWithSpace;
        }
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setRoles(String rolesAsString) {
        this.roles = new ArrayList<>(Arrays.asList(rolesAsString.split(",")));
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
