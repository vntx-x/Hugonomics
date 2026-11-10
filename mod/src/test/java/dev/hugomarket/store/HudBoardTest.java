package dev.hugomarket.store;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudBoardTest {
    @Test
    void ordersSectionsAndRemovesEmptyOnes() {
        HudBoard board = new HudBoard();
        HudBoard.Line watch = new HudBoard.Line("Diamant ab 95", 1);
        HudBoard.Line status = new HudBoard.Line("Heute +1k", 2);
        board.set(HudBoard.WATCHLIST, List.of(watch));
        board.set(HudBoard.STATUS, List.of(status));
        assertEquals(List.of(status, watch), board.lines());
        board.set(HudBoard.STATUS, List.of());
        assertEquals(List.of(watch), board.lines());
        board.set(HudBoard.WATCHLIST, List.of());
        assertTrue(board.lines().isEmpty());
    }

    @Test
    void unchangedSectionKeepsTheSameList() {
        HudBoard board = new HudBoard();
        board.set(HudBoard.STATUS, List.of(new HudBoard.Line("a", 1)));
        List<HudBoard.Line> before = board.lines();
        board.set(HudBoard.STATUS, List.of(new HudBoard.Line("a", 1)));
        assertSame(before, board.lines());
    }
}
