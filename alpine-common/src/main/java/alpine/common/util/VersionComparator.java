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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that compares semantic versions from one to another.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class VersionComparator {

    private final int major;
    private final int minor;
    private final int revision;
    private boolean isSnapshot;
    private int prereleaseNumber;

    /**
     * Constructs a new VersionComparator using the specified semantic version.
     * @param version the semantic version
     */
    public VersionComparator(String version) {
        final int[] versions = parse(version);
        major = versions[0];
        minor = versions[1];
        revision = versions[2];

        if (versions[3] > 0) {
            isSnapshot = true;
            prereleaseNumber = versions[3];
        }
    }

    /**
     * Parses the version.
     * @param version the version to parse
     * @return an int array consisting of major, minor, revision, and suffix
     */
    private int[] parse(String version) {
        final Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)-?(SNAPSHOT)?\\.?(\\d*)?").matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("Malformed version string: " + version);
        }

        return new int[] {Integer.parseInt(m.group(1)),   // major
                Integer.parseInt(m.group(2)),             // minor
                Integer.parseInt(m.group(3)),             // revision
                m.group(4) == null ? 0                    // no SNAPSHOT suffix
                        : m.group(5).isEmpty() ? 0        // "SNAPSHOT"
                        : Integer.parseInt(m.group(5)),   // "SNAPSHOT.123"
        };
    }

    /**
     * Determines if the specified VersionComparator is newer than this instance.
     * @param comparator a VersionComparator to compare to
     * @return true if specified version if newer, false if not
     */
    public boolean isNewerThan(VersionComparator comparator) {
        if (this.major > comparator.getMajor()) {
            return true;
        }  else if (this.major == comparator.getMajor() && this.minor > comparator.getMinor()) {
            return true;
        } else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision > comparator.getRevision()) {
            return true;
        } else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision == comparator.getRevision() && this.prereleaseNumber > comparator.getPrereleaseNumber()) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the specified VersionComparator is older than this instance.
     * @param comparator a VersionComparator to compare to
     * @return true if specified version if older, false if not
     */
    public boolean isOlderThan(VersionComparator comparator) {
        if (this.major < comparator.getMajor()) {
            return true;
        } else if (this.major == comparator.getMajor() && this.minor < comparator.getMinor()) {
            return true;
        } else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision < comparator.getRevision()) {
            return true;
        } else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision == comparator.getRevision() && this.prereleaseNumber < comparator.getPrereleaseNumber()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof VersionComparator) {
            final VersionComparator comparator =  (VersionComparator) object;
            return this.major == comparator.getMajor()
                    && this.minor == comparator.getMinor()
                    && this.revision == comparator.getRevision()
                    && this.isSnapshot == comparator.isSnapshot()
                    && this.prereleaseNumber == comparator.getPrereleaseNumber();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 1000 * (major + 1) + 100 * (minor + 1) + 10 * (revision + 1) + (prereleaseNumber + 1);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    public int getPrereleaseNumber() {
        return prereleaseNumber;
    }

    @Override
    public String toString() {
        // Do not change this. Upgrade logic depends on the format and that the format can be parsed by this class
        final StringBuilder sb = new StringBuilder();
        sb.append(major).append(".").append(minor).append(".").append(revision);
        if (isSnapshot) {
            sb.append("-SNAPSHOT.").append(prereleaseNumber);
        }
        return sb.toString();
    }

}
