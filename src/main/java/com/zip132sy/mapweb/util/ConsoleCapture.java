package com.zip132sy.mapweb.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 控制台输出捕获工具。
 * 通过反射动态挂载 Log4j2 的 Appender，捕获命令执行期间产生的日志输出。
 *
 * 使用反射的原因：不同服务端（Spigot / Paper）的 Log4j 版本与包路径可能不同，
 * 直接 import 会导致编译或运行时找不到类。反射方式更稳妥。
 *
 * 注意：Appender 是全局的，多个请求同时捕获时输出可能混合。
 */
public final class ConsoleCapture {

    /** 捕获到的日志行 */
    private final List<String> lines = Collections.synchronizedList(new ArrayList<String>());

    private Object appender;
    private Object logger;
    private boolean attached = false;

    /**
     * 开始捕获。
     *
     * @return 是否成功挂载 Appender
     */
    public boolean start() {
        try {
            Class<?> logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
            Method getLogger = logManagerClass.getMethod("getLogger", String.class);
            logger = getLogger.invoke(null, "Minecraft");

            Class<?> appenderClass = Class.forName("org.apache.logging.log4j.core.Appender");
            Class<?> logEventClass = Class.forName("org.apache.logging.log4j.core.LogEvent");

            // 用动态代理实现 Appender 接口
            appender = java.lang.reflect.Proxy.newProxyInstance(
                    appenderClass.getClassLoader(),
                    new Class<?>[]{appenderClass},
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("getName".equals(name)) {
                                return "MapWebConsoleCapture";
                            }
                            if ("isStarted".equals(name)) {
                                return true;
                            }
                            if ("append".equals(name) && args != null && args.length > 0) {
                                try {
                                    Method getMessage = logEventClass.getMethod("getMessage");
                                    Object message = getMessage.invoke(args[0]);
                                    if (message != null) {
                                        Method getFormattedMessage = message.getClass().getMethod("getFormattedMessage");
                                        Object text = getFormattedMessage.invoke(message);
                                        if (text != null) {
                                            lines.add(text.toString());
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // 单条日志解析失败忽略
                                }
                                return null;
                            }
                            if ("equals".equals(name)) {
                                return proxy == args[0];
                            }
                            if ("hashCode".equals(name)) {
                                return System.identityHashCode(proxy);
                            }
                            if ("toString".equals(name)) {
                                return "MapWebConsoleCapture";
                            }
                            // 其余方法返回默认值
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class) {
                                return false;
                            }
                            if (returnType == int.class) {
                                return 0;
                            }
                            return null;
                        }
                    });

            // 挂载到 logger
            Class<?> coreLoggerClass = Class.forName("org.apache.logging.log4j.core.Logger");
            if (!coreLoggerClass.isInstance(logger)) {
                return false;
            }
            Method addAppender = coreLoggerClass.getMethod("addAppender", appenderClass);
            addAppender.invoke(logger, appender);
            attached = true;
            return true;
        } catch (Throwable t) {
            attached = false;
            return false;
        }
    }

    /**
     * 停止捕获并摘除 Appender。
     */
    public void stop() {
        if (!attached || logger == null || appender == null) {
            return;
        }
        try {
            Class<?> appenderClass = Class.forName("org.apache.logging.log4j.core.Appender");
            Class<?> coreLoggerClass = Class.forName("org.apache.logging.log4j.core.Logger");
            Method removeAppender = coreLoggerClass.getMethod("removeAppender", appenderClass);
            removeAppender.invoke(logger, appender);
        } catch (Throwable ignored) {
            // 摘除失败忽略
        } finally {
            attached = false;
        }
    }

    /**
     * 获取捕获到的所有输出行。
     */
    public List<String> getLines() {
        synchronized (lines) {
            return new ArrayList<String>(lines);
        }
    }

    /**
     * 把捕获到的输出拼成单个字符串。
     */
    public String getText() {
        StringBuilder sb = new StringBuilder();
        synchronized (lines) {
            for (String line : lines) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
