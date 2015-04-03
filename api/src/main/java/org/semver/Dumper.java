/**
 * Copyright 2012-2014 Julien Eluard and contributors
 * This project includes software developed by Julien Eluard: https://github.com/jeluard/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.semver;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osjava.jardiff.AbstractInfo;
import org.osjava.jardiff.ClassInfo;
import org.osjava.jardiff.FieldInfo;
import org.osjava.jardiff.MethodInfo;
import org.osjava.jardiff.Tools;
import org.semver.Delta.Difference;

/**
 *
 * Helper methods to dump {@link Delta}.
 *
 */
public class Dumper {

    private Dumper() {
    }

    protected static String extractActionType(final Difference difference) {
        final String actionType = difference.getClass().getSimpleName();
        return actionType.endsWith("e")?actionType+"d":actionType+"ed";
    }

    protected static String extractInfoType(final AbstractInfo info) {
        final String simpleClassName = info.getClass().getSimpleName();
        return simpleClassName.substring(0, simpleClassName.indexOf("Info"));
    }

    protected static String extractDetails(final Difference difference) {
        if (difference instanceof Delta.Change) {
            final Delta.Change change = (Delta.Change) difference;
            return extractChangeDetails(difference.getInfo(), change.getModifiedInfo());
        } else if (difference instanceof Delta.CompatChange) {
            final Delta.CompatChange change = (Delta.CompatChange) difference;
            return extractCompatChangeDetails(difference.getInfo(), change.getModifiedInfo());
        } else {
            return extractDetails(difference.getInfo())+", access["+extractAccessDetails(difference.getInfo())+"]";
        }
    }

    protected static String extractDetails(final AbstractInfo info) {
        final StringBuilder builder = new StringBuilder();
        if (!(info instanceof ClassInfo)) {
            builder.append(info.getName());
            builder.append(", ").append(info.toString());
        }
        return builder.toString();
    }

    protected static String extractChangeDetails(final AbstractInfo previousInfo, final AbstractInfo currentInfo) {
        final StringBuilder builder = new StringBuilder();
        if (!(previousInfo instanceof ClassInfo)) {
            builder.append(previousInfo.getName());
            final String pSig = previousInfo.getSignature();
            final String pDesc = previousInfo.getDesc();
            final String cSig = currentInfo.getSignature();
            final String cDesc = currentInfo.getDesc();
            if( null == pSig && null != cSig ||
                null != pSig && !pSig.equals(cSig) ) {
                builder.append(", sig[").append(pSig).append(" -> ").append(cSig).append("]");
            }
            if( null == pDesc && null != cDesc ||
                null != pDesc && !pDesc.equals(cDesc) ) {
                builder.append(", desc[").append(pDesc).append(" -> ").append(cDesc).append("]");
            }
        }
        if( previousInfo instanceof FieldInfo ) {
            final FieldInfo fPreInfo = (FieldInfo)previousInfo;
            final FieldInfo fCurInfo = (FieldInfo)currentInfo;
            final Object preValue = fPreInfo.getValue();
            final Object curValue = fCurInfo.getValue();
            final String preType = null != preValue ? preValue.getClass().getName() : "nil";
            final String curType = null != curValue ? curValue.getClass().getName() : "nil";
            if (Tools.isFieldTypeChange(preValue, curValue)) {
                builder.append(", type[").append(preType)
                .append(" -> ").append(curType).append("]");
            }
        }
        builder.append(", access[");
        return extractAccessDetails(builder, previousInfo, currentInfo).append("]").toString().trim();
    }

    protected static String extractCompatChangeDetails(final AbstractInfo previousInfo, final AbstractInfo currentInfo) {
        final StringBuilder builder = new StringBuilder();
        if (!(previousInfo instanceof ClassInfo)) {
            builder.append(previousInfo.getName());
        }
        if( previousInfo instanceof MethodInfo ) {
            final MethodInfo mPreInfo = (MethodInfo)previousInfo;
            final MethodInfo mCurInfo = (MethodInfo)currentInfo;
            final String[] preThrows = mPreInfo.getExceptions();
            final String[] curThrows = mCurInfo.getExceptions();
            if (Tools.isThrowsClauseChange(preThrows, curThrows)) {
                final HashSet<String> preThrowsSet;
                final HashSet<String> curThrowsSet;
                if( null != preThrows ) {
                    preThrowsSet = new HashSet<String>(Arrays.asList(preThrows));
                } else {
                    preThrowsSet = new HashSet<String>();
                }
                if( null != curThrows ) {
                    curThrowsSet = new HashSet<String>(Arrays.asList(curThrows));
                } else {
                    curThrowsSet = new HashSet<String>();
                }
                builder.append(", throws[").append(preThrowsSet.toString())
                .append(" -> ").append(curThrowsSet.toString()).append("]");
            }
        } else if( previousInfo instanceof FieldInfo ) {
            final FieldInfo fPreInfo = (FieldInfo)previousInfo;
            final FieldInfo fCurInfo = (FieldInfo)currentInfo;
            final Object preValue = fPreInfo.getValue();
            final Object curValue = fCurInfo.getValue();
            final String preType = null != preValue ? preValue.getClass().getName() : "nil";
            final String curType = null != curValue ? curValue.getClass().getName() : "nil";
            if (Tools.isFieldTypeChange(preValue, curValue)) {
                builder.append(", type[").append(preType)
                .append(" -> ").append(curType).append("]");
            }
            if (Tools.isFieldValueChange(preValue, curValue)) {
                builder.append(", value[").append(preValue)
                .append(" -> ").append(curValue).append("]");
            }
        }
        builder.append(", access[");
        return extractAccessDetails(builder, previousInfo, currentInfo).append("]").toString().trim();
    }

    protected static void accumulateAccessDetails(final String access, final boolean previousAccess, final boolean currentAccess, final List<String> added, final List<String> removed) {
        if (previousAccess != currentAccess) {
            if (previousAccess) {
                removed.add(access);
            } else {
                added.add(access);
            }
        }
    }

    protected static String extractAccessDetails(final AbstractInfo previousInfo, final AbstractInfo currentInfo) {
        return extractAccessDetails(new StringBuilder(), previousInfo, currentInfo).toString().trim();
    }
    protected static StringBuilder extractAccessDetails(final StringBuilder details, final AbstractInfo previousInfo, final AbstractInfo currentInfo) {
        final List<String> added = new LinkedList<String>();
        final List<String> removed = new LinkedList<String>();
        accumulateAccessDetails("abstract", previousInfo.isAbstract(), currentInfo.isAbstract(), added, removed);
        accumulateAccessDetails("annotation", previousInfo.isAnnotation(), currentInfo.isAnnotation(), added, removed);
        accumulateAccessDetails("bridge", previousInfo.isBridge(), currentInfo.isBridge(), added, removed);
        accumulateAccessDetails("enum", previousInfo.isEnum(), currentInfo.isEnum(), added, removed);
        accumulateAccessDetails("final", previousInfo.isFinal(), currentInfo.isFinal(), added, removed);
        accumulateAccessDetails("interface", previousInfo.isInterface(), currentInfo.isInterface(), added, removed);
        accumulateAccessDetails("native", previousInfo.isNative(), currentInfo.isNative(), added, removed);
        accumulateAccessDetails("package-private", previousInfo.isPackagePrivate(), currentInfo.isPackagePrivate(), added, removed);
        accumulateAccessDetails("private", previousInfo.isPrivate(), currentInfo.isPrivate(), added, removed);
        accumulateAccessDetails("protected", previousInfo.isProtected(), currentInfo.isProtected(), added, removed);
        accumulateAccessDetails("public", previousInfo.isPublic(), currentInfo.isPublic(), added, removed);
        accumulateAccessDetails("static", previousInfo.isStatic(), currentInfo.isStatic(), added, removed);
        accumulateAccessDetails("strict", previousInfo.isStrict(), currentInfo.isStrict(), added, removed);
        accumulateAccessDetails("super", previousInfo.isSuper(), currentInfo.isSuper(), added, removed);
        accumulateAccessDetails("synchronized", previousInfo.isSynchronized(), currentInfo.isSynchronized(), added, removed);
        accumulateAccessDetails("synthetic", previousInfo.isSynthetic(), currentInfo.isSynthetic(), added, removed);
        accumulateAccessDetails("transcient", previousInfo.isTransient(), currentInfo.isTransient(), added, removed);
        accumulateAccessDetails("varargs", previousInfo.isVarargs(), currentInfo.isVarargs(), added, removed);
        accumulateAccessDetails("volatile", previousInfo.isVolatile(), currentInfo.isVolatile(), added, removed);
        if (!added.isEmpty()) {
            details.append("added: ");
            for (final String access : added) {
                details.append(access).append(" ");
            }
        }
        if (!removed.isEmpty()) {
            details.append("removed: ");
            for (final String access : removed) {
                details.append(access).append(" ");
            }
        }
        return details;
    }

    protected static void accumulateAccessDetails(final String access, final boolean hasAccess, final List<String> accessList) {
        if (hasAccess) {
            accessList.add(access);
        }
    }

    protected static String extractAccessDetails(final AbstractInfo info) {
        final List<String> accessList = new LinkedList<String>();
        accumulateAccessDetails("abstract", info.isAbstract(), accessList);
        accumulateAccessDetails("annotation", info.isAnnotation(), accessList);
        accumulateAccessDetails("bridge", info.isBridge(), accessList);
        accumulateAccessDetails("enum", info.isEnum(), accessList);
        accumulateAccessDetails("final", info.isFinal(), accessList);
        accumulateAccessDetails("interface", info.isInterface(), accessList);
        accumulateAccessDetails("native", info.isNative(), accessList);
        accumulateAccessDetails("package-private", info.isPackagePrivate(), accessList);
        accumulateAccessDetails("private", info.isPrivate(), accessList);
        accumulateAccessDetails("protected", info.isProtected(), accessList);
        accumulateAccessDetails("public", info.isPublic(), accessList);
        accumulateAccessDetails("static", info.isStatic(), accessList);
        accumulateAccessDetails("strict", info.isStrict(), accessList);
        accumulateAccessDetails("super", info.isSuper(), accessList);
        accumulateAccessDetails("synchronized", info.isSynchronized(), accessList);
        accumulateAccessDetails("synthetic", info.isSynthetic(), accessList);
        accumulateAccessDetails("transcient", info.isTransient(), accessList);
        accumulateAccessDetails("varargs", info.isVarargs(), accessList);
        accumulateAccessDetails("volatile", info.isVolatile(), accessList);
        final StringBuilder details = new StringBuilder();
        if (!accessList.isEmpty()) {
            for (final String access : accessList) {
                details.append(access).append(" ");
            }
        }
        return details.toString().trim();
    }

    /**
     * Dumps on {@link System#out} all differences, sorted by class name.
     * @param delta the delta to be dumped
     */
    public static void dump(final Delta delta) {
        dump(delta, System.out);
    }

    /**
     * Dumps on <code>out</code> all differences, sorted by class name.
     * @param delta the delta to be dumped
     * @param out
     */
    public static void dump(final Delta delta, final PrintStream out) {
        final List<Difference> sortedDifferences = new LinkedList<Difference>(delta.getDifferences());
        Collections.sort(sortedDifferences);
        dump(sortedDifferences, out);
    }

    /**
     * Dumps on <code>out</code> all of the given sorted differences.
     * @param sortedDifferences the sorted differences to be dumped
     * @param out the print output stream
     */
    public static void dump(final List<Difference> sortedDifferences, final PrintStream out) {
        String currentClassName = "";
        for (final Difference difference : sortedDifferences) {
            if (!currentClassName.equals(difference.getClassName())) {
                out.println("Class "+difference.getClassName());
            }
            out.println(" "+extractActionType(difference)+" "+extractInfoType(difference.getInfo())+
                        " "+extractDetails(difference));
            currentClassName = difference.getClassName();
        }
    }

    /**
     * Dumps on <code>out</code> all differences separated by its type in the order
     * <code>remove</code>, <code>change</code>, <code>deprecate</code> and <code>add</code>.
     * <p>
     * Prepends statistics per class regarding difference type.
     * </p>
     * @param delta the delta to be dumped
     * @param iwidth the integer width for formated integer counter
     * @param out the print output stream
     */
    public static void dumpFullStats(final Delta delta, final int iwidth, final PrintStream out) {
        final Set<Difference> diffs = delta.getDifferences();

        final List<Difference> diffsAdd = new ArrayList<Difference>();
        final List<Difference> diffsChange = new ArrayList<Difference>();
        final List<Difference> diffsCompatChange = new ArrayList<Difference>();
        final List<Difference> diffsDeprecate = new ArrayList<Difference>();
        final List<Difference> diffsRemove = new ArrayList<Difference>();
        final Map<String, DiffCount> className2DiffCount = new HashMap<String, DiffCount>();

        int maxClassNameLen = 0;

        for(final Iterator<Difference> iter = diffs.iterator(); iter.hasNext(); ) {
            final Difference diff = iter.next();
            final String className = diff.getClassName();
            maxClassNameLen = Math.max(maxClassNameLen, className.length());

            DiffCount dc = className2DiffCount.get(className);
            if( null == dc ) {
                dc = new DiffCount(className);
                className2DiffCount.put(className, dc);
            }

            if( diff instanceof Delta.Add ) {
                diffsAdd.add(diff);
                dc.additions++;
            } else if( diff instanceof Delta.Change ) {
                diffsChange.add(diff);
                dc.changes++;
            } else if( diff instanceof Delta.CompatChange ) {
                diffsCompatChange.add(diff);
                dc.compatChanges++;
            } else if( diff instanceof Delta.Deprecate ) {
                diffsDeprecate.add(diff);
                dc.deprecates++;
            } else if( diff instanceof Delta.Remove ) {
                diffsRemove.add(diff);
                dc.removes++;
            }
        }
        Collections.sort(diffsAdd);
        Collections.sort(diffsChange);
        Collections.sort(diffsCompatChange);
        Collections.sort(diffsDeprecate);
        Collections.sort(diffsRemove);

        final List<String> classNames = new ArrayList<String>(className2DiffCount.keySet());
        Collections.sort(classNames);

        System.err.println("Summary: "+diffs.size()+" differences in "+classNames.size()+" classes:");
        System.err.println("  Remove "+diffsRemove.size()+
                           ", Change "+diffsChange.size()+
                           ", CompatChange "+diffsCompatChange.size()+
                           ", Deprecate "+diffsDeprecate.size()+
                           ", Add "+diffsAdd.size());
        System.err.printf("%n");

        int iterI = 0;
        for(final Iterator<String> iter = classNames.iterator(); iter.hasNext(); iterI++) {
            final String className = iter.next();
            final DiffCount dc = className2DiffCount.get(className);
            System.err.printf("%"+iwidth+"d/%"+iwidth+"d: %-"+maxClassNameLen+"s: %s%n", iterI, classNames.size(), className, dc.format(iwidth));
        }

        System.err.printf("%n%nRemoves%n%n");
        dump(diffsRemove, System.err);

        System.err.printf("%n%nChanges%n%n");
        dump(diffsChange, System.err);

        System.err.printf("%n%nCompatChanges%n%n");
        dump(diffsCompatChange, System.err);

        System.err.printf("%n%nDeprecates%n%n");
        dump(diffsDeprecate, System.err);

        System.err.printf("%n%nAdditions%n%n");
        dump(diffsAdd, System.err);
        System.err.printf("%n%n");
    }

    static class DiffCount {
        public DiffCount(final String name) { this.name = name; }
        public final String name;
        public int removes;
        public int changes;
        public int compatChanges;
        public int deprecates;
        public int additions;
        public String toString() { return name+": Remove "+removes+", Change "+changes+", CompatChange "+compatChanges+", Deprecate "+deprecates+", Add "+additions; }
        public String format(final int iwidth) {
            return String.format("Remove %"+iwidth+"d, Change %"+iwidth+"d, CompatChange %"+iwidth+"d, Deprecate %"+iwidth+"d, Add %"+iwidth+"d",
                                    removes, changes, compatChanges, deprecates, additions);
        }
    }

}
