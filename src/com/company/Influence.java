package com.company;

import java.util.ArrayList;

class Influence {
    private Coordinate unswept;
    private ArrayList<Coordinate> adjSwept;

     Influence(Coordinate unswept, ArrayList<Coordinate> adjSwept) {
        this.unswept = unswept;
        this.adjSwept = adjSwept;
    }

    ArrayList<Coordinate> getAdj() {
        return this.adjSwept;
    }

    Coordinate getInfluencer() {
        return this.unswept;
    }
}
