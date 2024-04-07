package entity;

import Utils.Sep;

public class AnalysisCount  implements  Comparable<AnalysisCount>{
    public ConfEntity conf;
    public int count = 0;

    public AnalysisCount(ConfEntity conf){
        this.conf = conf;
    }
    public void setCount(){
        this.count++;
    }
    public void setCount(int count){
        this.count = this.count + count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    conf:");
        sb.append(this.conf);
        sb.append("    count:");
        sb.append(this.count);

        return sb.toString();
    }
    @Override
    public int compareTo(AnalysisCount a){
        if (this.count<a.count)
            return 1;
        else
            return -1;
    }
}
