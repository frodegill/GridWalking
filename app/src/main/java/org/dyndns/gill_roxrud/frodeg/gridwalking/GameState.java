package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class GameState {

    private static GameState instance = null;
    Grid grid;
    Bonus bonus;
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

    void Load() {
        Persist persist = new Persist();
        persist.Load(grid, bonus);
    }

    void Save() {
        Persist persist = new Persist();
        persist.Save(grid, bonus);
    }

    public void onPositionChanged(double x_pos, double y_pos) {
        Point<Double> pos = new Point(x_pos, y_pos);
        try {
            if (true == grid.Discover(pos) || null != bonus.ValidBonusKeyFromPos(pos)) {
                Save();
            }
        } catch (InvalidPositionException e) {
        }
    }
}
