package share.shiguri.code.util;

import share.shiguri.code.rtree.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class RTreeUtil {
    public static void stdoutRTree(RTree rTree) {
        List<RTNode> layer = new ArrayList<>();
        RTNode root = rTree.getRoot();
        layer.add(root);
        List<RTNode> nextLayer = new ArrayList<>();
        StringBuilder leafLayerStringBuilder = new StringBuilder("leaf :");

        while (layer.size() != 0) {
            Iterator<RTNode> iterator = layer.iterator();
            StringJoiner stringJoiner = new StringJoiner(" | ", "{", "}");
            while (iterator.hasNext()) {
                RTNode node = iterator.next();
                MaximumBoundingBox mbb = node.getMaximumBoundingBox();
                String str = formatToString(mbb, rTree.getDimension());
                stringJoiner.add(str);

                if (node instanceof RTIndexNode) {
                    // indexNode, 则把节点加入下一层。
                    RTIndexNode indexNode = (RTIndexNode) node;
                    int usedCount = indexNode.getUsedCount();
                    for (int index = 0; index < usedCount; index++) {
                        nextLayer.add(indexNode.getChild(index));
                    }
                } else if (node instanceof RTLeafNode) {
                    // 打印叶子节点的包含的所有mbb
                    RTLeafNode leafNode = (RTLeafNode) node;
                    StringJoiner leafLayerStringJoiner = new StringJoiner(",", "<", ">");
                    for (int index = 0; index < leafNode.getUsedCount(); index++) {
                        MaximumBoundingBox dataMbb = leafNode.getDataOfIndex(index);
                        String dataStr = formatToString(dataMbb, rTree.getDimension());
                        leafLayerStringJoiner.add(dataStr);
                    }
                    leafLayerStringBuilder.append(leafLayerStringJoiner.toString());
                }
            }
            // 下一层
            layer = nextLayer;
            nextLayer = new ArrayList<>();

            System.out.println(stringJoiner.toString());
        }

        // 输出leaf层包含的所有数据
        System.out.println(leafLayerStringBuilder.toString());
    }

    public static String formatToString(MaximumBoundingBox box, int dimension) {
        Point leftBottomPoint = box.getLeftBottomPoint();
        Point rightTopPoint = box.getRightTopPoint();

        StringBuilder stringBuilder = new StringBuilder();

        StringJoiner stringJoinerLeft = new StringJoiner(",", "(", ")");
        for (int index = 0; index < dimension; index++) {
            stringJoinerLeft.add(String.valueOf(leftBottomPoint.getValueOfIndex(index)));
        }

        StringJoiner stringJoinerRight = new StringJoiner(",", "(", ")");
        for (int index = 0; index < dimension; index++) {
            stringJoinerRight.add(String.valueOf(rightTopPoint.getValueOfIndex(index)));
        }

        stringBuilder.append("[").append(stringJoinerLeft.toString()).append(stringJoinerRight.toString()).append("]");
        return stringBuilder.toString();
    }
}
