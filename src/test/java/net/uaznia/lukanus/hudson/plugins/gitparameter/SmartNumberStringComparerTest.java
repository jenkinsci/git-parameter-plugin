package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import org.junit.jupiter.api.Test;

class SmartNumberStringComparerTest {

    @Test
    void testSmartNumberStringComparerWorksWithSameNumberComponents() {
        Comparator<String> comparer = new SmartNumberStringComparer();
        assertTrue(comparer.compare("v_1.1.0.2", "v_1.1.1.1") < 0);
        assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.1.1") == 0);
        assertTrue(comparer.compare("v_1.1.1.1", "v_2.0.0.0") < 0);
        assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.1.0") > 0);
    }

    @Test
    void testSmartNumberStringComparerVeryLongReleaseNumber() {
        Comparator<String> comparer = new SmartNumberStringComparer();
        assertTrue(comparer.compare("v_1.1.20150122112449123456789.1", "v_1.1.20150122112449123456788.1") > 0);
        assertTrue(comparer.compare("v_1.1.20150122112449123456789.1", "v_1.1.20150122112449123456789.1") == 0);
        assertTrue(comparer.compare("v_1.1.20150122112449123456789.1", "v_1.1.20150122112449223456789.1") < 0);
    }

    @Test
    void testSmartNumberStringComparerWorksWithDifferentNumberComponents() {
        Comparator<String> comparer = new SmartNumberStringComparer();
        assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.0") > 0);
        assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.2") < 0);
        assertTrue(comparer.compare("v_1", "v_2.0.0.0") < 0);
    }
}
