import kotlinx.collections.immutable.*
import kotlin.Exception

sealed class Monotype {
    override fun toString(): String = javaClass.simpleName

    object Number : Monotype()
    object Boolean : Monotype()
    data class Function(val argument: Monotype, val result: Monotype) : Monotype()
    data class Var(val name: String) : Monotype()
    data class Unknown(val unknown: Int) : Monotype()
    data class LinkedList(val ty: Monotype) : Monotype()

    fun pretty(): String = prettyInner(false)
    private fun prettyInner(parens: kotlin.Boolean): String {
        return when (this) {
            Number -> "Number"
            Boolean -> "Boolean"
            is Var -> name
            is Unknown -> "u$unknown"
            is LinkedList -> "[${ty.pretty()}]"
            is Function -> {
                val inner = "${argument.prettyInner(true)} -> ${result.pretty()}"
                if (parens) "($inner)" else inner
            }
        }
    }

    fun unknowns(): PersistentSet<Int> {
        return when (this) {
            Number, Boolean, is Var -> persistentSetOf()
            is Function -> argument.unknowns().addAll(result.unknowns())
            is Unknown -> persistentSetOf(unknown)
            is LinkedList -> ty.unknowns()
        }
    }
}

fun substitute(type: Monotype, v: String, replacement: Monotype): Monotype = when (type) {
    Monotype.Number, Monotype.Boolean, is Monotype.Unknown -> type
    is Monotype.Var -> if (type.name == v) replacement else type
    is Monotype.Function -> Monotype.Function(
        substitute(type.argument, v, replacement),
        substitute(type.result, v, replacement)
    )
    is Monotype.LinkedList -> Monotype.LinkedList(substitute(type.ty, v, replacement))
}

data class Polytype(val vars: List<String>, val type: Monotype) {
    fun pretty(): String =
        if (vars.isEmpty()) type.pretty() else "∀ ${vars.joinToString(" ")}. ${type.pretty()}"

    fun unknowns(): PersistentSet<Int> = type.unknowns()

    companion object {
        fun fromMono(ty: Monotype) = Polytype(emptyList(), ty)
    }
}

typealias Solution = HashMap<Int, Monotype>

var solution: Solution = hashMapOf()

fun printSolution() {
    solution.forEach { (u, t) ->
        println("u$u |-> ${t.pretty()}")
    }
}

fun applySolution(solution: Solution, type: Monotype): Monotype = when (type) {
    Monotype.Number -> Monotype.Number
    Monotype.Boolean -> Monotype.Boolean
    is Monotype.Var -> type
    is Monotype.Unknown ->
        solution[type.unknown]?.let { applySolution(solution, it) } ?: type
    is Monotype.Function -> Monotype.Function(
        applySolution(solution, type.argument),
        applySolution(solution, type.result)
    )
    is Monotype.LinkedList -> Monotype.LinkedList(applySolution(solution, type.ty))
}

fun unify(t1: Monotype, t2: Monotype) {
    val t1 = applySolution(solution, t1)
    val t2 = applySolution(solution, t2)

    when {
        t1 == t2 -> return
        t1 is Monotype.Unknown -> {
            solveUnknown(t1.unknown, t2)
        }
        t2 is Monotype.Unknown -> {
            solveUnknown(t2.unknown, t1)
        }
        t1 is Monotype.Function && t2 is Monotype.Function -> {
            unify(t1.argument, t2.argument)
            unify(t1.result, t2.result)
        }
        t1 is Monotype.LinkedList && t2 is Monotype.LinkedList -> unify(t1.ty, t2.ty)
        else -> throw Exception("Can't unify ${t1.pretty()} with ${t2.pretty()}")
    }
}

private fun solveUnknown(unknown: Int, ty: Monotype) {
    if (ty.unknowns().contains(unknown))
        throw Exception("\nOccurs check failed for: u${unknown} ~ ${ty.pretty()}")
    solution[unknown] = ty
}

typealias Context = PersistentMap<String, Polytype>

fun unknownsInCtx(ctx: Context): PersistentSet<Int> =
    ctx.values.fold(persistentSetOf()) { acc, ty ->
        acc.addAll(ty.unknowns())
    }

val initialContext: Context = persistentHashMapOf(
    "add" to Polytype.fromMono(Monotype.Function(Monotype.Number, Monotype.Function(Monotype.Number, Monotype.Number))),
    "subtract" to Polytype.fromMono(
        Monotype.Function(
            Monotype.Number,
            Monotype.Function(Monotype.Number, Monotype.Number)
        )
    ),
    "multiply" to Polytype.fromMono(
        Monotype.Function(
            Monotype.Number,
            Monotype.Function(Monotype.Number, Monotype.Number)
        )
    ),
    "equals" to Polytype.fromMono(
        Monotype.Function(
            Monotype.Number,
            Monotype.Function(Monotype.Number, Monotype.Boolean)
        )
    ),
    "cons" to Polytype(
        listOf("a"),
        Monotype.Function(
            Monotype.Var("a"),
            Monotype.Function(
                Monotype.LinkedList(Monotype.Var("a")),
                Monotype.LinkedList(Monotype.Var("a"))
            )
        )
    ), // cons : forall a. a -> [a] -> [a]
    "nil" to Polytype(
        listOf("a"), Monotype.LinkedList(Monotype.Var("a"))
    ), // nil : forall a. [a]
    "isEmpty" to Polytype(
        listOf("a"), Monotype.Function(
            Monotype.LinkedList(Monotype.Var("a")),
            Monotype.Boolean
        )
    ), // isEmpty : forall a. [a] -> Boolean
    "head" to Polytype(
        listOf("a"), Monotype.Function(
            Monotype.LinkedList(Monotype.Var("a")),
            Monotype.Var("a")
        )
    ),// head : forall a. [a] -> a
    "tail" to Polytype(
        listOf("a"), Monotype.Function(
            Monotype.LinkedList(Monotype.Var("a")),
            Monotype.LinkedList(Monotype.Var("a"))
        )
    )// tail : forall a. [a] -> [a]
)

var supply: Int = 0
fun freshUnknown(): Monotype = Monotype.Unknown(++supply)

fun instantiate(ty: Polytype): Monotype =
    ty.vars.fold(ty.type) { acc, v ->
        val unknown = freshUnknown()
        substitute(acc, v, unknown)
    }

fun generalize(ctx: Context, ty: Monotype): Polytype {
    val ty = applySolution(solution, ty)
    val contextUnknowns = unknownsInCtx(ctx)
    val unknownsWithVars = ty.unknowns()
        .filter { u -> !contextUnknowns.contains(u) }
        .map { it to "a$it" }
    val genSolution: Solution = HashMap()
    genSolution.putAll(unknownsWithVars.map { (u, v) -> u to Monotype.Var(v) })

    return Polytype(unknownsWithVars.map { it.second }, applySolution(genSolution, ty))
}

// Type inference
fun infer(ctx: Context, expr: Expr): Monotype {
    return when (expr) {
        is Expr.Number -> Monotype.Number
        is Expr.Boolean -> Monotype.Boolean
        is Expr.Var -> ctx[expr.name]?.let(::instantiate) ?: throw Exception("Unknown variable ${expr.name}")
        is Expr.Lambda -> {
            val tyArg = freshUnknown()
            val newCtx = ctx.put(expr.binder, Polytype.fromMono(tyArg))
            val tyBody = infer(newCtx, expr.body)
            Monotype.Function(tyArg, tyBody)
        }
        is Expr.If -> {
            val tyCond = infer(ctx, expr.condition)
            val tyThen = infer(ctx, expr.thenBranch)
            val tyElse = infer(ctx, expr.elseBranch)

            unify(tyCond, Monotype.Boolean)
            unify(tyThen, tyElse)

            tyThen // tyElse
        }
        is Expr.Application -> {
            val tyFun = infer(ctx, expr.func)
            val tyArg = infer(ctx, expr.argument)
            val tyRes = freshUnknown()
            unify(tyFun, Monotype.Function(tyArg, tyRes))
            tyRes
        }
        is Expr.Let -> {
            val newCtx = if (expr.isRecursive) {
                val tyBinder = freshUnknown()
                // TODO: Polymorph recursive bindings?
                val innerCtx = ctx.put(expr.binder, Polytype.fromMono(tyBinder))
                val inferredBinder = infer(innerCtx, expr.expr)
                unify(tyBinder, inferredBinder)
                innerCtx
            } else {
                val tyBinder = infer(ctx, expr.expr)
                ctx.put(expr.binder, generalize(ctx, tyBinder))
            }
            infer(newCtx, expr.body)
        }
        is Expr.LinkedList -> {
            val elementType = freshUnknown()
            expr.values.forEach { element ->
                unify(elementType, infer(ctx, element))
            }
            Monotype.LinkedList(elementType)
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
        println(expr + " : ${applySolution(solution, ty).pretty()}")
        val result = eval(initialEnv, e)
        println("=======")
        println(result)
    }


//    testInfer("""let x = 42 in true""")
//    val h20 = """
//        let rec fak = \x ->
//            if x == 0
//                then 1
//                else x * fak (x - 1) in
//        fak 5
//    """.trimIndent()
//    testInfer(h20)
//
//    val next = """
//        let identity = \x -> x in
//        identity identity
//    """.trimIndent()
//    testInfer(next)

    val listExpr = """
        let empty = nil in
        let listOne = cons (10 + 20) empty in
        let listTwo = cons (20 - 10) listOne in
        tail listTwo
        """.trimIndent()
    testInfer(listExpr)

    val mapping = """
        let rec map = \f -> \xs -> 
          if isEmpty xs 
          then nil 
          else cons (f (head xs)) (map f (tail xs)) in
        map (\x -> x + 10) [1, 2, 3]
    """.trimIndent()
    testInfer(mapping)

    // filter : forall a. (a -> Boolean) -> [a] -> [a]

    val filtering = """
        let not = \b -> if b then false else true in
        let rec filter = \f -> \xs -> 
            if isEmpty xs 
            then nil 
            else if f (head xs)
                then cons (head xs) (filter f (tail xs)) 
                else filter f (tail xs) in
        filter (\x -> not (isEmpty x)) [[], [10], [], [20, 30, 2 + 5]]
    """.trimIndent()
    testInfer(filtering)
    // printSolution()

}