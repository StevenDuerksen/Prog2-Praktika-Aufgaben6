package filter.ast.builder;

import filter.FilterLexer;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class AstBuilders {

  public static Expr fromQuery(String query, Function<FilterParser.QueryContext, Expr> translator) {
    return simplify(translator.apply(parse(query)));
  }

  public static Expr simplify(Expr e) {
    return switch (e) {
      case Expr.And(var left, var right) -> {
        Expr simplifiedLeft = simplify(left);
        Expr simplifiedRight = simplify(right);

        // x AND x = x
        if (simplifiedLeft.equals(simplifiedRight)) {
          yield simplifiedLeft;
        } else {
          yield new Expr.And(simplifiedLeft, simplifiedRight);
        }
      }
      case Expr.Or(var left, var right) -> {
        Expr simplifiedLeft = simplify(left);
        Expr simplifiedRight = simplify(right);

        // x OR x = x
        if (simplifiedLeft.equals(simplifiedRight)) {
          yield simplifiedLeft;
        } else {
          yield new Expr.Or(simplifiedLeft, simplifiedRight);
        }
      }
      case Expr.Not(var inner) -> {
        Expr simplifiedInner = simplify(inner);

        yield switch (simplifiedInner) {
          // doppelte Negation
          case Expr.Not(var doubleInner) -> doubleInner;
          // De Morgan And
          case Expr.And(var left, var right) ->
              simplify(new Expr.Or(new Expr.Not(left), new Expr.Not(right)));
          // De Morgan Right
          case Expr.Or(var left, var right) ->
              simplify(new Expr.And(new Expr.Not(left), new Expr.Not(right)));
          // negierung Operator
          case Expr.Comparison(var field, var op, var value) ->
              new Expr.Comparison(field, negateOp(op), value);
          default -> new Expr.Not(simplifiedInner);
        };
      }
      case Expr.Comparison(var field, var op, var value) -> e;
      case Expr.InList(var field, var values) -> {
        // Duplikate entfernen
        List<Value> distinctValues = values.stream().distinct().toList();
        // Falls nur noch ein ELement existiert
        if (distinctValues.size() == 1) {
          yield new Expr.Comparison(field, CompOp.EQ, distinctValues.getFirst());
        }

        yield new Expr.InList(field, distinctValues);
      }
    };
  }

  public static CompOp negateOp(CompOp op) {
    return switch (op) {
      case EQ -> CompOp.NE;
      case NE -> CompOp.EQ;
      case LT -> CompOp.GE;
      case LE -> CompOp.GT;
      case GT -> CompOp.LE;
      case GE -> CompOp.LT;
    };
  }

  public static FilterParser.QueryContext parse(String query) {
    var cs = CharStreams.fromString(query);
    var lexer = new FilterLexer(cs);
    var tokens = new CommonTokenStream(lexer);
    var parser = new FilterParser(tokens);

    var ctx = parser.query();
    if (parser.getNumberOfSyntaxErrors() > 0)
      throw new IllegalStateException("Syntax errors in query: " + query);

    return ctx;
  }
}
