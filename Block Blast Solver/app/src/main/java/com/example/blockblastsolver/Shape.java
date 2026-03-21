package com.example.blockblastsolver;

public class Shape {
    public int[][] grid;
    public int rows;
    public int cols;

    public Shape(int[][] grid) {
        this.rows = grid.length;
        this.cols = grid[0].length;
        this.grid = grid;
    }
}
