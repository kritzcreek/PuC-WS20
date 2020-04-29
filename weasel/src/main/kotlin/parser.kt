class Parser(val tokens: Lexer) {

    fun parseExpression(): Expr {
        val atoms = mutableListOf<Expr>()
        while (true) {
            val atom = parseAtom() ?: break
            atoms += atom
        }

        return when {
            atoms.isEmpty() -> throw Exception("Unexpected ${tokens.peek()} expected expression")
            else -> atoms.drop(1).fold(atoms[0]) { acc, expr ->
                Expr.Application(acc, expr)
            }
        }
    }

    fun parseAtom(): Expr? = when (val t = tokens.peek()) {
        is Token.NUMBER -> number()
        is Token.BOOLEAN -> boolean()
        is Token.IDENT -> ident()
        is Token.LEFT_PAREN -> parenthesized()
        is Token.LAMBDA -> lambda()
        is Token.IF -> ifExpression()
        else -> null
    }

    private fun ifExpression(): Expr.If {
        expectNext<Token.IF>("if")
        val condition = parseExpression()
        expectNext<Token.THEN>("then")
        val thenBranch = parseExpression()
        expectNext<Token.ELSE>("else")
        val elseBranch = parseExpression()
        return Expr.If(condition, thenBranch, elseBranch)
    }

    private fun lambda(): Expr.Lambda {
        expectNext<Token.LAMBDA>("lambda")
        val binder = expectNext<Token.IDENT>("binder").ident
        expectNext<Token.RIGHT_ARROW>("right arrow")
        val body = parseExpression()
        return Expr.Lambda(binder, body)
    }

    private fun parenthesized(): Expr {
        expectNext<Token.LEFT_PAREN>("opening paren")
        val expr = parseExpression()
        expectNext<Token.RIGHT_PAREN>("closing paren")
        return expr
    }

    private fun ident(): Expr.Var {
        val ident = expectNext<Token.IDENT>("ident").ident
        return Expr.Var(ident)
    }

    private fun boolean(): Expr.Boolean {
        val boolean = expectNext<Token.BOOLEAN>("boolean").boolean
        return Expr.Boolean(boolean)
    }

    private fun number(): Expr.Number {
        val number = expectNext<Token.NUMBER>("number").number
        return Expr.Number(number)
    }

    private inline fun <reified T> expectNext(msg: String): T {
        val next = tokens.next()
        if (next is T) {
            return next
        } else {
            throw Exception("Expected $msg, but saw $next")
        }
    }
}

fun main() {
    val input = """
        if (\x1 -> equals 20 x1) 25
        then true
        else add 3 (multiply 4 5)
    """.trimIndent()

    val lexer = Lexer(input)
    val parser = Parser(lexer)

    val expr = parser.parseExpression()
    println("Parsed\n=======")
    println(expr)
    println()
    println("Evaled\n=======")
    println(eval(initialEnv, expr))

    fun parseExpr(s: String) = Parser(Lexer(s)).parseExpression()

    val z = parseExpr("""\f -> (\x -> f \v -> x x v) (\x -> f \v -> x x v)""")
    val faculty = parseExpr("""
        \fac -> \x -> 
            if equals x 0
            then 1
            else multiply x (fac (subtract x 1))
    """.trimIndent())

    println(eval(initialEnv, Expr.Application(Expr.Application(z, faculty), Expr.Number(5))))
}