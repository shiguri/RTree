package share.shiguri.code.rtree;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RTree {
    private RTNode root;
    private int rTreeType;
    // 节点容量
    private int nodeCapacity = -1;
    // 节点填充因子，用于计算每个节点的最小条目数
    private double fillFactor = -1;
    private int dimension;

    public RTree(int nodeCapacity, int rTreeType, double fillFactor, int dimension) {
        this.rTreeType = rTreeType;
        this.nodeCapacity = nodeCapacity;
        this.fillFactor = fillFactor;
        this.dimension = dimension;
        this.root = new RTLeafNode(this, null);
    }

    public int getNodeCapacity() {
        return this.nodeCapacity;
    }

    public int getDimension() {
        return this.dimension;
    }

    public double getFillFactor() {
        return this.fillFactor;
    }

    public int getTreeType() {
        return this.rTreeType;
    }

    public void setRoot(RTNode root) {
        this.root = root;
    }

    public RTNode getRoot() {return this.root;}

    public boolean insert(MaximumBoundingBox mbb) {
        if (mbb == null) {
            throw new IllegalArgumentException("can not insert null");
        }

        if (mbb.getLeftBottomPoint().dimension() != this.getDimension()) {
            throw new IllegalArgumentException("dimension not equal");
        }

        RTLeafNode leaf = root.chooseLeaf(mbb);

        return leaf.insert(mbb);
    }

    public int delete(MaximumBoundingBox mbb) {
        if (mbb == null) {
            throw new IllegalArgumentException("can not insert null");
        }

        if (mbb.getLeftBottomPoint().dimension() != this.getDimension()) {
            throw new IllegalArgumentException("dimension not equal");
        }

        RTLeafNode leaf = root.findLeaf(mbb);
        if (leaf != null) {
            return leaf.delete(mbb);
        }

        return -1;
    }

    public List<RTNode> traversePostOrder(RTNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root can't be null");
        }

        List<RTNode> list = new ArrayList<>();
        list.add(root);
        if (!root.isLeaf()) {
            RTIndexNode target = (RTIndexNode) root;
            for (int index = 0; index < target.usedCount; index++) {
                RTNode child = target.getChild(index);
                List<RTNode> nodeList = traversePostOrder(child);
                list.addAll(nodeList);
            }
        }

        return list;
    }
}
