//Zahidullah
package PlainEnglish;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

// TokenManager wraps the token list and gives the parser safe access to tokens
public class TokenManager {

    // The list of tokens remaining to be consumed
    private final LinkedList<Token> tokens;

    // Constructor: takes the token list from the lexer
    public TokenManager(List<Token> tokens) {
        this.tokens = new LinkedList<>(tokens);
    }

    // Returns true if there are no more tokens left
    public boolean done() {
        return tokens.isEmpty();
    }

    // Returns the token type at the given offset ahead, or empty if out of bounds
    public Optional<Token.TokenTypes> peek(int offset) {
        if (offset >= tokens.size()) {
            return Optional.empty();
        }
        return Optional.of(tokens.get(offset).Type);
    }

    // Returns true if the next token matches the given type
    public boolean nextIs(Token.TokenTypes type) {
        return peek(0).map(t -> t == type).orElse(false);
    }

    // Returns true if the next two tokens match the two given types
    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        return peek(0).map(t -> t == first).orElse(false)
            && peek(1).map(t -> t == second).orElse(false);
    }

    // Returns the line number of the next token, or 0 if no tokens left
    public int getCurrentLine() {
        if (tokens.isEmpty()) return 0;
        return tokens.peek().LineNumber;
    }

    // Returns the column number of the next token, or 0 if no tokens left
    public int getCurrentColumnNumber() {
        if (tokens.isEmpty()) return 0;
        return tokens.peek().ColumnNumber;
    }

    // Consumes and returns the next token if it matches the given type, otherwise returns empty
    public Optional<Token> matchAndRemove(Token.TokenTypes type) {
        if (!tokens.isEmpty() && tokens.peek().Type == type) {
            return Optional.of(tokens.poll());
        }
        return Optional.empty();
    }
}