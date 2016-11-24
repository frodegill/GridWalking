package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class GameState {

    private static GameState instance = null;
    private Grid grid;
    private Bonus bonus;
    private GridWalkingDBHelper db;

    private boolean useDataConnection = true;
    private Long selectedGridKey = null;

    Point<Double> currentPos = new Point<>(Grid.EAST+1.0, Grid.NORTH+1.0);


    public GameState() {
        grid = new Grid();
        bonus = new Bonus();
        db = new GridWalkingDBHelper(GridWalkingApplication.getContext());
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

    public GridWalkingDBHelper getDB() {return db;}

    public boolean getUseDataConnection() {
        return useDataConnection;
    }

    public void setUseDataConnection(boolean useDataConnection) {
        this.useDataConnection = useDataConnection;
    }

    public Long getSelectedGridKey() {
        return selectedGridKey;
    }

    public void setSelectedGridKey(Long selectedGridKey) {
        this.selectedGridKey = selectedGridKey;
    }

    public Point<Double> getCurrentPos() {
        return currentPos;
    }

    public void onPositionChanged(MapFragment mapFragment, double x_pos, double y_pos) {
        currentPos.set(x_pos, y_pos);
        try {
            if (grid.Discover(currentPos) || null != bonus.ValidBonusKeyFromPos(currentPos)) {
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }
}
