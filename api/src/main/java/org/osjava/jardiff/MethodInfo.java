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
 * A class to hold information about a method.
 *
 * @author <a href="mailto:antony@cyberiantiger.org">Antony Riley</a>
 */
public final class MethodInfo extends AbstractMemberInfo
{
    /**
     * The method descriptor.
     */
    private final String desc;

    /**
     * The signature of the method.
     */
    private final String signature;

    /**
     * An array of the exceptions thrown by this method.
     */
    private final String[] exceptions;

    /**
     * Create a new MethodInfo with the specified parameters.
     *
     * @param className The name of the class this method belongs to
     * @param access The access flags for the method.
     * @param name The name of the method.
     * @param signature The signature of the method.
     * @param exceptions The exceptions thrown by the method.
     */
    public MethodInfo(final String className, final int access, final String name, final String desc, final String signature,
                      final String[] exceptions) {
        super(className, access, name);
        this.desc = desc;
        this.signature = signature;
        this.exceptions = exceptions;
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
     * Get the array of exceptions which can be thrown by the method.
     *
     * @return the exceptions as a String[] of internal names.
     */
    public final String[] getExceptions() {
        return exceptions;
    }

    public String toString() {
        return "desc["+desc+"], sig["+signature+"], throws "+(null != exceptions ? Arrays.asList(exceptions) : "nil");
    }
}
