package com.company;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class AI {

    private static final double EPSILON = 0.00000000001;
    private static boolean[][] suspectedMine;
    private static double[][] probabilityMine;
    private static final boolean STEP_BY_STEP = true;
    private static final boolean AUTO = false;
    private static final boolean DISCARD_DEATH_TURN_ONE = true;
    private static final int AUTO_TIME = 1000;
    private static final int PREFERRED_COORDINATE = 3;
    private static double newProbability;
    private static double lowestProb;
    private static int numSwept;
    private static int numSusMines;
    private static int numGuessed;
    private static int gameResult;
    private static ArrayList<Coordinate> moves = new ArrayList<Coordinate>();
    private static boolean changed;
    private static boolean randomSim;
    private static final int SIMULATIONS_MAX = (int) Math.pow(2, 20); // Exponent must be less than 64

    // A region is a collection of unswept tiles paired with a list of swept tiles whose value they influence
    // Two regions are disjoint if no two unswept tiles in the regions share any adjacent swept tiles
    // Analyze a region of size N by trying 2 ^ N combinations of mine or no mine on each tile in the region
    // and checking if the configuration is valid given the numbers on swept tiles
    // The probability of a tile being a mine is the number of valid configurations with the tile being a mine
    // divided by the number of valid configurations
    // For low remaining tiles, consider all remaining tiles as a "super-region"
    // If only one contiguous region exists and the region contains all remaining unswept tiles,
    // when checking for valid mine configurations, remove all configurations with a number of mines different from
    // the actual number of remaining mines

    // 0 = fail
    // 1 = win
    // -1 = dead turn one (also considered an invalid run)
    public static AIOutput mineSweeper() {
        probabilityMine = new double[Board.getHeight()][Board.getWidth()];
        suspectedMine = new boolean[Board.getHeight()][Board.getWidth()];
        try {
            if (AUTO && !Main.isTesting()) {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            moves.clear();
            double baseProbMine = (double) Board.getNumMines() / (double) (Board.getWidth() * Board.getHeight());
            for (int i = 0; i < Board.getHeight(); i++) {
                for (int j = 0; j < Board.getWidth(); j++) {
                    probabilityMine[i][j] = baseProbMine;
                    suspectedMine[i][j] = false;
                }
            }
            numSwept = 0;
            numSusMines = 0;
            numGuessed = 0;
            ArrayList<Coordinate> adj = new ArrayList<Coordinate>();
            Scanner key = new Scanner(System.in);
            while (!Board.endGame()) {
                randomSim = false;
                changed = false;
                //update probabilities
                for (int i = 0; i < Board.getHeight(); i++) {
                    for (int j = 0; j < Board.getWidth(); j++) {
                        newProbability = (double) (Board.getNumMines() - numSusMines)
                                / (double) ((Board.getWidth() * Board.getWidth()) - numSwept);
                        adj = new ArrayList<Coordinate>(getAdj(i, j));
                        newProbability += sigmoid(bias(i, j, Board.getHeight(), Board.getWidth())) / 10000.0;
                        newProbability = sigmoid(newProbability);
                        if (adj.size() > getAdjUnSwept(i, j).size()) {
                            newProbability = 1.0;
                            for (int k = 0; k < adj.size(); k++) {
                                if (Board.getBoard()[adj.get(k).getX()][adj.get(k).getY()] != -1) {
                                    newProbability *= (1.0 - ((double) (Board.getBoard()[adj.get(k).getX()][adj.get(k).getY()]
                                            - getAdjSusMines(adj.get(k).getX(), adj.get(k).getY()).size())
                                            / (double) (getAdjUnSwept(adj.get(k).getX(), adj.get(k).getY()).size()
                                            - getAdjSusMines(adj.get(k).getX(), adj.get(k).getY()).size())));
                                }
                            }
                            newProbability = 1.0 - newProbability;
                            newProbability += (double) (Board.getNumMines() - numSusMines)
                                    / (double) ((Board.getWidth() * Board.getWidth()) - numSwept);
                        /*
                        newProbability = Math.min(newProbability, 0.99);
                        */
                            newProbability += sigmoid(bias(i, j, Board.getHeight(), Board.getWidth())) / 100.0;
                            newProbability = sigmoid(newProbability);
                            if (adj.size() > getAdjUnSwept(i, j).size()) {
                                for (int k = 0; k < adj.size(); k++) {
                                    if (Board.getBoard()[adj.get(k).getX()][adj.get(k).getY()]
                                            - getAdjSusMines(adj.get(k).getX(), adj.get(k).getY()).size() == 0) {
                                        newProbability = 0.0;
                                        break;
                                    }
                                    if ((double) (Board.getBoard()[adj.get(k).getX()][adj.get(k).getY()]
                                            - getAdjSusMines(adj.get(k).getX(), adj.get(k).getY()).size())
                                            / (double) (getAdjUnSwept(adj.get(k).getX(), adj.get(k).getY()).size()
                                            - getAdjSusMines(adj.get(k).getX(), adj.get(k).getY()).size()) > 1.0 - EPSILON) {
                                        newProbability = 1.0;
                                        break;
                                    }
                                }
                            }
                        }
                        if (suspectedMine[i][j]) {
                            newProbability = 1.0;
                        }
                        if (Board.getBoard()[i][j] != -1) {
                            newProbability = 0.0;
                        }
                        if (Math.abs(newProbability - probabilityMine[i][j]) > EPSILON) {
                            probabilityMine[i][j] = newProbability;
                            changed = true;
                            if (newProbability > 1.0 - EPSILON && !suspectedMine[i][j]) {
                                System.out.println("MINE");
                                suspectedMine[i][j] = true;
                                numSusMines++;
                            }
                        }
                    }
                }
                if (!Main.isTesting()) {
                    if (STEP_BY_STEP) {
                        key.nextInt();
                    }
                    if (AUTO) {
                        try {
                            Thread.sleep(AUTO_TIME);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                changed = sweepVoid();
                if (!Main.isTesting()) {
                    if (STEP_BY_STEP) {
                        key.nextInt();
                    }
                    if (AUTO) {
                        try {
                            Thread.sleep(AUTO_TIME);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!changed) {
                    if (Board.getHeight() * Board.getWidth() - numSusMines - numSwept
                            > (int) Math.ceil(Math.log(SIMULATIONS_MAX)/Math.log(2.0))) {
                        ArrayList<Region> regions = getRegions();
                        for (int i = 0; i < regions.size(); i++) {
                            if (!simulateMineProbabilityRegion(regions.get(i), false)) {
                                randomSim = true;
                            }
                        }
                    } else {
                        simulateMineProbabilityRegion(getSuperRegion(), true);
                    }
                    sweepLowestProb();
                }
            }

            if (!Main.isTesting()) {
                if (STEP_BY_STEP) {
                    key.nextInt();
                }
                if (AUTO) {
                    try {
                        Thread.sleep(AUTO_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!Board.isDead()) {
                for (int i = 0; i < Board.getHeight(); i++) {
                    for (int j = 0; j < Board.getWidth(); j++) {
                        if (Board.getBoard()[i][j] == -1 && !suspectedMine[i][j]) {
                            suspectedMine[i][j] = true;
                            numSusMines++;
                        }
                    }
                }
            }
            //Board.reset();
            //System.out.print(numSwept);
            if (Board.isDead()) {
                if (numSwept == 1 && DISCARD_DEATH_TURN_ONE) {
                    gameResult = -1;
                } else {
                    gameResult = 0;
                }
            } else {
                gameResult = 1;
            }
            return new AIOutput(numGuessed, numSusMines, numSwept, gameResult, moves);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Returns true if certain, otherwise returns false
    public static boolean simulateMineProbabilityRegion(Region reg, boolean endGame) {
        boolean[] regionalMine = new boolean[reg.size()];
        int[] validMineCount = new int[reg.size()];
        for (int i = 0; i < validMineCount.length; i++) {
            validMineCount[i] = 0;
        }
        if (Math.pow(2, reg.size()) > SIMULATIONS_MAX) { // Random Simulate
            Random rand = new Random();
            for (int i = 0; i < SIMULATIONS_MAX; i++) {
                for (int j = 0; j < regionalMine.length; j++) {
                    regionalMine[j] = rand.nextBoolean();
                }
                if (Region.valid(reg, regionalMine)) {
                    for (int k = 0; k < validMineCount.length; k++) {
                        if (regionalMine[k]) {
                            validMineCount[k]++;
                        }
                    }
                }
            }
            for (int i = 0; i < reg.size(); i++) {
                probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                        = (double) validMineCount[i] / (double) SIMULATIONS_MAX;
            }
            return false;
        } else {
            for (long i = 0; i < Math.pow(2, reg.size()); i++) {
                regionMineArrangement(regionalMine, i);
                if (Region.valid(reg, regionalMine)) {
                    if (endGame) {
                        if (Region.numMines(i) == Board.getNumMines() - numSusMines) {
                            for (int k = 0; k < validMineCount.length; k++) {
                                if (regionalMine[k]) {
                                    validMineCount[k]++;
                                }
                            }
                        }
                    } else {
                        for (int k = 0; k < validMineCount.length; k++) {
                            if (regionalMine[k]) {
                                validMineCount[k]++;
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < reg.size(); i++) {
                probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                        = (double) validMineCount[i] / (double) SIMULATIONS_MAX;
            }
            return true;
        }
    }

    public static void regionMineArrangement(boolean[] regionalMine, long arrangementNum) {
        String arrangement = Long.toBinaryString(arrangementNum);
        arrangement = padZero(arrangement, regionalMine.length);
        for (int i = 0; i < regionalMine.length; i++) {
            regionalMine[i] = arrangement.charAt(i) == '1';
        }
    }

    public static String padZero(String arrangement, int length) {
        while (arrangement.length() < length) {
            arrangement = "0" + arrangement;
        }
        return arrangement;
    }

    public static ArrayList<Region> getRegions() {
        ArrayList<Region> regions = new ArrayList<Region>();
        ArrayList<Influence> influences = new ArrayList<Influence>(getInfluences());
        for (int i = 0; i < influences.size(); i++) {
            for (int j = 0; j < regions.size(); j++) {
                if (Region.infInRegion(influences.get(i), regions.get(j))) {
                    regions.add(new Region(influences.get(i)));
                } else {
                    regions.get(j).add(influences.get(i));
                }
            }
        }
        // merge non-disjoint regions
        changed = true;
        boolean breakFlag = false;
        while (changed) {
            changed = false;
            breakFlag = false;
            for (int i = 0; i < regions.size() - 1; i++) {
                for (int j = i + 1; j < regions.size(); j++) {
                    if (!Region.disjoint(regions.get(i), regions.get(j))) {
                        regions.get(i).addAll(regions.get(j));
                        regions.remove(j);
                        changed = true;
                        breakFlag = true;
                        break;
                    }
                }
                if (breakFlag) {
                    break;
                }
            }
        }
        return regions;
    }

    public static Region getSuperRegion() {
        return new Region(getInfluences());
    }

    public static ArrayList<Influence> getInfluences() {
        ArrayList<Influence> influences = new ArrayList<Influence>();
        for (int i = 0; i < Board.getHeight(); i++) {
            for (int j = 0; j < Board.getWidth(); j++) {
                if (Board.getBoard()[i][j] == -1 && getAdjSwept(i, j).size() != 0) {
                    influences.add(new Influence(new Coordinate(i, j), getAdjSwept(i, j)));
                }
            }
        }
        return influences;
    }

    public static void sweepLowestProb() {
        Coordinate lowest = new Coordinate(0, 0);
        lowestProb = 1.0;
        for (int i = 0; i < Board.getHeight(); i++) {
            for (int j = 0; j < Board.getWidth(); j++) {
                if (probabilityMine[i][j] < lowestProb && Board.getBoard()[i][j] == -1 && !suspectedMine[i][j]
                        || Board.getBoard()[lowest.getX()][lowest.getY()] != -1 && Board.getBoard()[i][j] == -1 && !suspectedMine[i][j]) {
                    lowest.setX(i);
                    lowest.setY(j);
                    lowestProb = probabilityMine[i][j];
                }
            }
        }
        moves.add(new Coordinate(lowest.getX(), lowest.getY()));
        Board.sweep(lowest.getX(), lowest.getY());
        if (lowestProb > 0.0 + EPSILON || randomSim) {
            numGuessed++;
        }
        numSwept++;
    }

    // returns true if swept something
    public static boolean sweepVoid() {
        boolean swept = false;
        //sweep zeroes
        for (int i = 0; i < Board.getHeight(); i++) {
            for (int j = 0; j < Board.getWidth(); j++) {
                if (probabilityMine[i][j] < 0.0 + EPSILON && Board.getBoard()[i][j] == -1 && !suspectedMine[i][j]) {
                    numSwept++;
                    moves.add(new Coordinate(i, j));
                    Board.sweep(i, j);
                    swept = true;
                }
            }
        }
        return swept;
    }

    public static ArrayList<Coordinate> getAdjUnSwept(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<Coordinate>(getAdj(x, y));
        ArrayList<Coordinate> toRemove = new ArrayList<Coordinate>();
        for (int i = 0; i < adj.size(); i++) {
            if (Board.getBoard()[adj.get(i).getX()][adj.get(i).getY()] != -1) {
                toRemove.add(adj.get(i));
            }
        }
        adj.removeAll(toRemove);
        return adj;
    }

    public static ArrayList<Coordinate> getAdjSwept(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<Coordinate>(getAdj(x, y));
        ArrayList<Coordinate> toRemove = new ArrayList<Coordinate>();
        for (int i = 0; i < adj.size(); i++) {
            if (Board.getBoard()[adj.get(i).getX()][adj.get(i).getY()] == -1) {
                toRemove.add(adj.get(i));
            }
        }
        adj.removeAll(toRemove);
        return adj;
    }

    public static ArrayList<Coordinate> getAdjSusMines(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<Coordinate>(getAdj(x, y));
        ArrayList<Coordinate> toRemove = new ArrayList<Coordinate>();
        for (int i = 0; i < adj.size(); i++) {
            if (!suspectedMine[adj.get(i).getX()][adj.get(i).getY()]) {
                toRemove.add(adj.get(i));
            }
        }
        adj.removeAll(toRemove);
        return adj;
    }

    public static ArrayList<Coordinate> getAdj(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<Coordinate>();
        if (x != 0) {
            adj.add(new Coordinate(x - 1, y));
            if (y != 0) {
                adj.add(new Coordinate( x - 1, y - 1));
            }
            if (y != Board.getWidth() - 1) {
                adj.add(new Coordinate(x - 1, y + 1));
            }
        }
        if (x != Board.getHeight() - 1) {
            adj.add(new Coordinate(x + 1, y));
            if (y != 0) {
                adj.add(new Coordinate( x + 1, y - 1));
            }
            if (y != Board.getWidth() - 1) {
                adj.add(new Coordinate(x + 1, y + 1));
            }
        }
        if (y != 0) {
            adj.add(new Coordinate(x, y - 1));
        }
        if (y != Board.getWidth() - 1) {
            adj.add(new Coordinate(x, y + 1));
        }
        //Board.reset();
        return adj;
    }


    public static double[][] getMineProbabilities() {
        return probabilityMine;
    }

    public static boolean[][] getSuspectedMine() {
        return suspectedMine;
    }

    // Function has min at |x| = |y| = 1;
    public static double bias(int i, int j, int maxI, int maxJ) {
        double x = (double) i;
        double y = (double) j;
        //board is recentered by subtracting half the x and y dimension from each x and y value respectively
        //dividing by the amplitude of the x and y part of where 3,3 end up scales the board so that it is squared
        //with 3,3 mapping to -1,-1 and everything else linearly scaling accordingly
        double xScale = Math.abs((PREFERRED_COORDINATE - (double) maxI / 2.0));
        double yScale = Math.abs((PREFERRED_COORDINATE - (double) maxJ / 2.0));
        //rescale so that game board {3,3} maps to {-1, -1} and there is reflective symmetry about the x and y axis
        //the x and y axis cross the centre of the board horizontally and vertically respectively
        x -= (double) maxI / 2.0;
        y -= (double) maxJ / 2.0;
        x /= xScale;
        y /= yScale;
        return (((Math.pow(x, 4) - 2 * x * x + Math.pow(y, 4) - 2 * y * y) / 4.0) + 0.5);
    }

    public static double sigmoid(double x) {
        return (0.99 / (1 + Math.exp(-5* (x - 0.5))));
    }

}
