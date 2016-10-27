package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class GameState {

    private static GameState instance = null;
    private Grid grid;
    private Bonus bonus;
    private Persist persist;
    boolean showMap;

    Point<Double> currentPos = new Point<>(Grid.EAST+1.0, Grid.NORTH+1.0);


    public GameState() {

        grid = new Grid();
        bonus = new Bonus();
        persist = new Persist();
        showMap = true;

//        persist.Load();
    }

    public static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }

    public Grid getGrid() {
        return grid;
    }

    public Bonus getBonus() {
        return bonus;
    }

    public Point<Double> getCurrentPos() {
        return currentPos;
    }

    public void onPositionChanged(MapFragment mapFragment, double x_pos, double y_pos) {
        currentPos.set(x_pos, y_pos);
        try {
            if (grid.Discover(currentPos) || null != bonus.ValidBonusKeyFromPos(currentPos)) {
                persist.setIsModified();
                mapFragment.onScoreUpdated();
            }
            persist.saveIfModified();
        } catch (InvalidPositionException e) {
        }
    }
}
