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

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Class to render a byte count as a human-readable string such as "1 KB" or "1 GB" instead of a raw number
 * such as "1024" or "1073741824".  See format method for examples.  Note that the human-readable values are
 * rendered in a rough way (e.g. if it is a little over a KB we call it a KB).  Use the format2() methods to
 * display the exact byte count after the human-readable rough count.
 * <p><br></p>
 * This class does not extend the JDK Format class because the idea was to KISS!
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class ByteFormat {

    private static final int BYTE = 1;
    private static final int KILOBYTE = 1024;
    private static final int MEGABYTE = 1024 * KILOBYTE;
    private static final int GIGABYTE = 1024 * MEGABYTE;

    private static final long LIMITS[] = {GIGABYTE, MEGABYTE, KILOBYTE, BYTE};

    private final NumberFormat numberFormat;
    private String[] names;

    /**
     * Construct a ByteFormat() instance with the default names[] array and min/max fraction digits.
     *
     * @since 1.0.0
     */
    public ByteFormat() {
        numberFormat = NumberFormat.getIntegerInstance(Locale.ENGLISH);
        names = new String[]{" GB", " MB", " KB", " byte"};
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(1);
    }

    /**
     * Given a raw byte count such as 1024 or 1048576, format it in human-readable form such as
     * "1 KB" or "1 GB".
     * <p><br></p>
     * Here are some example results using the default min/max values for fraction digits:
     * <p><br></p>
     * 0                   - 0 bytes
     * 1                   - 1 byte
     * 1023                - 1,023 bytes
     * 1024 (1 KB)         - 1 KB
     * 1025                - 1 KB
     * 2000                - 2 KB
     * 1048575             - 1,024 KB
     * 1048576 (1 MB)      - 1 MB
     * 1048577             - 1 MB
     * 5000000             - 4.8 MB
     * 1073741824 (1 GB)   - 1 GB
     *
     * @param count int
     * @return String
     * @throws NumberFormatException Thrown if specified count is less than 0.
     * @since 1.0.0
     */
    public String format(int count)     {
        if (count < 0) {
            throw new NumberFormatException("ByteFormat: Byte count cannot be less than 0");
        }

        for (int i = 0; i < names.length; ++i) {
            if (count < LIMITS[i]) {
                continue;
            }

            final float fVal = ((float) count) / LIMITS[i];

            // for KB, MB and GB we don't ever add an "s", but we do for "byte" if the count is other than 1
            final String name = i == names.length - 1 && fVal != 1.0 ? names[i] + "s" : names[i];

            synchronized (numberFormat) {
                return numberFormat.format(fVal) + name;
            }
        }

        // if we're here, num must have been 0
        return "0" + names[names.length - 1] + "s";
    }

    /**
     * Similar to format(int), but the raw byte count is placed in parentheses following the formatted value (if the
     * value is greater than 1023 bytes).
     * e.g.:
     * <p><br></p>
     * 1023 - 1,023 bytes
     * 1025 - 1 KB (1025 bytes)
     * @param count int
     * @return String
     * @since 1.0.0
     */
    public String format2(int count) {
        if (count < KILOBYTE) {
            return format(count);
        }

        synchronized (numberFormat) {
            return format(count) + " (" + numberFormat.format(count) + " bytes)";
        }
    }

    /**
     * Same as format(int), but accepts a long.  Note that the maximum unit this class can deal with is
     * gigabytes, so if the specified count is very large it will still be expressed in terms of
     * gigabytes, e.g. "5,000,000 GB".
     * <p><br></p>
     * Example results:<pre>
     * format(Integer.MAX_VALUE + 1L)   - 2 GB
     * format(Long.MAX_VALUE)           - 8,589,934,592 GB</pre>
     * @param count int
     * @return String
     * @since 1.0.0
     */
    public String format(long count) {
        if (count <= Integer.MAX_VALUE) {
            return format((int) count);
        }

        final double dVal = ((double) count) / LIMITS[0];
        synchronized (numberFormat) {
            return numberFormat.format(dVal) + names[0];
        }
    }

    /**
     * Similar to {@link #format(long)}, but the raw byte count is placed in parentheses following the formatted value (if the
     * value is greater than 1023 bytes).
     * e.g.:<pre>
     * 1023 - 1,023 bytes
     * 1025 - 1 KB (1025 bytes)</pre>
     * @param count int
     * @return String
     * @since 1.0.0
     */
    public String format2(long count) {
        if (count < KILOBYTE) {
            return format(count);
        }

        synchronized (numberFormat) {
            return format(count) + " (" + numberFormat.format(count) + " bytes)";
        }
    }

    /**
     * Set the minimum number of fraction digits.  (See constructor for default.)
     * @param d int
     * @since 1.0.0
     */
    public void setMinimumFractionDigits(int d) {
        numberFormat.setMinimumFractionDigits(d);
    }

    /**
     * Set the minimum number of fraction digits.  (See constructor for default.)
     * @param d int
     * @since 1.9.0
     */
    public ByteFormat minimumFractionDigits(int d) {
        this.setMinimumFractionDigits(d);
        return this;
    }

    /**
     * Set the maximum number of fraction digits.  (See constructor for default.)
     * @param d int
     * @since 1.0.0
     */
    public void setMaximumFractionDigits(int d) {
        numberFormat.setMaximumFractionDigits(d);
    }

    /**
     * Set the maximum number of fraction digits.  (See constructor for default.)
     * @param d int
     * @since 1.9.0
     */
    public ByteFormat maximumFractionDigits(int d) {
        this.setMaximumFractionDigits(d);
        return this;
    }

    /**
     * Set the names[] array to something other than the default one (see constructor).  You could do this,
     * for example, if you wanted to spell out "gigabytes" instead of using "GB".
     * Note:  The new array must be the same size as the original.  (There is no error checking to enforce this.)
     * @param names String[]
     * @since 1.0.0
     */
    public void setNames(String[] names) {
        if (names == null) {
            this.names = null;
        } else {
            this.names = names.clone();
        }
    }

    /**
     * Set the names[] array to something other than the default one (see constructor).  You could do this,
     * for example, if you wanted to spell out "gigabytes" instead of using "GB".
     * Note:  The new array must be the same size as the original.  (There is no error checking to enforce this.)
     * @param names String[]
     * @since 1.9.0
     */
    public ByteFormat names(String[] names) {
        this.setNames(names);
        return this;
    }

}
