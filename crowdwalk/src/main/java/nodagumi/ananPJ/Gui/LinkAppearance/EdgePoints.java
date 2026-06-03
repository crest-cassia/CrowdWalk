package nodagumi.ananPJ.Gui.LinkAppearance;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.NetworkMap;

/**
 * リンクの縁取りライン(交差点できれいに繋がる)を生成するための座標を計算して保持する
 */
public class EdgePoints {
    /**
     * リンクの縁が交差する座標
     */
    public class Points {
        public Point2D fromNodeLeftPoint = null;
        public Point2D fromNodeRightPoint = null;
        public Point2D toNodeLeftPoint = null;
        public Point2D toNodeRightPoint = null;

        public Points() {}

        public void setLeftPoint(MapLink link, MapNode node, Point2D point) {
            if (link.getFrom() == node) {
                fromNodeLeftPoint = point;
            } else {
                toNodeLeftPoint = point;
            }
        }

        public void setRightPoint(MapLink link, MapNode node, Point2D point) {
            if (link.getFrom() == node) {
                fromNodeRightPoint = point;
            } else {
                toNodeRightPoint = point;
            }
        }
    }

    /**
     * 地図データ。
     */
    private NetworkMap networkMap;

    /**
     * 除外するリンクを示すタグ
     */
    private String[] exclusionTags;

    /**
     * マップ全体のリンクの縁が交差する座標
     */
    private HashMap<MapLink, Points> edgePoints = null;

    /**
     * コンストラクタ
     */
    public EdgePoints(NetworkMap networkMap, String[] exclusionTags) {
        this.networkMap = networkMap;
        this.exclusionTags = exclusionTags;
    }

    /**
     * リンクの縁取りラインを生成するための座標を取得する
     */
    public Points getEdgePoints(MapLink link) {
        synchronized (this) {
            if (edgePoints == null) {
                edgePoints = calcEdgePoints();
            }
        }
        return edgePoints.get(link);
    }

    /**
     * リンクの縁が交差する座標を求める
     */
    public HashMap<MapLink, Points> calcEdgePoints() {
        // ノードに接続されたリンク(正規のリンクのみ)
        HashMap<MapNode, ArrayList<MapLink>> regularLinksAtNode = new HashMap();
        for (MapNode node : networkMap.getNodes()) {
            ArrayList<MapLink> links = new ArrayList();
            for (MapLink link : node.getLinks()) {
                if (link.getOther(node) == node) {
                    continue;
                }
                int index = 0;
                while (index < exclusionTags.length && ! link.hasSubTag(exclusionTags[index])) {
                    index++;
                }
                if (index < exclusionTags.length) {
                    continue;
                }
                links.add(link);
            }
            regularLinksAtNode.put(node, links);
        }

        HashMap<MapLink, Points> linkPoints = new HashMap();
        for (MapNode node : networkMap.getNodes()) {
            ArrayList<MapLink> links = regularLinksAtNode.get(node);
            if (links.isEmpty()) {
                continue;
            }
            if (links.size() == 1) {
                MapLink link = links.get(0);
                MapNode oppositeNode = link.getOther(node);
                // 単独リンクの処理
                if (regularLinksAtNode.get(oppositeNode).size() == 1) {
                    Point3D p1 = new Point3D(oppositeNode.getX() - node.getX(), oppositeNode.getY() - node.getY(), 0);
                    Point3D p2 = p1.normalize().crossProduct(0, 0, link.getWidth() / 2.0);
                    double dx = p2.getX();
                    double dy = p2.getY();
                    Point2D a1 = new Point2D(node.getX() - dx, node.getY() - dy);
                    Point2D b1 = new Point2D(node.getX() + dx, node.getY() + dy);
                    if (! linkPoints.containsKey(link)) {
                        linkPoints.put(link, new Points());
                    }
                    linkPoints.get(link).setLeftPoint(link, node, a1);
                    linkPoints.get(link).setRightPoint(link, node, b1);
                }
                continue;
            }

            Point2D nodePoint = new Point2D(node.getX(), node.getY());
            for (int index = 0; index < links.size(); index++) {
                MapLink link = links.get(index);
                MapLink prevLink = links.get((index + links.size() - 1) % links.size());
                MapLink nextLink = links.get((index + 1) % links.size());
                MapNode prevOppositeNode = prevLink.getOther(node);
                MapNode oppositeNode = link.getOther(node);
                MapNode nextOppositeNode = nextLink.getOther(node);
                Point2D oppositeNodePoint = new Point2D(oppositeNode.getX(), oppositeNode.getY());

                if (! linkPoints.containsKey(link)) {
                    linkPoints.put(link, new Points());
                }
                if (! linkPoints.containsKey(nextLink)) {
                    linkPoints.put(nextLink, new Points());
                }
                Point3D p1 = new Point3D(oppositeNode.getX() - node.getX(), oppositeNode.getY() - node.getY(), 0);
                Point3D p2 = p1.normalize().crossProduct(0, 0, link.getWidth() / 2.0);
                double dx = p2.getX();
                double dy = p2.getY();
                // links は node を中心として -π を起点とした時計回りの順にソートされている
                // link 直線に dx, dy をプラスしたものが道幅の(links 順に見て)起点側の縁を表す
                // よって link 直線に dx, dy をマイナスしたものと、次の link 直線に dx, dy をプラスしたものの交点座標が、かど座標となる
                Point2D a1 = new Point2D(node.getX() - dx, node.getY() - dy);
                Point2D a2 = new Point2D(oppositeNode.getX() - dx, oppositeNode.getY() - dy);

                p1 = new Point3D(nextOppositeNode.getX() - node.getX(), nextOppositeNode.getY() - node.getY(), 0);
                p2 = p1.normalize().crossProduct(0, 0, nextLink.getWidth() / 2.0);
                dx = p2.getX();
                dy = p2.getY();
                Point2D b1 = new Point2D(node.getX() + dx, node.getY() + dy);
                Point2D b2 = new Point2D(nextOppositeNode.getX() + dx, nextOppositeNode.getY() + dy);

                double prevAngle = angle(prevOppositeNode, node, oppositeNode);
                double angle = angle(oppositeNode, node, nextOppositeNode);
                if (
                    // 曲がり角が突き出てしまう
                    angle <= 30.0 && links.size() == 2
                    // 交点座標が遙か彼方になってしまうかもしれない
                    || angle >= 178.0
                    // 道幅の差が大きいためきれいに繋がらない
                    || angle >= 165.0 && (Math.max(link.getWidth(), nextLink.getWidth()) / Math.min(link.getWidth(), nextLink.getWidth())) > 1.5
                ) {
                    linkPoints.get(link).setLeftPoint(link, node, a1);
                    linkPoints.get(nextLink).setRightPoint(nextLink, node, b1);
                } else {
                    Point2D intersectionPoint = intersection(a1, a2, b1, b2);
                    if (
                        // 交点座標が道幅の1.5倍以上ノードから離れている
                        nodePoint.distance(intersectionPoint) > Math.max(link.getWidth(), nextLink.getWidth()) * 1.5
                        // 交点座標がノードよりも先にはみ出ている
                        && nodePoint.distance(intersectionPoint) > nodePoint.distance(oppositeNodePoint)
                    ) {
                        linkPoints.get(link).setLeftPoint(link, node, a1);
                        linkPoints.get(nextLink).setRightPoint(nextLink, node, b1);
                    } else {
                        linkPoints.get(link).setLeftPoint(link, node, intersectionPoint);
                        linkPoints.get(nextLink).setRightPoint(nextLink, node, intersectionPoint);
                    }
                }
                if (regularLinksAtNode.get(oppositeNode).size() == 1) {
                    linkPoints.get(link).setRightPoint(link, oppositeNode, a2);
                }
                if (regularLinksAtNode.get(nextOppositeNode).size() == 1) {
                    linkPoints.get(nextLink).setLeftPoint(nextLink, nextOppositeNode, b2);
                }
            }
        }
        return linkPoints;
    }

    /**
     * 3点のノードがなす角度を求める.
     *
     * 0.0～180.0 を返す
     */
    public static double angle(MapNode nodeA, MapNode nodeB, MapNode nodeC) {
        Point2D pointA = new Point2D(nodeA.getX(), nodeA.getY());
        Point2D pointB = new Point2D(nodeB.getX(), nodeB.getY());
        Point2D pointC = new Point2D(nodeC.getX(), nodeC.getY());
        return pointB.angle(pointA, pointC);
    }

    /**
     * 2直線の交点座標を求める.
     *
     * ※直線 a, b は平行ではないこと。
     */
    public static Point2D intersection(Point2D a1, Point2D a2, Point2D b1, Point2D b2) {
        double f1 = a2.getX() - a1.getX();
        double g1 = a2.getY() - a1.getY();
        double f2 = b2.getX() - b1.getX();
        double g2 = b2.getY() - b1.getY();
        double dx = b1.getX() - a1.getX();
        double dy = b1.getY() - a1.getY();
        double t1 = (f2 * dy - g2 * dx) / (f2 * g1 - f1 * g2);
        return new Point2D(a1.getX() + f1 * t1, a1.getY() + g1 * t1);
    }
}
