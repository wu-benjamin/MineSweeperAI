package com.company;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Main extends JPanel implements Runnable {

    static final boolean TEST = true;
    static final boolean AUTO = true;
    static final int AUTO_TIME = 250;
    static final boolean STEP_BY_STEP = !AUTO;
    private static final int ITERATIONS = 5;

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
    static JLabel labelProgress4 = new JLabel();
    static JProgressBar progress4 = new JProgressBar();
    static JLabel labelProgress5 = new JLabel();
    static JProgressBar progress5 = new JProgressBar();

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

    public static void main(String[] args) throws IOException {
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
            Runtime.getRuntime().addShutdownHook(new TestCloseMessage());
            addTests();
            JPanel testPanel = new JPanel();
            frame.setVisible(true);
            frame.setSize(WINDOW_SIZE + 20, WINDOW_SIZE * HEIGHT / WIDTH + 20);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false);
            testPanel.setPreferredSize(new Dimension(650, 350));
            testPanel.setFocusable(true);
            testPanel.setDoubleBuffered(true);
            frame.setBackground(Color.BLACK);
            frame.add(testPanel);
            frame.pack();
            GridLayout layout = new GridLayout(5, 2);
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
            testPanel.add(labelProgress4);
            testPanel.add(progress4);
            testPanel.add(labelProgress5);
            testPanel.add(progress5);
            progress1.setMaximum(tests.size());
            progress1.setMinimum(0);
            progress2.setMaximum(ITERATIONS);
            progress2.setMinimum(0);
            progress3.setMinimum(0);
            progress4.setMinimum(0);
            progress5.setMinimum(0);
            progress1.setValue(0);
            progress2.setValue(0);
            progress3.setValue(0);
            progress4.setValue(0);
            progress5.setValue(0);
            Main.labelProgress4.setText("   Regions simulated 0 of 0");
            Main.labelProgress5.setText("   Arrangement 0 of 0");
            int validTry;
            int success;
            int invalidTry;
            int maxMove;
            int numGuessed;
            int numMarkedMine;
            AIOutput out = new AIOutput();
            ArrayList<String> summary = new ArrayList<String>();
            double[] swept = new double[ITERATIONS];
            double[] guessed = new double[ITERATIONS];
            double[] marked = new double[ITERATIONS];
            FileWriter writer = null;
            try {
                writer = new FileWriter("TestOut.txt", true);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                writer.write("Tests began: " +  dtf.format(now) + "\r\n\r\n");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    // Closes writer
                    writer.close();
                }
            }
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
                summary.add("Max simulated region: " + AI.MAX_DETERMINED_SIM_REGION);
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
        System.exit(0);
    }

    public static void save() throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter("TestOut.txt", true);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            writer.write("Tests ended: " + dtf.format(now) + "\r\n\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                // Closes writer
                writer.close();
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

    static  class TestCloseMessage extends Thread {
        @Override
        public void run() {
            try {
                save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
