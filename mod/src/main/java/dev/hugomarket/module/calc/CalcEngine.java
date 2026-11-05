package dev.hugomarket.module.calc;

import java.util.ArrayList;
import java.util.List;

public final class CalcEngine {
    public record Result(boolean ok, double value, String error) {
        static Result of(double value) {
            return new Result(true, value, null);
        }

        static Result error(String error) {
            return new Result(false, 0, error);
        }
    }

    private enum Kind {
        NUMBER, PLUS, MINUS, MUL, DIV, PERCENT, OPEN, CLOSE, END
    }

    private record Token(Kind kind, double value) {
    }

    private static final class Operand {
        final double value;
        final boolean percent;

        Operand(double value, boolean percent) {
            this.value = value;
            this.percent = percent;
        }
    }

    private final List<Token> tokens = new ArrayList<>();
    private int pos;

    private CalcEngine() {
    }

    public static Result evaluate(String expression) {
        CalcEngine engine = new CalcEngine();
        try {
            engine.tokenize(expression);
            if (engine.tokens.size() == 1) {
                return Result.error("empty");
            }
            Operand result = engine.expr();
            if (engine.peek().kind() != Kind.END) {
                return Result.error("syntax");
            }
            double value = result.percent ? result.value / 100.0 : result.value;
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return Result.error("math");
            }
            return Result.of(value);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    private void tokenize(String input) {
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == ' ') {
                i++;
                continue;
            }
            if (Character.isDigit(c) || c == ',' || (c == '.' && i + 1 < input.length() && Character.isDigit(input.charAt(i + 1)))) {
                int start = i;
                while (i < input.length() && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.' || input.charAt(i) == ',')) {
                    i++;
                }
                double value = parseNumber(input.substring(start, i));
                if (i < input.length()) {
                    char suffix = Character.toLowerCase(input.charAt(i));
                    double factor = switch (suffix) {
                        case 'k' -> 1_000;
                        case 'm' -> 1_000_000;
                        case 'b' -> 1_000_000_000;
                        default -> 0;
                    };
                    if (factor > 0) {
                        value *= factor;
                        i++;
                    }
                }
                tokens.add(new Token(Kind.NUMBER, value));
                continue;
            }
            Kind kind = switch (c) {
                case '+' -> Kind.PLUS;
                case '-', '−' -> Kind.MINUS;
                case '*', 'x', 'X', '×' -> Kind.MUL;
                case '/', ':', '÷' -> Kind.DIV;
                case '%' -> Kind.PERCENT;
                case '(' -> Kind.OPEN;
                case ')' -> Kind.CLOSE;
                default -> throw new IllegalStateException("syntax");
            };
            tokens.add(new Token(kind, 0));
            i++;
        }
        tokens.add(new Token(Kind.END, 0));
    }

    private static double parseNumber(String text) {
        String normalized = text.replace(".", "").replace(',', '.');
        if (normalized.isEmpty() || normalized.equals(".")) {
            throw new IllegalStateException("syntax");
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("syntax");
        }
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token next() {
        return tokens.get(pos++);
    }

    private Operand expr() {
        Operand left = term();
        while (peek().kind() == Kind.PLUS || peek().kind() == Kind.MINUS) {
            boolean plus = next().kind() == Kind.PLUS;
            Operand right = term();
            double rightValue = right.percent ? left.value * right.value / 100.0 : right.value;
            double leftValue = left.percent ? left.value / 100.0 : left.value;
            left = new Operand(plus ? leftValue + rightValue : leftValue - rightValue, false);
        }
        return left;
    }

    private Operand term() {
        Operand left = factor();
        while (peek().kind() == Kind.MUL || peek().kind() == Kind.DIV) {
            boolean mul = next().kind() == Kind.MUL;
            Operand right = factor();
            double l = left.percent ? left.value / 100.0 : left.value;
            double r = right.percent ? right.value / 100.0 : right.value;
            if (!mul && r == 0) {
                throw new IllegalStateException("div0");
            }
            left = new Operand(mul ? l * r : l / r, false);
        }
        return left;
    }

    private Operand factor() {
        if (peek().kind() == Kind.MINUS) {
            next();
            Operand inner = factor();
            return new Operand(-inner.value, inner.percent);
        }
        Operand primary = primary();
        if (peek().kind() == Kind.PERCENT) {
            next();
            return new Operand(primary.percent ? primary.value / 100.0 : primary.value, true);
        }
        return primary;
    }

    private Operand primary() {
        Token token = next();
        if (token.kind() == Kind.NUMBER) {
            return new Operand(token.value(), false);
        }
        if (token.kind() == Kind.OPEN) {
            Operand inner = expr();
            if (next().kind() != Kind.CLOSE) {
                throw new IllegalStateException("paren");
            }
            return new Operand(inner.percent ? inner.value / 100.0 : inner.value, false);
        }
        throw new IllegalStateException("syntax");
    }
}
