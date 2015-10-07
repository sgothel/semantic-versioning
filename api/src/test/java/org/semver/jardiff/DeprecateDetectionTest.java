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
package org.semver.jardiff;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.osjava.jardiff.ClassInfo;
import org.osjava.jardiff.JarDiff;
import org.osjava.jardiff.SimpleDiffCriteria;
import org.semver.Delta.Deprecate;
import org.semver.Delta.Difference;
import org.semver.Dumper;

public class DeprecateDetectionTest {

  public static abstract class InheritanceRoot {
    public abstract void aMethod();
  }

  public static class DirectDescendant extends InheritanceRoot {
    @Override
    public void aMethod() {}
  }

  public static class ClassA extends InheritanceRoot {
    @Override
    public void aMethod() {}

    public int aField = 0;
  }

  public static class ClassB extends DirectDescendant {
	  @Override
	  @Deprecated
	  public void aMethod() {}

	  @Deprecated
	  public int aField = 0;
  }

  @Test
  public void shouldInheritedMethodMatchImplementedMethod() throws Exception {
    /**
     * The situation we are testing is as follows:
     * Abstract class InheritanceRoot is initially implemented directly by ClassA.
     * ClassA is later modified to extend another implementation of InheritanceRoot
     * and the methods required by InheritanceRoot are now removed from ClassA directly,
     * and instead inherited from the new parent, DirectDescendant. For the purposes of
     * this test, this new ClassA is represented by ClassB (as we can't have the same
     * class declared twice in a test -- in real life, this would both be ClassA's,
     * in different jars).
     */
    final Map<String, ClassInfo> oldClassInfoMap = new HashMap<String, ClassInfo>();
    final Map<String, ClassInfo> newClassInfoMap = new HashMap<String, ClassInfo>();
    final JarDiff jd = new JarDiff();
    addClassInfo(oldClassInfoMap, ClassA.class, jd);
    addClassInfo(oldClassInfoMap, DirectDescendant.class, jd);
    addClassInfo(oldClassInfoMap, InheritanceRoot.class, jd);
    addClassInfo(newClassInfoMap, ClassB.class, jd);
    addClassInfo(newClassInfoMap, DirectDescendant.class, jd);
    addClassInfo(newClassInfoMap, InheritanceRoot.class, jd);

    // Make B look like A
    newClassInfoMap.put("org/semver/jardiff/DeprecateDetectionTest$ClassA",
            newClassInfoMap.get("org/semver/jardiff/DeprecateDetectionTest$ClassB"));
    newClassInfoMap.remove("org/semver/jardiff/DeprecateDetectionTest$ClassB");
    final DifferenceAccumulatingHandler handler = new DifferenceAccumulatingHandler();
    jd.diff(handler, new SimpleDiffCriteria(true),
        "0.1.0", "0.2.0", oldClassInfoMap, newClassInfoMap);

    // Dumper.dump(handler.getDelta());
    Dumper.dumpFullStats(handler.getDelta(), 4, System.out);

    final Set<Difference> differences = handler.getDelta().getDifferences();
	Assert.assertEquals("differences found", 3, differences.size());
	// Naive search for Deprecate.
	boolean hasDeprecate = false;
	for (final Difference d : differences) {
		if (d instanceof Deprecate)
			hasDeprecate = true;
	}
	Assert.assertTrue("No Delta.Deprecate found", hasDeprecate);
  }

  private void addClassInfo(final Map<String, ClassInfo> classMap, final Class klass, final JarDiff jd) throws Exception {
    final ClassInfo classInfo = jd.loadClassInfo(new ClassReader(klass.getName()));
    classMap.put(classInfo.getName(), classInfo);
  }
}
