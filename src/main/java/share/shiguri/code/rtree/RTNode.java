package share.shiguri.code.rtree;

import java.util.Arrays;
import java.util.List;

public abstract class RTNode {
    //节点所在的树
    protected RTree rTree;
    //节点所在层级
    protected int level;
    //当前节点所含的条目，类似于子节点
    protected MaximumBoundingBox[] data;
    //当前节点的父节点
    protected RTNode parent;
    //节点在data中存有的条目数目
    protected int usedCount;
    //
    protected int insertIndex;
    //
    protected int deleteIndex;

    public RTNode (RTree rTree, int level, RTNode parent) {
        this.rTree = rTree;
        this.level = level;
        this.parent = parent;
        //多出来的一个用于节点的分裂
        this.data = new MaximumBoundingBox[rTree.getNodeCapacity() + 1];
        usedCount = 0;
    }

    public RTNode getParent() {
        return parent;
    }

    public int getUsedCount(){return usedCount;}

    public MaximumBoundingBox getDataOfIndex(int index) {
        return (MaximumBoundingBox) data[index].clone();
    }

    /**
     * 向当前节点的条目中添加一个MaximumBoundingBox
     * @param box 待添加的MaximumBoundingBox
     */
    protected void addMaximumBoundingBox(MaximumBoundingBox box) {
        if (this.usedCount == rTree.getNodeCapacity()) {
            throw new IllegalArgumentException("try to insert MaximumBoundingBox to a full RTNode");
        }

        this.data[usedCount] = box;
        usedCount += 1;
    }

    /**
     * <p>
     *     根据MBB在当前节点中的条目索引，删除该MBB
     * @param index 待删除的条目在当前节点中的索引。
     */
    protected void deleteMaximumBoundingBox(int index){
        if (null == this.data[index + 1]) { // 如果该节点后面还有条目
            //把 index + 1 后面的节点整体往前移动1位。
            System.arraycopy(this.data, index + 1, this.data, index, this.usedCount - index - 1);
            this.data[this.usedCount -1] = null;
        } else {
            this.data[index] = null;
        }
        this.usedCount -= 1;
    }

    /**
     * 压缩算法 叶节点L中刚刚删除了一个条目，如果这个结点的条目数太少下溢，则移除该结点，同时将该结点中剩余的条目重定位到其他结点中。
     * 如果有必要，要逐级向上进行这种移除，调整向上传递的路径上的所有外包矩形，使其尽可能小，直到根节点。
     *
     * @param reinsert 存储移除结点中剩余条目
     */
    protected void condenseTree(List<RTNode> reinsert){
        if (isRoot()) {
            // 待删除节点是根节点，且其子条目只有一条，则令这条子条目为新的根节点。
            if (isLeaf() && 1 == this.usedCount) {
                RTIndexNode root = (RTIndexNode) this;
                RTLeafNode child = (RTLeafNode) root.getChild(0);

                root.children.remove(this);
                child.parent = null;
                rTree.setRoot(child);
            }
        } else {
            RTNode parent = this.getParent();
            // ? 是否可以为1
            long minCapacity = Math.round(rTree.getNodeCapacity() * rTree.getFillFactor());
            if (this.usedCount < minCapacity) {
                // deleteIndex ？
                parent.deleteMaximumBoundingBox(parent.deleteIndex);
                ((RTIndexNode) parent).children.remove(this);
                this.parent = null;
                reinsert.add(this);
            } else {
                // 直接更新MBB
                parent.data[parent.deleteIndex] = parent.getMaximumBoundingBox();
            }
            //将变化向上传播
            parent.condenseTree(reinsert);
        }
    }

    /**
     * 当前节点的存放条目已到达上限，再往其中加入节点时才调用。用于将原节点的所有MBB(包括试图加入的这个MBB)分裂成两组。
     * @param box MaximumBoundingBoxm, 当前节点空间已满后，试图向加入至其中的MBB。
     * @return int[][] 该二维数组存放了节点分裂后，每个组的MBB在原节点中的索引。
     */
    protected int[][] quadraticSplit(MaximumBoundingBox box) {
        if (null == box) {
            throw new IllegalArgumentException("MaximumBoundingBox is null when quadraticSplit");
        }

        //先将MBB加入进预留的最后一个空间，实际已经超出节点的空间容量。
        this.data[this.usedCount] = box;
        int total = usedCount + 1;
        //分裂后，每组只有 total / 2 个条目
        int capacity = total/ 2 + 1;
        //每个节点的最小条目数目，该数目最小为2
        long minNodeSize = Math.round(rTree.getNodeCapacity() * rTree.getFillFactor());
        minNodeSize = minNodeSize < 2 ? 2 : minNodeSize;

        //用于标记被分配过的条目
        int[] mask = new int[total];
        Arrays.fill(mask, 1);
        //还未分配的条目的数目
        int remaining = total;

        // 记录分裂后各组分配到的MBB在原data中的索引
        int[] groupOne = new int[capacity];
        int[] groupTwo = new int[capacity];
        int indexOne = 0;
        int indexTwo = 0;

        // 挑选最初的两个种子，加入到各自的分组。
        int[] seeds = pickSeed();
        groupOne[indexOne++] = seeds[0];
        groupTwo[indexTwo++] = seeds[1];
        remaining -= 2;
        mask[groupOne[0]] = -1;
        mask[groupTwo[0]] = -1;

        while(remaining > 0) {
            // 将剩余的条目全部分配到groupOne，算法结束
            if (minNodeSize - indexOne == remaining) {
                for (int seq = 0; seq < total; seq++) {
                    if (1 == mask[seq]) {
                        groupOne[indexOne++] = seq;
                        mask[seq] = -1;
                        remaining -= 1;
                    }
                }
            } else if (minNodeSize - indexTwo == remaining){
            // 将剩余的条目全部分配到groupTwo，算法结束
                if (minNodeSize - indexTwo == remaining) {
                    for (int seq = 0; seq < total; seq++) {
                        if (1 == mask[seq]) {
                            groupTwo[indexTwo++] = seq;
                            mask[seq] = -1;
                            remaining -= 1;
                        }
                    }
                }
            } else {
                // groupOne 当前的外包矩形
                MaximumBoundingBox seedOneBox = (MaximumBoundingBox) this.data[groupOne[0]].clone();
                for (int index = 0; index < indexOne; index++) {
                    seedOneBox = seedOneBox.unionAsMaximumBoundingBox(this.data[groupOne[index]]);
                }
                // groupTwo 当前的外包矩形
                MaximumBoundingBox seedTwoBox = (MaximumBoundingBox) this.data[groupTwo[0]].clone();
                for (int index = 0; index < indexTwo; index++) {
                    seedTwoBox = seedTwoBox.unionAsMaximumBoundingBox(this.data[groupTwo[index]]);
                }

                // 找出下一个待分配的条目
                double maxDif = Double.NEGATIVE_INFINITY;
                int indexOfMaxDif = -1;
                for (int index = 0; index < total; index++) {
                    // 还未分配
                    if (1 == mask[index]) {
                        MaximumBoundingBox option = this.data[index];
                        //加入groupOne后产生的面积增量
                        double areaDiffOne = getAreaIncrement(seedOneBox, option);
                        //加入groupTwo后产生的面积增量
                        double areaDiffTwo = getAreaIncrement(seedTwoBox, option);

                        if (Math.abs(areaDiffOne - areaDiffTwo) > maxDif) {
                            maxDif = Math.abs(areaDiffOne - areaDiffTwo);
                            indexOfMaxDif = index;
                        }
                    }
                }

                MaximumBoundingBox added = this.data[indexOfMaxDif];
                double areaDiffOne = getAreaIncrement(seedOneBox, added);
                double areaDiffTwo = getAreaIncrement(seedTwoBox, added);

                if (areaDiffOne > areaDiffTwo) { //先比较面积增量
                    // DiffOne 大于 DiffTwo, 说明待选的条目具体 GroupTwo更近，加入GroupTwo。反之
                    groupTwo[indexTwo++] = indexOfMaxDif;
                } else if (areaDiffOne < areaDiffTwo) {
                    groupOne[indexOne++] = indexOfMaxDif;
                } else if (seedOneBox.getArea() > seedTwoBox.getArea()) { //再比较自身面积
                    groupTwo[indexTwo++] = indexOfMaxDif;
                } else if (seedOneBox.getArea() < seedTwoBox.getArea()) {
                    groupOne[indexOne++] = indexOfMaxDif;
                } else if (indexOne > indexTwo) { //最后比较条目数
                    groupTwo[indexTwo++] = indexOfMaxDif;
                } else if (indexOne < indexTwo) {
                    groupOne[indexOne++] = indexOfMaxDif;
                } else { // 默认加入到groupOne
                    groupOne[indexOne++] = indexOfMaxDif;
                }
                mask[indexOfMaxDif] = -1;
                remaining -= 1;
            }
        } // end while

        int[][] choice = new int[2][];
        choice[0] = new int[indexOne];
        choice[1] = new int[indexTwo];
        for (int index = 0; index < indexOne; index++) {
            choice[0][index] = groupOne[index];
        }
        for (int index = 0; index < indexTwo; index++) {
            choice[1][index] = groupTwo[index];
        }
        return choice;
    }

    /**
     * 在当前节点空间已满，需要作分裂时用，遍历所有的data元素构成的二元组，根据公式计算以该二元组分裂后产生的新的空间的冗余，
     * 取冗余最大的一组。
     * 公式为 d = area(MBB(mbb_i, mbb_j)) - area(mbb_i) - area(mbb_j)
     * @return int[2]
     */
    protected int[] pickSeed() {
        double max = Double.NEGATIVE_INFINITY;
        int seedOne = 0;
        int seedTwo = 0;
        // 注意节点的范围是 0~usedSpace。而不是data.length
        for (int pre = 0; pre < this.usedCount; pre++){
            for (int post = pre + 1; post < this.usedCount; post++) {
                MaximumBoundingBox preMbb = data[pre];
                MaximumBoundingBox postMbb = data[post];
                double space = preMbb.unionAsMaximumBoundingBox(postMbb).getArea() - preMbb.getArea() - postMbb.getArea();
                if (space > max) {
                    max = space;
                    seedOne = pre;
                    seedTwo = post;
                }
            }
        }
        return new int[]{seedOne, seedTwo};
    }

    /**
     * 返回一个外包矩形和另一个外包矩形合并后得到的外包矩形相对最初的外包矩形的面积增量。
     * 公式为 area(union(region, added)) - area(origin);
     * @param origin 原外包矩形
     * @param added 需要添加的外包矩形
     * @return 外包矩形的面积增量
     */
    protected double getAreaIncrement(MaximumBoundingBox origin, MaximumBoundingBox added) {
        MaximumBoundingBox union = origin.unionAsMaximumBoundingBox(added);
        return union.getArea() - origin.getArea();
    }

    /**
     * 返回一个能包含当前节点所有条目的MBB
     * @return MaximumBoundingBox
     */
    public MaximumBoundingBox getMaximumBoundingBox() {
        if (this.usedCount > 0) {
            MaximumBoundingBox[] dataCopy = Arrays.copyOf(this.data, this.usedCount);
            MaximumBoundingBox box = dataCopy[0];
            for (int index = 1; index < dataCopy.length; index++) {
                box = box.unionAsMaximumBoundingBox(dataCopy[index]);
            }
            return box;
        } else {
            return MaximumBoundingBox.create(Point.create(new double[]{0, 0}), Point.create(new double[] {0, 0}));
        }
    }

    /**
     * 当前节点是否是空节点
     * @return boolean
     */
    public boolean isEmpty() {
        return 0 == this.usedCount;
    }

    /**
     * 是否是根节点
     * @return boolean
     */
    public boolean isRoot() {
        return null == this.parent;
    }

    /**
     * 是否是非叶节点
     * @return boolean
     */
    public boolean isIndex() {
        return !isLeaf();
    }

    /**
     * 是否是叶子节点
     * @return boolean
     */
    public boolean isLeaf() {
        return 0 == this.level;
    }

    /**
     * <b>步骤CL1：</b>初始化――记R树的根节点为N。<br>
     * <b>步骤CL2：</b>检查叶节点――如果N是个叶节点，返回N<br>
     * <b>步骤CL3：</b>选择子树――如果N不是叶节点，则从N中所有的条目中选出一个最佳的条目F，
     * 选择的标准是：如果E加入F后，F的外廓矩形FI扩张最小，则F就是最佳的条目。如果有两个
     * 条目在加入E后外廓矩形的扩张程度相等，则在这两者中选择外廓矩形较小的那个。<br>
     * <b>步骤CL4：</b>向下寻找直至达到叶节点――记Fp指向的孩子节点为N，然后返回步骤CL2循环运算， 直至查找到叶节点。
     * <p>
     *
     * @param maximumBoundingBox
     * @return RTLeafNode
     */
    public abstract RTLeafNode chooseLeaf(MaximumBoundingBox maximumBoundingBox);

    /**
     * R树的根节点为T，查找包含rectangle的叶子结点
     * <p>
     * 1、如果T不是叶子结点，则逐个查找T中的每个条目是否包围rectangle，若包围则递归调用findLeaf()<br>
     * 2、如果T是一个叶子结点，则逐个检查T中的每个条目能否匹配rectangle<br>
     *
     * @param maximumBoundingBox mbb
     * @return 返回包含mbb的叶节点
     */
    protected abstract RTLeafNode findLeaf(MaximumBoundingBox maximumBoundingBox);
}
