package com.company;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Random;

public class Board {
    private static boolean[][] mines;
    private static int width;
    private static int height;
    private static int numMines;
    private static int[][] gameBoard;
    private static int numSafe;
    private static boolean win;
    private static boolean dead;
    private static Random rand = new Random();

    Board(int width, int height, int numMines) throws Exception {
        win = false;
        dead = false;
        Board.width = width;
        Board.height = height;
        Board.numMines = numMines;

        if (width * height <= numMines) {
            throw new Exception("Too many mines!");
        }
        boolean[][] mines = new boolean[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                mines[i][j] = false;
            }
        }
        // Test deduction end game
        /*
        mines[0][0] = true;
        mines[0][2] = true;
        mines[0][4] = true;
        mines[0][5] = true;
        mines[0][7] = true;
        */

        // wut
        /*
        mines[0][1] = true;
        mines[1][7] = true;
        mines[3][1] = true;
        mines[3][7] = true;
        mines[4][2] = true;
        mines[4][3] = true;
        mines[4][7] = true;
        mines[5][2] = true;
        mines[6][1] = true;
        mines[6][8] = true;
        */

        // wut #2
        /*
        mines[2][2] = true;
        mines[2][4] = true;
        mines[4][2] = true;
        */
        Board.mines = mines;

        int nextMine;
        for (int i = 0; i < numMines; i++) {
            nextMine = rand.nextInt(width * height - i);
            setMine(nextMine);
        }


        int[][] gameBoard = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                gameBoard[i][j] = -1;
            }
        }
        Board.gameBoard = gameBoard;
        numSafe = width * height - numMines;
    }

    private static void setMine(int index) {
        int currIndex = -1;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (!mines[i][j]) {
                    currIndex++;
                }
                if (currIndex == index) {
                    mines[i][j] = true;
                    return;
                }
            }
        }
    }

    public static void reset() {
        numSafe = width * height - numMines;
        dead = false;
        win = false;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                gameBoard[i][j] = -1;
            }
        }
    }

    static boolean isDead() {
        return dead;
    }

    static boolean endGame() {
        return dead || win;
    }

    static int getWidth() {
        return width;
    }

    static int getHeight() {
        return height;
    }

    static int getNumMines() {
        return numMines;
    }

    private static void update(int x, int y) {
        if (mines[x][y]) {
            gameBoard[x][y] = 9;
            return;
        }
        int value = 0;
        if (x != 0) {
            if (mines[x-1][y]) {
                value++;
            }
            if (y != 0 && mines[x-1][y-1]) {
                value++;
            }
            if (y != width - 1 && mines[x-1][y+1]) {
                value++;
            }
        }
        if (x != height - 1) {
            if (mines[x+1][y]) {
                value++;
            }
            if (y != width - 1 && mines[x+1][y+1]) {
                value++;
            }
            if (y != 0 && mines[x+1][y-1]) {
                value++;
            }
        }
        if (y != 0 && mines[x][y-1]) {
            value++;
        }
        if (y != width - 1 && mines[x][y+1]) {
            value++;
        }
        gameBoard[x][y] = value;
    }

    static void sweep(int x, int y) {
        if (gameBoard[x][y] == -1) {
            numSafe--;
        }
        //System.out.println(numSafe);
        if (mines[x][y]) {
            update(x, y);
            dead = true;
        } else if (numSafe == 0) {
            update(x, y);
            win = true;
        } else {
            update(x, y);
        }
    }

    static int[][] getBoard() {
        return gameBoard;
    }

    void paintComponent(Graphics2D g) {
        String prob;
        DecimalFormat df = new DecimalFormat("#.##");
        if (dead) {
            g.setColor(Color.BLACK);
            g.drawString("D", 0, 20);
        }
        if (win) {
            g.setColor(Color.BLACK);
            g.drawString("W", 0, 20);
        }
        g.setColor(Color.PINK);
        g.fill3DRect(20, 20, Main.getUnitCellSize() * width,
                Main.getUnitCellSize() * height, false);
        g.setColor(Color.BLACK);
        for (int i = 0; i < width; i++) {
            g.fill3DRect(20 + Main.getUnitCellSize() * i, 20, 2,
                    Main.getUnitCellSize() * height, false);
        }
        for (int i = 0; i < height; i++) {
            g.fill3DRect(20, 20 + Main.getUnitCellSize() * i, Main.getUnitCellSize() * width,
                    2, false);
        }
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (mines[i][j]) {
                    if (gameBoard[i][j] == 9) {
                        g.setColor(Color.RED);
                    } else if (AI.getSuspectedMine()[i][j]) {
                        g.setColor(Color.GREEN);
                    } else {
                        //g.setColor(Color.DARK_GRAY);
                        g.setColor(new Color(255,
                                (int) (255 - 165 * AI.getMineProbabilities()[i][j]),
                                (int) (255 - 85 * AI.getMineProbabilities()[i][j])));
                    }
                    g.fill3DRect(20 + j * Main.getUnitCellSize(), 20 + i * Main.getUnitCellSize(),
                            Main.getUnitCellSize(), Main.getUnitCellSize(), false);
                    if (gameBoard[i][j] != 9 && !AI.getSuspectedMine()[i][j]) {
                        g.setColor(new Color(0, 0, 0));
                        g.fill3DRect(20 + j * Main.getUnitCellSize(), 20 + i * Main.getUnitCellSize(),
                                Main.getUnitCellSize() / 2, Main.getUnitCellSize() / 2, false);
                    } else {
                        g.setColor(Color.WHITE);
                        g.drawString("B", 20 + j * Main.getUnitCellSize() + Main.getUnitCellSize() / 2,
                                20 + (i + 1) * Main.getUnitCellSize() - Main.getUnitCellSize() / 2);
                    }
                    g.setColor(Color.WHITE);
                } else {
                    if (gameBoard[i][j] > -1) {
                        g.setColor(Color.WHITE);
                        if (AI.getSuspectedMine()[i][j]) {
                            g.setColor(Color.GREEN);
                        }
                        g.fill3DRect(20 + j * Main.getUnitCellSize(), 20 + i * Main.getUnitCellSize(),
                                Main.getUnitCellSize(), Main.getUnitCellSize(), false);
                        g.setColor(Color.BLACK);
                        g.drawString(Integer.toString(gameBoard[i][j]), 20 + j * Main.getUnitCellSize() + Main.getUnitCellSize() / 2,
                                20 + (i + 1) * Main.getUnitCellSize() - Main.getUnitCellSize() / 2);
                    }
                     else if (gameBoard[i][j] == -1) {
                        g.setColor(new Color(255,
                                (int) (255 - 165 * AI.getMineProbabilities()[i][j]),
                                (int) (255 - 85 * AI.getMineProbabilities()[i][j])));
                        g.fill3DRect(20 + j * Main.getUnitCellSize(), 20 + i * Main.getUnitCellSize(),
                                Main.getUnitCellSize(), Main.getUnitCellSize(), false);
                    }
                }
                prob = df.format(AI.getMineProbabilities()[i][j]);
                if (mines[i][j]) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.BLACK);
                }
                g.drawString(prob, 20 + j * Main.getUnitCellSize(), 10 + (i + 1) * Main.getUnitCellSize());
            }
        }
        for (int i = 0; i < height; i++) {
            g.setColor(Color.BLACK);
            g.drawString(Integer.toString(i), 2,
                    20 + i * Main.getUnitCellSize() + Main.getUnitCellSize() / 2);
        }
        for (int i = 0; i < width; i++) {
            g.setColor(Color.BLACK);
            g.drawString(Integer.toString(i),
                    15 + (i + 1) * Main.getUnitCellSize() - Main.getUnitCellSize() / 2,
                    15);
        }
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (AI.getSuspectedMine()[i][j]) {
                    g.setColor(Color.GREEN);
                }
            }
        }
    }
}
