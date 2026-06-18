package filter.ast.builder;

import filter.FilterBaseVisitor;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.*;

public class AstBuilderVisitor extends FilterBaseVisitor<Void> {

  private final Deque<Expr> exprStack = new ArrayDeque<>();
  private final Deque<Value> valueStack = new ArrayDeque<>();
  private final Deque<List<Value>> valueListStack = new ArrayDeque<>();

  // Public entry point
  public Expr translate(FilterParser.QueryContext ctx) {
    exprStack.clear();
    valueStack.clear();
    valueListStack.clear();

    visit(ctx);

    return exprStack.pop();
  }

  // query  : expr EOF
  @Override
  public Void visitQuery(FilterParser.QueryContext ctx) {
    visit(ctx.expr());
    return null;
  }

  // expr: orExpr
  @Override
  public Void visitExpr(FilterParser.ExprContext ctx) {
    visit(ctx.orExpr());
    return null;
  }

  // orExpr : andExpr (OR andExpr)*
  @Override
  public Void visitOrExpr(FilterParser.OrExprContext ctx) {
    visit(ctx.andExpr(0));
    Expr left = exprStack.pop();

    for (int i = 1; i < ctx.andExpr().size(); i++) {
      visit(ctx.andExpr(i));

      Expr right = exprStack.pop();
      left = new Expr.Or(left, right);
    }

    exprStack.push(left);

    return null;
  }

  // andExpr: notExpr (AND notExpr)*
  @Override
  public Void visitAndExpr(FilterParser.AndExprContext ctx) {
    visit(ctx.notExpr(0));

    Expr left = exprStack.pop();

    for (int i = 1; i < ctx.notExpr().size(); i++) {
      visit(ctx.notExpr(i));

      Expr right = exprStack.pop();

      left = new Expr.And(left, right);
    }

    exprStack.push(left);

    return null;
  }

  // notExpr: NOT notExpr | primary
  @Override
  public Void visitNotExpr(FilterParser.NotExprContext ctx) {
    if (ctx.notExpr() != null) {
      visit(ctx.notExpr());

      Expr inner = exprStack.pop();

      exprStack.push(new Expr.Not(inner));
    } else {
      visit(ctx.primary());
    }
    return null;
  }

  // primary: comparison | '(' expr ')'
  @Override
  public Void visitPrimary(FilterParser.PrimaryContext ctx) {
    if (ctx.comparison() != null) {
      visit(ctx.comparison());
    } else {
      visit(ctx.expr());
    }

    return null;
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  @Override
  public Void visitComparison(FilterParser.ComparisonContext ctx) {
    String text = ctx.IDENTIFIER().getText();

    if (ctx.literal() != null) {
      visit(ctx.literal());

      Value value = valueStack.pop();
      CompOp op = CompOp.fromSymbol(ctx.op.getText());

      exprStack.push(new Expr.Comparison(text, op, value));
    } else {
      visit(ctx.literalList());

      List<Value> values = valueListStack.pop();

      exprStack.push(new Expr.InList(text, values));
    }
    return null;
  }

  // literalList: literal (',' literal)*
  @Override
  public Void visitLiteralList(FilterParser.LiteralListContext ctx) {
    List<Value> values = new ArrayList<>();

    for (int i = 0; i < ctx.literal().size(); i++) {
      visit(ctx.literal(i));

      Value value = valueStack.pop();

      values.add(value);
    }

    valueListStack.push(values);

    return null;
  }

  // literal: STRING | NUMBER
  @Override
  public Void visitLiteral(FilterParser.LiteralContext ctx) {
    if (ctx.STRING() != null) {
      String text = ctx.STRING().getText();

      String value = text.substring(1, text.length() - 1);

      valueStack.push(new Value.Str(value));
    } else if (ctx.NUMBER() != null) {
      String text = ctx.NUMBER().getText();

      int value = Integer.parseInt(text);

      valueStack.push(new Value.Num(value));
    }
    return null;
  }
}
