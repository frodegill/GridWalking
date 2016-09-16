package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class Rect<T> {
    private T top;
    private T left;
    private T bottom;
    private T right;

    public Rect() {
    }

    public Rect(T top, T left, T bottom, T right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public Rect(Rect<T> r) {
        this.top = r.getTop();
        this.left = r.getLeft();
        this.bottom = r.getBottom();
        this.right = r.getRight();
    }

    public void setTop(T top) {
        this.top = top;
    }

    public void setLeft(T left) {
        this.left = left;
    }

    public void setBottom(T bottom) {
        this.bottom = bottom;
    }

    public void setRight(T right) {
        this.right = right;
    }

    public void setUpperLeft(Point<T> upperLeft) {
        this.top = upperLeft.getY();
        this.left = upperLeft.getX();
    }

    public void setLowerRight(Point<T> lowerRight) {
        this.bottom = lowerRight.getY();
        this.right = lowerRight.getX();
    }

    public T getTop() {
        return top;
    }

    public T getLeft() {
        return left;
    }

    public T getBottom() {
        return bottom;
    }

    public T getRight() {
        return right;
    }

    public Point<T> getUpperLeft() {
        return new Point<T>(left, top);
    }

    public Point<T> getUpperRight() {
        return new Point<T>(right, top);
    }

    public Point<T> getLowerLeft() {
        return new Point<T>(left, bottom);
    }

    public Point<T> getLowerRight() {
        return new Point<T>(right, bottom);
    }
}
