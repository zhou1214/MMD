package entity;

public class OutputCount implements  Comparable<OutputCount>{
    public ConfEntity conf;
    public int count = 0;
    public double ratio = 0.0;
    public OutputCount(ConfEntity conf){
        this.conf = conf;
    }

    public ConfEntity getConf(){
        return this.conf;
    }
    public int getCount(){
        return this.count;
    }
    public double getRatio(){
        return this.ratio;
    }
    public void setCount(int count){
        this.count = this.count + count;
    }
    public void setRatio(double ra){
        this.ratio = ra;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    conf:");
        sb.append(this.conf);
//        sb.append("    count:");
//        sb.append(this.count);
        sb.append("    ratio:");
        sb.append(this.ratio);

        return sb.toString();
    }
    @Override
    public int compareTo(OutputCount o){
        if (this.ratio<o.ratio)
            return 1;
        else
            return -1;
    }
}
