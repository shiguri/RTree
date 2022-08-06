package share.shiguri.code.rtree;

public class MaximumBoundingBox {
    private final Point leftBottom;
    private final Point rightTop;

    private MaximumBoundingBox(Point leftBottom, Point rightTop) {
        this.leftBottom = leftBottom;
        this.rightTop = rightTop;
    }

    public static MaximumBoundingBox create(Point leftBottom, Point rightTop) {
        try {
            if (leftBottom != null && rightTop != null && leftBottom.dimension() == rightTop.dimension()) {
                return new MaximumBoundingBox(leftBottom.clone(), rightTop.clone());
            } else {
                throw new IllegalArgumentException("leftBottom or rightTop is invalid or their dimension don't equal");
            }
        }catch (CloneNotSupportedException e) {
            // clone is always supported
            return null;
        }
    }

    public Point getLeftBottomPoint() {
        try {
            return leftBottom.clone();
        }catch (CloneNotSupportedException e) {
            // clone is always supported
            return null;
        }
    }

    public Point getRightTopPoint() {
        try {
            return rightTop.clone();
        }catch (CloneNotSupportedException e) {
            // clone is always supported
            return null;
        }
    }

    /**
     * 返回一个MBB的维度
     * @return int 维度
     */
    public int dimension() {
        return leftBottom.dimension();
    }

    /**
     * 当前MBB与一个同纬度的MBB合并成一个新的MBB
     * @param other MaximumBoundingBox
     * @return MaximumBoundingBox
     */
    public MaximumBoundingBox unionAsMaximumBoundingBox(MaximumBoundingBox other) {
        if (other == null) {
            throw new IllegalArgumentException("maximumBoundingBox can't be null");
        }
        if (other.dimension() != this.dimension()) {
            throw new IllegalArgumentException("maximumBoundingBoxes' dimension must equal");
        }

        double[] dataOfLeftBottom = new double[this.dimension()];
        double[] dataOfRightTop = new double[this.dimension()];

        for (int index = 0; index < dimension(); index++) {
            dataOfLeftBottom[index] =
                    Math.max(this.leftBottom.getValueOfIndex(index), other.leftBottom.getValueOfIndex(index));
            dataOfRightTop[index] =
                    Math.max(this.rightTop.getValueOfIndex(index), other.rightTop.getValueOfIndex(index));
        }

        Point newLeftBottom = Point.create(dataOfLeftBottom);
        Point newRightTop = Point.create(dataOfRightTop);

        return MaximumBoundingBox.create(newLeftBottom, newRightTop);
    }

    /**
     * 返回面积
     * @return double 面积
     */
    public double getArea() {
        double area = 1d;
        for (int index = 0; index < dimension(); index++) {
            area *= rightTop.getValueOfIndex(index) - leftBottom.getValueOfIndex(index);
        }
        return area;
    }

    /**
     * 返回当前对象是否与给定的MBB相交
     * @param other MaximumBoundingBox
     * @return boolean
     */
    public boolean isIntersection(MaximumBoundingBox other) {
        if (other == null) {
            throw new IllegalArgumentException("maximumBoundingBox can't be null");
        }
        if (other.dimension() != this.dimension()) {
            throw new IllegalArgumentException("two maximumBoundingBoxes' dimension don't equal");
        }

        for (int index = 0; index < this.dimension(); index++) {
            if (other.leftBottom.getValueOfIndex(index) > this.rightTop.getValueOfIndex(index) ||
            other.rightTop.getValueOfIndex(index) < this.leftBottom.getValueOfIndex(index)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 返回当前对象与给定MBB相交区域的面积
     * @param other MBB
     * @return double
     */
    public double getIntersectionArea(MaximumBoundingBox other) {
        if (!isIntersection(other)) {
            return 0.0;
        }

        double area = 1.0;
        for (int index = 0; index < this.dimension(); index++) {
            double left1 = this.leftBottom.getValueOfIndex(index);
            double right1 = this.rightTop.getValueOfIndex(index);
            double left2 = other.leftBottom.getValueOfIndex(index);
            double right2 = other.rightTop.getValueOfIndex(index);

            // this在other的左边
            if (left1 <= left2 && right1 <= right2) {
                area *= (right1 - left1) - (left2 - left1);
            }
            // this在other的右边
            else if (left1 >= left2 && right1 >= right2) {
                area *= (right2 - left2) - (left1 - left2);
            }
            // this在other里面
            else if (left1 >= left2 && right1 <= right2) {
                area *= right1 - left1;
            }
            // this包含other
            else if (left1 <= left2 && right1 >= right2) {
                area *= right2 - left2;
            }
        }

        return area;
    }

    /**
     * 判断 other是否被this包含。
     * @param other MaximumBoundingBox
     * @return boolean
     */
    public boolean enclosure(MaximumBoundingBox other) {
        if (other == null) {
            throw new IllegalArgumentException("MaximumBoundingBox can't be null");
        }
        if (other.dimension() != this.dimension()) {
            throw new IllegalArgumentException("two maximumBoundingBoxes' dimension don't equal");
        }

        for (int index = 0; index < dimension(); index++) {
            if (other.leftBottom.getValueOfIndex(index) < this.leftBottom.getValueOfIndex(index)
            || other.rightTop.getValueOfIndex(index) > this.rightTop.getValueOfIndex(index)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MBB: {")
                .append("leftBottom:").append(leftBottom.toString())
                .append(",")
                .append("rightTop:").append(rightTop.toString())
                .append("}");
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }

        if (obj instanceof MaximumBoundingBox) {
            MaximumBoundingBox other = (MaximumBoundingBox) obj;
            return other.leftBottom.equals(this.leftBottom) && other.rightTop.equals(this.rightTop);
        }
        return false;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Point leftBottomClone = this.leftBottom.clone();
        Point rightBottomClone = this.rightTop.clone();
        return MaximumBoundingBox.create(leftBottomClone, rightBottomClone);
    }

    @Override
    public int hashCode() {
        int hashCode= 3;
        hashCode = 31 * hashCode + leftBottom.hashCode();
        hashCode = 31 * hashCode + rightTop.hashCode();
        return hashCode;
    }
}
