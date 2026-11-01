package framework;

import java.lang.reflect.Method;
import java.util.Objects;

public final class Mapping {
    private final Class<?> type;
    private final Method method;

    public Mapping(Class<?> type, Method method) {
        this.type = Objects.requireNonNull(type, "type");
        this.method = Objects.requireNonNull(method, "method");
    }

    public Class<?> getType() {
        return type;
    }

    public Method getMethod() {
        return method;
    }
}
