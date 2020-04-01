sealed class Expression {
    data class Number(val number: Int) : Expression()
    data class Addition(val summand1: Expression, val summand2: Expression) : Expression()
    data class Multiplication(val factor1: Expression, val factor2: Expression) : Expression()
    data class Negation(val expr: Expression) : Expression()
}

sealed class SmallerExpression {
    data class Number(val number: Int) : SmallerExpression()
    data class Addition(val summand1: SmallerExpression, val summand2: SmallerExpression) : SmallerExpression()
    data class Multiplication(val factor1: SmallerExpression, val factor2: SmallerExpression) : SmallerExpression()
}

fun smallerExpression(expr: Expression): SmallerExpression {
    return when (expr) {
        is Expression.Number ->
            SmallerExpression.Number(expr.number)
        is Expression.Addition ->
            SmallerExpression.Addition(smallerExpression(expr.summand1), smallerExpression(expr.summand2))
        is Expression.Multiplication ->
            SmallerExpression.Multiplication(smallerExpression(expr.factor1), smallerExpression(expr.factor2))
        is Expression.Negation ->
            SmallerExpression.Multiplication(SmallerExpression.Number(-1), smallerExpression(expr.expr))
    }
}

fun evalSmallerExpression(expr: SmallerExpression): Int {
    return when (expr) {
        is SmallerExpression.Number ->
            expr.number
        is SmallerExpression.Addition ->
            evalSmallerExpression(expr.summand1) + evalSmallerExpression(expr.summand2)
        is SmallerExpression.Multiplication ->
            evalSmallerExpression(expr.factor1) * evalSmallerExpression(expr.factor2)
    }
}


fun evalExpr(expr: Expression): Int {
    return when (expr) {
        is Expression.Addition ->
            evalExpr(expr.summand1) + evalExpr(expr.summand2)
        is Expression.Number ->
            expr.number
        is Expression.Multiplication ->
            evalExpr(expr.factor1) * evalExpr(expr.factor2)
        is Expression.Negation ->
            -1 * evalExpr(expr.expr)
    }
}


fun main() {
    // 3 * (4 + 5)
    println(
        evalExpr(
            Expression.Multiplication(
                Expression.Number(3),
                Expression.Addition(
                    Expression.Number(4),
                    Expression.Number(5)
                )
            )
        )
    )

    val ourExpr = Expression.Multiplication(
        Expression.Number(4),
        Expression.Negation(Expression.Number(5))
    )

    println(evalExpr(ourExpr))

    println(evalSmallerExpression(smallerExpression(ourExpr)))


}