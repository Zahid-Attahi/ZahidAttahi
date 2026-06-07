# PlainEnglish Interpreter

A tree-walking interpreter for **PlainEnglish**, a custom programming language designed to read like natural English. Written entirely in Java — solo project.

## What It Does

PlainEnglish lets you write programs using English-like syntax saved in `.eng` files. The interpreter reads your source code, parses it into an abstract syntax tree, and executes it directly.

**Supported features:**
- Variables (`make`, `set`)
- Arithmetic (`+`, `-`, `*`, `/`, `%`)
- Boolean logic (`and`, `or`, `not`)
- Comparisons (`==`, `!=`, `<`, `>`, `<=`, `>=`)
- Conditionals (`if` / `else`)
- Loops
- Methods with parameters
- Custom types / objects with fields
- Built-in `Print` function

## Project Structure

| File | Role |
|------|------|
| `Lexer.java` | Tokenizes raw `.eng` source code |
| `PlainEnglishParser.java` | Builds an AST from tokens |
| `Interpreter.java` | Walks the AST and executes the program |

## How to Run

**Compile:**
```bash
javac PlainEnglish/*.java
```

**Run a `.eng` file:**
```bash
java PlainEnglish.Interpreter yourprogram.eng
```

You can pass multiple files at once — each will be executed in order.

## Example

A simple PlainEnglish program might look like:

```
Run
  Make x number
  Set x to 5
  If x is greater than 3
    Print "x is big"
```

## Built With

- Java
- Custom Lexer → Parser → Interpreter pipeline
- No external libraries

## Author

Built solo as a language implementation project — covers the full pipeline from raw text to execution.
