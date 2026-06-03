package nodagumi.ananPJ.misc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * JSONIC を使って HashMap にデコードした JSON データから値を取得する
 */
public class JsonicHashMapGetter {
    /**
     * デコードされた JSON データ
     */
    protected HashMap parameters;

    /**
     * デコードされた JSON データの設定
     */
    public void setParameters(HashMap parameters) {
        if (parameters == null) {
            this.parameters = new HashMap();
        } else {
            this.parameters = parameters;
        }
    }

    /**
     * デコードされた JSON データの取得
     */
    public HashMap getParameters() {
        return parameters;
    }

    /**
     * boolean 型のパラメータを取得する
     */
    public boolean getBooleanParameter(String name, boolean defaultValue) throws Exception {
        Object parameter = parameters.get(name);
        if (parameter == null) {
            return defaultValue;
        }
        if (! (parameter instanceof Boolean)) {
            throw new Exception("Parameter type error - \"" + name + "\" : " + parameter.toString());
        }
        return (Boolean)parameter;
    }

    /**
     * String 型のパラメータを取得する
     */
    public String getStringParameter(String name, String defaultValue) throws Exception {
        Object parameter = parameters.get(name);
        if (parameter == null) {
            return defaultValue;
        }
        if (! (parameter instanceof String)) {
            throw new Exception("Parameter type error - \"" + name + "\" : " + parameter.toString());
        }
        return (String)parameter;
    }

    /**
     * BigDecimal 型のパラメータを取得する
     */
    public BigDecimal getBigDecimalParameter(String name, BigDecimal defaultValue) throws Exception {
        Object parameter = parameters.get(name);
        if (parameter == null) {
            return defaultValue;
        }
        if (! (parameter instanceof BigDecimal)) {
            if (parameter instanceof String) {
                try {
                    parameter = new BigDecimal((String)parameter);
                } catch (NumberFormatException e) {
                    throw new Exception("Parameter is not numeric - \"" + name + "\" : " + parameter.toString());
                }
            } else {
                throw new Exception("Parameter type error - \"" + name + "\" : " + parameter.toString());
            }
        }
        return (BigDecimal)parameter;
    }

    /**
     * double 型のパラメータを取得する
     */
    public double getDoubleParameter(String name, double defaultValue) throws Exception {
        return getBigDecimalParameter(name, new BigDecimal(defaultValue)).doubleValue();
    }

    /**
     * float 型のパラメータを取得する
     */
    public float getFloatParameter(String name, float defaultValue) throws Exception {
        return getBigDecimalParameter(name, new BigDecimal(defaultValue)).floatValue();
    }

    /**
     * int 型のパラメータを取得する
     */
    public int getIntParameter(String name, int defaultValue) throws Exception {
        return getBigDecimalParameter(name, new BigDecimal(defaultValue)).intValue();
    }

    /**
     * long 型のパラメータを取得する
     */
    public long getLongParameter(String name, long defaultValue) throws Exception {
        return getBigDecimalParameter(name, new BigDecimal(defaultValue)).longValue();
    }

    /**
     * {@code HashMap<String, Object>} 型のパラメータを取得する
     */
    public HashMap<String, Object> getHashMapParameter(String name, HashMap<String, Object> defaultValue) throws Exception {
        Object parameter = parameters.get(name);
        if (parameter == null) {
            return defaultValue;
        }
        if (! (parameter instanceof HashMap)) {
            throw new Exception("Parameter type error - \"" + name + "\" : " + parameter.toString());
        }
        return (HashMap<String, Object>)parameter;
    }

    /**
     * ArrayList 型のパラメータを取得する
     */
    public ArrayList getArrayListParameter(String name, ArrayList defaultValue) throws Exception {
        Object parameter = parameters.get(name);
        if (parameter == null) {
            return defaultValue;
        }
        if (! (parameter instanceof ArrayList)) {
            throw new Exception("Parameter type error - \"" + name + "\" : " + parameter.toString());
        }
        return (ArrayList)parameter;
    }

    /**
     * ArrayList &lt;BigDecimal&gt; 型のパラメータを取得する
     */
    public ArrayList<BigDecimal> getBigDecimalArrayList(String name, ArrayList<BigDecimal> defaultValue) throws Exception {
        return (ArrayList<BigDecimal>)getArrayListParameter(name, defaultValue);
    }
}
