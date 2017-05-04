package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class Point<T> {
    private T x;
    private T y;

    Point(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point<T> p) {
        this.x = p.getX();
        this.y = p.getY();
    }

    public void setX(T x) {
        this.x = x;
    }

    public void setY(T y) {
        this.y = y;
    }

    void set(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public T getX() {
        return x;
    }

    public T getY() {
        return y;
    }
}
