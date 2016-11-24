package org.dyndns.gill_roxrud.frodeg.gridwalking;


class Rect<T> {
    private T top;
    private T left;
    private T bottom;
    private T right;

    Rect() {
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

    void setTop(T top) {
        this.top = top;
    }

    void setLeft(T left) {
        this.left = left;
    }

    void setBottom(T bottom) {
        this.bottom = bottom;
    }

    void setRight(T right) {
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

    private T getTop() {
        return top;
    }

    T getLeft() {
        return left;
    }

    T getBottom() {
        return bottom;
    }

    private T getRight() {
        return right;
    }

    Point<T> getUpperLeft() {
        return new Point<>(left, top);
    }

    Point<T> getUpperRight() {
        return new Point<>(right, top);
    }

    Point<T> getLowerLeft() {
        return new Point<>(left, bottom);
    }

    Point<T> getLowerRight() {
        return new Point<>(right, bottom);
    }
}
