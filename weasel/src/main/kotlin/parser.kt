import kotlin.math.exp

class Parser(val tokens: Lexer) {

    fun parseExpression() = parseOperatorExpression(0)

    private fun parseOperatorExpression(minBindingPower: Int): Expr {
        var leftHandSide = parseApplication()
        loop@ while (true) {
            val operator = when (val op = tokens.peek()) {
                is Token.OPERATOR -> op.operator
                else -> break@loop
            }
            val fn = functionForOperator(operator)
            val (leftBP, rightBP) = bindingPowerForOperator(operator)
            if (leftBP < minBindingPower) break
            expectNext<Token.OPERATOR>("operator")
            val rightHandSide = parseOperatorExpression(rightBP)
            leftHandSide = Expr.Application(Expr.Application(fn, leftHandSide), rightHandSide)
        }
        return leftHandSide
    }

    private fun functionForOperator(operator: String): Expr = when (operator) {
        "+" -> Expr.Var("add")
        "-" -> Expr.Var("subtract")
        "*" -> Expr.Var("multiply")
        "==" -> Expr.Var("equals")
        else -> throw Exception("Unknown operator $operator")
    }

    private fun bindingPowerForOperator(operator: String): Pair<Int, Int> = when (operator) {
        "==" -> 1 to 2
        "+", "-" -> 2 to 3
        "*" -> 3 to 4
        else -> throw Exception("Unknown operator $operator")
    }

    private fun parseApplication(): Expr {
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

    private fun parseAtom(): Expr? = when (tokens.peek()) {
        is Token.NUMBER -> number()
        is Token.BOOLEAN -> boolean()
        is Token.IDENT -> ident()
        is Token.LEFT_PAREN -> parenthesized()
        is Token.LAMBDA -> lambda()
        is Token.IF -> ifExpression()
        is Token.LET -> letExpression()
        is Token.LEFT_BRACKET -> listExpression()
        else -> null
    }

    private fun listExpression(): Expr {
        val values = mutableListOf<Expr>()
        expectNext<Token.LEFT_BRACKET>("left bracket")
        if (tokens.peek() !is Token.RIGHT_BRACKET) {
            values += parseExpression()
            while (tokens.peek() is Token.COMMA) {
                expectNext<Token.COMMA>("comma")
                values += parseExpression()
            }
        }
        expectNext<Token.RIGHT_BRACKET>("right bracket")
        return Expr.LinkedList(values)
    }

    private fun letExpression(): Expr {
        expectNext<Token.LET>("let")
        var isRecursive = false
        if (tokens.peek() is Token.REC) {
            expectNext<Token.REC>("rec")
            isRecursive = true
        }
        val binder = expectNext<Token.IDENT>("binder").ident
        expectNext<Token.EQUALS>("equals")
        val expr = parseExpression()
        expectNext<Token.IN>("in")
        val body = parseExpression()
        return Expr.Let(isRecursive, binder, expr, body)
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
        let x =
          let y = 10 in
          y + 11 in
        x + x
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
    val faculty = parseExpr(
        """
        \fac -> \x ->
            if x == 0
            then 1
            else x * fac (x - 1)
    """.trimIndent()
    )

    println(eval(initialEnv, Expr.Application(Expr.Application(z, faculty), Expr.Number(5))))
}