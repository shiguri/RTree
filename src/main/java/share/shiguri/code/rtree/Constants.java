package share.shiguri.code.rtree;

public class Constants {
    public static final int MAX_NUMBER_OF_ENTRIES_IN_NODE = 20; // 节点中最大条目数目
    public static final int MIN_NUMBER_OF_ENTRIES_IN_NODE = 8;  // 节点中最小条目数目
    public static final int RT_LEAF_NODE_DIMENSION = 2;

    // 树的类型常量
    public static final int RTREE_LINEAR = 0; // 线性
    public static final int RTREE_QUADRATIC = 1; // 二维
    public static final int RTREE_EXPONENTIAL = 2; // 多维
    public static final int RSTAR = 3; // 星型

    public static final int NIL = -1;
}
