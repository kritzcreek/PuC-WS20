import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

sealed class Expr {
    data class Number(val number: Int) : Expr()
    data class Boolean(val bool: kotlin.Boolean) : Expr()
    data class Var(val name: String) : Expr()
    data class Lambda(val binder: String, val body: Expr) : Expr()
    data class Closure(val binder: String, val body: Expr, val env: Env) : Expr()
    data class Application(val func: Expr, val argument: Expr) : Expr()
    data class If(val condition: Expr, val thenBranch: Expr, val elseBranch: Expr) : Expr()
}

typealias Env = PersistentMap<String, Expr>

val emptyEnv: Env = persistentHashMapOf()
val initialEnv: Env = persistentHashMapOf(
    "add" to Expr.Closure(
        "x",
        Expr.Lambda("y", Expr.Var("#add")),
        emptyEnv
    ),
    "multiply" to Expr.Closure(
        "x",
        Expr.Lambda("y", Expr.Var("#multiply")),
        emptyEnv
    ),
    "equals" to Expr.Closure(
        "x",
        Expr.Lambda("y", Expr.Var("#equals")),
        emptyEnv
    )
)

fun eval(env: Env, expr: Expr): Expr {
    return when (expr) {
        is Expr.Number, is Expr.Boolean, is Expr.Closure -> expr
        is Expr.Var -> when (expr.name) {
            "#add" -> {
                val summand1 = env["x"]!!
                val summand2 = env["y"]!!
                if (summand1 is Expr.Number && summand2 is Expr.Number) {
                    Expr.Number(summand1.number + summand2.number)
                } else {
                    throw Exception("Can't add $summand1 to $summand2")
                }
            }
            "#multiply" -> {
                val x = env["x"]!!
                val y = env["y"]!!
                if (x is Expr.Number && y is Expr.Number) {
                    Expr.Number(x.number * y.number)
                } else {
                    throw Exception("Can't multiply $x with $y")
                }
            }
            "#equals" -> {
                val x = env["x"]!!
                val y = env["y"]!!
                if (x is Expr.Number && y is Expr.Number) {
                    Expr.Boolean(x.number == y.number)
                } else if (x is Expr.Boolean && y is Expr.Boolean) {
                    Expr.Boolean(x.bool == y.bool)
                } else {
                    throw Exception("Can't compare $x to $y for equality")
                }
            }
            else -> env[expr.name] ?: throw Exception("${expr.name} was undefined.")
        }
        is Expr.Lambda -> Expr.Closure(expr.binder, expr.body, env)
        is Expr.Application ->
            when (val closure = eval(env, expr.func)) {
                is Expr.Closure -> {
                    val newEnv = closure.env.put(closure.binder, eval(env, expr.argument))
                    eval(newEnv, closure.body)
                }
                else -> throw Exception("$closure is not a function")
            }
        is Expr.If -> {
            val cond = eval(env, expr.condition)
            if (cond !is Expr.Boolean) {
                throw Exception("JavaScript is forbidden")
            }
            if (cond.bool) {
                eval(env, expr.thenBranch)
            } else {
                eval(env, expr.elseBranch)
            }

        }

    }
}

fun kotlinFaculty(x: Int): Int =
    if (x == 0) 1 else x * kotlinFaculty(x - 1)

fun main() {

    fun binary(op: String, x: Expr, y: Expr): Expr =
        Expr.Application(Expr.Application(Expr.Var(op), x), y)

    val conditionNumber = binary("equals", Expr.Number(20), Expr.Number(20))
    val conditionBoolean = binary("equals", Expr.Boolean(true), Expr.Boolean(true))
    val thenBranch = binary("add", Expr.Number(21), Expr.Number(22))
    val elseBranch = Expr.Number(84)
    // println(eval(initialEnv, Expr.If(conditionNumber, thenBranch, elseBranch))) // 30


    val innerZ = Expr.Lambda(
        "x",
        Expr.Application(
            Expr.Var("f"),
            Expr.Lambda(
                "v",
                Expr.Application(
                    Expr.Application(
                        Expr.Var("x"), Expr.Var("x")
                    ), Expr.Var("v")

                )
            )
        )
    )
    val z = Expr.Lambda("f", Expr.Application(innerZ, innerZ))

    val faculty =
        Expr.Lambda(
            "faculty",
            Expr.Lambda(
                "x", Expr.If(
                    binary("equals", Expr.Var("x"), Expr.Number(0)),
                    Expr.Number(1),
                    binary(
                        "multiply",
                        Expr.Var("x"),
                        Expr.Application(
                            Expr.Var("faculty"),
                            binary(
                                "add",
                                Expr.Var("x"),
                                Expr.Number(-1)
                            )
                        )
                    )
                )
            )
        )

    println(kotlinFaculty(5))
    println(eval(initialEnv, Expr.Application(Expr.Application(z, faculty), Expr.Number(5))))

    // fib 0 = 1
    // fib 1 = 1
    // fib n = fib (n - 1) + fib (n - 2)
    // Uebung bis zum naechsten mal! (Ihr braucht wieder Z)
}
