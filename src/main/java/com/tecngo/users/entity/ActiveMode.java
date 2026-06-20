package com.tecngo.users.entity;

public enum ActiveMode {
    CLIENT,
    TECHNICIAN;

    public Role asRole() {
        return Role.valueOf(name());
    }
}
