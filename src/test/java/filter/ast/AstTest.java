package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.FilterLexer;
import filter.FilterParser;
import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

class AstTest {

  private FilterParser.QueryContext parse(String query) {
    var input = CharStreams.fromString(query);
    var lexer = new FilterLexer(input);
    var tokens = new CommonTokenStream(lexer);
    var parser = new FilterParser(tokens);

    return parser.query();
  }

  private Expr parseWithPattern(String query) {
    return new AstBuilderPattern().translate(parse(query));
  }

  private Expr parseWithVisitor(String query) {
    return new AstBuilderVisitor().translate(parse(query));
  }

  private void assertBothBuilders(String query, Expr expected) {
    assertEquals(expected, parseWithPattern(query));
    assertEquals(expected, parseWithVisitor(query));
  }

  @Test
  void shouldParseStringComparison() {
    Expr expected = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));

    assertBothBuilders("artist == \"Beatles\"", expected);
  }

  @Test
  void shouldParseNumberComparison() {
    Expr expected = new Expr.Comparison("year", CompOp.LE, new Value.Num(1990));

    assertBothBuilders("year <= 1990", expected);
  }

  @Test
  void shouldParseInList() {
    Expr expected = new Expr.InList("genre", List.of(new Value.Str("rock"), new Value.Str("jazz")));

    assertBothBuilders("genre in (\"rock\", \"jazz\")", expected);
  }

  @Test
  void shouldParseNotExpressionWithoutSimplifying() {
    Expr expected =
        new Expr.Not(new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")));

    assertBothBuilders("not artist == \"Beatles\"", expected);
  }

  @Test
  void shouldParseAndExpression() {
    Expr expected =
        new Expr.And(
            new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock")),
            new Expr.Comparison("year", CompOp.GE, new Value.Num(1970)));

    assertBothBuilders("genre == \"rock\" and year >= 1970", expected);
  }

  @Test
  void shouldParseOrExpression() {
    Expr expected =
        new Expr.Or(
            new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock")),
            new Expr.Comparison("genre", CompOp.EQ, new Value.Str("jazz")));

    assertBothBuilders("genre == \"rock\" or genre == \"jazz\"", expected);
  }

  @Test
  void shouldRespectAndBeforeOr() {
    Expr expected =
        new Expr.Or(
            new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock")),
            new Expr.And(
                new Expr.Comparison("year", CompOp.LE, new Value.Num(1990)),
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"))));

    assertBothBuilders("genre == \"rock\" or year <= 1990 and artist == \"Beatles\"", expected);
  }

  @Test
  void shouldRespectParentheses() {
    Expr expected =
        new Expr.And(
            new Expr.Or(
                new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock")),
                new Expr.Comparison("genre", CompOp.EQ, new Value.Str("jazz"))),
            new Expr.Comparison("year", CompOp.GE, new Value.Num(1980)));

    assertBothBuilders("(genre == \"rock\" or genre == \"jazz\") and year >= 1980", expected);
  }

  @Test
  void shouldParseMultipleAndLeftAssociative() {
    Expr expected =
        new Expr.And(
            new Expr.And(
                new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock")),
                new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Queen"))),
            new Expr.Comparison("year", CompOp.GE, new Value.Num(1970)));

    assertBothBuilders("genre == \"rock\" and artist == \"Queen\" and year >= 1970", expected);
  }

  @Test
  void shouldParseMultipleOrLeftAssociative() {
    Expr expected =
        new Expr.Or(
            new Expr.Or(
                new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock")),
                new Expr.Comparison("genre", CompOp.EQ, new Value.Str("jazz"))),
            new Expr.Comparison("genre", CompOp.EQ, new Value.Str("blues")));

    assertBothBuilders("genre == \"rock\" or genre == \"jazz\" or genre == \"blues\"", expected);
  }
}
