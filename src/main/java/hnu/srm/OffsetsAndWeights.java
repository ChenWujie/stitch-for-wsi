package hnu.srm;

public class OffsetsAndWeights {
    MyPosition[][][] offsets;
    int[][][] weights;
    String[][] fileNames;

    public OffsetsAndWeights(MyPosition[][][] offsets, int[][][] weights, String[][] fileNames) {
        this.offsets = offsets;
        this.weights = weights;
        this.fileNames = fileNames;
    }

    @Override
    public String toString() {
        int yNums=this.offsets.length, xNums=this.offsets[0].length;
        String offset="", weight="";
        for(int r=0; r<yNums; r++) {
            offset += "\t\t";
            weight += "\t\t";
            for(int c=0; c<xNums; c++) {
                if(this.offsets[r][c][0]!=null)
                offset += this.offsets[r][c][0].toString() + "\t\t";
                weight += this.weights[r][c][0] + "\t\t";
            }
            offset += "\n";
            weight += "\n";
            for(int c=0; c<xNums; c++) {
                if(this.offsets[r][c][1]!=null)
                offset += this.offsets[r][c][1].toString() + "\t\t";
                weight += this.weights[r][c][1] + "\t\t";
            }
            offset += "\n";
            weight += "\n";
        }
        return offset + "\n" + weight;
    }
}
