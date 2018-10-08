package net.uaznia.lukanus.hudson.plugins.gitparameter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.DEFAULT_VALUE;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.PT_REVISION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SortTest {
    @Test
    public void testSortTagsYieldsCorrectOrderWithSmartSortEnabled() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", null, null, SortMode.ASCENDING_SMART, SelectedValue.NONE, null, false);
        Set<String> tags = new HashSet<String>();
        tags.add("v_1.0.0.2");
        tags.add("v_1.0.0.5");
        tags.add("v_1.0.1.1");
        tags.add("v_1.0.0.0");
        tags.add("v_1.0.0.10");

        ArrayList<String> orderedTags = instance.sortByName(tags);

        assertEquals("v_1.0.0.0", orderedTags.get(0));
        assertEquals("v_1.0.0.2", orderedTags.get(1));
        assertEquals("v_1.0.0.5", orderedTags.get(2));
        assertEquals("v_1.0.0.10", orderedTags.get(3));
        assertEquals("v_1.0.1.1", orderedTags.get(4));
    }

    @Test
    public void testSortTagsYieldsCorrectOrderWithSmartSortDisabled() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", null, null, SortMode.ASCENDING, SelectedValue.NONE, null, false);
        Set<String> tags = new HashSet<String>();
        tags.add("v_1.0.0.2");
        tags.add("v_1.0.0.5");
        tags.add("v_1.0.1.1");
        tags.add("v_1.0.0.0");
        tags.add("v_1.0.0.10");

        ArrayList<String> orderedTags = instance.sortByName(tags);

        assertEquals("v_1.0.0.0", orderedTags.get(0));
        assertEquals("v_1.0.0.10", orderedTags.get(1));
        assertEquals("v_1.0.0.2", orderedTags.get(2));
        assertEquals("v_1.0.0.5", orderedTags.get(3));
        assertEquals("v_1.0.1.1", orderedTags.get(4));
    }

    @Test
    public void testSortMode_getIsUsingSmartSort() {
        assertFalse(SortMode.NONE.getIsUsingSmartSort());
        assertFalse(SortMode.ASCENDING.getIsUsingSmartSort());
        assertTrue(SortMode.ASCENDING_SMART.getIsUsingSmartSort());
        assertFalse(SortMode.DESCENDING.getIsUsingSmartSort());
        assertTrue(SortMode.DESCENDING_SMART.getIsUsingSmartSort());
    }

    @Test
    public void testSortMode_getIsDescending() {
        assertFalse(SortMode.NONE.getIsDescending());
        assertFalse(SortMode.ASCENDING.getIsDescending());
        assertFalse(SortMode.ASCENDING_SMART.getIsDescending());
        assertTrue(SortMode.DESCENDING.getIsDescending());
        assertTrue(SortMode.DESCENDING_SMART.getIsDescending());
    }

    @Test
    public void testSortMode_getIsSorting() {
        assertFalse(SortMode.NONE.getIsSorting());
        assertTrue(SortMode.ASCENDING.getIsSorting());
        assertTrue(SortMode.ASCENDING_SMART.getIsSorting());
        assertTrue(SortMode.DESCENDING.getIsSorting());
        assertTrue(SortMode.DESCENDING_SMART.getIsSorting());
    }
}
