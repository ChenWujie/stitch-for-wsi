package hnu.srm;

public class PositionAndWeight {
    MyPosition myPosition;
    int weight;
    double rmse;

    public PositionAndWeight(MyPosition myPosition, int weight, double rmse) {
        this.myPosition = myPosition;
        this.weight = weight;
        this.rmse = rmse;
    }
}
