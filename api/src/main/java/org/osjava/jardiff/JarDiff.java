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

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

/*
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
*/

/**
 * A class to perform a diff between two jar files.
 *
 * @author <a href="mailto:antony@cyberiantiger.org">Antony Riley</a>
 */
public class JarDiff
{
    /**
     * A map containing information about classes which are dependencies.
     * Keys are internal class names.
     * Values are instances of ClassInfo.
     */
    protected Map depClassInfo = new HashMap();

    /**
     * A map containing information about classes in the old jar file.
     * Keys are internal class names.
     * Values are instances of ClassInfo.
     */
    protected Map<String, ClassInfo> oldClassInfo = new TreeMap<String, ClassInfo>();

    /**
     * A map containing information about classes in the new jar file.
     * Keys are internal class names.
     * Values are instances of ClassInfo.
     */
    protected Map<String, ClassInfo> newClassInfo = new TreeMap<String, ClassInfo>();

    /**
     * An array of dependencies which are jar files, or urls.
     */
    private URL[] deps;

    /**
     * A class loader used for loading dependency classes.
     */
    private URLClassLoader depLoader;

    /**
     * The name of the old version.
     */
    private String oldVersion;

    /**
     * The name of the new version.
     */
    private String newVersion;

    /**
     * Class info visitor, used to load information about classes.
     */
    private final ClassInfoVisitor infoVisitor = new ClassInfoVisitor();

    /**
     * Create a new JarDiff object.
     */
    public JarDiff() {
    }

    /**
     * Set the name of the old version.
     *
     * @param oldVersion the name
     */
    public void setOldVersion(final String oldVersion) {
        this.oldVersion = oldVersion;
    }

    /**
     * Get the name of the old version.
     *
     * @return the name
     */
    public String getOldVersion() {
        return oldVersion;
    }

    /**
     * Set the name of the new version.
     *
     * @param newVersion the version
     */
    public void setNewVersion(final String newVersion) {
        this.newVersion = newVersion;
    }

    /**
     * Get the name of the new version.
     *
     * @return the name
     */
    public String getNewVersion() {
        return newVersion;
    }

    /**
     * Set the dependencies.
     *
     * @param deps an array of urls pointing to jar files or directories
     *             containing classes which are required dependencies.
     */
    public void setDependencies(final URL[] deps) {
        this.deps = deps;
    }

    /**
     * Get the dependencies.
     *
     * @return the dependencies as an array of URLs
     */
    public URL[] getDependencies() {
        return deps;
    }

    /**
     * Load classinfo given a ClassReader.
     *
     * @param reader the ClassReader
     * @return the ClassInfo
     */
    public synchronized ClassInfo loadClassInfo(final ClassReader reader)
        throws IOException
    {
        infoVisitor.reset();
        reader.accept(infoVisitor, 0);
        return infoVisitor.getClassInfo();
    }

    /**
     * Load all the classes from the specified URL and store information
     * about them in the specified map.
     * This currently only works for jar files, <b>not</b> directories
     * which contain classes in subdirectories or in the current directory.
     *
     * @param infoMap the map to store the ClassInfo in.
     * @throws DiffException if there is an exception reading info about a
     *                       class.
     */
    private void loadClasses(final Map infoMap, final URL path) throws DiffException {
        try {
            File jarFile = null;
            if(!"file".equals(path.getProtocol()) || path.getHost() != null) {
                // If it's not a local file, store it as a temporary jar file.
                // java.util.jar.JarFile requires a local file handle.
                jarFile = File.createTempFile("jardiff","jar");
                // Mark it to be deleted on exit.
                jarFile.deleteOnExit();
                final InputStream in = path.openStream();
                final OutputStream out = new FileOutputStream(jarFile);
                final byte[] buffer = new byte[4096];
                int i;
                while( (i = in.read(buffer,0,buffer.length)) != -1) {
                    out.write(buffer, 0, i);
                }
                in.close();
                out.close();
            } else {
                // Else it's a local file, nothing special to do.
                jarFile = new File(path.getPath());
            }
            loadClasses(infoMap, jarFile);
        } catch (final IOException ioe) {
            throw new DiffException(ioe);
        }
    }

    /**
     * Load all the classes from the specified URL and store information
     * about them in the specified map.
     * This currently only works for jar files, <b>not</b> directories
     * which contain classes in subdirectories or in the current directory.
     *
     * @param infoMap the map to store the ClassInfo in.
     * @param file the jarfile to load classes from.
     * @throws IOException if there is an IOException reading info about a
     *                     class.
     */
    private void loadClasses(final Map infoMap, final File file) throws DiffException {
        try {
            final JarFile jar = new JarFile(file);
            final Enumeration e = jar.entries();
            while (e.hasMoreElements()) {
                final JarEntry entry = (JarEntry) e.nextElement();
                final String name = entry.getName();
                if (!entry.isDirectory() && name.endsWith(".class")) {
                    final ClassReader reader
                        = new ClassReader(jar.getInputStream(entry));
                    final ClassInfo ci = loadClassInfo(reader);
                    infoMap.put(ci.getName(), ci);
                }
            }
        } catch (final IOException ioe) {
            throw new DiffException(ioe);
        }
    }

    /**
     * Load old classes from the specified URL.
     *
     * @param loc The location of a jar file to load classes from.
     * @throws DiffException if there is an IOException.
     */
    public void loadOldClasses(final URL loc) throws DiffException {
        loadClasses(oldClassInfo, loc);
    }

    /**
     * Load new classes from the specified URL.
     *
     * @param loc The location of a jar file to load classes from.
     * @throws DiffException if there is an IOException.
     */
    public void loadNewClasses(final URL loc) throws DiffException {
        loadClasses(newClassInfo, loc);
    }

    /**
     * Load old classes from the specified File.
     *
     * @param file The location of a jar file to load classes from.
     * @throws DiffException if there is an IOException
     */
    public void loadOldClasses(final File file) throws DiffException {
        loadClasses(oldClassInfo, file);
    }

    /**
     * Load new classes from the specified File.
     *
     * @param file The location of a jar file to load classes from.
     * @throws DiffException if there is an IOException
     */
    public void loadNewClasses(final File file) throws DiffException {
        loadClasses(newClassInfo, file);
    }

    /**
     * Perform a diff sending the output to the specified handler, using
     * the specified criteria to select diffs.
     *
     * @param handler The handler to receive and handle differences.
     * @param criteria The criteria we use to select differences.
     * @throws DiffException when there is an underlying exception, e.g.
     *                       writing to a file caused an IOException
     */
    public void diff(final DiffHandler handler, final DiffCriteria criteria)
        throws DiffException
    {
        diff(handler, criteria, oldVersion, newVersion, oldClassInfo, newClassInfo);
    }

    public void diff(final DiffHandler handler, final DiffCriteria criteria,
        final String oldVersion, final String newVersion,
        final Map<String, ClassInfo> oldClassInfo, final Map<String, ClassInfo> newClassInfo) throws DiffException
    {
        // TODO: Build the name from the MANIFEST rather than the filename
        handler.startDiff(oldVersion, newVersion);

        handler.startOldContents();
        for (final ClassInfo ci : oldClassInfo.values()) {
            if (criteria.validClass(ci)) {
                handler.contains(ci);
            }
        }
        handler.endOldContents();

        handler.startNewContents();
        for (final ClassInfo ci : newClassInfo.values()) {
            if (criteria.validClass(ci)) {
                handler.contains(ci);
            }
        }
        handler.endNewContents();

        final Set<String> onlyOld = new TreeSet<String>(oldClassInfo.keySet());
        final Set<String> onlyNew = new TreeSet<String>(newClassInfo.keySet());
        final Set<String> both = new TreeSet<String>(oldClassInfo.keySet());
        onlyOld.removeAll(newClassInfo.keySet());
        onlyNew.removeAll(oldClassInfo.keySet());
        both.retainAll(newClassInfo.keySet());

        handler.startRemoved();
        for (final String s : onlyOld) {
            final ClassInfo ci = oldClassInfo.get(s);
            if (criteria.validClass(ci)) {
                handler.classRemoved(ci);
            }
        }
        handler.endRemoved();

        handler.startAdded();
        for (final String s : onlyNew) {
            final ClassInfo ci = newClassInfo.get(s);
            if (criteria.validClass(ci)) {
                handler.classAdded(ci);
            }
        }
        handler.endAdded();

        final Set<String> removedMethods = new TreeSet<String>();
        final Set<String> removedFields = new TreeSet<String>();
        final Set<String> addedMethods = new TreeSet<String>();
        final Set<String> addedFields = new TreeSet<String>();
        final Set<String> changedMethods = new TreeSet<String>();
        final Set<String> changedFields = new TreeSet<String>();

        handler.startChanged();
        for (final String s : both) {
            final ClassInfo oci = oldClassInfo.get(s);
            final ClassInfo nci = newClassInfo.get(s);
            if (criteria.validClass(oci) || criteria.validClass(nci)) {
                final Map<String, MethodInfo> oldMethods = oci.getMethodMap();
                final Map<String, FieldInfo> oldFields = oci.getFieldMap();
                final Map<String, MethodInfo> newMethods = nci.getMethodMap();
                final Map<String, FieldInfo> newFields = nci.getFieldMap();

                final Map<String, MethodInfo> extNewMethods = new HashMap<String, MethodInfo>(newMethods);
                final Map<String, FieldInfo> extNewFields = new HashMap<String, FieldInfo>(newFields);

                String superClass = nci.getSupername();
                while (superClass != null && newClassInfo.containsKey(superClass)) {
                    final ClassInfo sci = newClassInfo.get(superClass);
                    for (final Map.Entry<String, FieldInfo> entry : sci.getFieldMap().entrySet()) {
                        if (!(entry.getValue()).isPrivate()
                                && !extNewFields.containsKey(entry.getKey())) {
                            extNewFields.put(entry.getKey(), entry.getValue());
                        }
                    }
                    for (final Map.Entry<String, MethodInfo> entry : sci.getMethodMap().entrySet()) {
                        if (!(entry.getValue()).isPrivate()
                                && !extNewMethods.containsKey(entry.getKey())) {
                            extNewMethods.put(entry.getKey(), entry.getValue());
                        }
                    }
                    superClass = sci.getSupername();
                }

                for (final Map.Entry<String, MethodInfo> entry : oldMethods.entrySet()) {
                    if (criteria.validMethod(entry.getValue()))
                        removedMethods.add(entry.getKey());
                }
                for (final Map.Entry<String, FieldInfo> entry : oldFields.entrySet()) {
                    if (criteria.validField(entry.getValue()))
                        removedFields.add(entry.getKey());
                }

                for (final Map.Entry<String, MethodInfo> entry : newMethods.entrySet()) {
                    if (criteria.validMethod(entry.getValue()))
                        addedMethods.add(entry.getKey());
                }
                for (final Map.Entry<String, FieldInfo> entry : newFields.entrySet()) {
                    if (criteria.validField(entry.getValue()))
                        addedFields.add(entry.getKey());
                }

                // We add all the old methods that match the criteria
                changedMethods.addAll(removedMethods);
                // We keep the intersection of these with all the new methods
                // to detect as changed a method that no longer match the
                // criteria (i.e. a method that was public and is now private)
                changedMethods.retainAll(newMethods.keySet());
                removedMethods.removeAll(changedMethods);
                removedMethods.removeAll(extNewMethods.keySet());
                addedMethods.removeAll(changedMethods);
                changedFields.addAll(removedFields);
                changedFields.retainAll(newFields.keySet());
                removedFields.removeAll(changedFields);
                removedFields.removeAll(extNewFields.keySet());
                addedFields.removeAll(changedFields);

                Iterator<String> j = changedMethods.iterator();
                while (j.hasNext()) {
                    final String desc = j.next();
                    final MethodInfo oldInfo = oldMethods.get(desc);
                    final MethodInfo newInfo = newMethods.get(desc);
                    if (!criteria.differs(oldInfo, newInfo))
                        j.remove();
                }
                j = changedFields.iterator();
                while (j.hasNext()) {
                    final String desc = j.next();
                    final FieldInfo oldInfo = oldFields.get(desc);
                    final FieldInfo newInfo = newFields.get(desc);
                    if (!criteria.differs(oldInfo, newInfo))
                        j.remove();
                }

                final boolean classchanged = criteria.differs(oci, nci);
                if (classchanged || !removedMethods.isEmpty()
                        || !removedFields.isEmpty() || !addedMethods.isEmpty()
                        || !addedFields.isEmpty() || !changedMethods.isEmpty()
                        || !changedFields.isEmpty()) {
                    handler.startClassChanged(s);

                    handler.startRemoved();
                    for (final String field : removedFields) {
                        handler.fieldRemoved(oldFields.get(field));
                    }
                    for (final String method : removedMethods) {
                        handler.methodRemoved(oldMethods.get(method));
                    }
                    handler.endRemoved();

                    handler.startAdded();
                    for (final String field : addedFields) {
                        handler.fieldAdded(newFields.get(field));
                    }
                    for (final String method : addedMethods) {
                        handler.methodAdded(newMethods.get(method));
                    }
                    handler.endAdded();

                    handler.startChanged();
                    if (classchanged) {
			            // Was only deprecated?
			            if (wasDeprecated(oci, nci)
				            && !criteria.differs(cloneDeprecated(oci), nci)) {
			                handler.classDeprecated(oci, nci);
			            } else {
			                handler.classChanged(oci, nci);
			            }
                    }

                    for (final String field : changedFields) {
                        final FieldInfo oldFieldInfo = oldFields.get(field);
                        final FieldInfo newFieldInfo = newFields.get(field);
                        // Was only deprecated?
                        if (wasDeprecated(oldFieldInfo, newFieldInfo)
                            && !criteria.differs(
                                cloneDeprecated(oldFieldInfo),
                                newFieldInfo)) {
                            handler.fieldDeprecated(oldFieldInfo, newFieldInfo);
                        } else if( !criteria.differsBinary(oldFieldInfo, newFieldInfo)) {
                            handler.fieldChangedCompat(oldFieldInfo, newFieldInfo);
                        } else {
                            handler.fieldChanged(oldFieldInfo, newFieldInfo);
                        }
                    }
                    for (final String method : changedMethods) {
                        final MethodInfo oldMethodInfo = oldMethods.get(method);
                        final MethodInfo newMethodInfo = newMethods.get(method);
                        // Was only deprecated?
                        if (wasDeprecated(oldMethodInfo, newMethodInfo)
                            && !criteria.differs(
                                cloneDeprecated(oldMethodInfo),
                                newMethodInfo)) {
                            handler.methodDeprecated(oldMethodInfo,
                                newMethodInfo);
                        } else if ( !criteria.differsBinary(oldMethodInfo, newMethodInfo) ) {
                            handler.methodChangedCompat(oldMethodInfo, newMethodInfo);
                        } else {
                            handler.methodChanged(oldMethodInfo, newMethodInfo);
                        }
                    }
                    handler.endChanged();
                    handler.endClassChanged();

                    removedMethods.clear();
                    removedFields.clear();
                    addedMethods.clear();
                    addedFields.clear();
                    changedMethods.clear();
                    changedFields.clear();
                }
            }
        }
        handler.endChanged();

        handler.endDiff();
    }

    /**
     * Determines if an {@link AbstractInfo} was deprecated. (Shortcut to avoid
     * creating cloned deprecated infos).
     */
    private static boolean wasDeprecated(final AbstractInfo oldInfo,
	    final AbstractInfo newInfo) {
	return !oldInfo.isDeprecated() && newInfo.isDeprecated();
    }

    /**
     * Determines if an {@link AbstractInfo} was deprecated. (Shortcut to avoid
     * creating cloned deprecated infos).
     */
    private static boolean throwClauseDiffers(final AbstractInfo oldInfo,
        final AbstractInfo newInfo) {
    return !oldInfo.isDeprecated() && newInfo.isDeprecated();
    }

    /**
     * Clones the class info, but changes access, setting deprecated flag.
     *
     * @param classInfo
     *            the original class info
     * @return the cloned and deprecated info.
     */
    private static ClassInfo cloneDeprecated(final ClassInfo classInfo) {
	return new ClassInfo(classInfo.getVersion(), classInfo.getAccess()
		| Opcodes.ACC_DEPRECATED, classInfo.getName(),
		classInfo.getSignature(), classInfo.getSupername(),
		classInfo.getInterfaces(), classInfo.getMethodMap(),
		classInfo.getFieldMap());
    }

    /**
     * Clones the method, but changes access, setting deprecated flag.
     *
     * @param methodInfo
     *            the original method info
     * @return the cloned and deprecated method info.
     */
    private static MethodInfo cloneDeprecated(final MethodInfo methodInfo) {
	return new MethodInfo(methodInfo.getAccess() | Opcodes.ACC_DEPRECATED,
		methodInfo.getName(), methodInfo.getDesc(),
		methodInfo.getSignature(), methodInfo.getExceptions());
    }

    /**
     * Clones the field info, but changes access, setting deprecated flag.
     *
     * @param fieldInfo
     *            the original field info
     * @return the cloned and deprecated field info.
     */
    private static FieldInfo cloneDeprecated(final FieldInfo fieldInfo) {
	return new FieldInfo(fieldInfo.getAccess() | Opcodes.ACC_DEPRECATED,
		fieldInfo.getName(), fieldInfo.getDesc(),
		fieldInfo.getSignature(), fieldInfo.getValue());
    }
}
