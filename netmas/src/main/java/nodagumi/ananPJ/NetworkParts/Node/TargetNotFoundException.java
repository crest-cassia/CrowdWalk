// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts.Node;

/**
 * MapNode クラスの getDistance() で target の情報が見つからない場合に発生する例外
 */
public class TargetNotFoundException extends Exception {
    public TargetNotFoundException(String message) {
        super(message);
    }
}
