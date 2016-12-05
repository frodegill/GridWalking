package org.dyndns.gill_roxrud.frodeg.gridwalking;


class GameState {

    private static GameState instance = null;
    private Grid grid;
    private Bonus bonus;
    private GridWalkingDBHelper db;

    private boolean useDataConnection = true;
    private Integer selectedGridKey = null;

    private Point<Double> currentPos = new Point<>(Grid.EAST+1.0, Grid.NORTH+1.0);


    private GameState() {
        grid = new Grid();
        bonus = new Bonus();
        db = new GridWalkingDBHelper(GridWalkingApplication.getContext());
    }

    static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }

    Grid getGrid() {
        return grid;
    }

    Bonus getBonus() {
        return bonus;
    }

    GridWalkingDBHelper getDB() {return db;}

    boolean getUseDataConnection() {
        return useDataConnection;
    }

    void setUseDataConnection(boolean useDataConnection) {
        this.useDataConnection = useDataConnection;
    }

    Integer getSelectedGridKey() {
        return selectedGridKey;
    }

    void setSelectedGridKey(Integer selectedGridKey) {
        this.selectedGridKey = selectedGridKey;
    }

    Point<Double> getCurrentPos() {
        return currentPos;
    }

    void onPositionChanged(MapFragment mapFragment, double x_pos, double y_pos) {
        currentPos.set(x_pos, y_pos);
        try {
            if (grid.Discover(currentPos, false) || null!=bonus.ValidBonusKeyFromPos(currentPos)) {
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }
}
