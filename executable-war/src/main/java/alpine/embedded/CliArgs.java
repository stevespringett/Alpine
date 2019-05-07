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
package alpine.embedded;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * This class parses command-line arguments. This could be achieved via Apache Commons CLI,
 * JSAPI, jopt-simple, or a host of other libraries, all of which add additional dependencies
 * to support a wide array of use cases, most of which do not apply to the embedded server.
 * <p>
 * Adapted from http://tutorials.jenkov.com/java-howto/java-command-line-argument-parser.html
 */
public class CliArgs {

    private String[] args;
    private final HashMap<String, Integer> switchIndexes = new HashMap<>();
    private final TreeSet<Integer> takenIndexes = new TreeSet<>();
    //private List<String> targets = new ArrayList<>();

    public CliArgs(final String[] args) {
        parse(args);
    }

    public void parse(final String[] arguments) {
        if (arguments != null) {
            this.args = arguments.clone();
        }
        //locate switches.
        switchIndexes.clear();
        takenIndexes.clear();
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                switchIndexes.put(args[i], i);
                takenIndexes.add(i);
            }
        }
    }

    public String[] args() {
        if (args == null) {
            return null;
        } else {
            return args.clone();
        }
    }

    public String arg(final int index) {
        return args[index];
    }

    public boolean switchPresent(final String switchName) {
        return switchIndexes.containsKey(switchName);
    }

    public String switchValue(final String switchName) {
        return switchValue(switchName, null);
    }

    public String switchValue(final String switchName, final String defaultValue) {
        if (!switchIndexes.containsKey(switchName)) {
            return defaultValue;
        }
        final int switchIndex = switchIndexes.get(switchName);
        if (switchIndex + 1 < args.length) {
            takenIndexes.add(switchIndex + 1);
            return args[switchIndex + 1];
        }
        return defaultValue;
    }

    public Integer switchIntegerValue(final String switchName) {
        return switchIntegerValue(switchName, null);
    }

    public Integer switchIntegerValue(final String switchName, final Integer defaultValue) {
        final String switchValue = switchValue(switchName, null);
        if (switchValue == null) {
            return defaultValue;
        }
        return Integer.parseInt(switchValue);
    }

    public Long switchLongValue(final String switchName) {
        return switchLongValue(switchName, null);
    }

    public Long switchLongValue(final String switchName, final Long defaultValue) {
        final String switchValue = switchValue(switchName, null);
        if (switchValue == null) {
            return defaultValue;
        }
        return Long.parseLong(switchValue);
    }

    public Double switchDoubleValue(final String switchName) {
        return switchDoubleValue(switchName, null);
    }

    public Double switchDoubleValue(final String switchName, final Double defaultValue) {
        final String switchValue = switchValue(switchName, null);
        if (switchValue == null) {
            return defaultValue;
        }
        return Double.parseDouble(switchValue);
    }

    public String[] switchValues(final String switchName) {
        if (!switchIndexes.containsKey(switchName)) {
            return new String[0];
        }
        final int switchIndex = switchIndexes.get(switchName);
        int nextArgIndex = switchIndex + 1;
        while (nextArgIndex < args.length && args[nextArgIndex].charAt(0) != '-') {
            takenIndexes.add(nextArgIndex);
            nextArgIndex++;
        }
        String[] values = new String[nextArgIndex - switchIndex - 1];
        for (int j = 0; j < values.length; j++) {
            values[j] = args[switchIndex + j + 1];
        }
        return values;
    }

    public <T> T switchPojo(final Class<T> pojoClass) {
        try {
            final T pojo = pojoClass.newInstance();
            final Field[] fields = pojoClass.getFields();
            for (final Field field : fields) {
                final Class fieldType = field.getType();
                final String fieldName = "-" + field.getName().replace('_', '-');
                if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                    field.set(pojo, switchPresent(fieldName));
                } else if (fieldType.equals(String.class)) {
                    if (switchValue(fieldName) != null) {
                        field.set(pojo, switchValue(fieldName));
                    }
                } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName));
                    }
                } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName).intValue());
                    }
                } else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName).shortValue());
                    }
                } else if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName).byteValue());
                    }
                } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                    if (switchDoubleValue(fieldName) != null) {
                        field.set(pojo, switchDoubleValue(fieldName));
                    }
                } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                    if (switchDoubleValue(fieldName) != null) {
                        field.set(pojo, switchDoubleValue(fieldName).floatValue());
                    }
                } else if (fieldType.equals(String[].class)) {
                    final String[] values = switchValues(fieldName);
                    if (values.length != 0) {
                        field.set(pojo, values);
                    }
                }
            }
            return pojo;
        } catch (Exception e) {
            throw new RuntimeException("Error creating switch POJO", e);
        }
    }

    public String[] targets() {
        String[] targetArray = new String[args.length - takenIndexes.size()];
        int targetIndex = 0;
        for (int i = 0; i < args.length; i++) {
            if (!takenIndexes.contains(i)) {
                targetArray[targetIndex++] = args[i];
            }
        }
        return targetArray;
    }
}