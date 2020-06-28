/**
 * @author jv.lee
 * @date 2020-06-23
 * @description
 */
public class MessageCreator {
    private static final String SN_HEADER = "收到暗号，我是（SN）:";
    private static final String PORT_HEADER = "这是暗号，请回电端口(Port):";

    /**
     * 创建固定port格式
     *
     * @param port
     * @return
     */
    public static String buildWithPort(int port) {
        return PORT_HEADER + port;
    }

    /**
     * 解析固定port格式
     *
     * @param data
     * @return
     */
    public static int parsePort(String data) {
        if (data.startsWith(PORT_HEADER)) {
            return Integer.parseInt(data.substring(PORT_HEADER.length()));
        }
        return -1;
    }

    /**
     * 创建sn格式数据
     *
     * @param sn
     * @return
     */
    public static String buildWithSn(String sn) {
        return SN_HEADER + sn;
    }

    /**
     * 解析sn格式数据
     *
     * @param data
     * @return
     */
    public static String parseSn(String data) {
        if (data.startsWith(SN_HEADER)) {
            return data.substring(SN_HEADER.length());
        }
        return null;
    }
}
