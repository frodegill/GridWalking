package org.dyndns.gill_roxrud.frodeg.gridwalking;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class BonusUnitTest {

    private static final double LESS_THAN_HALF_HOR_BONUS_DEGREE = Bonus.HALF_HOR_BONUS_DEGREE/2;
    private static final double LESS_THAN_HALF_VER_BONUS_DEGREE = Bonus.HALF_VER_BONUS_DEGREE/2;

    @Test(expected = InvalidPositionException.class)
    public void verticalInvalidSouthTest() throws Exception {
        Bonus.ToVerticalBonusGrid(Grid.SOUTH);
    }

    @Test(expected = InvalidPositionException.class)
    public void verticalInvalidNorthTest() throws Exception {
        Bonus.ToVerticalBonusGrid(Grid.NORTH);
    }

    @Test
    public void verticalTest() throws Exception {
        int testGrid = Bonus.ToVerticalBonusGrid(Grid.GRID_MAX_SOUTH);
        assertEquals(0, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(Grid.GRID_MAX_NORTH - LESS_THAN_HALF_VER_BONUS_DEGREE);
        assertEquals(Bonus.VER_BONUS_COUNT-1, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(0.0);
        assertEquals(Bonus.VER_BONUS_COUNT/2, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(0.0 + LESS_THAN_HALF_VER_BONUS_DEGREE);
        assertEquals(Bonus.VER_BONUS_COUNT/2, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(0.0 + Bonus.HALF_VER_BONUS_DEGREE);
        assertEquals(Bonus.VER_BONUS_COUNT/2, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(0.0 + 2*Bonus.HALF_VER_BONUS_DEGREE);
        assertEquals(Bonus.VER_BONUS_COUNT/2 + 1, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(0.0 - LESS_THAN_HALF_VER_BONUS_DEGREE);
        assertEquals(Bonus.VER_BONUS_COUNT/2 - 1, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(0.0 - Bonus.HALF_VER_BONUS_DEGREE);
        assertEquals(Bonus.VER_BONUS_COUNT/2 - 1, testGrid);

        testGrid = Bonus.ToVerticalBonusGrid(0.0 - 2*Bonus.HALF_VER_BONUS_DEGREE - LESS_THAN_HALF_VER_BONUS_DEGREE);
        assertEquals(Bonus.VER_BONUS_COUNT/2 - 2, testGrid);
    }

    @Test
    public void horizontalTest() throws Exception {
        int testGrid = Bonus.ToHorizontalBonusGrid(Grid.WEST);
        assertEquals(0, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(Grid.EAST);
        assertEquals(Bonus.HOR_BONUS_COUNT - 1, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(0.0);
        assertEquals(Bonus.HOR_BONUS_COUNT/2, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(0.0 + LESS_THAN_HALF_HOR_BONUS_DEGREE);
        assertEquals(Bonus.HOR_BONUS_COUNT/2, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(0.0 + Bonus.HALF_HOR_BONUS_DEGREE);
        assertEquals(Bonus.HOR_BONUS_COUNT/2, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(0.0 + 2*Bonus.HALF_HOR_BONUS_DEGREE);
        assertEquals(Bonus.HOR_BONUS_COUNT/2 + 1, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(0.0 - LESS_THAN_HALF_HOR_BONUS_DEGREE);
        assertEquals(Bonus.HOR_BONUS_COUNT/2 - 1, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(0.0 - Bonus.HALF_HOR_BONUS_DEGREE);
        assertEquals(Bonus.HOR_BONUS_COUNT/2 - 1, testGrid);

        testGrid = Bonus.ToHorizontalBonusGrid(0.0 - 2*Bonus.HALF_HOR_BONUS_DEGREE - LESS_THAN_HALF_HOR_BONUS_DEGREE);
        assertEquals(Bonus.HOR_BONUS_COUNT/2 - 2, testGrid);
    }

    @Test
    public void toBonusKeyTest() throws InvalidPositionException {
        assertEquals(0, Bonus.ToBonusKey(new Point(0, 0)));
        assertEquals("5000a", Long.toHexString(Bonus.ToBonusKey(new Point(10, 5))));
    }

    @Test
    public void fromBonusKeyTest() throws InvalidPositionException {
        Point<Integer> p;
        p = Bonus.FromBonusKey(0);
        assertTrue(p.getX()==0 && p.getY()==0);

        p = Bonus.FromBonusKey(0x5000a);
        assertTrue(p.getX()==10 && p.getY()==5);
    }

}