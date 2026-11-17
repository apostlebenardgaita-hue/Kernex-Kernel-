package com.example.domain

import com.example.data.model.CustomLanguage
import com.example.data.model.LinuxProfile
import kotlin.system.measureTimeMillis

enum class TokenType {
    KW_VAR, KW_PRINT, KW_FUNC, KW_IF, KW_ELSE, KW_LOOP, KW_RETURN, KW_TRUE, KW_FALSE,
    KW_IN, KW_AND, KW_OR, KW_NOT,
    IDENT, NUMBER, STRING,
    PLUS, MINUS, STAR, SLASH, PERCENT,
    EQ, EQEQ, NEQ, LT, GT, LTE, GTE,
    AND_OP, OR_OP, BANG,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
    COMMA, DOTDOT, EOF
}

data class Token(val type: TokenType, val lexeme: String, val line: Int)

data class ExecutionResult(
    val outputLines: List<String>,
    val tokenPreview: String,
    val astSummary: String,
    val durationMs: Long,
    val errorMessage: String? = null
)

data class CodeTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val builder: (CustomLanguage) -> String
)

private class ReturnSignal(val value: Any?) : RuntimeException(null, null, false, false)

object KernixLanguageEngine {

    private const val MAX_STEPS = 50_000
    private const val MAX_CALL_DEPTH = 64

    val templates: List<CodeTemplate> = listOf(
        CodeTemplate("linux_boot", "Linux Boot Probe", "Inspect active kernel & DE") { lang ->
            val c = lang.commentPrefix
            """
            $c Inspect your custom Linux & DE setup
            ${lang.kwVar} os = uname()
            ${lang.kwPrint} "Booting " + os
            ${lang.kwPrint} kernel_info()
            ${lang.kwPrint} de_info()
            ${lang.kwVar} ready = ${lang.kwTrue}
            ${lang.kwIf} ready {
                ${lang.kwPrint} service_unit("kernix-agent", "/usr/bin/kernixd")
            }
            """.trimIndent()
        },
        CodeTemplate("fibonacci", "Fibonacci Sequence", "Recursive function & loop") { lang ->
            val c = lang.commentPrefix
            """
            $c Easy Fibonacci in ${lang.name}
            ${lang.kwFunc} fib(n) {
                ${lang.kwIf} n <= 1 {
                    ${lang.kwReturn} n
                }
                ${lang.kwReturn} fib(n - 1) + fib(n - 2)
            }
            ${lang.kwLoop} i in 0..8 {
                ${lang.kwPrint} "fib(" + i + ") = " + fib(i)
            }
            """.trimIndent()
        },
        CodeTemplate("pkg_check", "Package Audit Loop", "Lists, loops & conditionals") { lang ->
            val c = lang.commentPrefix
            """
            $c Check core packages for your distro
            ${lang.kwVar} pkgs = ["linux-custom", "wayland", "pipewire", "mesa"]
            ${lang.kwPrint} "Packages queued: " + len(pkgs)
            ${lang.kwLoop} i in 0..3 {
                ${lang.kwVar} name = pkgs[i]
                ${lang.kwIf} i == 0 {
                    ${lang.kwPrint} "[CORE] " + name
                } ${lang.kwElse} {
                    ${lang.kwPrint} "[USER] " + name
                }
            }
            """.trimIndent()
        },
        CodeTemplate("repeat_demo", "Quick Repeat Counter", "Super-easy repeat N syntax") { lang ->
            val c = lang.commentPrefix
            """
            $c Ultra-simple repeat block
            ${lang.kwVar} count = 0
            ${lang.kwLoop} 5 {
                count = count + 1
                ${lang.kwPrint} "Step #" + count + " OK"
            }
            """.trimIndent()
        }
    )

    fun tokenize(source: String, lang: CustomLanguage): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        var line = 1
        val kwMap = mapOf(
            lang.kwVar to TokenType.KW_VAR,
            lang.kwPrint to TokenType.KW_PRINT,
            lang.kwFunc to TokenType.KW_FUNC,
            lang.kwIf to TokenType.KW_IF,
            lang.kwElse to TokenType.KW_ELSE,
            lang.kwLoop to TokenType.KW_LOOP,
            lang.kwReturn to TokenType.KW_RETURN,
            lang.kwTrue to TokenType.KW_TRUE,
            lang.kwFalse to TokenType.KW_FALSE,
            "in" to TokenType.KW_IN,
            "and" to TokenType.KW_AND,
            "or" to TokenType.KW_OR,
            "not" to TokenType.KW_NOT
        )

        while (i < source.length) {
            if (source.startsWith(lang.commentPrefix, i)) {
                while (i < source.length && source[i] != '\n') i++
                continue
            }
            val ch = source[i]
            when {
                ch == '\n' -> { line++; i++ }
                ch.isWhitespace() -> i++
                ch == '"' -> {
                    val startLine = line
                    i++
                    val sb = StringBuilder()
                    while (i < source.length && source[i] != '"') {
                        if (source[i] == '\n') line++
                        if (source[i] == '\\' && i + 1 < source.length) {
                            i++
                            when (source[i]) {
                                'n' -> sb.append('\n')
                                't' -> sb.append('\t')
                                '"' -> sb.append('"')
                                '\\' -> sb.append('\\')
                                else -> sb.append(source[i])
                            }
                        } else {
                            sb.append(source[i])
                        }
                        i++
                    }
                    if (i >= source.length) {
                        throw IllegalArgumentException("Line $startLine: Unterminated string literal")
                    }
                    i++
                    tokens += Token(TokenType.STRING, sb.toString(), startLine)
                }
                ch.isDigit() -> {
                    val start = i
                    while (i < source.length && source[i].isDigit()) i++
                    if (i + 1 < source.length && source[i] == '.' && source[i + 1].isDigit()) {
                        i++
                        while (i < source.length && source[i].isDigit()) i++
                    }
                    tokens += Token(TokenType.NUMBER, source.substring(start, i), line)
                }
                ch.isLetter() || ch == '_' -> {
                    val start = i
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
                    val word = source.substring(start, i)
                    tokens += Token(kwMap[word] ?: TokenType.IDENT, word, line)
                }
                source.startsWith("..", i) -> { tokens += Token(TokenType.DOTDOT, "..", line); i += 2 }
                source.startsWith("==", i) -> { tokens += Token(TokenType.EQEQ, "==", line); i += 2 }
                source.startsWith("!=", i) -> { tokens += Token(TokenType.NEQ, "!=", line); i += 2 }
                source.startsWith("<=", i) -> { tokens += Token(TokenType.LTE, "<=", line); i += 2 }
                source.startsWith(">=", i) -> { tokens += Token(TokenType.GTE, ">=", line); i += 2 }
                source.startsWith("&&", i) -> { tokens += Token(TokenType.AND_OP, "&&", line); i += 2 }
                source.startsWith("||", i) -> { tokens += Token(TokenType.OR_OP, "||", line); i += 2 }
                ch == '=' -> { tokens += Token(TokenType.EQ, "=", line); i++ }
                ch == '+' -> { tokens += Token(TokenType.PLUS, "+", line); i++ }
                ch == '-' -> { tokens += Token(TokenType.MINUS, "-", line); i++ }
                ch == '*' -> { tokens += Token(TokenType.STAR, "*", line); i++ }
                ch == '/' -> { tokens += Token(TokenType.SLASH, "/", line); i++ }
                ch == '%' -> { tokens += Token(TokenType.PERCENT, "%", line); i++ }
                ch == '<' -> { tokens += Token(TokenType.LT, "<", line); i++ }
                ch == '>' -> { tokens += Token(TokenType.GT, ">", line); i++ }
                ch == '!' -> { tokens += Token(TokenType.BANG, "!", line); i++ }
                ch == '(' -> { tokens += Token(TokenType.LPAREN, "(", line); i++ }
                ch == ')' -> { tokens += Token(TokenType.RPAREN, ")", line); i++ }
                ch == '{' -> { tokens += Token(TokenType.LBRACE, "{", line); i++ }
                ch == '}' -> { tokens += Token(TokenType.RBRACE, "}", line); i++ }
                ch == '[' -> { tokens += Token(TokenType.LBRACKET, "[", line); i++ }
                ch == ']' -> { tokens += Token(TokenType.RBRACKET, "]", line); i++ }
                ch == ',' -> { tokens += Token(TokenType.COMMA, ",", line); i++ }
                else -> throw IllegalArgumentException("Line $line: Unexpected character '$ch'")
            }
        }
        tokens += Token(TokenType.EOF, "", line)
        return tokens
    }

    fun execute(source: String, lang: CustomLanguage, profile: LinuxProfile): ExecutionResult {
        val output = mutableListOf<String>()
        var tokenPreview = ""
        var astSummary = ""
        var err: String? = null
        val elapsed = measureTimeMillis {
            try {
                val tokens = tokenize(source, lang)
                tokenPreview = tokens.filter { it.type != TokenType.EOF }
                    .take(40)
                    .joinToString("  ") { "${it.type.name}(${it.lexeme})" }
                val stmts = Parser(tokens).parseProgram()
                astSummary = "${stmts.size} top-level statement(s) parsed in ${lang.name}"
                val interp = Interpreter(output, profile)
                interp.execBlock(stmts, interp.globalEnv)
            } catch (e: Exception) {
                err = e.message ?: "Execution failed"
            }
        }
        return ExecutionResult(
            outputLines = output,
            tokenPreview = tokenPreview,
            astSummary = astSummary,
            durationMs = elapsed,
            errorMessage = err
        )
    }

    fun transpileToBash(source: String, lang: CustomLanguage, profile: LinuxProfile): String {
        val sb = StringBuilder()
        sb.appendLine("#!/usr/bin/env bash")
        sb.appendLine("# Transpiled from ${lang.name} (${lang.fileExtension}) for ${profile.name}")
        sb.appendLine("set -euo pipefail")
        sb.appendLine()
        for (rawLine in source.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                sb.appendLine()
                continue
            }
            if (line.startsWith(lang.commentPrefix)) {
                sb.appendLine("# ${line.removePrefix(lang.commentPrefix).trim()}")
                continue
            }
            when {
                line.startsWith("${lang.kwVar} ") -> {
                    val rest = line.removePrefix("${lang.kwVar} ").trim()
                    val parts = rest.split("=", limit = 2)
                    if (parts.size == 2) {
                        val name = parts[0].trim()
                        val expr = parts[1].trim().replace(" + ", " ")
                        sb.appendLine("$name=$expr")
                    } else {
                        sb.appendLine("# $line")
                    }
                }
                line.startsWith("${lang.kwPrint} ") -> {
                    val expr = line.removePrefix("${lang.kwPrint} ").trim()
                    sb.appendLine("echo $expr")
                }
                line.startsWith("${lang.kwIf} ") -> {
                    val cond = line.removePrefix("${lang.kwIf} ").removeSuffix("{").trim()
                    sb.appendLine("if [[ $cond ]]; then")
                }
                line.startsWith("} ${lang.kwElse}") || line.startsWith("${lang.kwElse}") -> {
                    sb.appendLine("else")
                }
                line == "}" -> sb.appendLine("fi # /done")
                else -> sb.appendLine("# $line")
            }
        }
        return sb.toString().trimEnd()
    }

    // --- AST ---
    private sealed interface Stmt
    private data class VarStmt(val name: String, val expr: Expr) : Stmt
    private data class AssignStmt(val name: String, val indexExpr: Expr?, val value: Expr) : Stmt
    private data class PrintStmt(val expr: Expr) : Stmt
    private data class ExprStmt(val expr: Expr) : Stmt
    private data class IfStmt(val cond: Expr, val thenBranch: List<Stmt>, val elseBranch: List<Stmt>?) : Stmt
    private data class RepeatCountStmt(val countExpr: Expr, val body: List<Stmt>) : Stmt
    private data class RangeLoopStmt(val varName: String, val start: Expr, val end: Expr, val body: List<Stmt>) : Stmt
    private data class WhileStmt(val cond: Expr, val body: List<Stmt>) : Stmt
    private data class FuncStmt(val name: String, val params: List<String>, val body: List<Stmt>) : Stmt
    private data class ReturnStmt(val expr: Expr?) : Stmt

    private sealed interface Expr
    private data class LiteralExpr(val value: Any?) : Expr
    private data class VarExpr(val name: String) : Expr
    private data class ListExpr(val items: List<Expr>) : Expr
    private data class IndexExpr(val target: Expr, val index: Expr) : Expr
    private data class UnaryExpr(val op: TokenType, val right: Expr) : Expr
    private data class BinaryExpr(val left: Expr, val op: TokenType, val right: Expr) : Expr
    private data class CallExpr(val name: String, val args: List<Expr>) : Expr

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun parseProgram(): List<Stmt> {
            val stmts = mutableListOf<Stmt>()
            while (!check(TokenType.EOF)) {
                stmts += parseStmt()
            }
            return stmts
        }

        private fun parseStmt(): Stmt = when (peek().type) {
            TokenType.KW_VAR -> {
                advance()
                val name = consume(TokenType.IDENT, "Expected variable name").lexeme
                consume(TokenType.EQ, "Expected '=' after variable name")
                VarStmt(name, parseExpr())
            }
            TokenType.KW_PRINT -> {
                advance()
                PrintStmt(parseExpr())
            }
            TokenType.KW_FUNC -> {
                advance()
                val name = consume(TokenType.IDENT, "Expected function name").lexeme
                consume(TokenType.LPAREN, "Expected '(' after function name")
                val params = mutableListOf<String>()
                if (!check(TokenType.RPAREN)) {
                    do {
                        params += consume(TokenType.IDENT, "Expected parameter name").lexeme
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Expected ')' after parameters")
                FuncStmt(name, params, parseBlock())
            }
            TokenType.KW_IF -> {
                advance()
                val cond = parseExpr()
                val thenBranch = parseBlock()
                val elseBranch = if (match(TokenType.KW_ELSE)) parseBlock() else null
                IfStmt(cond, thenBranch, elseBranch)
            }
            TokenType.KW_LOOP -> parseLoopStmt()
            TokenType.KW_RETURN -> {
                advance()
                val expr = if (check(TokenType.RBRACE) || check(TokenType.EOF)) null else parseExpr()
                ReturnStmt(expr)
            }
            else -> {
                if (check(TokenType.IDENT) && peekNext().type == TokenType.EQ) {
                    val name = advance().lexeme
                    advance() // =
                    AssignStmt(name, null, parseExpr())
                } else if (check(TokenType.IDENT) && peekNext().type == TokenType.LBRACKET) {
                    val saved = pos
                    val name = advance().lexeme
                    advance() // [
                    val idx = parseExpr()
                    if (match(TokenType.RBRACKET) && match(TokenType.EQ)) {
                        AssignStmt(name, idx, parseExpr())
                    } else {
                        pos = saved
                        ExprStmt(parseExpr())
                    }
                } else {
                    ExprStmt(parseExpr())
                }
            }
        }

        private fun parseLoopStmt(): Stmt {
            advance() // KW_LOOP
            if (check(TokenType.IDENT) && peekNext().type == TokenType.KW_IN) {
                val varName = advance().lexeme
                advance() // KW_IN
                val start = parseExpr()
                consume(TokenType.DOTDOT, "Expected '..' in range loop (e.g., 0..5)")
                val end = parseExpr()
                return RangeLoopStmt(varName, start, end, parseBlock())
            }
            val expr = parseExpr()
            val body = parseBlock()
            return if (expr is LiteralExpr && expr.value is Number) {
                RepeatCountStmt(expr, body)
            } else if (expr is BinaryExpr || expr is UnaryExpr || (expr is LiteralExpr && expr.value is Boolean)) {
                WhileStmt(expr, body)
            } else {
                RepeatCountStmt(expr, body)
            }
        }

        private fun parseBlock(): List<Stmt> {
            consume(TokenType.LBRACE, "Expected '{' to open block")
            val stmts = mutableListOf<Stmt>()
            while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                stmts += parseStmt()
            }
            consume(TokenType.RBRACE, "Expected '}' to close block")
            return stmts
        }

        private fun parseExpr(): Expr = parseOr()

        private fun parseOr(): Expr {
            var expr = parseAnd()
            while (match(TokenType.OR_OP, TokenType.KW_OR)) {
                val op = prev().type
                expr = BinaryExpr(expr, op, parseAnd())
            }
            return expr
        }

        private fun parseAnd(): Expr {
            var expr = parseEq()
            while (match(TokenType.AND_OP, TokenType.KW_AND)) {
                val op = prev().type
                expr = BinaryExpr(expr, op, parseEq())
            }
            return expr
        }

        private fun parseEq(): Expr {
            var expr = parseComp()
            while (match(TokenType.EQEQ, TokenType.NEQ)) {
                val op = prev().type
                expr = BinaryExpr(expr, op, parseComp())
            }
            return expr
        }

        private fun parseComp(): Expr {
            var expr = parseAdd()
            while (match(TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE)) {
                val op = prev().type
                expr = BinaryExpr(expr, op, parseAdd())
            }
            return expr
        }

        private fun parseAdd(): Expr {
            var expr = parseMul()
            while (match(TokenType.PLUS, TokenType.MINUS)) {
                val op = prev().type
                expr = BinaryExpr(expr, op, parseMul())
            }
            return expr
        }

        private fun parseMul(): Expr {
            var expr = parseUnary()
            while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
                val op = prev().type
                expr = BinaryExpr(expr, op, parseUnary())
            }
            return expr
        }

        private fun parseUnary(): Expr {
            if (match(TokenType.BANG, TokenType.KW_NOT, TokenType.MINUS)) {
                return UnaryExpr(prev().type, parseUnary())
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Expr {
            var expr = parsePrimary()
            while (true) {
                if (match(TokenType.LBRACKET)) {
                    val idx = parseExpr()
                    consume(TokenType.RBRACKET, "Expected ']'")
                    expr = IndexExpr(expr, idx)
                } else {
                    break
                }
            }
            return expr
        }

        private fun parsePrimary(): Expr {
            val tok = peek()
            return when (tok.type) {
                TokenType.NUMBER -> {
                    advance()
                    val num: Number = if (tok.lexeme.contains('.')) tok.lexeme.toDouble() else tok.lexeme.toLong()
                    LiteralExpr(num)
                }
                TokenType.STRING -> { advance(); LiteralExpr(tok.lexeme) }
                TokenType.KW_TRUE -> { advance(); LiteralExpr(true) }
                TokenType.KW_FALSE -> { advance(); LiteralExpr(false) }
                TokenType.LPAREN -> {
                    advance()
                    val inner = parseExpr()
                    consume(TokenType.RPAREN, "Expected ')'")
                    inner
                }
                TokenType.LBRACKET -> {
                    advance()
                    val items = mutableListOf<Expr>()
                    if (!check(TokenType.RBRACKET)) {
                        do { items += parseExpr() } while (match(TokenType.COMMA))
                    }
                    consume(TokenType.RBRACKET, "Expected ']'")
                    ListExpr(items)
                }
                TokenType.IDENT -> {
                    advance()
                    if (match(TokenType.LPAREN)) {
                        val args = mutableListOf<Expr>()
                        if (!check(TokenType.RPAREN)) {
                            do { args += parseExpr() } while (match(TokenType.COMMA))
                        }
                        consume(TokenType.RPAREN, "Expected ')' after arguments")
                        CallExpr(tok.lexeme, args)
                    } else {
                        VarExpr(tok.lexeme)
                    }
                }
                else -> throw IllegalArgumentException("Line ${tok.line}: Unexpected token '${tok.lexeme}'")
            }
        }

        private fun peek() = tokens[pos]
        private fun peekNext() = tokens.getOrElse(pos + 1) { tokens.last() }
        private fun prev() = tokens[pos - 1]
        private fun check(type: TokenType) = peek().type == type
        private fun advance(): Token = tokens[pos++]
        private fun match(vararg types: TokenType): Boolean {
            if (types.any { check(it) }) {
                advance(); return true
            }
            return false
        }
        private fun consume(type: TokenType, msg: String): Token {
            if (check(type)) return advance()
            throw IllegalArgumentException("Line ${peek().line}: $msg (found '${peek().lexeme}')")
        }
    }

    private class Env(val parent: Env? = null) {
        private val values = mutableMapOf<String, Any?>()
        fun define(name: String, v: Any?) { values[name] = v }
        fun get(name: String): Any? = when {
            values.containsKey(name) -> values[name]
            parent != null -> parent.get(name)
            else -> throw IllegalArgumentException("Undefined variable '$name'")
        }
        fun assign(name: String, v: Any?) {
            when {
                values.containsKey(name) -> values[name] = v
                parent != null -> parent.assign(name, v)
                else -> values[name] = v
            }
        }
    }

    private class Interpreter(
        private val output: MutableList<String>,
        private val profile: LinuxProfile
    ) {
        val globalEnv = Env()
        private val functions = mutableMapOf<String, FuncStmt>()
        private var steps = 0
        private var depth = 0

        private fun tick() {
            if (++steps > MAX_STEPS) {
                throw IllegalStateException("Execution stopped: exceeded $MAX_STEPS steps (possible infinite loop)")
            }
        }

        fun execBlock(stmts: List<Stmt>, env: Env) {
            for (s in stmts) execStmt(s, env)
        }

        private fun execStmt(stmt: Stmt, env: Env) {
            tick()
            when (stmt) {
                is VarStmt -> env.define(stmt.name, eval(stmt.expr, env))
                is AssignStmt -> {
                    val v = eval(stmt.value, env)
                    if (stmt.indexExpr == null) {
                        env.assign(stmt.name, v)
                    } else {
                        val list = env.get(stmt.name) as? MutableList<Any?>
                            ?: throw IllegalArgumentException("'${stmt.name}' is not a mutable list")
                        val idx = (eval(stmt.indexExpr, env) as? Number)?.toInt()
                            ?: throw IllegalArgumentException("List index must be a number")
                        list[idx] = v
                    }
                }
                is PrintStmt -> output += stringify(eval(stmt.expr, env))
                is ExprStmt -> eval(stmt.expr, env)
                is FuncStmt -> functions[stmt.name] = stmt
                is ReturnStmt -> throw ReturnSignal(stmt.expr?.let { eval(it, env) })
                is IfStmt -> {
                    if (isTruthy(eval(stmt.cond, env))) {
                        execBlock(stmt.thenBranch, Env(env))
                    } else if (stmt.elseBranch != null) {
                        execBlock(stmt.elseBranch, Env(env))
                    }
                }
                is RepeatCountStmt -> {
                    val count = (eval(stmt.countExpr, env) as? Number)?.toLong()
                        ?: throw IllegalArgumentException("Loop count must be a number")
                    for (i in 0 until count.coerceAtLeast(0L)) {
                        tick()
                        execBlock(stmt.body, Env(env))
                    }
                }
                is RangeLoopStmt -> {
                    val start = (eval(stmt.start, env) as? Number)?.toLong()
                        ?: throw IllegalArgumentException("Range start must be a number")
                    val end = (eval(stmt.end, env) as? Number)?.toLong()
                        ?: throw IllegalArgumentException("Range end must be a number")
                    for (i in start..end) {
                        tick()
                        val loopEnv = Env(env)
                        loopEnv.define(stmt.varName, i)
                        execBlock(stmt.body, loopEnv)
                    }
                }
                is WhileStmt -> {
                    while (isTruthy(eval(stmt.cond, env))) {
                        tick()
                        execBlock(stmt.body, Env(env))
                    }
                }
            }
        }

        private fun eval(expr: Expr, env: Env): Any? {
            tick()
            return when (expr) {
                is LiteralExpr -> expr.value
                is VarExpr -> env.get(expr.name)
                is ListExpr -> expr.items.map { eval(it, env) }.toMutableList()
                is IndexExpr -> {
                    val target = eval(expr.target, env)
                    val idx = (eval(expr.index, env) as? Number)?.toInt()
                        ?: throw IllegalArgumentException("Index must be an integer")
                    when (target) {
                        is List<*> -> target.getOrElse(idx) { throw IndexOutOfBoundsException("Index $idx out of bounds") }
                        is String -> target.getOrElse(idx) { throw IndexOutOfBoundsException("Index $idx out of bounds") }.toString()
                        else -> throw IllegalArgumentException("Cannot index value of type ${target?.javaClass?.simpleName}")
                    }
                }
                is UnaryExpr -> {
                    val r = eval(expr.right, env)
                    when (expr.op) {
                        TokenType.MINUS -> when (r) {
                            is Long -> -r
                            is Double -> -r
                            else -> throw IllegalArgumentException("Cannot negate '$r'")
                        }
                        TokenType.BANG, TokenType.KW_NOT -> !isTruthy(r)
                        else -> r
                    }
                }
                is BinaryExpr -> evalBinary(expr, env)
                is CallExpr -> evalCall(expr, env)
            }
        }

        private fun evalBinary(expr: BinaryExpr, env: Env): Any? {
            if (expr.op == TokenType.AND_OP || expr.op == TokenType.KW_AND) {
                return isTruthy(eval(expr.left, env)) && isTruthy(eval(expr.right, env))
            }
            if (expr.op == TokenType.OR_OP || expr.op == TokenType.KW_OR) {
                return isTruthy(eval(expr.left, env)) || isTruthy(eval(expr.right, env))
            }
            val l = eval(expr.left, env)
            val r = eval(expr.right, env)
            return when (expr.op) {
                TokenType.PLUS -> if (l is String || r is String) stringify(l) + stringify(r) else numOp(l, r) { a, b -> a + b }
                TokenType.MINUS -> numOp(l, r) { a, b -> a - b }
                TokenType.STAR -> numOp(l, r) { a, b -> a * b }
                TokenType.SLASH -> {
                    if ((r as? Number)?.toDouble() == 0.0) throw ArithmeticException("Division by zero")
                    numOp(l, r) { a, b -> a / b }
                }
                TokenType.PERCENT -> numOp(l, r) { a, b -> a % b }
                TokenType.EQEQ -> l == r
                TokenType.NEQ -> l != r
                TokenType.LT -> compareNums(l, r) < 0
                TokenType.GT -> compareNums(l, r) > 0
                TokenType.LTE -> compareNums(l, r) <= 0
                TokenType.GTE -> compareNums(l, r) >= 0
                else -> null
            }
        }

        private fun evalCall(expr: CallExpr, env: Env): Any? {
            val args = expr.args.map { eval(it, env) }
            when (expr.name) {
                "uname" -> return "${profile.name} (${profile.baseDistro} ${profile.architecture})"
                "kernel_info" -> return "Linux ${profile.kernelVersion} | ${profile.scheduler} | ${profile.preemption} | ${profile.kernelFlags.size} flags"
                "de_info" -> return "${profile.desktopEnv} + ${profile.statusBar} (${profile.terminal}, gaps=${profile.windowGaps}px)"
                "len" -> return when (val a = args.firstOrNull()) {
                    is List<*> -> a.size.toLong()
                    is String -> a.length.toLong()
                    else -> 0L
                }
                "push" -> {
                    val list = args.getOrNull(0) as? MutableList<Any?>
                        ?: throw IllegalArgumentException("push(list, value) expects a list")
                    list.add(args.getOrNull(1))
                    return list.size.toLong()
                }
                "range" -> {
                    val a = (args.getOrNull(0) as? Number)?.toLong() ?: 0L
                    val b = (args.getOrNull(1) as? Number)?.toLong() ?: 0L
                    return (a..b).toMutableList()
                }
                "str" -> return stringify(args.firstOrNull())
                "num" -> return args.firstOrNull()?.toString()?.toLongOrNull() ?: 0L
                "service_unit" -> {
                    val name = stringify(args.getOrNull(0))
                    val exec = stringify(args.getOrNull(1))
                    return "[Unit] Description=$name | ExecStart=$exec (init=${profile.initSystem})"
                }
            }
            val fn = functions[expr.name]
                ?: throw IllegalArgumentException("Unknown function '${expr.name}'")
            if (++depth > MAX_CALL_DEPTH) {
                throw IllegalStateException("Max recursion depth ($MAX_CALL_DEPTH) exceeded")
            }
            val fnEnv = Env(globalEnv)
            fn.params.forEachIndexed { idx, p -> fnEnv.define(p, args.getOrNull(idx)) }
            return try {
                execBlock(fn.body, fnEnv)
                null
            } catch (ret: ReturnSignal) {
                ret.value
            } finally {
                depth--
            }
        }

        private fun numOp(l: Any?, r: Any?, op: (Double, Double) -> Double): Number {
            if (l !is Number || r !is Number) {
                throw IllegalArgumentException("Expected numbers for math operation (got $l and $r)")
            }
            return if (l is Double || r is Double) {
                op(l.toDouble(), r.toDouble())
            } else {
                val res = op(l.toDouble(), r.toDouble())
                if (res % 1.0 == 0.0) res.toLong() else res
            }
        }

        private fun compareNums(l: Any?, r: Any?): Int {
            if (l !is Number || r !is Number) {
                throw IllegalArgumentException("Expected numbers for comparison (got $l and $r)")
            }
            return l.toDouble().compareTo(r.toDouble())
        }

        private fun isTruthy(v: Any?): Boolean = when (v) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0
            is String -> v.isNotEmpty()
            is List<*> -> v.isNotEmpty()
            else -> true
        }

        private fun stringify(v: Any?): String = when (v) {
            null -> "null"
            is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
            is List<*> -> v.joinToString(", ", "[", "]") { stringify(it) }
            else -> v.toString()
        }
    }
}
