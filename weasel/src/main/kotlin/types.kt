import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.Exception

sealed class Type {
    override fun toString(): String = javaClass.simpleName

    object Number: Type()
    object Boolean: Type()
    data class Function(val argument: Type, val result: Type): Type()
    data class Unknown(val unknown: Int): Type()

    fun pretty(): String = prettyInner(false)
    private fun prettyInner(parens: kotlin.Boolean): String {
        return when(this) {
            Number -> "Number"
            Boolean -> "Boolean"
            is Unknown -> "u$unknown"
            is Function -> {
                val inner = "${argument.prettyInner(true)} -> ${result.pretty()}"
                if (parens) "($inner)" else inner
            }
        }
    }

    fun unknowns(): PersistentSet<Int> {
        return when(this) {
            Number, Boolean -> persistentSetOf()
            is Function -> argument.unknowns().addAll(result.unknowns())
            is Unknown -> persistentSetOf(unknown)
        }
    }
}



typealias Solution = HashMap<Int, Type>
var solution: Solution = hashMapOf()

fun printSolution() {
    solution.forEach { (u, t) ->
        println("u$u |-> ${t.pretty()}")
    }
}

fun applySolution(type: Type): Type = when(type){
    Type.Number -> Type.Number
    Type.Boolean -> Type.Boolean
    is Type.Unknown ->
        solution[type.unknown]?.let(::applySolution) ?: type
    is Type.Function -> Type.Function(
        applySolution(type.argument),
        applySolution(type .result)
    )
}

fun unify(t1: Type, t2: Type) {
    val t1 = applySolution(t1)
    val t2 = applySolution(t2)

    when {
        t1 == t2 -> return
        t1 is Type.Unknown -> {
            solveUnknown(t1.unknown, t2)
        }
        t2 is Type.Unknown -> {
            solveUnknown(t2.unknown, t1)
        }
        t1 is Type.Function && t2 is Type.Function -> {
            unify(t1.argument, t2.argument)
            unify(t1.result, t2.result)
        }
        else -> throw Exception("Can't unify ${t1.pretty()} with ${t2.pretty()}")
    }
}

private fun solveUnknown(unknown: Int, ty: Type) {
    if (ty.unknowns().contains(unknown))
        throw Exception("\nOccurs check failed for: u${unknown} ~ ${ty.pretty()}")
    solution[unknown] = ty
}

typealias Context = PersistentMap<String, Type>
val initialContext: Context = persistentHashMapOf(
    "add" to Type.Function(Type.Number, Type.Function(Type.Number, Type.Number)),
    "subtract" to Type.Function(Type.Number, Type.Function(Type.Number, Type.Number)),
    "multiply" to Type.Function(Type.Number, Type.Function(Type.Number, Type.Number)),
    "equals" to Type.Function(Type.Number, Type.Function(Type.Number, Type.Boolean))
)

var supply: Int = 0
fun freshUnknown(): Type = Type.Unknown(++supply)

// Type inference
fun infer(ctx: Context, expr: Expr): Type {
    return when(expr){
        is Expr.Number -> Type.Number
        is Expr.Boolean -> Type.Boolean
        is Expr.Var -> ctx[expr.name] ?: throw Exception("Unknown variable ${expr.name}")
        is Expr.Lambda -> {
            val tyArg = freshUnknown()
            val newCtx = ctx.put(expr.binder, tyArg)
            val tyBody = infer(newCtx, expr.body)
            Type.Function(tyArg, tyBody)
        }
        is Expr.If -> {
            val tyCond = infer(ctx, expr.condition)
            val tyThen = infer(ctx, expr.thenBranch)
            val tyElse = infer(ctx, expr.elseBranch)

            unify(tyCond, Type.Boolean)
            unify(tyThen, tyElse)

            tyThen // tyElse
        }
        is Expr.Application -> {
            val tyFun = infer(ctx, expr.func)
            val tyArg = infer(ctx, expr.argument)
            val tyRes = freshUnknown()
            unify(tyFun, Type.Function(tyArg, tyRes))
            tyRes
        }
        is Expr.Let -> {
            val newCtx = if (expr.isRecursive) {
                val tyBinder = freshUnknown()
                val innerCtx = ctx.put(expr.binder, tyBinder)
                val inferredBinder = infer(innerCtx, expr.expr)
                unify(tyBinder, inferredBinder)
                innerCtx
            } else {
                val tyBinder = infer(ctx, expr.expr)
                ctx.put(expr.binder, tyBinder)
            }
            infer(newCtx, expr.body)
        }
        is Expr.Closure -> TODO()
    }
}

fun main() {

    fun testInfer(expr: String) {
        supply = 0
        solution = hashMapOf()
        val e = Parser(Lexer(expr)).parseExpression()
        val ty = infer(initialContext, e)
        println(expr + " : ${applySolution(ty).pretty()}")
    }


    testInfer("""let x = 42 in true""")
    val h20 = """
        let rec fak = \x -> 
            if x == 0 
                then 1 
                else x * fak (x - 1) in
        fak 5
    """.trimIndent()
    testInfer(h20)

    val next = """
        let identity = \x -> x in
        let x = identity 10 in
        let y = identity true in
        if y then x else 20
    """.trimIndent()
    testInfer(next)

    // printSolution()
}