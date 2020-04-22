import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

sealed class Expr {
    data class Plus(val summand1: Expr, val summand2: Expr) : Expr()
    data class Number(val number: Int) : Expr()
    data class Var(val name: String) : Expr()
    data class Lambda(val binder: String, val body: Expr) : Expr()
    data class Closure(val binder: String, val body: Expr, val env: Env) : Expr()
    data class Application(val func: Expr, val argument: Expr) : Expr()
}

typealias Env = PersistentMap<String, Expr>

val emptyEnv: Env = persistentHashMapOf()
fun eval(env: Env, expr: Expr): Expr {
    return when (expr) {
        is Expr.Plus -> {
            val summand1 = eval(env, expr.summand1)
            val summand2 = eval(env, expr.summand2)
            if (summand1 is Expr.Number && summand2 is Expr.Number) {
                Expr.Number(summand1.number + summand2.number)
            } else {
                throw Exception("Can't add $summand1 to $summand2")
            }
        }
        is Expr.Number -> expr
        is Expr.Closure -> expr
        is Expr.Var -> env[expr.name] ?: throw Exception("${expr.name} was undefined.")
        is Expr.Lambda -> Expr.Closure(expr.binder, expr.body, env)
        is Expr.Application ->
            when (val closure = eval(env, expr.func)) {
                is Expr.Closure -> {
                    val newEnv = closure.env.put(closure.binder, eval(env, expr.argument))
                    eval(newEnv, closure.body)
                }
                else -> throw Exception("$closure is not a function")
            }
    }
}

fun main() {
    val expr =
        Expr.Application(
            Expr.Application(
                Expr.Lambda(
                    "y",
                    Expr.Lambda(
                        "x",
                        Expr.Plus(Expr.Var("x"), Expr.Var("y"))
                    )
                ), Expr.Number(10)
            ), Expr.Number(20)
        )
    println(expr)
    println(eval(emptyEnv, expr)) // 30

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

    println(eval(emptyEnv, complicated)) // 42

}
