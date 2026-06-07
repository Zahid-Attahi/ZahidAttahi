//Zahidullah
package PlainEnglish;

import java.util.*;

// Lexer converts raw source code into a list of tokens
public class Lexer {

    // Reads characters from the source input
    private final TextManager tm;

    // Tracks the current line number
    private int line;

    // Tracks the current column number
    private int column;

    // Stack to track indentation levels
    private final Stack<Integer> indentStack;

    // Maps keyword strings to their token types
    private static final Map<String, Token.TokenTypes> KEYWORDS = new HashMap<>();

    // Register all reserved keywords
    static {
        KEYWORDS.put("to",    Token.TokenTypes.TO);
        KEYWORDS.put("a",     Token.TokenTypes.A);
        KEYWORDS.put("with",  Token.TokenTypes.WITH);
        KEYWORDS.put("named", Token.TokenTypes.NAMED);
        KEYWORDS.put("an",    Token.TokenTypes.AN);
        KEYWORDS.put("is",    Token.TokenTypes.IS);
        KEYWORDS.put("if",    Token.TokenTypes.IF);
        KEYWORDS.put("else",  Token.TokenTypes.ELSE);
        KEYWORDS.put("loop",  Token.TokenTypes.LOOP);
        KEYWORDS.put("set",   Token.TokenTypes.SET);
        KEYWORDS.put("make",  Token.TokenTypes.MAKE);
        KEYWORDS.put("of",    Token.TokenTypes.OF);
        KEYWORDS.put("true",  Token.TokenTypes.TRUE);
        KEYWORDS.put("false", Token.TokenTypes.FALSE);
        KEYWORDS.put("and",   Token.TokenTypes.AND);
        KEYWORDS.put("or",    Token.TokenTypes.OR);
        KEYWORDS.put("not",   Token.TokenTypes.NOT);
    }

    // Constructor: sets up the lexer with the source input
    public Lexer(String input) {
        tm          = new TextManager(input);
        line        = 1;
        column      = 1;
        indentStack = new Stack<>();
        // Base indentation level is 0
        indentStack.push(0);
    }

    // Main method: reads all characters and returns the full token list
    public LinkedList<Token> lex() throws SyntaxErrorException {
        LinkedList<Token> tokens = new LinkedList<>();

        // Loop through every character in the input
        while (!tm.isAtEnd()) {
            char c = tm.peek();

            // Skip carriage return characters
            if (c == '\r') {
                tm.advance();
                continue;
            }

            // Handle newline: emit NEWLINE token and process indentation
            if (c == '\n') {
                tokens.add(new Token(Token.TokenTypes.NEWLINE, line, column));
                tm.advance();
                line++;
                column = 1;
                processIndentation(tokens);
                continue;
            }

            // Skip spaces and tabs in the middle of a line
            if (c == ' ' || c == '\t') {
                advanceColumn();
                continue;
            }

            // Skip block comments that start with /*
            if (c == '/' && !tm.isAtEnd(1) && tm.peek(1) == '*') {
                skipComment();
                continue;
            }

            // Read a string literal starting with "
            if (c == '"') {
                tokens.add(readString());
                continue;
            }

            // Read a number if the character is a digit
            if (Character.isDigit(c)) {
                tokens.add(readNumber());
                continue;
            }

            // Read an identifier or keyword if the character is a letter or underscore
            if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifier());
                continue;
            }

            // Read a punctuation or operator token
            tokens.add(readPunctuation());
        }

        // At end of file, close all open indent levels with DEDENT tokens
        while (indentStack.size() > 1) {
            indentStack.pop();
            tokens.add(new Token(Token.TokenTypes.DEDENT, line, column));
        }
        // Add final NEWLINE at end of file
        tokens.add(new Token(Token.TokenTypes.NEWLINE, line, column));

        return tokens;
    }

    // Consumes one character and updates the column counter
    private char advanceColumn() {
        char c = tm.advance();
        // Tabs count as 4 spaces
        if (c == '\t') {
            column += 4;
        } else {
            column++;
        }
        return c;
    }

    // Handles indentation at the start of each new line
    private void processIndentation(LinkedList<Token> tokens) throws SyntaxErrorException {

        while (true) {
            // Count leading spaces on this line
            int spaces = 0;
            while (!tm.isAtEnd() && (tm.peek() == ' ' || tm.peek() == '\t')) {
                char ws = tm.advance();
                // Tab counts as 4 spaces
                if (ws == '\t') {
                    spaces += 4;
                    column += 4;
                } else {
                    spaces++;
                    column++;
                }
            }

            // If we hit end of file, close all open indent levels
            if (tm.isAtEnd()) {
                while (indentStack.size() > 1) {
                    indentStack.pop();
                    tokens.add(new Token(Token.TokenTypes.DEDENT, line, column));
                }
                return;
            }

            // If this is a blank line, emit NEWLINE and loop to next line
            if (tm.peek() == '\n' || tm.peek() == '\r') {
                tokens.add(new Token(Token.TokenTypes.NEWLINE, line, column));
                // Skip carriage return if present
                if (tm.peek() == '\r') tm.advance();
                tm.advance();
                line++;
                column = 1;
                continue;
            }

            // Indentation must be a multiple of 4
            if (spaces % 4 != 0) {
                throw new SyntaxErrorException(
                        "Indentation must be a multiple of 4 spaces, got " + spaces,
                        line, column);
            }

            // Get the current indentation level
            int currentIndent = indentStack.peek();

            // If indentation increased, emit INDENT tokens
            if (spaces > currentIndent) {
                while (indentStack.peek() < spaces) {
                    indentStack.push(indentStack.peek() + 4);
                    tokens.add(new Token(Token.TokenTypes.INDENT, line, column));
                }
            // If indentation decreased, emit DEDENT tokens
            } else if (spaces < currentIndent) {
                while (indentStack.peek() > spaces) {
                    indentStack.pop();
                    tokens.add(new Token(Token.TokenTypes.DEDENT, line, column));
                }
                // Make sure the new level matches a previous indent level
                if (indentStack.peek() != spaces) {
                    throw new SyntaxErrorException(
                            "Indentation does not match any enclosing block.",
                            line, column);
                }
            }
            // Done processing this line's indentation
            return;
        }
    }

    // Skips over a block comment /* ... */
    private void skipComment() throws SyntaxErrorException {
        int startLine = line;
        int startCol  = column;

        // Consume the opening / and *
        advanceColumn();
        advanceColumn();

        // Keep reading until we find the closing */
        while (!tm.isAtEnd()) {
            char c = tm.advance();
            // Track newlines inside comments
            if (c == '\n') {
                line++;
                column = 1;
            // Found closing */ so stop
            } else if (c == '*' && !tm.isAtEnd() && tm.peek() == '/') {
                tm.advance();
                column += 2;
                return;
            } else {
                column++;
            }
        }

        // If we reach here, the comment was never closed
        throw new SyntaxErrorException("Unterminated block comment", startLine, startCol);
    }

    // Reads a double-quoted string literal
    private Token readString() throws SyntaxErrorException {
        int startLine = line;

        // Consume the opening quote
        tm.advance();
        column++;
        int startCol = column;

        StringBuilder sb = new StringBuilder();

        // Read characters until the closing quote
        while (!tm.isAtEnd()) {
            char c = tm.advance();

            // Closing quote found, return the token
            if (c == '"') {
                column++;
                return new Token(Token.TokenTypes.STRINGLITERAL, startLine, startCol, sb.toString());
            }

            // Handle escape sequences like \n, \", \\, \t
            if (c == '\\' && !tm.isAtEnd()) {
                char esc = tm.advance();
                column += 2;
                switch (esc) {
                    case 'n':  sb.append('\n'); break;
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 't':  sb.append('\t'); break;
                    // Unknown escape: keep as-is
                    default:
                        sb.append('\\');
                        sb.append(esc);
                }
                continue;
            }

            // Track newlines inside strings
            if (c == '\n') {
                sb.append(c);
                line++;
                column = 1;
            } else {
                sb.append(c);
                column++;
            }
        }

        // String was never closed
        throw new SyntaxErrorException("Unterminated string literal", startLine, startCol);
    }

    // Reads a numeric literal (integer or decimal)
    private Token readNumber() {
        // Consume the first digit
        char first = tm.advance();
        column++;
        int startCol = column;

        StringBuilder sb = new StringBuilder();
        sb.append(first);

        // Keep reading digits
        while (!tm.isAtEnd() && Character.isDigit(tm.peek())) {
            sb.append(advanceColumn());
        }

        // Check for a decimal point followed by more digits
        if (!tm.isAtEnd() && tm.peek() == '.' && !tm.isAtEnd(1) && Character.isDigit(tm.peek(1))) {
            // Consume the dot
            sb.append(advanceColumn());
            // Consume the decimal digits
            while (!tm.isAtEnd() && Character.isDigit(tm.peek())) {
                sb.append(advanceColumn());
            }
        }

        return new Token(Token.TokenTypes.NUMBER, line, startCol, sb.toString());
    }

    // Reads an identifier or keyword
    private Token readIdentifier() {
        // Consume the first character
        char first = tm.advance();
        column++;
        int startCol = column;

        StringBuilder sb = new StringBuilder();
        sb.append(first);

        // Keep reading letters, digits, and underscores
        while (!tm.isAtEnd() && (Character.isLetterOrDigit(tm.peek()) || tm.peek() == '_')) {
            sb.append(advanceColumn());
        }

        String word = sb.toString();
        // Check if this word is a keyword (case-insensitive)
        Token.TokenTypes type = KEYWORDS.get(word.toLowerCase());

        // Return keyword token or identifier token
        if (type != null) {
            return new Token(type, line, startCol);
        }
        return new Token(Token.TokenTypes.IDENTIFIER, line, startCol, word);
    }

    // Reads a punctuation or operator token
    private Token readPunctuation() throws SyntaxErrorException {
        // Consume the character
        char c = tm.advance();
        column++;
        int startCol = column;

        switch (c) {
            case ',': return new Token(Token.TokenTypes.COMMA,      line, startCol);
            case '+': return new Token(Token.TokenTypes.PLUS,       line, startCol);
            case '-': return new Token(Token.TokenTypes.HYPHEN,     line, startCol);
            case '*': return new Token(Token.TokenTypes.ASTERISK,   line, startCol);
            case '/': return new Token(Token.TokenTypes.SLASH,      line, startCol);
            case '%': return new Token(Token.TokenTypes.PERCENT,    line, startCol);
            case '(': return new Token(Token.TokenTypes.OPENPAREN,  line, startCol);
            case ')': return new Token(Token.TokenTypes.CLOSEPAREN, line, startCol);

            case '=':
                // Check for == operator
                if (!tm.isAtEnd() && tm.peek() == '=') {
                    advanceColumn();
                    return new Token(Token.TokenTypes.DOUBLEEQUAL, line, startCol);
                }
                throw new SyntaxErrorException("Unexpected '='", line, startCol);

            case '!':
                // Check for != operator
                if (!tm.isAtEnd() && tm.peek() == '=') {
                    advanceColumn();
                    return new Token(Token.TokenTypes.NOTEQUAL, line, startCol);
                }
                throw new SyntaxErrorException("Unexpected '!'", line, startCol);

            case '>':
                // Check for >= operator
                if (!tm.isAtEnd() && tm.peek() == '=') {
                    advanceColumn();
                    return new Token(Token.TokenTypes.GREATERTHANEQUAL, line, startCol);
                }
                return new Token(Token.TokenTypes.GREATERTHAN, line, startCol);

            case '<':
                // Check for <= operator
                if (!tm.isAtEnd() && tm.peek() == '=') {
                    advanceColumn();
                    return new Token(Token.TokenTypes.LESSTHANEQUAL, line, startCol);
                }
                return new Token(Token.TokenTypes.LESSTHAN, line, startCol);

            // Unknown character
            default:
                throw new SyntaxErrorException("Unexpected character: '" + c + "'", line, startCol);
        }
    }
}