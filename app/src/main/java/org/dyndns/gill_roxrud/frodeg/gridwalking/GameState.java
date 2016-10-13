package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class GameState {

    private static GameState instance = null;
    private Grid grid;
    private Bonus bonus;
    boolean showMap;

    Point<Double> currentPos = new Point(Grid.EAST+1.0, Grid.NORTH+1.0);


    public GameState() {

        grid = new Grid();
        bonus = new Bonus();
        showMap = true;

        Load();
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

    void Load() {
        Persist persist = new Persist();
        persist.Load(grid, bonus);
    }

    void Save() {
        Persist persist = new Persist();
        persist.Save(grid, bonus);
    }

    public void onPositionChanged(MapFragment mapFragment, double x_pos, double y_pos) {
        currentPos.set(x_pos, y_pos);
        try {
            if (true == grid.Discover(currentPos) || null != bonus.ValidBonusKeyFromPos(currentPos)) {
                Save();
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }
}
