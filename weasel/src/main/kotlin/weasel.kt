sealed class Expr {
    data class Plus(val summand1: Expr, val summand2: Expr): Expr()
    data class Number(val number: Int) : Expr()
    data class Var(val name: String) : Expr()
    data class Lambda(val binder: String, val body: Expr) : Expr()
    data class Application(val func: Expr, val argument: Expr) : Expr()
}

fun eval(expr: Expr): Expr {
    return when (expr) {
        is Expr.Plus -> {
            val summand1 = eval(expr.summand1)
            val summand2 = eval(expr.summand2)
            if (summand1 is Expr.Number && summand2 is Expr.Number) {
                Expr.Number(summand1.number + summand2.number)
            } else {
                throw Exception("Can't add $summand1 to $summand2")
            }
        }
        is Expr.Number -> expr
        is Expr.Var -> expr
        is Expr.Lambda -> expr
        is Expr.Application -> {
            when (val lambda = eval(expr.func)) {
                is Expr.Lambda -> eval(substitute(lambda.binder, eval(expr.argument), lambda.body))
                else -> throw Exception("${lambda} is not a function")
            }
        }
    }
}

// Replaces all occurences of `binder` with `replacement`
// [binder -> replacement] expr
fun substitute(binder: String, replacement: Expr, expr: Expr): Expr {
    return when (expr) {
        is Expr.Plus -> Expr.Plus(
            substitute(binder, replacement, expr.summand1),
            substitute(binder, replacement, expr.summand2)
        )
        is Expr.Number -> expr
        is Expr.Var -> if (expr.name == binder) {
            replacement
        } else {
            expr
        }
        is Expr.Lambda -> if (expr.binder == binder) {
            expr
        } else {
            Expr.Lambda(expr.binder, substitute(binder, replacement, expr.body))
        }
        is Expr.Application -> Expr.Application(
            substitute(binder, replacement, expr.func),
            substitute(binder, replacement, expr.argument)
        )
    }
}

fun main() {
    val expr = Expr.Application(Expr.Lambda("y", Expr.Var("y")), Expr.Number(10))
    println(expr)
    println(eval(expr))

    val complicated = Expr.Application(
        Expr.Application(
            Expr.Lambda(
                "f",
                Expr.Lambda(
                    "x",
                    Expr.Application(
                        Expr.Var("f"),
                        Expr.Application(
                            Expr.Var("f"),
                            Expr.Var("x")
                        )
                    )
                )
            ),
            Expr.Lambda("x", Expr.Plus(Expr.Var("x"), Expr.Number(1)))
        ),
        Expr.Number(40)
    )

    println(eval(complicated))

}