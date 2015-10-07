package org.osjava.jardiff;

public abstract class AbstractMemberInfo extends AbstractInfo {

    /**
     * The internal name of this member's class
     */
    private final String className;

    public AbstractMemberInfo(final String className, final int access, final String name) {
        super(access, name);
        this.className = className;
    }

    /**
     * Get the internal name of member's class
     *
     * @return the name
     */
    public final String getClassName() {
        return className;
    }


}
