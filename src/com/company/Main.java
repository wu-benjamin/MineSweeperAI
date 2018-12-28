package com.company;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Scanner;

public class Main extends JPanel implements Runnable {

    private static Board board;
    // Ensure WIDTH >= HEIGHT for cleaner display;
    // Display is cleaner when WINDOW_SIZE is a multiple of WIDTH
    // WIDTH, HEIGHT, AND NUM_MINES used only when not in TEST mode (TEST = false to work)
    private static final int WIDTH = 9;
    private static final int HEIGHT = 9;
    private static final int NUM_MINES = 10;
    private static final int WINDOW_SIZE = 600;
    private static JFrame frame = new JFrame("MineSweeper");
    private static Main panel = new Main();
    private static final boolean TEST = false;
    private static final int ITERATIONS = 1000;
    private static ArrayList<TestConditions> tests = new ArrayList<TestConditions>();

    public static boolean isTesting() {
        return TEST;
    }

    public static void addTests() {
        tests.add(new TestConditions(9, 9, 10));
        tests.add(new TestConditions(8, 8, 10));
        tests.add(new TestConditions(16, 16, 40));
        tests.add(new TestConditions(24, 24, 99));
        tests.add(new TestConditions(30, 16, 99));
    }

    public void run() {
        while (true) {
            try {
                repaint();
                Thread.sleep(50);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        board.paintComponent(g2);
    }

    public static int getUnitCellSize() {
        return WINDOW_SIZE / WIDTH;
    }

    public static void main(String[] args) {
        if (!TEST) {
            frame.setVisible(true);
            frame.setSize(WINDOW_SIZE + 20, WINDOW_SIZE * HEIGHT / WIDTH + 20);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false);
            panel.setPreferredSize(new Dimension(WINDOW_SIZE + 20, WINDOW_SIZE * HEIGHT / WIDTH + 20));
            panel.setFocusable(true);
            panel.setDoubleBuffered(true);
            frame.setBackground(Color.BLACK);
            frame.add(panel);
            frame.pack();
            Thread thread = new Thread(panel);
            thread.start();
            try {
                board = new Board(WIDTH, HEIGHT, NUM_MINES);
            } catch (Exception e) {
                System.out.println("Error creating board!");
            }
            try {
                AI.mineSweeper();
            } catch (Exception e) {
                System.out.println("AI no do :(");
            }
        } else {
            int validTry;
            int success;
            int invalidTry;
            int maxMove;
            int numGuessed;
            int numMarkedMine;
            AIOutput out = new AIOutput();
            ArrayList<String> summary = new ArrayList<String>();
            addTests();
            for (int testNum = 0; testNum < tests.size(); testNum++) {
                validTry = 0;
                success = 0;
                invalidTry = 0;
                maxMove = 0;
                numGuessed = 0;
                numMarkedMine = 0;
                for (int i = 0; i < ITERATIONS; i++) {
                    try {
                        board = new Board(tests.get(testNum).width, tests.get(testNum).height,
                                tests.get(testNum).numMines);
                    } catch (Exception e) {
                        System.out.println("Error creating board!");
                    }
                    try {
                        out = AI.mineSweeper();
                    } catch (Exception e) {
                        System.out.println("AI no do :(");
                    }
                    switch (out.gameResult) {
                        // dead after move 1
                        case 0:
                            validTry++;
                            maxMove += out.maxMove;
                            numGuessed += out.numGuessed;
                            numMarkedMine += out.numMarked;
                            break;
                        // win    
                        case 1:
                            validTry++;
                            success++;
                            maxMove += out.maxMove;
                            numGuessed += out.numGuessed;
                            numMarkedMine += out.numMarked;
                            break;
                        // dead on move 1 or error
                        default:
                            invalidTry++;
                    }
                    System.out.println("Test: " + (testNum + 1) + " out of " + tests.size() +
                            "\nIteration: " + (i + 1) + " out of " + ITERATIONS);
                }
                summary.add("Test Conditions: W * H, NUM_MINES: " + tests.get(testNum).width + " * "
                + tests.get(testNum).height + ", " + tests.get(testNum).numMines);
                summary.add("Iterations: " + ITERATIONS);
                summary.add("Number of invalid tries: " + invalidTry);
                summary.add("Number of valid tries: " + validTry);
                summary.add("Number of successful tries: " + success);
                summary.add("Swept " + (double) maxMove / (double) validTry + " tiles in a valid try on average");
                summary.add("Guessed " + (double) numGuessed / (double) validTry + " tiles in a valid try on average");
                summary.add("Marked " + (double) numMarkedMine / (double) validTry + " mines in a valid try on average");
                summary.add("Probability of success in a valid try: " + (double) success / (double) validTry);
                summary.add("\n");
            }
            System.out.println("\n\n*******************************");
            for (int i = 0; i < summary.size(); i++) {
                System.out.println(summary.get(i));
            }
        }
    }
}
