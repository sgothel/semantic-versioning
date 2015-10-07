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

/**
 * Information about a field of a class.
 *
 * @author <a href="mailto:antony@cyberiantiger.org">Antony Riley</a>
 */
public final class FieldInfo extends AbstractMemberInfo
{
    /**
     * The field descriptor for this field.
     */
    private final String desc;

    /**
     * The signature for this field.
     */
    private final String signature;

    /**
     * The initial value of this field.
     */
    private final Object value;

    /**
     * Create a new FieldInfo
     *
     * @param className The name of the class this field belongs to
     * @param access The access flags.
     * @param name The name of the field.
     * @param desc The field descriptor.
     * @param signature The signature of this field.
     * @param value The initial value of the field.
     */
    public FieldInfo(final String className, final int access, final String name, final String desc, final String signature,
                     final Object value) {
        super(className, access, name);
        this.desc = desc;
        this.signature = signature;
        this.value = value;
    }

    @Override
    public final String getDesc() {
        return desc;
    }

    @Override
    public final String getSignature() {
        return signature;
    }

    /**
     * Get the initial value for this fieldinfo
     *
     * @return The initial value.
     */
    public final Object getValue() {
        return value;
    }

    public String toString() {
        final String t = null != value ? value.getClass().getName() : null;
        return "desc["+desc+"], sig["+signature+"], value[type "+t+", data "+value;
    }
}
