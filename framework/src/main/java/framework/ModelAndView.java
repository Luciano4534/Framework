package framework;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ModelAndView {
    private String view;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public ModelAndView(String view) {
        this.view = Objects.requireNonNull(view, "view");
    }

    public ModelAndView addAttribut(String key, Object value) {
        data.put(Objects.requireNonNull(key, "key"), value);
        return this;
    }

    public ModelAndView addAttribute(String key, Object value) {
        return addAttribut(key, value);
    }

    public String getView() {
        return view;
    }

    public void setview(String view) {
        this.view = Objects.requireNonNull(view, "view");
    }

    public void setView(String view) {
        setview(view);
    }

    public Map<String, Object> getData() {
        return data;
    }
}
