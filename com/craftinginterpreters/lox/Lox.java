package com.craftinginterpreters.lox;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.craftinginterpreters.lox.Scanner.*;


public class Lox {

    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false;
    static boolean hadRuntimeError = false;


    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // 1. You must declare the parser variable here
        Parser parser = new Parser(tokens);

        // 2. You changed this from parser.parse() to parser.parse() returning a List
        List<Stmt> statements = parser.parse();

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a syntax error.
        if (hadError) return;
        //System.out.println(new AstPrinter().print(expression));

        if (statements.size() == 1 &&
                statements.get(0) instanceof Stmt.Expression) {

            Stmt.Expression stmt =
                    (Stmt.Expression) statements.get(0);

            Object value = interpreter.evaluate(stmt.expression);
            System.out.println(interpreter.stringify(value));

        } else {
            interpreter.interpret(statements);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }


    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }


    private static void report(int line, String where, String message) {

        if (hadError) System.exit(65);
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }


    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}