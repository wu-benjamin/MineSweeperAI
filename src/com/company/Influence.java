package com.company;

import java.util.ArrayList;

public class Influence {
    private Coordinate unswept;
    private ArrayList<Coordinate> adjSwept;

    public Influence(Coordinate unswept, ArrayList<Coordinate> adjSwept) {
        this.unswept = unswept;
        this.adjSwept = adjSwept;
        return;
    }

    public ArrayList<Coordinate> getAdj() {
        return this.adjSwept;
    }

    public Coordinate getInfluencer() {
        return this.unswept;
    }
}
