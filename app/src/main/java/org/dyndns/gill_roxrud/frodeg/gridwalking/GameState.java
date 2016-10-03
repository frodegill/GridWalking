package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class GameState {

    private static GameState instance = null;
    private Grid grid;
    private Bonus bonus;
    boolean showMap;

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

    void Load() {
        Persist persist = new Persist();
        persist.Load(grid, bonus);
    }

    void Save() {
        Persist persist = new Persist();
        persist.Save(grid, bonus);
    }

    public void onPositionChanged(MapFragment mapFragment, double x_pos, double y_pos) {
        Point<Double> pos = new Point(x_pos, y_pos);
        try {
            if (true == grid.Discover(pos) || null != bonus.ValidBonusKeyFromPos(pos)) {
                Save();
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }
}
