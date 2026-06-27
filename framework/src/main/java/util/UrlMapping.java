package util;

/**
 * Represents the mapping details of a URL to a specific controller class and method name.
 */
public class UrlMapping {
    private String className;
    private String methodName;

    /**
     * Default constructor.
     */
    public UrlMapping() {
    }

    /**
     * Parameterized constructor.
     *
     * @param className  the fully qualified name of the controller class
     * @param methodName the name of the method to execute
     */
    public UrlMapping(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        return "UrlMapping{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
