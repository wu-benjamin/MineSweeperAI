package com.company;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

class AI {

    private static final double EPSILON = 0.00000000001;
    private static boolean[][] suspectedMine;
    private static double[][] probabilityMine;
    private static final boolean STEP_BY_STEP = false;
    private static final boolean AUTO = true;
    private static final boolean DISCARD_DEATH_TURN_ONE = true; // Discounts turn one deaths from win rate in AIOutput
    private static final int AUTO_TIME = 250;
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
    private static final int MIN_NP_NQ = 10;
    private static final int MAX_DETERMINED_SIM_REGION = 15; // Must be less than 63
    private static final long SIMULATIONS_MAX = (long) Math.pow(2, MAX_DETERMINED_SIM_REGION) + 1;

    // 0 = fail
    // 1 = win
    // -1 = dead turn one (also considered an invalid run)
    static AIOutput mineSweeper() {
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
            ArrayList<Coordinate> adj;
            Scanner key = new Scanner(System.in);
            while (!Board.endGame()) {
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
                randomSim = false;
                changed = false;
                //update probabilities
                for (int i = 0; i < Board.getHeight(); i++) {
                    for (int j = 0; j < Board.getWidth(); j++) {
                        newProbability = (double) (Board.getNumMines() - numSusMines)
                                / (double) ((Board.getWidth() * Board.getWidth()) - numSwept);
                        adj = new ArrayList<>(getAdj(i, j));
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
                        if (suspectedMine[i][j] || Board.getBoard()[i][j] == 9) {
                            newProbability = 1.0;
                        }
                        if (Board.getBoard()[i][j] != -1 && Board.getBoard()[i][j] != 9) {
                            newProbability = 0.0;
                        }
                        if (Math.abs(newProbability - probabilityMine[i][j]) > EPSILON) {
                            probabilityMine[i][j] = newProbability;
                            changed = true;
                            if (newProbability > 1.0 - EPSILON && !suspectedMine[i][j]) {
                                //System.out.println("MINE");
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
                //System.out.println("Cycle: " + numSwept);
                if (sweepVoid()) {
                    changed = true;
                }
                if (!changed) {
                    sweepLowestProb();
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
    private static boolean simulateMineProbabilityRegion(Region reg, boolean endGame) {
        Scanner key = new Scanner(System.in);
        //System.out.println("sim");
        int validConfigs = 0;
        boolean[] regionalMine = new boolean[reg.size()];
        int[] validMineCount = new int[reg.size()];
        for (int i = 0; i < validMineCount.length; i++) {
            validMineCount[i] = 0;
            regionalMine[i] = true;
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
        if (Math.pow(2, reg.size()) > SIMULATIONS_MAX) { // Random Simulate
            Random rand = new Random();
            for (int i = 0; i < SIMULATIONS_MAX; i++) {
                for (int j = 0; j < regionalMine.length; j++) {
                    regionalMine[j] = rand.nextBoolean();
                }
                if (Region.valid(reg, regionalMine)) {
                    validConfigs++;
                    for (int k = 0; k < validMineCount.length; k++) {
                        if (regionalMine[k]) {
                            validMineCount[k]++;
                        }
                    }
                }
            }
            //System.out.println(validConfigs + "*");

            for (int i = 0; i < reg.size(); i++) {
                if (validConfigs * (double) validMineCount[i] / (double) validConfigs >= MIN_NP_NQ
                        && validConfigs * (1.0 - (double) validMineCount[i] / (double) validConfigs) >= MIN_NP_NQ
                    /*validConfigs != 0*/ /*&& validMineCount[i] / validConfigs == 0
                        || validConfigs != 0 && validMineCount[i] / validConfigs == 1*/) {
                    probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                            = (double) validMineCount[i] / (double) validConfigs;
                    if (validMineCount[i] != 0 && validMineCount[i] != validConfigs) {
                        probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                                += sigmoid(bias(reg.get(i).getInfluencer().getX(), reg.get(i).getInfluencer().getY(),
                                Board.getHeight(), Board.getWidth())) / 10000.0;
                        probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                                = sigmoid(probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]);
                    }
                }
            }
            return false;
        } else {
            for (long i = 0; i < (int) Math.pow(2, reg.size()); i++) {
                regionalMine = regionMineArrangement(regionalMine, i);
                if (Region.valid(reg, regionalMine)) {
                    if (endGame) {
                        //System.out.println("Region.numMines(i) = " + Region.numMines(i) + " Board.getNumMines() =  " + Board.getNumMines() + " numSusMines = " + numSusMines);
                        if (Region.numMines(i) == Board.getNumMines() - numSusMines) {
                            validConfigs++;
                            for (int k = 0; k < validMineCount.length; k++) {
                                if (regionalMine[k]) {
                                    validMineCount[k]++;
                                }
                            }
                        }
                    } else {
                        validConfigs++;
                        for (int k = 0; k < validMineCount.length; k++) {
                            if (regionalMine[k]) {
                                validMineCount[k]++;
                            }
                        }
                    }
                }
            }
            //System.out.println(validConfigs);
            for (int i = 0; i < reg.size(); i++) {
                if (validConfigs != 0 /*&& validMineCount[i] / validConfigs == 0
                        || validConfigs != 0 && validMineCount[i] / validConfigs == 1*/) {
                    probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                            = (double) validMineCount[i] / (double) validConfigs;
                    if (validMineCount[i] != 0 && validMineCount[i] != validConfigs) {
                        probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                                += sigmoid(bias(reg.get(i).getInfluencer().getX(), reg.get(i).getInfluencer().getY(),
                                Board.getHeight(), Board.getWidth())) / 10000.0;
                        probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]
                                = sigmoid(probabilityMine[reg.get(i).getInfluencer().getX()][reg.get(i).getInfluencer().getY()]);
                    }
                }
            }
            return true;
        }
    }

    private static boolean[] regionMineArrangement(boolean[] regionalMine, long arrangementNum) {
        String arrangement = Long.toBinaryString(arrangementNum);
        arrangement = padZero(arrangement, regionalMine.length);
        for (int i = 0; i < regionalMine.length; i++) {
            if (arrangement.charAt(i) == '1') {
                regionalMine[i] = true;
            } else {
                regionalMine[i] = false;
            }
        }
        //System.out.println(Region.numMines(arrangementNum));
        return regionalMine;
    }

    private static String padZero(String arrangement, int length) {
        while (arrangement.length() < length) {
            arrangement = "0" + arrangement;
        }
        //System.out.println(arrangement);
        return arrangement;
    }

    private static ArrayList<Region> getRegions() {
        ArrayList<Region> regions = new ArrayList<>();
        //System.out.println("getInfluences");
        ArrayList<Influence> influences = new ArrayList<>(getInfluences());
        boolean in;
        for (int i = 0; i < influences.size(); i++) {
            in = false;
            for (int j = 0; j < regions.size(); j++) {
                if (Region.infInRegion(influences.get(i), regions.get(j))) {
                    regions.get(j).add(influences.get(i));
                    in = true;
                    break;
                }
            }
            if (!in) {
                regions.add(new Region(influences.get(i)));
            }
        }
        // merge non-disjoint regions
        boolean merged = true;
        boolean breakFlag;
        while (merged) {
            merged = false;
            breakFlag = false;
            for (int i = 0; i < regions.size() - 1; i++) {
                for (int j = i + 1; j < regions.size(); j++) {
                    if (!Region.disjoint(regions.get(i), regions.get(j))) {
                        regions.get(i).addAll(regions.get(j));
                        regions.remove(j);
                        merged = true;
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

    private static Region getSuperRegion() {
        //System.out.println("getInfluences");
        return new Region(getInfluences());
    }

    private static ArrayList<Influence> getInfluences() {
        ArrayList<Influence> influences = new ArrayList<>();
        for (int i = 0; i < Board.getHeight(); i++) {
            for (int j = 0; j < Board.getWidth(); j++) {
                if (Board.getBoard()[i][j] == -1
                        && getAdjSwept(i, j).size() != 0
                        && !suspectedMine[i][j]) {
                    influences.add(new Influence(new Coordinate(i, j), getAdjSwept(i, j)));
                }
            }
        }
        return influences;
    }

    private static void sweepLowestProb() {
        //System.out.println("sweep lowest");
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
        if (lowestProb > 0.0 + EPSILON) {
            //System.out.println("getRegion");
            Scanner key = new Scanner(System.in);
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
            if (Board.getHeight() * Board.getWidth() - numSusMines - numSwept
                    > (long) Math.ceil(Math.log(SIMULATIONS_MAX) / Math.log(2.0))) {
                ArrayList<Region> regions = getRegions();
                for (int i = 0; i < regions.size(); i++) {
                    if (!simulateMineProbabilityRegion(regions.get(i), false)) {
                        randomSim = true;
                    }
                }
            } else {
                simulateMineProbabilityRegion(getSuperRegion(), true);
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
            boolean toReturn = false;
            for (int i = 0; i < Board.getHeight(); i++) {
                for (int j = 0; j < Board.getWidth(); j++) {
                    if (probabilityMine[i][j] > 1.0 - EPSILON && !suspectedMine[i][j]) {
                        //System.out.println("MINE");
                        suspectedMine[i][j] = true;
                        numSusMines++;
                        if (randomSim) {
                            numGuessed++;
                        }
                        toReturn = true;
                        if (randomSim) {
                            return;
                        }
                    }
                }
            }
            if (!randomSim) {
                if (sweepVoid()) {
                    return;
                }
            }
            if (toReturn) {
                return;
            }
            lowestProb = 1.0;
            for (int i = 0; i < Board.getHeight(); i++) {
                for (int j = 0; j < Board.getWidth(); j++) {
                    if (probabilityMine[i][j] < lowestProb && Board.getBoard()[i][j] == -1 && !suspectedMine[i][j]
                            || Board.getBoard()[lowest.getX()][lowest.getY()] != -1 && Board.getBoard()[i][j] == -1 && !suspectedMine[i][j]) {
                        lowest.setX(i);
                        lowest.setY(j);
                        lowestProb = probabilityMine[i][j];
                        //System.out.println("I should always print this before dying");
                    }
                }
            }
        }
        if (lowestProb > 0.0 + EPSILON || randomSim) {
            numGuessed++;
        }
        Board.sweep(lowest.getX(), lowest.getY());
        moves.add(new Coordinate(lowest.getX(), lowest.getY()));
        numSwept++;
        //System.out.println("done");
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

    private static ArrayList<Coordinate> getAdjUnSwept(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<>(getAdj(x, y));
        ArrayList<Coordinate> toRemove = new ArrayList<>();
        for (int i = 0; i < adj.size(); i++) {
            if (Board.getBoard()[adj.get(i).getX()][adj.get(i).getY()] != -1) {
                toRemove.add(adj.get(i));
            }
        }
        adj.removeAll(toRemove);
        return adj;
    }

    private static ArrayList<Coordinate> getAdjSwept(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<>(getAdj(x, y));
        ArrayList<Coordinate> toRemove = new ArrayList<>();
        for (int i = 0; i < adj.size(); i++) {
            if (Board.getBoard()[adj.get(i).getX()][adj.get(i).getY()] == -1) {
                toRemove.add(adj.get(i));
            }
        }
        adj.removeAll(toRemove);
        return adj;
    }

    static ArrayList<Coordinate> getAdjSusMines(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<>(getAdj(x, y));
        ArrayList<Coordinate> toRemove = new ArrayList<>();
        for (int i = 0; i < adj.size(); i++) {
            if (!suspectedMine[adj.get(i).getX()][adj.get(i).getY()]) {
                toRemove.add(adj.get(i));
            }
        }
        adj.removeAll(toRemove);
        return adj;
    }

    private static ArrayList<Coordinate> getAdj(int x, int y) {
        ArrayList<Coordinate> adj = new ArrayList<>();
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


    static double[][] getMineProbabilities() {
        return probabilityMine;
    }

    static boolean[][] getSuspectedMine() {
        return suspectedMine;
    }

    // Function has min at |x| = |y| = 1;
    private static double bias(int i, int j, int maxI, int maxJ) {
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

    private static double sigmoid(double x) {
        return (0.99 / (1 + Math.exp(-5* (x - 0.5))));
    }
}
