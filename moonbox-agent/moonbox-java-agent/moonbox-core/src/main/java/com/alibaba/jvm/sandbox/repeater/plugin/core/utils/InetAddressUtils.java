package com.alibaba.jvm.sandbox.repeater.plugin.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Enumeration;

/**
 * @author huang-gz
 * @date 2023/04/07
 */
public class InetAddressUtils {
    private static final Logger log = LoggerFactory.getLogger(InetAddressUtils.class);

    /**
     * getLocalIp
     *
     * @return ip
     */
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                // 202404101 yijiakang 【功能完善】 完善分布式节点 获取本机 ip 问题，任务号:20240102165830000104
                String netInterfaceName = ni.getName();
                // 需要过滤掉本机 k8s 相关的 ip
                // 20241027 yijiakang 【功能完善】 完善分布式节点 获取本机 ip 问题，排除环路地址和虚拟地址 任务号:20240102165830000104
                if (ni.isLoopback() ||
                        ni.isVirtual() ||
                        netInterfaceName.startsWith("veth") ||
                        netInterfaceName.startsWith("cni") ||
                        // 20241127 yijiakang 【功能完善】排除 br- virbr0,  virbr0 是一种虚拟网桥，它将宿主机和虚拟机之间的网络通信连接起来。它允许虚拟机和宿主机之间、虚拟机和外部网络之间的通信。(20240102165830000104)
                        netInterfaceName.startsWith("br-") ||
                        netInterfaceName.startsWith("virbr0") ||
                        netInterfaceName.startsWith("flannel") ||
                        netInterfaceName.startsWith("docker")) {
                    continue;
                }
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    ip = address.nextElement();
                    if ( ip.isSiteLocalAddress() &&
                            !ip.isLoopbackAddress() &&
                            ip.getHostAddress().indexOf(":") == -1) {
                        return ip.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // default value for disaster
        return "127.0.0.1";
    }
}
