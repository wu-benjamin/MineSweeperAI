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

    static boolean equal(Influence i1, Influence i2) {
         if (!Coordinate.same(i1.unswept, i2.unswept)) {
             return false;
         }
         if (i1.adjSwept.size() != i2.adjSwept.size()) {
             return false;
         }
         for (int i = 0; i < i1.adjSwept.size(); i++) {
             if (!Coordinate.same(i1.adjSwept.get(i), i2.adjSwept.get(i))) {
                 return false;
             }
         }
         return true;
    }
}
