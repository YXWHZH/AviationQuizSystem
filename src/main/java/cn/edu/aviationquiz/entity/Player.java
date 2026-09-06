package cn.edu.aviationquiz.entity;

public record Player(long id, String studentNumber, String name) {
    public Player {
        if (studentNumber == null || studentNumber.isBlank()) throw new IllegalArgumentException("学号不能为空");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("姓名不能为空");
    }
}
