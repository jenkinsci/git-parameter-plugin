package net.uaznia.lukanus.hudson.plugins.gitparameter;

enum SortMode {
    NONE, ASCENDING_SMART, DESCENDING_SMART, ASCENDING, DESCENDING;

    public boolean getIsUsingSmartSort() {
        return this == ASCENDING_SMART || this == DESCENDING_SMART;
    }

    public boolean getIsDescending() {
        return this == DESCENDING || this == DESCENDING_SMART;
    }

    public boolean getIsSorting() {
        return this != NONE;
    }
}
