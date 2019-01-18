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

    public void setNumMarked(int numMarked) {
        this.numMarked = numMarked;
    }

    public void setNumGuessed(int numGuessed) {
        this.numGuessed = numGuessed;
    }

    public void setMaxMove(int moveNum) {
        this.maxMove = moveNum;
    }

    public void setGameResult(int gameResult) {
        this.gameResult = gameResult;
    }

    public void setMoves(ArrayList<Coordinate> moves) {
        this.moves = new ArrayList<>(moves);
    }
}
