---
title: Student Support Code for 'Filter-DSL' Task
---

<!-- pandoc -s -f markdown -t markdown --columns=94 --reference-links=true README.md -->

## About

This represents the student support code for the [Filter-DSL task].

## License

This [work] by [Carsten Gips] and [contributors] is licensed under [MIT].

  [Filter-DSL task]: https://github.com/Programmiermethoden-CampusMinden/Prog2-Lecture/tree/master/homework
  [work]: https://github.com/Programmiermethoden-CampusMinden/prog2_ybel_filterdsl
  [Carsten Gips]: https://github.com/cagix
  [contributors]: https://github.com/Programmiermethoden-CampusMinden/prog2_ybel_filterdsl/graphs/contributors
  [MIT]: LICENSE.md

### Aufgabe 4: Vergleich Visitor-Pattern vs. Pattern Matching

Beim Visitor-Pattern werden die passenden Methoden durch den von ANTLR erzeugten Visitor aufgerufen. Dadurch orientiert sich die Implementierung stark an der Grammatik. Ein Vorteil ist, dass für jede Grammatikregel eine eigene Methode existiert und die Struktur dadurch klar zugeordnet werden kann. Nachteilig ist, dass die Umsetzung schnell indirekt wird. Hier mussten zusätzlich Stacks verwendet werden, um Zwischenergebnisse wie Ausdrücke oder Literale zu speichern. Dadurch entstehen leichter Fehler, wenn Werte falsch gepusht oder gepoppt werden.

Bei der Pattern-Matching-Variante wird der Parse-Tree direkter und rekursiv verarbeitet. Die Methoden geben direkt passende Objekte wie Expr oder Value zurück. Dadurch ist der Ablauf verständlicher, weil man besser sieht, welcher Teil des Parse-Trees zu welchem AST-Knoten wird. Außerdem werden keine zusätzlichen Stacks benötigt. Ein Nachteil ist, dass man den Durchlauf selbst korrekt steuern muss und bei Änderungen an der Grammatik die passenden Fälle manuell anpassen muss.
