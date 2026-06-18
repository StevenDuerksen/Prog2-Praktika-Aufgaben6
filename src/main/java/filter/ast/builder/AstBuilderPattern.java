package filter.ast.builder;

import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayList;
import java.util.List;

public class AstBuilderPattern {

  // Public entry point
  // query  : expr EOF
  public Expr translate(FilterParser.QueryContext ctx) {
    return buildExpr(ctx.expr());
  }

  // expr: orExpr
  private Expr buildExpr(FilterParser.ExprContext ctx) {
    return buildOrExpr(ctx.orExpr());
  }

  // orExpr : andExpr (OR andExpr)*
  private Expr buildOrExpr(FilterParser.OrExprContext ctx) {
    Expr left = buildAndExpr(ctx.andExpr(0));

    for (int i = 1; i < ctx.andExpr().size(); i++) {
      Expr right = buildAndExpr(ctx.andExpr(i));
      left = new Expr.Or(left, right);
    }
    return left;
  }

  // andExpr: notExpr (AND notExpr)*
  private Expr buildAndExpr(FilterParser.AndExprContext ctx) {
    Expr left = buildNotExpr(ctx.notExpr(0));

    for (int i = 1; i < ctx.notExpr().size(); i++) {
      Expr right = buildNotExpr(ctx.notExpr(i));
      left = new Expr.And(left, right);
    }
    return left;
  }

  // notExpr: NOT notExpr | primary
  private Expr buildNotExpr(FilterParser.NotExprContext ctx) {
    return switch (ctx.getStart().getType()) {
      case FilterParser.NOT -> new Expr.Not(buildNotExpr(ctx.notExpr()));
      default -> buildPrimary(ctx.primary());
    };
  }

  // primary: comparison | '(' expr ')'
  private Expr buildPrimary(FilterParser.PrimaryContext ctx) {
    return switch (ctx.getChild(0)) {
      case FilterParser.ComparisonContext comparison -> buildComparison(comparison);
      default -> buildExpr(ctx.expr());
    };
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  private Expr buildComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.IDENTIFIER().getText();

    if (ctx.IN() != null) {
      List<Value> values = buildLiteralList(ctx.literalList());
      return new Expr.InList(field, values);
    }

    CompOp op = CompOp.fromSymbol(ctx.op.getText());
    Value value = buildLiteral(ctx.literal());

    return new Expr.Comparison(field, op, value);
  }

  // literalList: literal (',' literal)*
  private List<Value> buildLiteralList(FilterParser.LiteralListContext ctx) {
    List<Value> values = new ArrayList<>();

    for (int i = 0; i < ctx.literal().size(); i++) {
      Value lit = buildLiteral(ctx.literal(i));
      values.add(lit);
    }

    return values;
  }

  // literal: STRING | NUMBER
  private Value buildLiteral(FilterParser.LiteralContext ctx) {
    return switch (ctx.getStart().getType()) {
      case FilterParser.STRING ->
          new Value.Str(ctx.STRING().getText().substring(1, ctx.STRING().getText().length() - 1));
      case FilterParser.NUMBER -> new Value.Num(Integer.parseInt(ctx.NUMBER().getText()));
      default -> throw new IllegalArgumentException("Kein Literal" + ctx.getText());
    };
  }
}
