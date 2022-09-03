package share.shiguri.code.rtree;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName RTIndexNode
 * @Description 非叶子节点
 */
public class RTIndexNode extends RTNode{
    protected List<RTNode> children;

    public RTIndexNode(RTree rTree, RTNode parent, int level) {
        super(rTree, level, parent);
        children = new ArrayList<>();
    }

    public static RTIndexNode create(RTree rTree, RTNode parent, int level) {
        return new RTIndexNode(rTree, parent, level);
    }

    public RTNode getChild(int index){
        if (index < 0 || index > this.children.size()) {
            throw new IllegalArgumentException("index out of RTIndexTree Children size");
        }

        return this.children.get(index);
    }

    /**
     * <p>
     *     1. 计算各个子节点的条目与mbb的面积重叠之和，选重叠面积之和最小的子节点<br>
     *     2. 若有多个子节点都有最小重叠面积，选面积增量最小的子节点<br>
     *     3. 若多个子节点的面积增量都最小，选本身面积最小的子节点<br>
     * @param mbb
     * @return 最小重叠面积的子节点的索引。
     */
    private int findLeastOverlap(MaximumBoundingBox mbb) {
        double overlap = Double.POSITIVE_INFINITY;
        int leastOverlapItemIndex = -1;

        for (int indexOfChild = 0; indexOfChild < this.usedCount; indexOfChild++) {
            RTNode childItem = this.getChild(indexOfChild);
            double overlapOfAllItems = 0;

            for (int indexOfChildMbb = 0; indexOfChildMbb < childItem.usedCount; indexOfChildMbb++) {
                // 计算子节点中所有条目与该mbb的重叠面积之和。
                overlapOfAllItems += childItem.data[indexOfChildMbb].getIntersectionArea(mbb);
            }

            if (overlapOfAllItems < overlap) {
                overlap = overlapOfAllItems;
                leastOverlapItemIndex = indexOfChild;
            } else if (overlapOfAllItems == overlap) {
                // 若有多个候选子节点与mbb的重叠子面积相等，则选择加入mbb后，面积增量最小的mbb
                MaximumBoundingBox currentItemMbb = this.data[indexOfChild];
                MaximumBoundingBox otherItemMbb = this.data[leastOverlapItemIndex];

                double currentItemAreaIncr = currentItemMbb.unionAsMaximumBoundingBox(mbb).getArea() -
                        currentItemMbb.getArea();
                double otherItemAreaIncr = otherItemMbb.unionAsMaximumBoundingBox(mbb).getArea() -
                        otherItemMbb.getArea();

                if (currentItemAreaIncr < otherItemAreaIncr) {
                    leastOverlapItemIndex = indexOfChild;
                } else if (currentItemAreaIncr == otherItemAreaIncr) {
                    // 若面积增量相同，选自身面积小的子节点。
                    leastOverlapItemIndex = (currentItemMbb.getArea() >= otherItemMbb.getArea())
                            ? leastOverlapItemIndex : indexOfChild;
                }

            }
        }

        return leastOverlapItemIndex;
    }

    /**
     * 返回面积增量最小的节点的索引，若多个节点的面积增量都是最小的，则选择自身面积最小的一个。
     * @param mbb 新增的MBB
     * @return int 面积增量最小的节点的索引。
     */
    private int findLeastEnlargement(MaximumBoundingBox mbb) {
        double incrArea = Double.POSITIVE_INFINITY;
        int seq = -1;

        for (int index = 0; index < this.usedCount; index++) {
            MaximumBoundingBox nodeMbb = this.data[index];
            double enlargement = nodeMbb.unionAsMaximumBoundingBox(mbb).getArea() - nodeMbb.getArea();
            if (enlargement < incrArea) {
                incrArea = enlargement;
                seq = index;
            } else if (enlargement == incrArea) {
                MaximumBoundingBox nodeSeq = this.data[index];
                seq = nodeSeq.getArea() <= nodeMbb.getArea() ? seq : index;
            }
        }

        return seq;
    }

    /**
     * 插入新的MBB后，从插入的叶子节点向上调整，直到根节点。
     * @param rtNode1 引起需要调整的节点。
     * @param rtNode2 分裂出来的节点，若未分裂，则定为null
     */
    public void adjustRtree(RTNode rtNode1, RTNode rtNode2){
        // 先更新旧节点及其MBB在父节点中的值
        this.data[this.insertIndex] = rtNode1.getMaximumBoundingBox();
        this.children.set(this.insertIndex, rtNode1);

        if (rtNode2 != null) {
            this.insert(rtNode2);
        } else if (!isRoot()) {
            // 没有节点分裂，将调整传递到父节点。
            RTIndexNode parent = (RTIndexNode) this.getParent();
            parent.adjustRtree(this, null);
        }

    }

    /**
     * 中间节点的插入
     * @param rtNode 待插入的新节点
     * @return boolean 若插入新节点后需要分裂，则返回true，否则返回false
     */
    protected boolean insert(RTNode rtNode){
        // 当前节点还有剩余空间，不需要分裂直接插入
        if (this.usedCount < this.rTree.getNodeCapacity()) {
            int index = this.usedCount;
            this.data[index] = rtNode.getMaximumBoundingBox();
            this.children.add(rtNode);
            this.usedCount += 1;
            rtNode.parent = this;
            // parent 不是根节点，则从当前节点开始调整。
            RTIndexNode parent = (RTIndexNode) this.parent;
            if (parent != null) {
               parent.adjustRtree(this, null);
            }
            return false;

        } else {
            RTIndexNode[] seeds = this.splitIndex(rtNode);
            RTIndexNode one = seeds[0];
            RTIndexNode two = seeds[1];

            if (isRoot()) {
                // 新建根节点，层数加1
                RTIndexNode newRoot = new RTIndexNode(this.rTree, null, this.level + 1);

                // 将两个新节点加入到根节点的子节点列表中。
                newRoot.children.add(one);
                newRoot.children.add(two);

                newRoot.addMaximumBoundingBox(one.getMaximumBoundingBox());
                newRoot.addMaximumBoundingBox(two.getMaximumBoundingBox());

                one.parent = newRoot;
                two.parent = newRoot;

                this.rTree.setRoot(newRoot);
            } else {
                // 不是根节点，向上调整树
                RTIndexNode parent = (RTIndexNode) this.parent;
                parent.adjustRtree(one, two);
            }
            return true;
        }
    }

    /**
     * 中间节点的分裂
     * @param rtNode
     * @return RTIndexNode[], length = 2, 分别取出分裂后的第一个和第二个中间节点。
     */
    private RTIndexNode[] splitIndex(RTNode rtNode) {
        int[][] group = null;
        switch(this.rTree.getTreeType()) {
            case Constants.RTREE_QUADRATIC:
                group = quadraticSplit(rtNode.getMaximumBoundingBox());
                this.children.add(rtNode);
                rtNode.parent = this;
                break;
            case Constants.RTREE_LINEAR:
            case Constants.RTREE_EXPONENTIAL:
            case Constants.RSTAR:
                throw new IllegalArgumentException("not support Rtree type");
            default:
                throw new IllegalArgumentException("invalid Rtree type");
        }

        RTIndexNode indexOne = new RTIndexNode(this.rTree, this.parent, this.level);
        RTIndexNode indexTwo = new RTIndexNode(this.rTree, this.parent, this.level);

        int[] group1 = group[0];
        int[] group2 = group[1];
        // 把子节点按照索引分到新分裂的两个节点中
        for (int seq : group1) {
            indexOne.children.add(this.children.get(seq));
            indexOne.addMaximumBoundingBox(this.data[seq]);
            this.children.get(seq).parent = indexOne;
        }
        for (int seq : group2) {
            indexTwo.children.add(this.children.get(seq));
            indexTwo.addMaximumBoundingBox(this.data[seq]);
            this.children.get(seq).parent = indexTwo;
        }

        return new RTIndexNode[]{indexOne, indexTwo};
    }

    /**
     * 选择叶子节点
     * @param maximumBoundingBox mbb
     * @return RTLeafNode
     */
    @Override
    public RTLeafNode chooseLeaf(MaximumBoundingBox maximumBoundingBox) {
        int index = -1;

        switch (this.rTree.getTreeType()) {
            case Constants.RTREE_LINEAR:
            case Constants.RTREE_QUADRATIC:
            case Constants.RTREE_EXPONENTIAL:
                index = this.findLeastEnlargement(maximumBoundingBox);
                break;
            case Constants.RSTAR:
                // 此节点的子节点就是叶子节点
                if (level == 1) {
                    index = findLeastOverlap(maximumBoundingBox);
                } else {
                    index = findLeastEnlargement(maximumBoundingBox);
                }
                break;
            default:
                throw new IllegalArgumentException("invalid Rtree type");
        }

        // 记录插入的路径
        this.insertIndex = index;

        return this.children.get(index).chooseLeaf(maximumBoundingBox);
    }

    @Override
    public RTLeafNode findLeaf(MaximumBoundingBox maximumBoundingBox) {
        for (int index = 0; index < this.usedCount; index++) {
            if (this.data[index].enclosure(maximumBoundingBox)) {
                this.deleteIndex = index;
                RTLeafNode leaf = this.children.get(index).findLeaf(maximumBoundingBox);
                if (leaf != null) {
                    return leaf;
                }
            }
        }
        return null;
    }
}
