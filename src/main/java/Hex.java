import java.math.BigInteger;
import java.util.Objects;

//Code taken from https://github.com/nbaars/pwnedpasswords4j/blob/main/pwnedpasswords4j-client/src/main/java/com/github/nbaars/pwnedpasswords4j/client/Hex.java
//in order to change that the password won't be saved as String in memory
class Hex {

    private String hex;

    private Hex(String s) {
        this.hex = s.toUpperCase();
    }

    public static Hex from(byte[] b) {
        return new Hex(new BigInteger(1, b).toString(16));
    }

    public static Hex from(String s) {
        return new Hex(s);
    }

    public Hex firstFive() {
        return new Hex(hex.substring(0, 5));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hex hex1 = (Hex) o;
        return Objects.equals(hex, hex1.hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hex);
    }

    @Override
    public String toString() {
        return hex;
    }
}