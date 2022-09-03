package share.shiguri.code.rtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @ClassName RTLeafNode
 * @Description RTree 叶子节点
 */
public class RTLeafNode extends RTNode{
    public RTLeafNode(RTree rTree, RTNode parent) {
        super(rTree, 0, parent);
    }

    public static RTLeafNode create(RTree rTree, RTNode parent) {
        return new RTLeafNode(rTree, parent);
    }

    /**
     * <p>
     *     向叶子节点中插入Mbb。<br>
     *     1.若当前叶子节点的容量能容纳新插入的Mbb，直接插入，并从父节点开始调整树。<br>
     *     2.若当前叶子节点容量不能容纳新插入的Mbb，则需要分裂当前节点为两个新的叶子节点
     * @param insertMbb 待插入的MaximumBoundingBox
     * @return boolean
     */
    public boolean insert(MaximumBoundingBox insertMbb) {
        if (this.usedCount < rTree.getNodeCapacity()) {
            // 未超过当前叶子节点的容量，直接增加。
            this.data[this.usedCount] = insertMbb;
            this.usedCount += 1;

            if (null != this.parent) {
                // 不需要分裂节点，只需要从父节点开始调整树。
                ((RTIndexNode) this.parent).adjustRtree(this, null);
            }
        } else {
            // 超过单个叶子节点的容量，则需要分裂节点。
            RTLeafNode[] splitNodes = this.splitLeaf(insertMbb);
            RTLeafNode one = splitNodes[0];
            RTLeafNode two = splitNodes[1];

            if (isRoot()) {
                // 根节点分裂，则需要创建新的根节点
                RTIndexNode newRoot = RTIndexNode.create(this.rTree, null, this.level + 1);
                this.rTree.setRoot(newRoot);
                newRoot.addMaximumBoundingBox(one.getMaximumBoundingBox());
                newRoot.addMaximumBoundingBox(two.getMaximumBoundingBox());

                one.parent = newRoot;
                two.parent = newRoot;
                newRoot.children.add(one);
                newRoot.children.add(two);

            } else {
                RTIndexNode parentNode = (RTIndexNode) this.parent;
                parentNode.adjustRtree(one, two);
            }
        }

        return true;
    }

    /**
     * 叶子节点分裂。当RTLeafNode容量已满，又插入一个新的Mbb，则会将原RTLeafNode分裂成两个新的RTLeafNode
     * @param insertMbb 新插入的Mbb
     * @return RTLeafNode[], 包含两个元素，即分裂新产生的两个RTLeafNode
     */
    public RTLeafNode[] splitLeaf(MaximumBoundingBox insertMbb) {
        int[][] group = new int[][]{new int[]{}, new int[]{}};
        switch (this.rTree.getTreeType()) {
            case Constants.RTREE_LINEAR:
                break;
            case Constants.RTREE_QUADRATIC:
                group = this.quadraticSplit(insertMbb);
                break;
            case Constants.RTREE_EXPONENTIAL:
                break;
            case Constants.RSTAR:
                break;
            default:
                throw new IllegalArgumentException("Invalid Tree Type");
        }

        RTLeafNode one = new RTLeafNode(rTree, this.parent);
        RTLeafNode two = new RTLeafNode(rTree, this.parent);
        int[] dataIndexOfOne = group[0];
        int[] dataIndexOfTwo = group[1];
        for (int index : dataIndexOfOne) {
            one.addMaximumBoundingBox(this.data[index]);
        }
        for (int index : dataIndexOfTwo) {
            two.addMaximumBoundingBox(this.data[index]);
        }

        return new RTLeafNode[]{one, two};
    }

    /**
     * 从叶节点中删除deleteMbb。
     * <p>
     *     1.从叶子节点中删除deleteMbb<br>
     *     2.调用condenseTree()返回所有被移除的节点的集合，把其中的叶子节点的条目重新插入到RTree中<br>
     *     3.非叶子节点从节点开始遍历，找出其下的所有叶子节点。把叶子节点中的所有条目重新插入到RTree中。
     * @param deleteMbb 待删除的MaximumBoundingBox
     * @return
     */
    protected int delete (MaximumBoundingBox deleteMbb) {
        for (int index = 0; index < this.usedCount; index++) {
            if (this.data[index].equals(deleteMbb)) {
                // 直接删除Mbb
                this.deleteMaximumBoundingBox(index);

                List<RTNode> reInsert = new ArrayList<>();
                this.condenseTree(reInsert);

                //重新插入删除节点中的剩余条目
                for (int seq = 0; seq < reInsert.size(); seq++) {
                    RTNode node = reInsert.get(seq);
                    // 叶子节点，直接插入
                    if (node.isLeaf()) {
                        for (int mbbIndex = 0; mbbIndex < node.usedCount; mbbIndex++) {
                            rTree.insert(node.data[mbbIndex]);
                        }
                    } else {
                        // ？？？ 需要后续遍历？
                        List<RTNode> traverseNodes = rTree.traversePostOrder(node);

                        //将其中的叶子节点重新插入
                        for (int nodeIndex = 0; nodeIndex < traverseNodes.size(); nodeIndex++) {
                            RTNode rtNode = traverseNodes.get(nodeIndex);
                            if (rtNode.isLeaf()) {
                                for (int mbbIndex = 0; mbbIndex < rtNode.usedCount; mbbIndex++) {
                                    rTree.insert(rtNode.data[mbbIndex]);
                                }
                            }
                        }
                    }
                }

                // ? deleteIndex 怎么使用？
                return this.deleteIndex;
            }// end if
        }// end for
        return -1;
    }

    @Override
    public RTLeafNode chooseLeaf(MaximumBoundingBox maximumBoundingBox) {
        this.insertIndex = this.usedCount;
        return this;
    }

    @Override
    protected RTLeafNode findLeaf(MaximumBoundingBox maximumBoundingBox) {
        for (int index = 0; index < this.usedCount; index++) {
            if (this.data[index].enclosure(maximumBoundingBox)) {
                this.deleteIndex = index;
                return this;
            }
        }
        return null;
    }
}
