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
package org.osjava.jardiff;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A specific type of DiffCriteria which is only true for classes, methods
 * and fields which are not synthetic and public.
 *
 * @author <a href="mailto:antony@cyberiantiger.org">Antony Riley</a>
 */
public class PublicDiffCriteria implements DiffCriteria
{
    @Override
    public boolean equals(final Object arg) {
        if (arg == this) {
            return true;
        } else if ( !(arg instanceof PublicDiffCriteria) ) {
            return false;
        }
        return true; // no states
    }
    @Override
    public String toString() { return "PublicDiffCriteria"; }

    /**
     * Check if a class is valid.
     * If the class is not synthetic and public, return true.
     *
     * @param info Info describing the class.
     * @return True if the class meets the criteria, false otherwise.
     */
    public boolean validClass(final ClassInfo info) {
        return !info.isSynthetic() && info.isPublic();
    }

    /**
     * Check if a method is valid.
     * If the method is not synthetic and public, return true.
     *
     * @param info Info describing the method.
     * @return True if the method meets the criteria, false otherwise.
     */
    public boolean validMethod(final MethodInfo info) {
        return !info.isSynthetic() && info.isPublic();
    }

    /**
     * Check if a field is valid.
     * If the method is not synthetic and public, return true.
     *
     * @param info Info describing the field.
     * @return True if the field meets the criteria, false otherwise.
     */
    public boolean validField(final FieldInfo info) {
        return !info.isSynthetic() && info.isPublic();
    }

    /**
     * Check if there is a change between two versions of a class.
     * Returns true if the access flags differ, or if the superclass differs
     * or if the implemented interfaces differ.
     *
     * @param oldInfo Info about the old version of the class.
     * @param newInfo Info about the new version of the class.
     * @return True if the classes differ, false otherwise.
     */
    public boolean differs(final ClassInfo oldInfo, final ClassInfo newInfo) {
        if (Tools.isClassAccessChange(oldInfo.getAccess(), newInfo.getAccess()))
            return true;
        // Yes classes can have a null supername, e.g. java.lang.Object !
        if(oldInfo.getSupername() == null) {
            if(newInfo.getSupername() != null) {
                return true;
            }
        } else if (!oldInfo.getSupername().equals(newInfo.getSupername())) {
            return true;
        }
        final Set<String> oldInterfaces
            = new HashSet(Arrays.asList(oldInfo.getInterfaces()));
        final Set<String> newInterfaces
            = new HashSet(Arrays.asList(newInfo.getInterfaces()));
        if (!oldInterfaces.equals(newInterfaces))
            return true;
        return false;
    }

    @Override
    public boolean differs(final MethodInfo oldInfo, final MethodInfo newInfo) {
        return // Tools.isDescChange(oldInfo.getDesc(), newInfo.getDesc()) ||
               Tools.isMethodAccessChange(oldInfo.getAccess(), newInfo.getAccess()) ||
               Tools.isThrowsClauseChange(oldInfo.getExceptions(), newInfo.getExceptions());
    }
    @Override
    public boolean differsBinary(final MethodInfo oldInfo, final MethodInfo newInfo) {
        return // Tools.isDescChange(oldInfo.getDesc(), newInfo.getDesc()) ||
               Tools.isMethodAccessChange(oldInfo.getAccess(), newInfo.getAccess());
    }

    @Override
    public boolean differs(final FieldInfo oldInfo, final FieldInfo newInfo) {
        return Tools.isFieldAccessChange(oldInfo.getAccess(), newInfo.getAccess()) ||
               // Tools.isFieldTypeChange(oldInfo.getValue(), newInfo.getValue())     ||
               Tools.isFieldValueChange(oldInfo.getValue(), newInfo.getValue());
    }
    @Override
    public boolean differsBinary(final FieldInfo oldInfo, final FieldInfo newInfo) {
        return Tools.isFieldAccessChange(oldInfo.getAccess(), newInfo.getAccess()); // &&
               // Tools.isFieldTypeChange(oldInfo.getValue(), newInfo.getValue());
    }
}
