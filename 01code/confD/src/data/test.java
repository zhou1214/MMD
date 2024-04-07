package data;

public class test {

    public static void main(String[] args){
        int a =2;
        int b =4;
        int c = add(a,b);
        int d = sub(c,a);
        if (c > b){
            d = c *a;
            System.out.println(d);
        }

        else{
            c = d* a;
            System.out.println(c);
        }

    }
    public static int add(int a,int b){
        return a+b;
    }
    public static int sub(int a,int b){
        return a-b;
    }
    public static int mul(int a,int b){
        return a*b;
    }

}
