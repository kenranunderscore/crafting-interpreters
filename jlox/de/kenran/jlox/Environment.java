package de.kenran.jlox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  private final Map<String, Object> values = new HashMap<>();

  void define(String name, Object value) {
    this.values.put(name, value);
  }

  Object get(Token name) {
    if (this.values.containsKey(name.lexeme)) {
      return this.values.get(name.lexeme);
    }
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (this.values.containsKey(name.lexeme)) {
      this.values.put(name.lexeme, value);
      return;
    }
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }
}
