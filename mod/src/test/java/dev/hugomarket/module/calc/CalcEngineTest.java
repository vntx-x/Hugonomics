package dev.hugomarket.module.calc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalcEngineTest {
    private static double eval(String expression) {
        CalcEngine.Result result = CalcEngine.evaluate(expression);
        assertTrue(result.ok(), () -> expression + " -> " + result.error());
        return result.value();
    }

    @Test
    void precedenceAndParentheses() {
        assertEquals(14, eval("2+3×4"), 1e-9);
        assertEquals(20, eval("(2+3)*4"), 1e-9);
        assertEquals(2.5, eval("10/4"), 1e-9);
        assertEquals(-6, eval("-2*3"), 1e-9);
        assertEquals(7, eval("10 - 3"), 1e-9);
        assertEquals(7, eval("10 − 3"), 1e-9);
    }

    @Test
    void magnitudeSuffixes() {
        assertEquals(576_000, eval("12*48k"), 1e-9);
        assertEquals(1_500_000, eval("1,5M"), 1e-9);
        assertEquals(2_000_000_000, eval("2b"), 1e-9);
        assertEquals(3_000, eval("3K"), 1e-9);
    }

    @Test
    void germanNumberFormat() {
        assertEquals(1_250_000, eval("1.250.000"), 1e-9);
        assertEquals(42.24, eval("42,24"), 1e-9);
        assertEquals(1_250_000.5, eval("1.250.000,5"), 1e-9);
    }

    @Test
    void percentIsRelativeAfterPlusMinusAndAbsoluteOtherwise() {
        assertEquals(1_030_000, eval("1M + 3%"), 1e-9);
        assertEquals(970_000, eval("1M - 3%"), 1e-9);
        assertEquals(30_000, eval("1M * 3%"), 1e-9);
        assertEquals(0.5, eval("50%"), 1e-9);
        assertEquals(95_000, eval("100k - 5%"), 1e-9);
    }

    @Test
    void errorsAreReportedNotThrown() {
        assertFalse(CalcEngine.evaluate("").ok());
        assertFalse(CalcEngine.evaluate("2+").ok());
        assertFalse(CalcEngine.evaluate("(2+3").ok());
        assertFalse(CalcEngine.evaluate("5/0").ok());
        assertFalse(CalcEngine.evaluate("abc").ok());
        assertEquals("div0", CalcEngine.evaluate("5/0").error());
    }

    @Test
    void formatsResults() {
        assertEquals("576.000", CalcFormat.full(576_000));
        assertEquals("42,24", CalcFormat.full(42.24));
        assertEquals("1.250.000,5", CalcFormat.full(1_250_000.5));
        assertEquals("-3", CalcFormat.full(-3));
        assertEquals("576K", CalcFormat.compact(576_000));
        assertEquals("1.3B", CalcFormat.compact(1_250_000_000));
        assertEquals("576000", CalcFormat.asInput(576_000));
        assertEquals("42,24", CalcFormat.asInput(42.24));
    }

    @Test
    void stateEvaluatesAndKeepsHistory() {
        CalcState state = new CalcState();
        for (char c : "12*48k".toCharArray()) {
            state.input(c);
        }
        assertEquals("12×48K", state.expression());
        assertEquals(576_000, state.result().value(), 1e-9);
        state.evaluate();
        assertEquals(1, state.history().size());
        assertEquals("576000", state.expression());
        state.input('+');
        state.input('1');
        assertEquals(576_001, state.result().value(), 1e-9);
        state.evaluate();
        state.input('7');
        assertEquals("7", state.expression());
    }
}
