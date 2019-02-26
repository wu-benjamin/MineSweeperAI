package com.company;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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

    static void save(ArrayList<String> summary) throws IOException {
        // Records progress
        FileWriter out = null;
        try {
            out = new FileWriter("TestOut.txt", true);
            for (int i = 0; i < summary.size(); i++) {
                out.write(summary.get(i) + "\r\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                // Closes writer
                out.close();
            }
        }
        summary.clear();
    }
}
