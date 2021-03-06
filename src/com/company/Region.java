package com.company;

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

import java.util.ArrayList;

public class Region {
    private ArrayList<Influence> members;
    private boolean changed;

    Region(ArrayList<Influence> influences) {
        this.changed = true;
        this.members = new ArrayList<>(influences);
    }

    Region(Influence inf) {
        this.changed = true;
        ArrayList<Influence> members = new ArrayList<>();
        members.add(inf);
        this.members = members;
    }

    void add(Influence inf) {
        this.members.add(inf);
    }

    int size() {
        return this.members.size();
    }

    Influence get(int index) {
        return this.members.get(index);
    }

    void addAll(Region reg) {
        this.members.addAll(reg.members);
    }

    boolean getChanged() {
        return this.changed;
    }

    void setChanged(boolean changed) {
        this.changed = changed;
    }

    static boolean disjoint(Region a, Region b) {
        boolean[][] influenced = new boolean[Board.getHeight()][Board.getWidth()];
        for (int i = 0; i < Board.getHeight(); i++) {
            for (int j = 0; j < Board.getWidth(); j++) {
                influenced[i][j] = false;
            }
        }
        for (int i = 0; i < a.size(); i++) {
            for (int j = 0; j < a.get(i).getAdj().size(); j++) {
                influenced[a.get(i).getAdj().get(j).getX()][a.get(i).getAdj().get(j).getY()] = true;
            }
        }
        for (int i = 0; i < b.size(); i++) {
            for (int j = 0; j < b.get(i).getAdj().size(); j++) {
                if (influenced[b.get(i).getAdj().get(j).getX()][b.get(i).getAdj().get(j).getY()]) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean infInRegion(Influence inf, Region reg) {
        for (int i = 0; i < reg.size(); i++) {
            for (int j = 0; j < inf.getAdj().size(); j++) {
                for (int k = 0; k < reg.get(i).getAdj().size(); k++) {
                    if (Coordinate.same(inf.getAdj().get(j), reg.get(i).getAdj().get(k))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static int numMines(long arrangement) {
        return Long.bitCount(arrangement);
    }

    private static boolean inRegion(Region reg, int x, int y) {
        for (int i = 0; i < reg.size(); i++) {
            for(int j = 0; j < reg.get(i).getAdj().size(); j++) {
                if (x == reg.get(i).getAdj().get(j).getX() && y == reg.get(i).getAdj().get(j).getY()) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean valid(Region reg, boolean[] mines) {
        int[][] simValue = new int[Board.getHeight()][Board.getWidth()];
        for (int i = 0; i < simValue.length; i++) {
            for (int j = 0; j < simValue[0].length; j++) {
                simValue[i][j] = AI.getAdjSusMines(i, j).size();
            }
        }
        for (int i = 0; i < reg.size(); i++) {
            if (mines[i]) {
                for (int j = 0; j < reg.get(i).getAdj().size(); j++) {
                    simValue[reg.get(i).getAdj().get(j).getX()][reg.get(i).getAdj().get(j).getY()]++;
                }
            }
        }
        // Check if simValue agrees with actual values
        for (int i = 0; i < Board.getHeight(); i++) {
            for (int j = 0; j < Board.getWidth(); j++) {
                if (inRegion(reg, i, j)) {
                    if (simValue[i][j] != Board.getBoard()[i][j]) {
                        /*
                        for (int k = 0; k < mines.length; k++) {
                            if (!mines[k]) {
                                System.out.print(0);
                            } else {
                                System.out.print(1);
                            }
                        }
                        System.out.println();
                        System.out.println("i: " + i + " j: " + j + " simVal: " + simValue[i][j] + " BoardVal: " + Board.getBoard()[i][j]);
                        */
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static boolean equal(Region r1, Region r2) {
        if (r1.members.size() != r2.members.size()) {
            return false;
        }
        for (int i = 0; i < r1.members.size(); i++) {
             if (!Influence.equal(r1.members.get(i), r2.members.get(i))) {
                return false;
            }
        }
        return true;
    }
}
