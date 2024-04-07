package entity;

public class ConfEntity {
    private String className;
    private String confName;

    public ConfEntity(String className, String confName) {
        this.className = className;
        this.confName = confName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getConfName() {
        return confName;
    }

    public void setConfName(String confName) {
        this.confName = confName;
    }
    @Override
    public String toString() {
        return className + " : " + confName;
    }
}
