//Zahidullah
package PlainEnglish;

// TextManager wraps a source string and provides character-by-character access
public class TextManager {

    // The source text that never changes
    private final String text;

    // Current position in the text (0-based)
    private int position;

    // Constructor: sets up the text manager with the source input
    public TextManager(String input) {
        this.text     = input;
        this.position = 0;
    }

    // Returns true if all characters have been consumed
    public boolean isAtEnd() {
        return position >= text.length();
    }

    // Returns true if looking ahead by offset goes past the end
    public boolean isAtEnd(int offset) {
        return (position + offset) >= text.length();
    }

    // Returns the current character without consuming it
    public char peek() {
        return text.charAt(position);
    }

    // Returns the character at the given offset ahead without consuming it
    public char peek(int offset) {
        return text.charAt(position + offset);
    }

    // Consumes and returns the current character, moving position forward
    public char advance() {
        return text.charAt(position++);
    }
}