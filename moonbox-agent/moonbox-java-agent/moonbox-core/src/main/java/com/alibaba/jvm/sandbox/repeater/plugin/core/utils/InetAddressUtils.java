package com.alibaba.jvm.sandbox.repeater.plugin.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
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
                            !ip.getHostAddress().contains(":")) {
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

    public static String getTomcatPort() {
        Set<String> ports = new LinkedHashSet<>();

        // 1. 优先从 JMX 获取真实运行端口
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            Set<ObjectName> connectorNames = mBeanServer.queryNames(
                    new ObjectName("*:type=Connector,*"),
                    null
            );

            for (ObjectName name : connectorNames) {
                Object protocolObj = getAttributeQuietly(mBeanServer, name, "protocol");
                Object schemeObj = getAttributeQuietly(mBeanServer, name, "scheme");
                Object portObj = getAttributeQuietly(mBeanServer, name, "port");
                Object localPortObj = getAttributeQuietly(mBeanServer, name, "localPort");
                Object stateNameObj = getAttributeQuietly(mBeanServer, name, "stateName");

                String protocol = protocolObj == null ? "" : protocolObj.toString().toLowerCase();
                String scheme = schemeObj == null ? "" : schemeObj.toString().toLowerCase();
                String stateName = stateNameObj == null ? "" : stateNameObj.toString();

                boolean isHttpConnector =
                        protocol.contains("http")
                                || "http".equals(scheme)
                                || "https".equals(scheme);

                if (!isHttpConnector) {
                    continue;
                }

                if (stateNameObj != null && !"STARTED".equalsIgnoreCase(stateName)) {
                    continue;
                }

                String port = null;

                if (localPortObj != null) {
                    int localPort = Integer.parseInt(localPortObj.toString());
                    if (localPort > 0) {
                        port = String.valueOf(localPort);
                    }
                }

                if (port == null && portObj != null) {
                    int p = Integer.parseInt(portObj.toString());
                    if (p > 0) {
                        port = String.valueOf(p);
                    }
                }

                if (port != null) {
                    ports.add(port);
                }
            }
        } catch (Throwable ignored) {
        }

        if (!ports.isEmpty()) {
            return String.join(",", ports);
        }

        // 2. 兜底读取 conf/server.xml
        ports.addAll(readTomcatPortsFromServerXml());

        return ports.isEmpty() ? "" : String.join(",", ports);
    }

    private static Object getAttributeQuietly(MBeanServer mBeanServer,
                                              ObjectName objectName,
                                              String attributeName) {
        try {
            return mBeanServer.getAttribute(objectName, attributeName);
        } catch (Throwable e) {
            return null;
        }
    }

    private static Set<String> readTomcatPortsFromServerXml() {
        Set<String> ports = new LinkedHashSet<>();

        String catalinaBase = System.getProperty("catalina.base");
        String catalinaHome = System.getProperty("catalina.home");

        readServerXmlPorts(catalinaBase, ports);
        readServerXmlPorts(catalinaHome, ports);

        return ports;
    }

    private static void readServerXmlPorts(String catalinaPath, Set<String> ports) {
        if (catalinaPath == null || catalinaPath.trim().isEmpty()) {
            return;
        }

        File serverXml = new File(catalinaPath, "conf/server.xml");

        if (!serverXml.exists() || !serverXml.isFile()) {
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // 防止 XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(serverXml);
            NodeList connectors = document.getElementsByTagName("Connector");

            for (int i = 0; i < connectors.getLength(); i++) {
                Element connector = (Element) connectors.item(i);

                String port = connector.getAttribute("port");
                String protocol = connector.getAttribute("protocol");
                String scheme = connector.getAttribute("scheme");

                protocol = protocol == null ? "" : protocol.toLowerCase();
                scheme = scheme == null ? "" : scheme.toLowerCase();

                boolean isHttpConnector =
                        protocol.contains("http")
                                || "http".equals(scheme)
                                || "https".equals(scheme)
                                || protocol.isEmpty();

                if (!isHttpConnector) {
                    continue;
                }

                if (port != null && port.matches("\\d+") && Integer.parseInt(port) > 0) {
                    ports.add(port);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
