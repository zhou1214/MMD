package entity;

import java.util.*;
import Utils.Utils;

public class ConfEntity {
    private final String className;
    private final String confName;
//    private final String assignMethod;
//    private final String seed;
    private String assignMethod = null;
    private final boolean isStatic;
    private String type = null;

    public ConfEntity(String className,String confName,boolean isStatic) {
        this.className = className;
        this.confName = confName;
        this.isStatic = isStatic;
    }
    public ConfEntity(String className,String confName,String assignMethod,boolean isStatic){
        this.className = className;
        this.confName = confName;
        this.assignMethod = assignMethod;
        this.isStatic = isStatic;
    }

    public String getClassName(){
        return this.className;
    }
    public String getConfName(){
        return this.confName;
    }
    public String getAssignMethod(){
        return this.assignMethod;
    }
    public boolean getIsStatic(){
        return this.isStatic;
    }
    public String getType(){
        return this.type;
    }
    public void  setType(String type){
        this.type = type;
    }
    public String getFullConfName(){
        return this.className+"."+this.confName;
    }

    public boolean equalsWithEntity(Object obj){
        if (obj instanceof ConfEntity){
            ConfEntity entity = (ConfEntity) obj;
            if (entity.className.equals(this.className) &&
                    entity.confName.equals(this.confName) &&
                    entity.isStatic == this.isStatic)
                return true;
        }
        return false;
    }

    public String allConf(){
        return this.className + ":"+this.confName+"@"+this.type+",static:"+this.isStatic;
    }

    @Override
    public String toString() {
        return className + " : " + confName + " @ "
                + ", " + type + ", static: " + isStatic;
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ConfEntity) {
            ConfEntity e = (ConfEntity)obj;
            return e.className.equals(this.className)
                    && e.confName.equals(this.confName)
//			    && e.type.equals(this.type)
                    && e.isStatic == this.isStatic;
        }
        return false;
    }

}
