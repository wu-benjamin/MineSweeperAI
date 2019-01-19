package com.company;

import java.util.ArrayList;

public class AIOutput {
    int numGuessed;
    int numMarked;
    int maxMove;
    int gameResult;
    ArrayList<Coordinate> moves;

    AIOutput() {
        this.numGuessed = -1;
        this.numMarked = -1;
        this.maxMove = -1;
        this.gameResult = -1;
        this.moves = new ArrayList<>();
    }

    AIOutput(int numGuessed, int numMarked, int moveNum, int gameResult, ArrayList<Coordinate> moves) {
        this.numGuessed = numGuessed;
        this.numMarked = numMarked;
        this.maxMove = moveNum;
        this.gameResult = gameResult;
        this.moves = new ArrayList<>(moves);
    }
}
