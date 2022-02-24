/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.common.util;

import alpine.common.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 * This class contains a collection of commonly used static methods which
 * reveal system-wide information. These provide no security-related fixes
 * to any issue, and are merely available as a convenience.
 *
 * @author Steve Springett
 * @since 1.0
 */
@SuppressWarnings("unused")
public final class SystemUtil {

    // A few of the methods need to be initialized before they can be used
    private static boolean hasInitialized;

    private static boolean isWindows;
    private static boolean isMac;
    private static boolean isLinux;
    private static boolean isUnix;
    private static boolean isSolaris;
    private static String lineEnder;

    private static boolean bit32;
    private static boolean bit64;

    private static final Runtime.Version JAVA_VERSION = Runtime.version();

    private static final String BIT_BUCKET_UNIX = "/dev/null";
    private static final String BIT_BUCKET_WIN = "NUL"; // Yes, it's only one 'L'

    private static final Logger LOGGER = Logger.getLogger(SystemUtil.class);

    /**
     * Private constructor
     */
    private SystemUtil() {
    }

    /**
     * Return true if the host operating system is Windows. Given the currently-supported set of platforms,
     * if this returns false, the OS must be some flavor of UNIX.
     * @return True if Windows, False if not
     * @since 1.0
     */
    public static boolean isWindows() {
        init();
        return isWindows;
    }

    /**
     * Return true if the host operating system is OS X.
     * @return True if OS X, False if not
     * @since 1.0
     */
    public static boolean isMac() {
        init();
        return isMac;
    }

    /**
     * Return true if the host operating system is Linux.
     * @return True if Linux, False if not
     * @since 1.0
     */
    public static boolean isLinux() {
        init();
        return isLinux;
    }

    /**
     * Return true if the host operating system is Unix/Linux.
     * This includes all OS's (except for Mac) which fall in this category.
     * @return True if Unix, False if not
     * @since 1.0
     */
    public static boolean isUnix() {
        init();
        return isUnix;
    }

    /**
     * Return true if the host operating system is SunOS/Solaris.
     * @return True if Solaris, False if not
     * @since 1.0
     */
    public static boolean isSolaris() {
        init();
        return isSolaris;
    }

    /**
     * Return the correct line ender for the host operating system, e.g. \r\n for Windows, \n for UNIX.
     * @return String containing the line ender for the host operating system
     * @since 1.0
     */
    public static String getLineEnder() {
        init();
        return lineEnder;
    }

    /**
     * Return true if the OS architecture model is 32bit.
     * @return True if 32bit, False if not
     * @since 1.0
     */
    public static boolean is32Bit() {
        init();
        return bit32;
    }

    /**
     * Return true if the OS architecture model is 64bit.
     * @return True if 64bit, False if not
     * @since 1.0
     */
    public static boolean is64Bit() {
        init();
        return bit64;
    }

    /**
     * Return the name of the host operating system.
     * @return String containing the name of the host operating system
     * @since 1.0
     */
    public static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Return the OS architecture.
     * @return String containing the OS architecture
     * @since 1.0
     */
    public static String getOsArchitecture() {
        return System.getProperty("os.arch");
    }

    /**
     * Return the OS version.
     * @return String containing the OS version
     * @since 1.0
     */
    public static String getOsVersion() {
        return System.getProperty("os.version");
    }

    /**
     * Return the file separator character that separates components of a file path.
     * This is "/" on UNIX and "\" on Windows.
     * @return String containing the file separator character
     * @since 1.0
     */
    public static String getFileSeparator() {
        return System.getProperty("file.separator");
    }

    /**
     * Return the path separator character used in java.class.path.
     * @return String containing the path separator character
     * @since 1.0
     */
    public static String getPathSeparator() {
        return System.getProperty("path.separator");
    }

    /**
     * Return the username that is executing the current running Java process.
     * @return String containing the username that is executing the current running Java process
     * @since 1.0
     */
    public static String getUserName() {
        return System.getProperty("user.name");
    }

    /**
     * Return the home directory of the user executing the current running Java process.
     * @return String containing the home directory of the user executing the current running Java process
     * @since 1.0
     */
    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    /**
     * Return the Java vendor.
     * @return String containing the Java vendor
     * @since 1.0
     */
    public static String getJavaVendor() {
        return System.getProperty("java.vendor");
    }

    /**
     * Return the Java version.
     * @return JavaVersion containing Java version information
     * @since 1.0
     */
    public static Runtime.Version getJavaVersion() {
        return JAVA_VERSION;
    }

    /**
     * Return the JAVA_HOME environment variable.
     * @return String containing the JAVA_HOME environment variable
     * @since 1.0
     */
    public static String getJavaHome() {
        return System.getProperty("java.home");
    }

    /**
     * Return the temporary directory to be used by Java.
     * @return String containing the temporary directory to be used by Java
     * @since 1.0
     */
    public static String getJavaTempDir() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Return the classpath.
     * @return String containing the classpath
     * @since 1.0
     */
    public static String getClassPath() {
        return System.getProperty("java.class.path");
    }

    /**
     * Return the library path.
     * @return String containing the library path
     * @since 1.0
     */
    public static String getLibraryPath() {
        return System.getProperty("java.library.path");
    }

    /**
     * Return the bit bucket for the OS. '/dev/null' for Unix and 'NUL' for Windows.
     * @return a String containing the bit bucket
     * @since 1.0
     */
    public static String getBitBucket() {
        if (isWindows()) {
            return BIT_BUCKET_WIN;
        } else {
            return BIT_BUCKET_UNIX;
        }
    }

    /**
     * Returns the number of processor cores available to the JVM.
     * @return an integer of the number of processor core
     * @since 1.0.0
     */
    public static int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Returns the number of processor cores available to the JVM.
     * @return an integer of the number of processor core
     * @since 1.9.0
     */
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * Initialize static variables.
     */
    private static void init() {
        if (hasInitialized) {
            return;
        }

        final String osName = System.getProperty("os.name");

        if (osName != null) {
            final String osNameLower = osName.toLowerCase();

            isWindows = osNameLower.contains("windows");
            isMac = osNameLower.contains("mac os x") || osNameLower.contains("darwin");
            isLinux = osNameLower.contains("nux");
            isUnix = osNameLower.contains("nix") || osNameLower.contains("nux");
            isSolaris = osNameLower.contains("sunos") || osNameLower.contains("solaris");
        }

        lineEnder = isWindows ? "\r\n" : "\n";

        final String model = System.getProperty("sun.arch.data.model");
        // sun.arch.data.model=32 // 32 bit JVM
        // sun.arch.data.model=64 // 64 bit JVM
        if (StringUtils.isBlank(model)) {
            bit32 = true;
            bit64 = false;
        } else if ("64".equals(model)) {
            bit32 = false;
            bit64 = true;
        } else {
            bit32 = true;
            bit64 = false;
        }

    }

}
