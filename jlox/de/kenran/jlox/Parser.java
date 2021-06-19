/* Lox's Grammar

   program         -> declaration* EOF ;
   declaration     -> varDecl
                   | statement ;
   varDecl         -> "var" IDENTIFIER ( "=" expression )? ";" ;
   statement       -> exprStatement
                   | forStmt
                   | ifStmt
                   | printStmt
                   | whileStmt
                   | block ;
   forStmt         -> "for" "(" ( varDecl | exprStmt | ";")
                   expression? ";"
                   expression? ")" statement ;
   whileStmt       -> "while" "(" expression ")" statement
   ifStmt          -> "if" "(" expression ")" statement
                   ( "else" statement )? ;
   block           -> "{" declaration* "}" ;
   exprStmt        -> expression ";" ;
   printStmt       -> "print" expression ";" ;
   expression      -> assignment ;
   assignment      -> IDENTIFIER "=" assignment
                   | logic_or ;
   logic_or        -> logic_and ( "or" logic_and )* ;
   logic_and       -> equality ( "and" equality )* ;
   equality        -> comparison ( ( "!=" | "==" ) comparison )* ;
   comparison      -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   term            -> factor ( ( "-" | "+" ) factor )* ;
   factor          -> unary ( ( "/" | "*" ) unary )* ;
   unary           -> ( "!" | "-" ) unary
                   | primary ;
   primary         -> NUMBER | STRING | "true" | "false" | nil
                   | "(" expression ")" | IDENTIFIER ;

*/

package de.kenran.jlox;

import static de.kenran.jlox.TokenType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  private Token peek() {
    return this.tokens.get(this.current);
  }

  private Token previous() {
    return this.tokens.get(this.current - 1);
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) this.current++;
    return previous();
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr assignment() {
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr expression() {
    return assignment();
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(peek(), message);
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expected ')' after expression");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expected expression");
  }

  /* Advance (that is, discard tokens without creating expressions)
   * until we arrive at the next statement.
   */
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expected ';' after expression.");
    return new Stmt.Expression(expr);
  }

  private Stmt printStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expected ';' after value.");
    return new Stmt.Print(expr);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expected '}' after block.");
    return statements;
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expected ')' after if condition.");
    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expected ')' after condition.");
    Stmt body = statement();
    return new Stmt.While(condition, body);
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'for'.");

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expected ';' after loop condition.");

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expected ')' after for clauses.");

    Stmt body = statement();
    if (increment != null) {
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }

    if (condition == null) {
      condition = new Expr.Literal(true);
    }
    body = new Stmt.While(condition, body);

    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private Stmt statement() {
    if (match(IF)) return ifStatement();
    if (match(FOR)) return forStatement();
    if (match(PRINT)) return printStatement();
    if (match(WHILE)) return whileStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());
    return expressionStatement();
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expected variable name.");
    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expected ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt declaration() {
    try {
      if (match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }
}
