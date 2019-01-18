package com.company;

class TestConditions {

    private int width;
    private int height;
    private int numMines;

    TestConditions(int width, int height, int numMines) {
        this.width = width;
        this.height = height;
        this.numMines = numMines;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getNumMines() {
        return numMines;
    }
}
