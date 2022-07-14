package com.example.hello;

public enum Medium {
    BLUETOOTH(0),
    INTERNET(1),
    APRS(2);

    private int medium;

    Medium(int medium)  {
        this.medium = medium;
    }
}
