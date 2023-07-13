package net.uaznia.lukanus.hudson.plugins.gitparameter.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import jenkins.security.stapler.StaplerAccessibleType;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.export.Flavor;

@ExportedBean(defaultVisibility = 4)
@StaplerAccessibleType
public class ItemsErrorModel implements HttpResponse {
    public static final ItemsErrorModel EMPTY = new ItemsErrorModel();

    @ExportedBean(defaultVisibility = 999)
    public static final class Option {
        @Exported
        public String name;

        @Exported
        public String value;

        public Option(String name) {
            this(name, name);
        }

        public Option(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }

    @Exported
    public List<Option> values = new ArrayList<>();

    @Exported
    public List<String> errors = new ArrayList<>();

    public void add(String value) {
        values.add(new Option(value));
    }

    public void add(String name, String value) {
        values.add(new Option(name, value));
    }

    public int size() {
        return values.size();
    }

    public Option get(int index) {
        return values.get(index);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void writeTo(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.serveExposedBean(req, this, Flavor.JSON);
    }

    @Override
    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node)
            throws IOException, ServletException {
        writeTo(req, rsp);
    }

    public static ItemsErrorModel create(String value, String... errors) {
        ItemsErrorModel itemsErrorModel = new ItemsErrorModel();
        if (value != null) {
            itemsErrorModel.add(value);
        }
        for (String error : errors) {
            if (StringUtils.isNotEmpty(error)) {
                itemsErrorModel.addError(error.replaceAll("\\n", "<br>"));
            }
        }
        return itemsErrorModel;
    }
}
