package jd.http;

import java.io.ByteArrayOutputStream;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;

public class ReusableByteArrayOutputStreamPool {
    public static class ReusableByteArrayOutputStream extends ByteArrayOutputStream {

        protected ReusableByteArrayOutputStream(final int size) {
            super(size);
        }

        public int bufferSize() {
            return this.buf.length;
        }

        public byte[] getInternalBuffer() {
            return this.buf;
        }
    }

    private static final LinkedList<SoftReference<ReusableByteArrayOutputStream>> pool = new LinkedList<SoftReference<ReusableByteArrayOutputStream>>();

    public static ReusableByteArrayOutputStream getReusableByteArrayOutputStream() {
        return ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(32);
    }

    public static ReusableByteArrayOutputStream getReusableByteArrayOutputStream(final int wishedMinimumSize) {
        final int wished = Math.max(32, wishedMinimumSize);
        synchronized (ReusableByteArrayOutputStreamPool.pool) {
            ReusableByteArrayOutputStream ret = null;
            if (!ReusableByteArrayOutputStreamPool.pool.isEmpty()) {
                final Iterator<SoftReference<ReusableByteArrayOutputStream>> it = ReusableByteArrayOutputStreamPool.pool.iterator();
                while (it.hasNext()) {
                    final SoftReference<ReusableByteArrayOutputStream> next = it.next();
                    ret = next.get();
                    if (ret != null) {
                        if (ret.bufferSize() >= wishedMinimumSize) {
                            it.remove();
                            return ret;
                        }
                        break;
                    }
                }
            }
            if (ret == null) {
                ret = new ReusableByteArrayOutputStream(wished);
            }
            return ret;
        }
    }

    public static void reuseReusableByteArrayOutputStream(final ReusableByteArrayOutputStream buf) {
        if (buf == null) { return; }
        synchronized (ReusableByteArrayOutputStreamPool.pool) {
            ReusableByteArrayOutputStreamPool.pool.add(new SoftReference<ReusableByteArrayOutputStream>(buf));
            buf.reset();
        }
    }
}
