package com.example.hello;

public enum Direction {
    FORWARD(0, "forward"),
    RIGHT(1, "right"),
    BACKWARD(2, "backward"),
    LEFT(3, "left");

    private int direction;
    private String string;

    Direction(int direction, String string)  {
        this.direction = direction;
        this.string = string;
    }
}
