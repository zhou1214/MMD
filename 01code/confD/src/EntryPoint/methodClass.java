package EntryPoint;

public class methodClass {
    private String classPath;
    private String methodName;

    public methodClass(String classPath, String methodName) {
        this.classPath = classPath;
        this.methodName = methodName;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getMethodName() {
        return methodName;
    }
}
