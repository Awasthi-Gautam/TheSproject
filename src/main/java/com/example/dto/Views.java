package com.example.dto;

public class Views {
    public interface Public {
    }

    public interface Teacher extends Public {
    }

    public interface Admin extends Teacher {
    }
}
