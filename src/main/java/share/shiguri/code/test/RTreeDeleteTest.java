package share.shiguri.code.test;

import share.shiguri.code.rtree.Constants;
import share.shiguri.code.rtree.MaximumBoundingBox;
import share.shiguri.code.rtree.Point;
import share.shiguri.code.rtree.RTree;
import share.shiguri.code.util.RTreeUtil;

import java.util.Arrays;

public class RTreeDeleteTest {
    public static void main(String[] args) {
        RTree rTree = new RTree(3, Constants.RTREE_QUADRATIC, 0.4, 2);
        double [][] coordinates = initCoordinates();

        for (int i = 0; i < coordinates.length ; i++) {
            double[] pointVal = coordinates[i];
            Point pointLeft = Point.create(Arrays.copyOfRange(pointVal, 0, 2));
            Point pointRight = Point.create(Arrays.copyOfRange(pointVal, 2, 4));
            MaximumBoundingBox mbb = MaximumBoundingBox.create(pointLeft, pointRight);
            rTree.insert(mbb);
        }
        RTreeUtil.stdoutRTree(rTree);
        System.out.println("========= Delete =========");

        for (int i = 0; i < coordinates.length; i++) {
            double[] pointVal = coordinates[i];
            Point pointLeft = Point.create(Arrays.copyOfRange(pointVal, 0, 2));
            Point pointRight = Point.create(Arrays.copyOfRange(pointVal, 2, 4));
            MaximumBoundingBox mbb = MaximumBoundingBox.create(pointLeft, pointRight);
            System.out.println("Delete MBB : " + mbb.toString());

            rTree.delete(mbb);
            RTreeUtil.stdoutRTree(rTree);
            System.out.println();
        }

        System.out.println("============ end =============");
        RTreeUtil.stdoutRTree(rTree);
    }

    private static double[][] initCoordinates() {
        double[][] coordinates = new double[10][];
        // 每一个一维数组代表一个矩形。
        coordinates[0] = new double[]{5, 30, 25, 35};
        coordinates[1] = new double[]{15, 38, 23, 50};
        coordinates[2] = new double[]{10, 23, 30, 28};
        coordinates[3] = new double[]{13, 10, 18, 15};
        coordinates[4] = new double[]{23, 10, 28, 20};
        coordinates[5] = new double[]{28, 30, 33, 40};
        coordinates[6] = new double[]{38, 13, 43, 30};
        coordinates[7] = new double[]{35, 37, 40, 43};
        coordinates[8] = new double[]{45, 8, 50, 50};
        coordinates[9] = new double[]{23, 55, 28, 70};

        return coordinates;
    }
}
