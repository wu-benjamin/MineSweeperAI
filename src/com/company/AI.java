package com.company;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

class AI {

    private static final double EPSILON = 0.00000000001;
    private static boolean[][] suspectedMine;
    private static double[][] probabilityMine;
    private static final boolean DISCARD_DEATH_TURN_ONE = true; // Discounts turn one deaths from win rate in AIOutput
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
    static final int MAX_DETERMINED_SIM_REGION = 20; // Must be less than 63 with current implementation
    private static final long SIMULATIONS_MAX = (long) Math.pow(2, MAX_DETERMINED_SIM_REGION) + 1;
    private static ArrayList<Region> regions = new ArrayList<>();

    // 0 = fail
    // 1 = win
    // -1 = dead turn one (also considered an invalid run)
    static AIOutput mineSweeper() {
        probabilityMine = new double[Board.getHeight()][Board.getWidth()];
        suspectedMine = new boolean[Board.getHeight()][Board.getWidth()];
        try {
            if (Main.AUTO && !Main.TEST) {
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
                if (!Main.TEST) {
                    if (Main.STEP_BY_STEP) {
                        key.nextInt();
                    }
                    if (Main.AUTO) {
                        try {
                            Thread.sleep(Main.AUTO_TIME);
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
                            if (newProbability == 1.0 || newProbability == 0.0 || probabilityMine[i][j] == baseProbMine) {
                                probabilityMine[i][j] = newProbability;
                                changed = true;
                            }
                            if (newProbability > 1.0 - EPSILON && !suspectedMine[i][j]) {
                                //System.out.println("MINE");
                                suspectedMine[i][j] = true;
                                numSusMines++;
                            }
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
                //System.out.println(numSwept + " tiles swept out of " + (Board.getWidth() * Board.getHeight() - Board.getNumMines()));
                Main.progress3.setValue(numSwept);
                Main.labelProgress3.setText("   Tile " + numSwept + " of " + (Board.getWidth() * Board.getHeight() - Board.getNumMines()));
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
            if (Board.getHeight() * Board.getWidth() - numSusMines - numSwept
                    > (long) Math.ceil(Math.log(SIMULATIONS_MAX) / Math.log(2.0))) {
                getRegions();
                //System.out.println(numChangedRegions());
                //System.out.println("Size: " + regions.size());
                int numChanged = numChangedRegions();
                Main.progress4.setMaximum(numChanged);
                Main.progress4.setValue(0);
                Main.labelProgress4.setText("   Regions simulated 0 of " + numChanged);
                int simulatedCount = 0;
                Main.progress4.setValue(simulatedCount);
                for (int i = 0; i < regions.size(); i++) {
                    if (regions.get(i).getChanged()) {
                        simulatedCount++;
                        if (!simulateMineProbabilityRegion(regions.get(i), false)) {
                            randomSim = true;
                        }
                        Main.labelProgress4.setText("   Regions simulated " + simulatedCount + " of " + numChanged);
                        Main.progress4.setValue(simulatedCount);
                    }
                }
            } else {
                //System.out.println("Endgame");
                Main.progress4.setMaximum(1);
                Main.progress4.setValue(0);
                Main.labelProgress4.setText("   Regions simulated 0 of 1");
                simulateMineProbabilityRegion(getSuperRegion(), true);
                Main.labelProgress4.setText("   Regions simulated 1 of 1");
                Main.progress4.setValue(1);
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

    // Returns true if certain, otherwise returns false
    private static boolean simulateMineProbabilityRegion(Region reg, boolean endGame) {
        //System.out.println("sim");
        int validConfigs = 0;
        boolean[] regionalMine = new boolean[reg.size()];
        int[] validMineCount = new int[reg.size()];
        for (int i = 0; i < validMineCount.length; i++) {
            validMineCount[i] = 0;
            regionalMine[i] = true;
        }
        Main.labelProgress5.setText("   Arrangement " + 0 + " of " + 0);
        Main.progress5.setValue(0);
        if (Math.pow(2, reg.size()) > SIMULATIONS_MAX) { // Random Simulate
            Random rand = new Random();
            Main.labelProgress5.setText("   Arrangement " + 0 + " of " + SIMULATIONS_MAX);
            Main.progress5.setMaximum(Integer.MAX_VALUE);
            for (long i = 0; i < SIMULATIONS_MAX; i++) {
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
                Main.progress5.setValue((int)(((double)i / (double)SIMULATIONS_MAX) * (double)Integer.MAX_VALUE));
                Main.labelProgress5.setText("   Arrangement " + i + " of " + SIMULATIONS_MAX);
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
            Main.labelProgress5.setText("   Arrangement " + 0 + " of " + (long) Math.pow(2, reg.size()));
            Main.progress5.setMaximum(Integer.MAX_VALUE);
            for (long i = 0; i < (long) Math.pow(2, reg.size()); i++) {
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
                Main.progress5.setValue((int)(((double)i / Math.pow(2, reg.size())) * (double)Integer.MAX_VALUE));
                Main.labelProgress5.setText("   Arrangement " + i + " of " + (long) Math.pow(2, reg.size()));
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

    private static void getRegions() {
        ArrayList<Region> newRegions = new ArrayList<>();
        //System.out.println("getInfluences");
        ArrayList<Influence> influences = new ArrayList<>(getInfluences());
        boolean in;
        for (int i = 0; i < influences.size(); i++) {
            in = false;
            for (int j = 0; j < newRegions.size(); j++) {
                if (Region.infInRegion(influences.get(i), newRegions.get(j))) {
                    newRegions.get(j).add(influences.get(i));
                    in = true;
                    break;
                }
            }
            if (!in) {
                newRegions.add(new Region(influences.get(i)));
            }
        }
        // merge non-disjoint regions
        boolean merged = true;
        boolean breakFlag;
        while (merged) {
            merged = false;
            breakFlag = false;
            for (int i = 0; i < newRegions.size() - 1; i++) {
                for (int j = i + 1; j < newRegions.size(); j++) {
                    if (!Region.disjoint(newRegions.get(i), newRegions.get(j))) {
                        newRegions.get(i).addAll(newRegions.get(j));
                        newRegions.remove(j);
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
        ArrayList<Region> newRegionToRemove = new ArrayList<>();
        ArrayList<Region> regionsToRemove = new ArrayList<>();
        boolean found;
        for (int i = 0; i < regions.size(); i++) {
            found = false;
            for (int j = 0; j < newRegions.size(); j++) {
                if (Region.equal(regions.get(i), newRegions.get(j))) {
                    newRegionToRemove.add(newRegions.get(j));
                    regions.get(i).setChanged(false);
                    //System.out.println("Unchanged");
                    found = true;
                }
            }
            if (!found) {
                regionsToRemove.add(regions.get(i));
            }
        }
        newRegions.removeAll(newRegionToRemove);
        regions.removeAll(regionsToRemove);
        regions.addAll(newRegions);
        newRegionToRemove.clear();
        regionsToRemove.clear();
        newRegions.clear();
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

    static int numChangedRegions() {
        int count = 0;
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).getChanged()) {
                count++;
            }
        }
        return count;
    }
}
