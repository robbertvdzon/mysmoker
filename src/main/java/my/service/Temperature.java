package my.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

@DynamoDBDocument
public class Temperature {
    private long t = 0;
    private double bt = 0; // smoker temp
    private double mt = 0; // meat temp
    private double f = 0; // fan
    private double bs = 0; // smokertempset

    public Temperature() {
    }

    public Temperature(long datetime, double bt, double mt, double f, double bs) {
        this.t = datetime;
        this.bt = bt;
        this.mt = mt;
        this.f = f;
        this.bs = bs;
    }

    public long getT() {
        return t;
    }

    public void setT(long t) {
        this.t = t;
    }

    public double getBt() {
        return bt;
    }

    public void setBt(double bt) {
        this.bt = bt;
    }

    public double getMt() {
        return mt;
    }

    public void setMt(double mt) {
        this.mt = mt;
    }

    public double getF() {
        return f;
    }

    public void setF(double f) {
        this.f = f;
    }

    public double getBs() {
        return bs;
    }

    public void setBs(double bs) {
        this.bs = bs;
    }
}
