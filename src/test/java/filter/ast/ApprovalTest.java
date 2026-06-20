package filter.ast;

import filter.FilterLexer;
import filter.FilterParser;
import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

class ApprovalTest {

  private FilterParser.QueryContext parse(String query) {
    var input = CharStreams.fromString(query);
    var lexer = new FilterLexer(input);
    var tokens = new CommonTokenStream(lexer);
    var parser = new FilterParser(tokens);

    return parser.query();
  }

  private String printPatternAst(String query) {
    Expr ast = new AstBuilderPattern().translate(parse(query));
    return AstPrinter.toString(ast);
  }

  private String printVisitorAst(String query) {
    Expr ast = new AstBuilderVisitor().translate(parse(query));
    return AstPrinter.toString(ast);
  }

  private void approveBothBuilders(String query) {
    String patternAst = printPatternAst(query);
    String visitorAst = printVisitorAst(query);

    String output =
        """
        Query:
        %s

        Pattern:
        %s

        Visitor:
        %s
        """
            .formatted(query, patternAst, visitorAst);

    Approvals.verify(output);
  }

  @Test
  void shouldApproveSimpleComparison() {
    approveBothBuilders("artist == \"Beatles\"");
  }

  @Test
  void shouldApproveAndExpression() {
    approveBothBuilders("artist == \"Beatles\" and year == 1965");
  }

  @Test
  void shouldApproveOrExpression() {
    approveBothBuilders("genre == \"rock\" or genre == \"jazz\"");
  }

  @Test
  void shouldApproveInListExpression() {
    approveBothBuilders("genre in (\"rock\", \"jazz\", \"blues\")");
  }

  @Test
  void shouldApproveNotExpression() {
    approveBothBuilders("not artist == \"Beatles\"");
  }

  @Test
  void shouldApproveAndBeforeOrPrecedence() {
    approveBothBuilders("genre == \"rock\" or year <= 1990 and artist == \"Beatles\"");
  }

  @Test
  void shouldApproveParentheses() {
    approveBothBuilders("(genre == \"rock\" or genre == \"jazz\") and year >= 1980");
  }

  @Test
  void shouldApproveComplexQuery() {
    approveBothBuilders(
        "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"");
  }
}
