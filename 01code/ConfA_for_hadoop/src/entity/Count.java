package entity;

import Utils.Sep;

public class Count {
    public ConfEntity conf;

    public int count = 0;

    public Count(ConfEntity conf){
        this.conf = conf;
        this.count++;
    }



    public String getConf(){
        return this.conf.getConfName();
    }
    public int getCount(){
        return this.count;
    }
    public ConfEntity getConfEntity(){
        return this.conf;
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("    same conf:");
        sb.append(this.conf);
        sb.append("    each same count:");
        sb.append(this.count);

        return sb.toString();
    }
}
