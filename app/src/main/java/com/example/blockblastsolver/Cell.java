package com.example.blockblastsolver;
public class Cell {
    private int row;
    private int col;
    private boolean filled;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.filled = false;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }
}
