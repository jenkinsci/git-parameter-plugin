package net.uaznia.lukanus.hudson.plugins.gitparameter;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;

/**
 * Compares strings but treats a sequence of digits as a single character.
 */
class SmartNumberStringComparer implements Comparator<String>, Serializable {

    /**
     * Gets the token starting at the given index. It will return the first
     * char if it is not a digit, otherwise it will return all consecutive
     * digits starting at index.
     *
     * @param str   The string to extract token from
     * @param index The start location
     */
    private String getToken(String str, int index) {
        char nextChar = str.charAt(index++);
        StringBuilder token =  new StringBuilder(String.valueOf(nextChar));

        // if the first char wasn't a digit then we're already done
        if (!Character.isDigit(nextChar))
            return token.toString();

        // the first char was a digit so continue until end of string or non
        // digit
        while (index < str.length()) {
            nextChar = str.charAt(index++);

            if (!Character.isDigit(nextChar))
                break;

            token.append(nextChar);
        }

        return token.toString();
    }

    /**
     * True if the string only contains digits
     */
    private boolean stringContainsInteger(String str) {
        for (int charIndex = 0; charIndex < str.length(); charIndex++) {
            if (!Character.isDigit(str.charAt(charIndex)))
                return false;
        }
        return true;
    }

    public int compare(String a, String b) {

        int aIndex = 0;
        int bIndex = 0;

        while (aIndex < a.length() && bIndex < b.length()) {
            String aToken = getToken(a, aIndex);
            String bToken = getToken(b, bIndex);
            int difference;

            if (stringContainsInteger(aToken)
                    && stringContainsInteger(bToken)) {
                BigInteger aInt = new BigInteger(aToken);
                BigInteger bInt = new BigInteger(bToken);
                difference = aInt.compareTo(bInt);
            } else {
                difference = aToken.compareTo(bToken);
            }

            if (difference != 0)
                return difference;

            aIndex += aToken.length();
            bIndex += bToken.length();
        }

        return Integer.compare(a.length(), b.length());
    }

}
