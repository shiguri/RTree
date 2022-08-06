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

    protected void deleteMaximumBoundingBox(MaximumBoundingBox){}

    protected void condenseTree(List<RTNode>){}


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
                MaximumBoundingBox seedTwoBox = this.data[groupTwo[0]].clone();
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
}
