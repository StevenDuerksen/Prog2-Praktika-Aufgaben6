package filter.ast;

import static filter.ast.builder.AstBuilders.simplify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.FilterLexer;
import filter.FilterParser;
import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import net.jqwik.api.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class RoundtripPropertiesTest {

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

  private Expr buildExpr(String query) {
    return new AstBuilderPattern().translate(parse(query));
  }

  @Property
  void patternAndVisitorShouldCreateSameAst(@ForAll("simpleQueries") String query) {
    assertEquals(printPatternAst(query), printVisitorAst(query));
  }

  @Property
  void patternRoundtripShouldBeStable(@ForAll("simpleQueries") String query) {
    String firstPrint = printPatternAst(query);
    String secondPrint = printPatternAst(firstPrint);

    assertEquals(firstPrint, secondPrint);
  }

  @Property
  void visitorRoundtripShouldBeStable(@ForAll("simpleQueries") String query) {
    String firstPrint = printVisitorAst(query);
    String secondPrint = printVisitorAst(firstPrint);

    assertEquals(firstPrint, secondPrint);
  }

  @Property
  void patternAndVisitorRoundtripShouldCreateSameAst(@ForAll("simpleQueries") String query) {
    String firstPrintVisitor = printVisitorAst(query);
    String secondPrintVisitor = printVisitorAst(firstPrintVisitor);

    String firstPrintPattern = printPatternAst(query);
    String secondPrintPattern = printPatternAst(firstPrintPattern);

    assertEquals(firstPrintVisitor, secondPrintVisitor);
    assertEquals(firstPrintPattern, secondPrintPattern);
    assertEquals(secondPrintVisitor, secondPrintPattern);
  }

  @Property
  void simplifyIsIdempotent(@ForAll("simpleQueries") String query) {
    Expr ast = buildExpr(query);
    Expr simplifyOnce = simplify(ast);
    Expr simplifyTwice = simplify(simplifyOnce);

    assertEquals(simplifyOnce, simplifyTwice);
  }

  @Property
  void andShouldBeIdempotent(@ForAll("comparisons") String comparison) {
    String query = "(" + comparison + ") and (" + comparison + ")";

    Expr expected = buildExpr(comparison);
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  @Property
  void orShouldBeIdempotent(@ForAll("comparisons") String comparison) {
    String query = "(" + comparison + ") or (" + comparison + ")";

    Expr expected = buildExpr(comparison);
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  @Property
  void doubleNegationsShouldSimplify(@ForAll("comparisons") String comparison) {
    String query = "not not " + "(" + comparison + ")";

    Expr expected = buildExpr(comparison);
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  @Property
  void duplicateOnlyInListShouldBecomeEqual(@ForAll("stringLiterals") String literal) {
    String query = "genre in " + "(" + literal + ", " + literal + ")";

    Expr expected = buildExpr("genre == " + literal);
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  @Property
  void listDuplicatesShouldBeRemoved(
      @ForAll("stringLiterals") String a, @ForAll("stringLiterals") String b) {
    Assume.that(!a.equals(b));
    String query = "genre in " + "(" + a + ", " + a + ", " + b + ")";

    Expr expected = buildExpr("genre in " + "(" + a + ", " + b + ")");
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  @Property
  void deMorganAndShouldSimplify(@ForAll("comparisons") String a, @ForAll("comparisons") String b) {
    Assume.that(!a.equals(b));
    String query = "not ((" + a + ") and (" + b + "))";

    Expr expected = simplify(buildExpr("(not (" + a + ")) or (not (" + b + "))"));
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  @Property
  void deMorganOrShouldSimplify(@ForAll("comparisons") String a, @ForAll("comparisons") String b) {
    Assume.that(!a.equals(b));
    String query = "not ((" + a + ") or (" + b + "))";

    Expr expected = simplify(buildExpr("(not (" + a + ")) and (not (" + b + "))"));
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  @Property
  void comparisonOperatorsShouldNegate(
      @ForAll("negatedOperators") Tuple.Tuple2<String, String> operators,
      @ForAll("numberLiterals") String number) {

    String op = operators.get1();
    String negatedOp = operators.get2();

    String query = "not (year " + op + " " + number + ")";

    Expr expected = buildExpr("year " + negatedOp + " " + number);
    Expr ast = buildExpr(query);
    Expr simplified = simplify(ast);

    assertEquals(expected, simplified);
  }

  // ---------- @Provide-Methods for Arbitraries ----------

  @Provide
  Arbitrary<Tuple.Tuple2<String, String>> negatedOperators() {
    return Arbitraries.of(
        Tuple.of("==", "!="),
        Tuple.of("!=", "=="),
        Tuple.of("<", ">="),
        Tuple.of("<=", ">"),
        Tuple.of(">", "<="),
        Tuple.of(">=", "<"));
  }

  @Provide
  Arbitrary<String> fields() {
    return Arbitraries.of("title", "artist", "genre", "year");
  }

  @Provide
  Arbitrary<String> stringLiterals() {
    return Arbitraries.strings()
        .withChars("abcxyz")
        .ofMinLength(1)
        .ofMaxLength(5)
        .map(s -> "\"" + s + "\"");
  }

  @Provide
  Arbitrary<String> numberLiterals() {
    return Arbitraries.integers().between(1900, 2025).map(Object::toString);
  }

  @Provide
  Arbitrary<String> comparisons() {
    Arbitrary<String> ops = Arbitraries.of("==", "!=", "<", "<=", ">", ">=");

    Arbitrary<String> stringComp =
        Combinators.combine(fields(), ops, stringLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    Arbitrary<String> numberComp =
        Combinators.combine(Arbitraries.of("year"), ops, numberLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    return Arbitraries.oneOf(stringComp, numberComp);
  }

  @Provide
  Arbitrary<String> simpleQueries() {
    return comparisons()
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(
            list -> {
              if (list.size() == 1) return list.getFirst();
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                  String conn = Arbitraries.of(" and ", " or ").sample();
                  sb.append(conn);
                }
                sb.append(list.get(i));
              }
              return sb.toString();
            });
  }
}
