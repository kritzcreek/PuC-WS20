sealed class Type {
    override fun toString(): String = javaClass.simpleName

    object Number: Type()
    object Boolean: Type()
    data class Function(val argument: Type, val result: Type): Type()

    fun pretty(): String = prettyInner(false)
    private fun prettyInner(parens: kotlin.Boolean): String {
        return when(this) {
            Number -> "Number"
            Boolean -> "Boolean"
            is Function -> {
                val inner = "${argument.prettyInner(true)} -> ${result.pretty()}"
                if (parens) "($inner)" else inner
            }
        }
    }
}

// Type inference
fun infer(expr: Expr): Type {
    TODO()
}

fun main() {
    println(Type.Number.pretty())
    println(Type.Boolean.pretty())
    println(Type.Function(Type.Number, Type.Boolean).pretty())
    println(Type.Function(Type.Number, Type.Function(Type.Number, Type.Number)).pretty())
    println(Type.Function(Type.Function(Type.Number, Type.Number), Type.Number).pretty())
}