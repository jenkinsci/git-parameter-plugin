package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;

public class UnsupportedJobType extends RuntimeException {
    public UnsupportedJobType(String message) {
        super(message);
    }
}
