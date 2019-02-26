package com.company;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Main extends JPanel implements Runnable {



    private static final boolean TEST = true;
    static final boolean AUTO = true;
    static final boolean STEP_BY_STEP = !AUTO;
    private static final int ITERATIONS = 1000;




    private static Board board;
    // Ensure WIDTH >= HEIGHT for cleaner display;
    // Display is cleaner when WINDOW_SIZE is a multiple of WIDTH
    // WIDTH, HEIGHT, AND NUM_MINES used only when not in TEST mode (TEST = false to work)
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final int NUM_MINES = 40;
    private static final int WINDOW_SIZE = 750;
    private static JFrame frame = new JFrame("MineSweeper");
    private static Main panel = new Main();
    private static ArrayList<TestConditions> tests = new ArrayList<>();
    static JLabel labelProgress3 = new JLabel();
    static JProgressBar progress3 = new JProgressBar();

    static boolean isTesting() {
        return TEST;
    }

    private static void addTests() {
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

    static int getUnitCellSize() {
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
            while (true) {
                try {
                    board = new Board(WIDTH, HEIGHT, NUM_MINES);
                } catch (Exception e) {
                    System.out.println("Error creating board!");
                }
                try {
                    AI.mineSweeper();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            JPanel testPanel = new JPanel();
            frame.setVisible(true);
            frame.setSize(WINDOW_SIZE + 20, WINDOW_SIZE * HEIGHT / WIDTH + 20);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false);
            testPanel.setPreferredSize(new Dimension(350, 200));
            testPanel.setFocusable(true);
            testPanel.setDoubleBuffered(true);
            frame.setBackground(Color.BLACK);
            frame.add(testPanel);
            frame.pack();
            GridLayout layout = new GridLayout(3, 2);
            testPanel.setLayout(layout);
            JLabel labelProgress1 = new JLabel();
            JProgressBar progress1 = new JProgressBar();
            JLabel labelProgress2 = new JLabel();
            JProgressBar progress2 = new JProgressBar();
            testPanel.add(labelProgress1);
            testPanel.add(progress1);
            testPanel.add(labelProgress2);
            testPanel.add(progress2);
            testPanel.add(labelProgress3);
            testPanel.add(progress3);
            progress1.setMaximum(tests.size());
            progress1.setMinimum(0);
            progress2.setMaximum(ITERATIONS);
            progress2.setMinimum(0);
            progress3.setMinimum(0);
            int validTry;
            int success;
            int invalidTry;
            int maxMove;
            int numGuessed;
            int numMarkedMine;
            AIOutput out = new AIOutput();
            ArrayList<String> summary = new ArrayList<String>();
            addTests();
            double[] swept = new double[ITERATIONS];
            double[] guessed = new double[ITERATIONS];
            double[] marked = new double[ITERATIONS];
            for (int testNum = 0; testNum < tests.size(); testNum++) {
                labelProgress1.setText("   Test " + (testNum + 1) + " of " + tests.size());
                progress1.setValue(testNum);
                progress3.setMaximum(tests.get(testNum).getHeight() * tests.get(testNum).getWidth() - tests.get(testNum).getNumMines());
                validTry = 0;
                success = 0;
                invalidTry = 0;
                maxMove = 0;
                numGuessed = 0;
                numMarkedMine = 0;
                for (int i = 0; i < ITERATIONS; i++) {
                    labelProgress2.setText("   Iteration " + (i + 1) + " of " + ITERATIONS);
                    progress2.setValue(i);
                    try {
                        board = new Board(tests.get(testNum).getWidth(), tests.get(testNum).getHeight(),
                                tests.get(testNum).getNumMines());
                    } catch (Exception e) {
                        System.out.println("Error creating board!");
                    }
                    try {
                        out = AI.mineSweeper();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    switch (out.gameResult) {
                        // dead after move 1
                        case 0:
                            validTry++;
                            maxMove += out.maxMove;
                            numGuessed += out.numGuessed;
                            numMarkedMine += out.numMarked;
                            swept[i] = out.maxMove;
                            guessed[i] = out.numGuessed;
                            marked[i] = out.numMarked;
                            break;
                        // win    
                        case 1:
                            validTry++;
                            success++;
                            maxMove += out.maxMove;
                            numGuessed += out.numGuessed;
                            numMarkedMine += out.numMarked;
                            swept[i] = out.maxMove;
                            guessed[i] = out.numGuessed;
                            marked[i] = out.numMarked;
                            break;
                        // dead on move 1 or error
                        default:
                            invalidTry++;
                    }
                    /* System.out.println("Test: " + (testNum + 1) + " out of " + tests.size() +
                            "\nIteration: " + (i + 1) + " out of " + ITERATIONS + "\n");
                    */
                }
                summary.add("Test Conditions: W * H, NUM_MINES: " + tests.get(testNum).getWidth() + " * "
                + tests.get(testNum).getHeight() + ", " + tests.get(testNum).getNumMines());
                summary.add("Iterations: " + ITERATIONS);
                summary.add("Number of invalid tries: " + invalidTry);
                summary.add("Number of valid tries: " + validTry);
                summary.add("Number of successful tries: " + success);
                summary.add("Swept " + (double) maxMove / (double) validTry + " tiles in a valid try on average with standard deviation " + calculateSD(swept));
                summary.add("Guessed " + (double) numGuessed / (double) validTry + " tiles in a valid try on average with standard deviation " + calculateSD(guessed));
                summary.add("Marked " + (double) numMarkedMine / (double) validTry + " mines in a valid try on average with standard deviation " + calculateSD(marked));
                summary.add("Probability of success in a valid try: " + (double) success / (double) validTry);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                summary.add("Time of test: " + dtf.format(now));
                summary.add("\r\n");
                try {
                    AIOutput.save(summary);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static double calculateSD(double numArray[])
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (numArray[i] == 0) {
                count++;
            }
        }
        length -= count;
        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            if (num != 0) {
                standardDeviation += Math.pow(num - mean, 2);
            }
        }

        return Math.sqrt(standardDeviation/length);
    }
}
