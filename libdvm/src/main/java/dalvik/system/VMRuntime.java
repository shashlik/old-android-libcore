/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dalvik.system;

/**
 * Provides an interface to VM-global, Dalvik-specific features.
 * An application cannot create its own Runtime instance, and must obtain
 * one from the getRuntime method.
 *
 * @hide
 */
public final class VMRuntime {

    /**
     * Holds the VMRuntime singleton.
     */
    private static final VMRuntime THE_ONE = new VMRuntime();

    private int targetSdkVersion;

    /**
     * Prevents this class from being instantiated.
     */
    private VMRuntime() {
    }

    /**
     * Returns the object that represents the VM instance's Dalvik-specific
     * runtime environment.
     *
     * @return the runtime object
     */
    public static VMRuntime getRuntime() {
        return THE_ONE;
    }

    /**
     * Returns a copy of the VM's command-line property settings.
     * These are in the form "name=value" rather than "-Dname=value".
     */
    public native String[] properties();

    /**
     * Returns the VM's boot class path.
     */
    public native String bootClassPath();

    /**
     * Returns the VM's class path.
     */
    public native String classPath();

    /**
     * Returns the VM's version.
     */
    public native String vmVersion();

    /**
     * Returns the name of the shared library providing the VM implementation.
     */
    public native String vmLibrary();

    /**
     * Gets the current ideal heap utilization, represented as a number
     * between zero and one.  After a GC happens, the Dalvik heap may
     * be resized so that (size of live objects) / (size of heap) is
     * equal to this number.
     *
     * @return the current ideal heap utilization
     */
    public native float getTargetHeapUtilization();

    /**
     * Sets the current ideal heap utilization, represented as a number
     * between zero and one.  After a GC happens, the Dalvik heap may
     * be resized so that (size of live objects) / (size of heap) is
     * equal to this number.
     *
     * <p>This is only a hint to the garbage collector and may be ignored.
     *
     * @param newTarget the new suggested ideal heap utilization.
     *                  This value may be adjusted internally.
     * @return the previous ideal heap utilization
     * @throws IllegalArgumentException if newTarget is &lt;= 0.0 or &gt;= 1.0
     */
    public float setTargetHeapUtilization(float newTarget) {
        if (newTarget <= 0.0f || newTarget >= 1.0f) {
            throw new IllegalArgumentException(newTarget +
                    " out of range (0,1)");
        }
        /* Synchronize to make sure that only one thread gets
         * a given "old" value if both update at the same time.
         * Allows for reliable save-and-restore semantics.
         */
        synchronized (this) {
            float oldTarget = getTargetHeapUtilization();
            nativeSetTargetHeapUtilization(newTarget);
            return oldTarget;
        }
    }

    /**
     * Sets the target SDK version. Should only be called before the
     * app starts to run, because it may change the VM's behavior in
     * dangerous ways. Use 0 to mean "current" (since callers won't
     * necessarily know the actual current SDK version, and the
     * allocated version numbers start at 1), and 10000 to mean
     * CUR_DEVELOPMENT.
     */
    public synchronized void setTargetSdkVersion(int targetSdkVersion) {
        this.targetSdkVersion = targetSdkVersion;
        setTargetSdkVersionNative(this.targetSdkVersion);
    }

    /**
     * Gets the target SDK version. See {@link #setTargetSdkVersion} for
     * special values.
     */
    public synchronized int getTargetSdkVersion() {
        return targetSdkVersion;
    }

    private native void setTargetSdkVersionNative(int targetSdkVersion);

    /**
     * This method exists for binary compatibility.  It was part of a
     * heap sizing API which was removed in Android 3.0 (Honeycomb).
     */
    @Deprecated
    public long getMinimumHeapSize() {
        return 0;
    }

    /**
     * This method exists for binary compatibility.  It was part of a
     * heap sizing API which was removed in Android 3.0 (Honeycomb).
     */
    @Deprecated
    public long setMinimumHeapSize(long size) {
        return 0;
    }

    /**
     * This method exists for binary compatibility.  It used to
     * perform a garbage collection that cleared SoftReferences.
     */
    @Deprecated
    public void gcSoftReferences() {}

    /**
     * This method exists for binary compatibility.  It is equivalent
     * to {@link System#runFinalization}.
     */
    @Deprecated
    public void runFinalizationSync() {
        System.runFinalization();
    }

    /**
     * Implements setTargetHeapUtilization().
     *
     * @param newTarget the new suggested ideal heap utilization.
     *                  This value may be adjusted internally.
     */
    private native void nativeSetTargetHeapUtilization(float newTarget);

    /**
     * This method exists for binary compatibility.  It was part of
     * the external allocation API which was removed in Android 3.0 (Honeycomb).
     */
    @Deprecated
    public boolean trackExternalAllocation(long size) {
        return true;
    }

    /**
     * This method exists for binary compatibility.  It was part of
     * the external allocation API which was removed in Android 3.0 (Honeycomb).
     */
    @Deprecated
    public void trackExternalFree(long size) {}

    /**
     * This method exists for binary compatibility.  It was part of
     * the external allocation API which was removed in Android 3.0 (Honeycomb).
     */
    @Deprecated
    public long getExternalBytesAllocated() {
        return 0;
    }

    /**
     * Tells the VM to enable the JIT compiler. If the VM does not have a JIT
     * implementation, calling this method should have no effect.
     */
    public native void startJitCompilation();

    /**
     * Tells the VM to disable the JIT compiler. If the VM does not have a JIT
     * implementation, calling this method should have no effect.
     */
    public native void disableJitCompilation();

    /**
     * Returns an array allocated in an area of the Java heap where it will never be moved.
     * This is used to implement native allocations on the Java heap, such as DirectByteBuffers
     * and Bitmaps.
     */
    public native Object newNonMovableArray(Class<?> componentType, int length);

    /**
     * Returns an array of at least minLength, but potentially larger. The increased size comes from
     * avoiding any padding after the array. The amount of padding varies depending on the
     * componentType and the memory allocator implementation.
     */
    public Object newUnpaddedArray(Class<?> componentType, int minLength) {
        // Dalvik has 32bit pointers, the array header is 16bytes plus 4bytes for dlmalloc,
        // allocations are 8byte aligned so having 4bytes of array data avoids padding.
        if (!componentType.isPrimitive()) {
            int size = ((minLength & 1) == 0) ? minLength + 1 : minLength;
            return java.lang.reflect.Array.newInstance(componentType, size);
        } else if (componentType == char.class) {
            int bytes = 20 + (2 * minLength);
            int alignedUpBytes = (bytes + 7) & -8;
            int dataBytes = alignedUpBytes - 20;
            int size = dataBytes / 2;
            return new char[size];
        } else if (componentType == int.class) {
            int size = ((minLength & 1) == 0) ? minLength + 1 : minLength;
            return new int[size];
        } else if (componentType == byte.class) {
            int bytes = 20 + minLength;
            int alignedUpBytes = (bytes + 7) & -8;
            int dataBytes = alignedUpBytes - 20;
            int size = dataBytes;
            return new byte[size];
        } else if (componentType == boolean.class) {
            int bytes = 20 + minLength;
            int alignedUpBytes = (bytes + 7) & -8;
            int dataBytes = alignedUpBytes - 20;
            int size = dataBytes;
            return new boolean[size];
        } else if (componentType == short.class) {
            int bytes = 20 + (2 * minLength);
            int alignedUpBytes = (bytes + 7) & -8;
            int dataBytes = alignedUpBytes - 20;
            int size = dataBytes / 2;
            return new short[size];
        } else if (componentType == float.class) {
            int size = ((minLength & 1) == 0) ? minLength + 1 : minLength;
            return new float[size];
        } else if (componentType == long.class) {
            return new long[minLength];
        } else if (componentType == double.class) {
            return new double[minLength];
        } else {
            assert componentType == void.class;
            throw new IllegalArgumentException("Can't allocate an array of void");
        }
    }

    /**
     * Returns the address of array[0]. This differs from using JNI in that JNI might lie and
     * give you the address of a copy of the array when in forcecopy mode.
     */
    public native long addressOf(Object array);

    /**
     * Removes any growth limits, allowing the application to allocate
     * up to the maximum heap size.
     */
    public native void clearGrowthLimit();

    /**
     * Returns true if either a Java debugger or native debugger is active.
     */
    public native boolean isDebuggerActive();

    /**
     * Registers a native allocation so that the heap knows about it and performs GC as required.
     * If the number of native allocated bytes exceeds the native allocation watermark, the
     * function requests a concurrent GC. If the native bytes allocated exceeds a second higher
     * watermark, it is determined that the application is registering native allocations at an
     * unusually high rate and a GC is performed inside of the function to prevent memory usage
     * from excessively increasing.
     */
    public native void registerNativeAllocation(int bytes);

    /**
     * Registers a native free by reducing the number of native bytes accounted for.
     */
    public native void registerNativeFree(int bytes);

    /**
     * Let the heap know of the new process state. This can change allocation and garbage collection
     * behavior regarding trimming and compaction.
     */
    public native void updateProcessState(int state);

    /**
     * Fill in dex caches with classes, fields, and methods that are
     * already loaded. Typically used after Zygote preloading.
     */
    public native void preloadDexCaches();

    /**
     * Register application info
     */
    public static void registerAppInfo(String appDir, String processName, String pkgname) {
        // Nothing to do in dalvik.
    }
}
