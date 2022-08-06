package share.shiguri.code.rtree;

import java.util.Arrays;
import java.util.StringJoiner;

public class Point {
    private final double[] data;

    private Point(double[] data) {
        this.data = data;
    }

    public static Point create(double[] data) {
        if (null == data || data.length < 2) {
            throw new IllegalArgumentException("dimension must more than 1");
        }

        double[] coordinate = Arrays.copyOf(data, data.length);
        return new Point(coordinate);
    }

    /**
     * @return int dimension of Point
     */
    public int dimension() {
        return data.length;
    }

    /**
     *
     * @param dimension (1 ~ maxDimension)
     * @return double value on the dimension
     */
    public double getValueOfDimension(int dimension) {
        if (dimension > dimension() || dimension < 1) {
            throw new RuntimeException("dimension not exist");
        } else {
            // dimension start from 1 not 0.
            return this.data[dimension - 1];
        }
    }

    /**
     * @param index (0 ~ length - 1)
     * @return double value of coordinate on index
     */
    public double getValueOfIndex(int index) {
        if (index > dimension() - 1 || index < 0) {
            throw new RuntimeException("index not correct");
        } else {
            return this.data[index];
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Point) {
            Point other = (Point) obj;
            if (other.dimension() == this.dimension()) {
                for (int index = 0; index < this.data.length; index++) {
                    if (other.data[index] != this.data[index]) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(",", "Point: [", "]");
        for (double coordinate : this.data) {
            stringJoiner.add(String.valueOf(coordinate));
        }
        return stringJoiner.toString();
    }

    @Override
    protected Point clone() throws CloneNotSupportedException {
        return Point.create(this.data);
    }
}
